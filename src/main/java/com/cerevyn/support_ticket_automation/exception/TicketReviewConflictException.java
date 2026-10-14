package com.cerevyn.support_ticket_automation.exception;

public class TicketReviewConflictException extends RuntimeException {
    public TicketReviewConflictException(String message) {
        super(message);
    }
}
