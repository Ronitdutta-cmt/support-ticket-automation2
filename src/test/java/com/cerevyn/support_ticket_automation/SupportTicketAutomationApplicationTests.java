package com.cerevyn.support_ticket_automation;

import com.cerevyn.support_ticket_automation.controller.ApiExceptionHandler.ApiError;
import com.cerevyn.support_ticket_automation.model.Ticket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:ticket-api-tests;DB_CLOSE_DELAY=-1"
)
class SupportTicketAutomationApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void createsPersistsAndRetrievesProcessedTicketOverHttp() {
        Ticket request = ticket("URGENT: I was charged twice for my invoice.");

        ResponseEntity<Ticket> response = restTemplate.postForEntity("/api/tickets", request, Ticket.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Ticket created = response.getBody();
        assertNotNull(created);
        assertNotNull(created.getTicketId());
        assertEquals("Billing", created.getCategory());
        assertEquals("HIGH", created.getPriority());
        assertEquals("Billing Support", created.getAssignedTeam());
        assertEquals("URGENT: I was charged twice for my invoice.", created.getSummary());
        assertEquals("Thank you for contacting us about your billing concern. Our Billing Support team will review it and follow up shortly.", created.getDraftResponse());
        assertEquals("PENDING_REVIEW", created.getStatus());

        ResponseEntity<Ticket> fetched = restTemplate.getForEntity(
                "/api/tickets/{ticketId}", Ticket.class, created.getTicketId());
        assertEquals(HttpStatus.OK, fetched.getStatusCode());
        assertNotNull(fetched.getBody());
        assertEquals(created.getTicketId(), fetched.getBody().getTicketId());
        assertEquals(created.getSummary(), fetched.getBody().getSummary());
    }

    @Test
    void rejectsInvalidMessagesWithUsefulBadRequest() {
        ResponseEntity<ApiError> blank = restTemplate.postForEntity(
                "/api/tickets", ticket("  "), ApiError.class);
        assertEquals(HttpStatus.BAD_REQUEST, blank.getStatusCode());
        assertNotNull(blank.getBody());
        assertTrue(blank.getBody().message().contains("message must not be blank"));

        ResponseEntity<ApiError> missing = restTemplate.postForEntity(
                "/api/tickets", new Ticket(), ApiError.class);
        assertEquals(HttpStatus.BAD_REQUEST, missing.getStatusCode());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<ApiError> malformed = restTemplate.postForEntity("/api/tickets",
                new HttpEntity<>("{not-json", headers), ApiError.class);
        assertEquals(HttpStatus.BAD_REQUEST, malformed.getStatusCode());
        assertTrue(malformed.getBody().message().contains("invalid JSON"));
    }

    @Test
    void reportsDuplicateTicketIdsAsConflict() {
        Ticket first = ticket("I need help with a refund.");
        first.setTicketId("duplicate-test-id");
        assertEquals(HttpStatus.CREATED,
                restTemplate.postForEntity("/api/tickets", first, Ticket.class).getStatusCode());

        Ticket duplicate = ticket("Another refund request.");
        duplicate.setTicketId("duplicate-test-id");
        ResponseEntity<ApiError> response = restTemplate.postForEntity(
                "/api/tickets", duplicate, ApiError.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().message().contains("already exists"));
    }

    @Test
    void listsTicketsAndReturnsNotFoundForUnknownId() {
        Ticket request = ticket("Please explain my plan.");
        request.setTicketId("list-test-id");
        restTemplate.postForEntity("/api/tickets", request, Ticket.class);

        ResponseEntity<Ticket[]> list = restTemplate.getForEntity("/api/tickets", Ticket[].class);
        assertEquals(HttpStatus.OK, list.getStatusCode());
        assertNotNull(list.getBody());
        assertTrue(list.getBody().length >= 1);
        assertTrue(Arrays.stream(list.getBody())
                .anyMatch(ticket -> "list-test-id".equals(ticket.getTicketId())));

        ResponseEntity<ApiError> missing = restTemplate.getForEntity(
                "/api/tickets/{ticketId}", ApiError.class, "unknown-ticket-id");
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());
        assertTrue(missing.getBody().message().contains("was not found"));
    }

    @Test
    void supportsApprovalAndPreventsReviewingTwice() {
        Ticket created = createWithId("approve-test-id");

        ResponseEntity<Ticket> approved = restTemplate.postForEntity(
                "/api/tickets/{ticketId}/approve", null, Ticket.class, created.getTicketId());
        assertEquals(HttpStatus.OK, approved.getStatusCode());
        assertEquals("APPROVED", approved.getBody().getStatus());

        ResponseEntity<ApiError> secondDecision = restTemplate.postForEntity(
                "/api/tickets/{ticketId}/reject", null, ApiError.class, created.getTicketId());
        assertEquals(HttpStatus.CONFLICT, secondDecision.getStatusCode());
        assertTrue(secondDecision.getBody().message().contains("already been reviewed"));
    }

    @Test
    void supportsRejection() {
        Ticket created = createWithId("reject-test-id");

        ResponseEntity<Ticket> rejected = restTemplate.postForEntity(
                "/api/tickets/{ticketId}/reject", null, Ticket.class, created.getTicketId());

        assertEquals(HttpStatus.OK, rejected.getStatusCode());
        assertEquals("REJECTED", rejected.getBody().getStatus());
    }

    private Ticket createWithId(String ticketId) {
        Ticket request = ticket("Please help with my account.");
        request.setTicketId(ticketId);
        ResponseEntity<Ticket> response = restTemplate.postForEntity("/api/tickets", request, Ticket.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return response.getBody();
    }

    private Ticket ticket(String message) {
        Ticket ticket = new Ticket();
        ticket.setMessage(message);
        return ticket;
    }
}
