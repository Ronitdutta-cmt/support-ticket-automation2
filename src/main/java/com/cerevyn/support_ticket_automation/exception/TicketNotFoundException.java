package com.cerevyn.support_ticket_automation.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String ticketId) {
        super("Ticket '" + ticketId + "' was not found");
    }
}
