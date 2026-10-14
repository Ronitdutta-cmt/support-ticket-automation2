package com.cerevyn.support_ticket_automation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @Column(nullable = false, updatable = false, length = 64)
    @Size(max = 64, message = "ticketId must be at most 64 characters")
    private String ticketId;

    @NotBlank(message = "message must not be blank")
    @Size(max = 4000, message = "message must be at most 4000 characters")
    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, length = 16)
    private String priority;

    @Column(nullable = false, length = 64)
    private String assignedTeam;

    @Column(nullable = false, length = 120)
    private String summary;

    @Column(nullable = false, length = 1000)
    private String draftResponse;

    @Column(nullable = false, length = 32)
    private String status;


    @Column(nullable = false)
    private boolean uncertain;

    public Ticket() {
    }

    public Ticket(String ticketId, String message) {
        this.ticketId = ticketId;
        this.message = message;
        this.status = "PENDING_REVIEW";
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAssignedTeam() {
        return assignedTeam;
    }

    public void setAssignedTeam(String assignedTeam) {
        this.assignedTeam = assignedTeam;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDraftResponse() {
        return draftResponse;
    }

    public void setDraftResponse(String draftResponse) {
        this.draftResponse = draftResponse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isUncertain() {
        return uncertain;
    }

    public void setUncertain(boolean uncertain) {
        this.uncertain = uncertain;
    }
}
