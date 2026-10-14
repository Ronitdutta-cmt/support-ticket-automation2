package com.cerevyn.support_ticket_automation.service;

import com.cerevyn.support_ticket_automation.model.Ticket;
import com.cerevyn.support_ticket_automation.repository.TicketRepository;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ClassificationEvaluationTest {

    private static final String DATASET_PATH = "/classification-tickets.csv";
    private static final List<String> CATEGORIES = List.of("Billing", "Technical", "Access", "General");

    @Test
    void evaluatesOnlyTheFixedTestSplitAndReportsMetrics() throws IOException {
        List<DatasetRow> rows = loadDataset();
        assertEquals(30, rows.size(), "Dataset must contain 30 labelled tickets");
        assertEquals(20, rows.stream().filter(row -> row.split().equals("train")).count());
        List<DatasetRow> testRows = rows.stream().filter(row -> row.split().equals("test")).toList();
        assertEquals(10, testRows.size(), "The fixed held-out test split must contain 10 tickets");

        TicketService ticketService = new TicketService(mock(TicketRepository.class));
        Map<String, Map<String, Integer>> confusion = emptyConfusionMatrix();
        List<Misclassification> incorrect = new ArrayList<>();
        int correct = 0;

        for (DatasetRow row : testRows) {
            Ticket ticket = new Ticket();
            ticket.setMessage(row.message());
            String predicted = ticketService.processTicket(ticket).getCategory();
            confusion.get(row.expectedCategory()).merge(predicted, 1, Integer::sum);
            if (row.expectedCategory().equals(predicted)) {
                correct++;
            } else {
                incorrect.add(new Misclassification(row.id(), row.expectedCategory(), predicted));
            }
        }

        double accuracy = (double) correct / testRows.size();
        System.out.printf("Classification evaluation: %d/%d correct; accuracy %.2f%%%n",
                correct, testRows.size(), accuracy * 100.0);
        System.out.println("Incorrect predictions (id | expected | predicted):");
        if (incorrect.isEmpty()) {
            System.out.println("  none");
        } else {
            incorrect.forEach(item -> System.out.printf("  %s | %s | %s%n",
                    item.id(), item.expected(), item.predicted()));
        }
        System.out.println("Confusion matrix (rows=expected, columns=predicted):");
        System.out.println("Expected\\Predicted | " + String.join(" | ", CATEGORIES));
        for (String expected : CATEGORIES) {
            System.out.printf("%-18s | %s%n", expected,
                    CATEGORIES.stream().map(category -> String.valueOf(confusion.get(expected).get(category)))
                            .reduce((left, right) -> left + " | " + right).orElse(""));
        }

        assertTrue(testRows.size() >= 10);
        assertTrue(accuracy >= 0.0 && accuracy <= 1.0);
        assertFalse(incorrect.stream().anyMatch(item -> item.id().isBlank()));
    }

    private List<DatasetRow> loadDataset() throws IOException {
        InputStream stream = getClass().getResourceAsStream(DATASET_PATH);
        assertTrue(stream != null, "Dataset resource not found: " + DATASET_PATH);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            assertEquals("id,message,expected_category,split", reader.readLine());
            List<DatasetRow> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", 4);
                assertEquals(4, columns.length, "Invalid dataset row: " + line);
                assertTrue(CATEGORIES.contains(columns[2]), "Unknown category in row: " + line);
                assertTrue(List.of("train", "test").contains(columns[3]), "Unknown split in row: " + line);
                rows.add(new DatasetRow(columns[0], columns[1], columns[2], columns[3]));
            }
            return rows;
        }
    }

    private Map<String, Map<String, Integer>> emptyConfusionMatrix() {
        Map<String, Map<String, Integer>> matrix = new LinkedHashMap<>();
        for (String expected : CATEGORIES) {
            Map<String, Integer> predictions = new LinkedHashMap<>();
            for (String predicted : CATEGORIES) {
                predictions.put(predicted, 0);
            }
            matrix.put(expected, predictions);
        }
        return matrix;
    }

    private record DatasetRow(String id, String message, String expectedCategory, String split) { }

    private record Misclassification(String id, String expected, String predicted) { }
}
