package com.cerevyn.support_ticket_automation.service;

import com.cerevyn.support_ticket_automation.exception.DuplicateTicketIdException;
import com.cerevyn.support_ticket_automation.model.Ticket;
import com.cerevyn.support_ticket_automation.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketServiceTests {

    private TicketRepository ticketRepository;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketRepository = mock(TicketRepository.class);
        ticketService = new TicketService(ticketRepository);
        when(ticketRepository.existsById(any())).thenReturn(false);
        when(ticketRepository.saveAndFlush(any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void classifiesAllCategoriesAndAssignsTeams() {
        assertCategory("I need a refund for this payment.", "Billing", "Billing Support");
        assertCategory("I forgot my password and cannot sign in.", "Access", "Access Support");
        assertCategory("The upload keeps crashing.", "Technical", "Technical Support");
        assertCategory("Please tell me your support hours.", "General", "General Support");
    }

    @Test
    void assignsEveryPriorityLevelIncludingUnavailableSystems() {
        assertEquals("HIGH", process("URGENT: The service is unavailable.").getPriority());
        assertEquals("HIGH", process("The system is unavailable.").getPriority());
        assertEquals("MEDIUM", process("I cannot sign in.").getPriority());
        assertEquals("MEDIUM", process("The upload failed with an error.").getPriority());
        assertEquals("LOW", process("What are your support hours?").getPriority());
    }

    @Test
    void appliesBillingThenAccessThenTechnicalCategoryPrecedence() {
        assertEquals("Billing", process("The app crashes and I cannot access billing invoices.").getCategory());
        assertEquals("Access", process("I cannot access the app after a crash.").getCategory());
        assertEquals("Technical", process("The app has a bug.").getCategory());
    }

    @Test
    void matchesKeywordsAsWholeWordsRatherThanSubstrings() {
        Ticket result = process("The refunding portal shows a terror message.");

        assertEquals("General", result.getCategory());
        assertEquals("LOW", result.getPriority());
    }

    @Test
    void summarizesAtSentenceEndingsAndNormalizesWhitespace() {
        assertEquals("First sentence.", process("First sentence. Second sentence!").getSummary());
        assertEquals("Where is my invoice?", process("Where is my invoice? Please help.").getSummary());
        assertEquals("The upload failed!", process("The upload failed! It still fails.").getSummary());
        assertEquals("I need help.", process("  I   need   help.  More details here.").getSummary());
    }

    @Test
    void truncatesLongSummariesAtWordBoundariesWithinTheLimit() {
        String summary = process("Please refund " + "account ".repeat(30)).getSummary();

        assertTrue(summary.length() <= 120);
        assertTrue(summary.endsWith("account..."), summary);
    }

    @Test
    void producesTheExpectedDraftForEveryCategory() {
        assertEquals("Thank you for contacting us about your billing concern. Our Billing Support team will review it and follow up shortly.",
                process("refund").getDraftResponse());
        assertEquals("Thank you for letting us know about your account access issue. Our Access Support team will review it and help you regain access.",
                process("password").getDraftResponse());
        assertEquals("Thank you for reporting this technical issue. Our Technical Support team will investigate and follow up shortly.",
                process("upload").getDraftResponse());
        assertEquals("Thank you for contacting us. Our Support team will review your request and follow up shortly.",
                process("hello").getDraftResponse());
    }

    @Test
    void rejectsNullTicketsAndMissingOrBlankMessages() {
        assertThrows(IllegalArgumentException.class, () -> ticketService.processTicket(null));
        assertThrows(IllegalArgumentException.class, () -> ticketService.processTicket(new Ticket()));
        assertThrows(IllegalArgumentException.class, () -> process("  \t "));
    }

    @Test
    void assignsAnIdAndPersistsNewTickets() {
        Ticket created = ticketService.createTicket(ticket("hello"));

        assertNotNull(created.getTicketId());
        assertTrue(created.getTicketId().length() <= 64);
        assertEquals("PENDING_REVIEW", created.getStatus());
    }

    @Test
    void rejectsDuplicateTicketIds() {
        Ticket ticket = ticket("hello");
        ticket.setTicketId("already-used");
        when(ticketRepository.existsById("already-used")).thenReturn(true);

        assertThrows(DuplicateTicketIdException.class, () -> ticketService.createTicket(ticket));
    }

    private void assertCategory(String message, String category, String team) {
        Ticket ticket = process(message);
        assertEquals(category, ticket.getCategory());
        assertEquals(team, ticket.getAssignedTeam());
        assertEquals("PENDING_REVIEW", ticket.getStatus());
    }

    private Ticket process(String message) {
        return ticketService.processTicket(ticket(message));
    }

    private Ticket ticket(String message) {
        Ticket ticket = new Ticket();
        ticket.setMessage(message);
        return ticket;
    }

	
@Test
void flagsUnmatchedMessageAsUncertain() {
    Ticket result = process("I need some help with my request");

    assertEquals("General", result.getCategory());
    assertEquals(true, result.isUncertain());
}

@Test
void doesNotFlagClearBillingMessageAsUncertain() {
    Ticket result = process("I was charged twice for my payment");

    assertEquals("Billing", result.getCategory());
    assertEquals(false, result.isUncertain());
}

}
