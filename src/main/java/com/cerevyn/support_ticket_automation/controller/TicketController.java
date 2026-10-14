package com.cerevyn.support_ticket_automation.controller;

import com.cerevyn.support_ticket_automation.model.Ticket;
import com.cerevyn.support_ticket_automation.service.TicketService;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ticket createTicket(@Valid @RequestBody Ticket ticket) {
        return ticketService.createTicket(ticket);
    }

    @GetMapping
    public List<Ticket> getTickets() {
        return ticketService.getTickets();
    }

    @GetMapping("/{ticketId}")
    public Ticket getTicket(@PathVariable String ticketId) {
        return ticketService.getTicket(ticketId);
    }

    @PostMapping("/{ticketId}/approve")
    public Ticket approveTicket(@PathVariable String ticketId) {
        return ticketService.approveTicket(ticketId);
    }

    @PostMapping("/{ticketId}/reject")
    public Ticket rejectTicket(@PathVariable String ticketId) {
        return ticketService.rejectTicket(ticketId);
    }
}
