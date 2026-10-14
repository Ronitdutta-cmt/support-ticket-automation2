# Support Ticket Automation

A Java 17 and Spring Boot REST API that classifies incoming support messages, drafts a response for human review, stores tickets in H2, and supports approval or rejection.

## Assignment and Submission

- **Problem statement:** PS04 — Azure Support Ticket Automation
- **Candidate name:** Ronit Dutta
- **Roll number:** 2310991195
- **Branch:** `master`
- **Contributions:** Implemented ticket classification, priority assignment, team routing, summaries, draft responses, persistence, input validation, duplicate detection, human-review approval/rejection, uncertainty detection, automated tests, Docker packaging, CI, and Azure deployment workflow documentation.

## Requirements

- Java 17 or newer
- Maven 3.9+ (or the Maven Wrapper)
- Docker, if building a container image

The app uses Spring Boot 3.5.6, Spring Web, Spring Data JPA, Jakarta Validation, and H2. No external AI service is used.

## Run locally

```powershell
./mvnw spring-boot:run
```

On Windows Command Prompt or PowerShell, use `./mvnw.cmd spring-boot:run`. Alternatively, use an installed Maven with `mvn spring-boot:run`.

By default, H2 stores data in `./data/support-tickets`. Override the JDBC URL with `DB_URL`; the default credentials are `sa` with an empty password and can be overridden with `DB_USERNAME` and `DB_PASSWORD`. The H2 console is disabled by default. Set `H2_CONSOLE_ENABLED=true` only for local development.

## Ticket classification

Rules are deterministic and case-insensitive. Matching uses whole words and listed phrases, so a keyword embedded inside a longer word does not match.

| Category | Example matching terms | Assigned team |
| --- | --- | --- |
| Billing | refund, payment, charged, invoice, billing | Billing Support |
| Access | login, password, account, access, sign in | Access Support |
| Technical | crash, error, bug, upload, technical | Technical Support |
| General | No category terms matched | General Support |

When a message matches multiple categories, precedence is **Billing > Access > Technical > General**. Priority is **HIGH** for urgent/critical language, outages, or unavailable/down systems; **MEDIUM** for failures, errors, inability, or something not working; otherwise **LOW**. A high-priority match takes precedence over a medium-priority match.

The summary is the first sentence (period, question mark, or exclamation point), with whitespace normalized and long summaries shortened to at most 120 characters. The generated category-specific response is a draft. Every new ticket begins with `PENDING_REVIEW`, requiring a person to approve or reject it. The `uncertain` boolean indicates whether classification needs additional attention: it is `true` when no category keywords match or when keywords from multiple categories match, and `false` when exactly one category matches. Category assignment still follows Billing > Access > Technical > General precedence.

## Dataset and classification evaluation

The fictional labelled dataset is [`src/test/resources/classification-tickets.csv`](src/test/resources/classification-tickets.csv). It contains 30 support messages labelled `Billing`, `Technical`, `Access`, or `General`, with stable IDs and a fixed split column: 20 `train` rows and 10 `test` rows. The test rows are reserved before measurement and are not used to tune the classifier. The evaluation reads only rows marked `test`.

[`ClassificationEvaluationTest.java`](src/test/java/com/cerevyn/support_ticket_automation/service/ClassificationEvaluationTest.java) evaluates those messages by calling the existing `TicketService.processTicket` method; it does not duplicate the classification rules. Accuracy is the number of correct category predictions divided by the number of test cases. The classification method and priority policy are described above: category matching is deterministic, case-insensitive, and whole-word based, with Billing > Access > Technical > General precedence; priority is HIGH for urgent/critical language, outages, or unavailable/down systems, MEDIUM for failure, error, or inability language, and LOW otherwise, with HIGH taking precedence.

Measured result on the fixed 10-ticket test split: **5 correct / 10 total = 50.00% accuracy**.

Incorrect predictions (ID: expected → predicted):

- `TKT-021`: Billing → General
- `TKT-023`: Billing → General
- `TKT-024`: Technical → General
- `TKT-026`: Technical → General
- `TKT-028`: Access → General

Confusion matrix (rows are expected labels; columns are predicted labels):

| Expected / Predicted | Billing | Technical | Access | General |
| --- | ---: | ---: | ---: | ---: |
| Billing | 1 | 0 | 0 | 2 |
| Technical | 0 | 1 | 0 | 2 |
| Access | 0 | 0 | 1 | 1 |
| General | 0 | 0 | 0 | 2 |

These results describe this small fixed test set and are not a claim about performance on other ticket data.

## API

Each ticket response includes an `uncertain` boolean. A message with no recognized category keywords is assigned `General` and has `uncertain: true`, while a clearly categorized billing message has `uncertain: false`.
| Method | Path | Behavior |
| --- | --- | --- |
| `POST` | `/api/tickets` | Validate, classify, persist, and return a ticket with HTTP 201 |
| `GET` | `/api/tickets` | Return all tickets |
| `GET` | `/api/tickets/{ticketId}` | Retrieve one ticket |
| `POST` | `/api/tickets/{ticketId}/approve` | Set a pending ticket to `APPROVED` |
| `POST` | `/api/tickets/{ticketId}/reject` | Set a pending ticket to `REJECTED` |

Only `PENDING_REVIEW` tickets can be approved or rejected. A second decision returns `409 Conflict`; an unknown ID returns `404 Not Found`. Missing, blank, or overlong messages and overlong ticket IDs return `400 Bad Request` with a JSON error containing the status, message, timestamp, and request path.

Example:

```powershell
$body = '{"message":"URGENT: I was charged twice for my invoice"}'
$ticket = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/tickets -ContentType "application/json" -Body $body
$ticket
Invoke-RestMethod -Uri "http://localhost:8080/api/tickets/$($ticket.ticketId)"
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/tickets/$($ticket.ticketId)/approve"
```

## Tests

Run the full suite:

```powershell
./mvnw test
```

The tests cover classification, priority, precedence, summaries, drafts, invalid inputs, persistence and retrieval through the API, duplicate IDs, and review transitions. API tests use an isolated in-memory H2 database.

## Docker

Build and run the image:

```powershell
docker build -t support-ticket-automation:local .
docker run --rm -p 8080:8080 -v support-ticket-data:/app/data support-ticket-automation:local
```

The named volume keeps the H2 file between container replacements. For any container hosting platform, mount durable storage at `/app/data` and keep the app at one replica while using H2.

## Continuous integration

GitHub Actions runs the Maven test suite on pushes and pull requests, then builds the Docker image. See [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

## Azure deployment

The manual workflow builds the Docker image, pushes it to Azure Container Registry (ACR), and deploys it to Azure App Service using OIDC. See the [Azure App Service deployment guide](docs/AZURE_DEPLOYMENT.md) for the required GitHub variables, secrets, and Azure federated credential and registry configuration.
