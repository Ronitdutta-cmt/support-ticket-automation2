package com.cerevyn.support_ticket_automation.exception;

public class DuplicateTicketIdException extends RuntimeException {
    public DuplicateTicketIdException(String ticketId) {
        super("A ticket with ID '" + ticketId + "' already exists");
    }
}
