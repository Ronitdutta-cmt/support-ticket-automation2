package com.cerevyn.support_ticket_automation.service;

import com.cerevyn.support_ticket_automation.model.Ticket;
import com.cerevyn.support_ticket_automation.exception.DuplicateTicketIdException;
import com.cerevyn.support_ticket_automation.exception.TicketNotFoundException;
import com.cerevyn.support_ticket_automation.exception.TicketReviewConflictException;
import com.cerevyn.support_ticket_automation.repository.TicketRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    private static final Map<String, String> CATEGORY_TEAMS = Map.of(
            "Billing", "Billing Support",
            "Access", "Access Support",
            "Technical", "Technical Support",
            "General", "General Support"
    );
    private static final String[] BILLING_KEYWORDS = {"refund", "payment", "charged", "invoice", "billing"};
    private static final String[] ACCESS_KEYWORDS = {"login", "password", "account", "access", "sign in"};
    private static final String[] TECHNICAL_KEYWORDS = {"crash", "error", "bug", "upload", "technical"};
    private static final String[] HIGH_PRIORITY_KEYWORDS = {
            "urgent", "critical", "completely down", "system unavailable", "system is unavailable",
            "service unavailable", "service is unavailable", "system down", "service down", "outage"
    };
    private static final String[] MEDIUM_PRIORITY_KEYWORDS = {
            "cannot", "can't", "unable", "failed", "error", "not working"
    };
    private static final int MAX_SUMMARY_LENGTH = 120;

    public Ticket createTicket(Ticket ticket) {
        processTicket(ticket);
        String ticketId = ticket.getTicketId();
        ticketId = ticketId == null || ticketId.isBlank() ? UUID.randomUUID().toString() : ticketId.trim();
        ticket.setTicketId(ticketId);
        if (ticketRepository.existsById(ticketId)) {
            throw new DuplicateTicketIdException(ticketId);
        }
        try {
            return ticketRepository.saveAndFlush(ticket);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateTicketIdException(ticketId);
        }
    }

    public Ticket getTicket(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    public List<Ticket> getTickets() {
        return ticketRepository.findAll();
    }

    public Ticket approveTicket(String ticketId) {
        return updateReviewStatus(ticketId, "APPROVED");
    }

    public Ticket rejectTicket(String ticketId) {
        return updateReviewStatus(ticketId, "REJECTED");
    }

    private Ticket updateReviewStatus(String ticketId, String status) {
        Ticket ticket = getTicket(ticketId);
        if (!"PENDING_REVIEW".equals(ticket.getStatus())) {
            throw new TicketReviewConflictException(
                    "Ticket '" + ticketId + "' has already been reviewed with status " + ticket.getStatus());
        }
        ticket.setStatus(status);
        return ticketRepository.save(ticket);
    }

    public Ticket processTicket(Ticket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket must not be null");
        }
        if (ticket.getMessage() == null || ticket.getMessage().isBlank()) {
            throw new IllegalArgumentException("Ticket message must not be blank");
        }
        String originalMessage = ticket.getMessage().trim().replaceAll("\\s+", " ");
        String message = originalMessage.toLowerCase(Locale.ROOT);


        boolean billingMatch = matchesAny(message, BILLING_KEYWORDS);
        boolean accessMatch = matchesAny(message, ACCESS_KEYWORDS);
        boolean technicalMatch = matchesAny(message, TECHNICAL_KEYWORDS);

        int matchedCategories = (billingMatch ? 1 : 0)
                + (accessMatch ? 1 : 0)
                + (technicalMatch ? 1 : 0);

        boolean uncertain = matchedCategories != 1;
        ticket.setUncertain(uncertain);

// Preserve the existing category precedence.
        String category = billingMatch ? "Billing"
                : accessMatch ? "Access"
                : technicalMatch ? "Technical"
                : "General";

        ticket.setCategory(category);
        ticket.setAssignedTeam(CATEGORY_TEAMS.get(category));
        // HIGH: urgent/critical/outage or unavailable/down service; MEDIUM: failure/error; LOW: otherwise.
        ticket.setPriority(matchesAny(message, HIGH_PRIORITY_KEYWORDS) ? "HIGH"
                : matchesAny(message, MEDIUM_PRIORITY_KEYWORDS) ? "MEDIUM" : "LOW");
        ticket.setSummary(summarize(originalMessage));
        ticket.setDraftResponse(draftResponse(category));
        ticket.setStatus("PENDING_REVIEW");
        return ticket;
    }

    private boolean matchesAny(String message, String[] keywords) {
        for (String keyword : keywords) {
            Pattern keywordPattern = Pattern.compile("(?<![\\p{L}\\p{N}_])"
                    + Pattern.quote(keyword) + "(?![\\p{L}\\p{N}_])");
            if (keywordPattern.matcher(message).find()) {
                return true;
            }
        }
        return false;
    }

    private String summarize(String message) {
        int sentenceEnd = -1;
        for (int i = 0; i < message.length(); i++) {
            char character = message.charAt(i);
            if (character == '.' || character == '?' || character == '!') {
                sentenceEnd = i + 1;
                break;
            }
        }
        String summary = sentenceEnd >= 0 ? message.substring(0, sentenceEnd).trim() : message;
        if (summary.length() <= MAX_SUMMARY_LENGTH) {
            return summary;
        }

        int contentLimit = MAX_SUMMARY_LENGTH - 3;
        int wordBoundary = summary.lastIndexOf(' ', contentLimit);
        int truncateAt = wordBoundary > 0 ? wordBoundary : contentLimit;
        return summary.substring(0, truncateAt).trim() + "...";
    }

    private String draftResponse(String category) {
        return switch (category) {
            case "Billing" -> "Thank you for contacting us about your billing concern. Our Billing Support team will review it and follow up shortly.";
            case "Access" -> "Thank you for letting us know about your account access issue. Our Access Support team will review it and help you regain access.";
            case "Technical" -> "Thank you for reporting this technical issue. Our Technical Support team will investigate and follow up shortly.";
            default -> "Thank you for contacting us. Our Support team will review your request and follow up shortly.";
        };
    }
}
