package com.cerevyn.support_ticket_automation.repository;

import com.cerevyn.support_ticket_automation.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, String> {
}
