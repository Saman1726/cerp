# Notification Service

## Purpose

Properties & Construction workflows constantly produce cross-service "someone needs to act on this" moments: a purchase order needs review, a bid needs a decision, a work order needs acknowledgement. Without a shared mechanism, every service that needs to notify a user (procurement, workflow, project, vendor, ...) would have to implement its own recipient resolution, its own read/unread tracking, and its own "my notifications" API — duplicating the same problem N times with N slightly different behaviors.

notification-service-java exists to centralize that problem instead of letting each service roll its own:

- **Kafka-mediated decoupling**: a service that needs to notify someone does not call this service's REST API or know anything about how delivery works. It publishes a `NotificationJobCreateRequest` event to Kafka and moves on; this service consumes it asynchronously, so the publisher is never blocked on (or coupled to) notification delivery.
- **Job -> per-recipient fan-out**: a single request such as "notify the procurement managers" is modeled as one `NotificationJob`, which fans out into one `Notification` row per resolved recipient. Job-level state ("is this fully handled?") and recipient-level state ("did *this* user act on it?") are tracked separately and queried independently.
- **"Fire and forget" for producers**: any service can trigger a user-facing, actionable notification (REVIEW, APPROVE, REJECT, ACKNOWLEDGE, EXECUTE, INPUT) without embedding email templates, inbox UI, read/unread tracking, or role-to-user lookups in its own codebase. That logic lives here, once.

## Architecture

```
┌──────────────┐     Kafka      ┌─────────────────────┐     Kafka      ┌───────────┐
│  Procurement │ ─────────────> │  Notification Service│ ─────────────> │  Frontend │
│    Service   │  create request│  (this service)     │  events       │  (UI)     │
└──────────────┘                └─────────────────────┘               └───────────┘
                                     │
                                     ▼
                              ┌──────────────┐
                              │   PostgreSQL  │
                              │ notification  │
                              │   schema      │
                              └──────────────┘
```

The service implements an **event-driven fan-out** pattern rather than a synchronous request/response API for notification creation:

1. **Request** - a source service (procurement, workflow, project, ...) publishes a `NotificationJobCreateRequest` to the `notification-job-create-request` Kafka topic. The request identifies recipients by *email or role* (e.g. `PROCUREMENT_MANAGER`), never by internal user ID, since the source service typically doesn't know internal user IDs either.
2. **Resolve recipients via identity-service** - `NotificationJobConsumer` calls **identity-service-java** (`GET /api/users/by-email`, `GET /api/users/by-roles`) to translate those emails/roles into concrete user IDs for the tenant. This is a hard runtime dependency: notification-service has no notion of users, roles, or tenant membership of its own, and duplicating that lookup here would create a second, divergent source of truth.
3. **Fan out to individual notifications** - one `NotificationJob` (the batch/aggregate) is persisted, and one `Notification` row is created per resolved recipient, each with its own PENDING -> READ -> ACTION_TAKEN lifecycle.
4. **Publish completion event** - a `NotificationJobCreated` event is published back to Kafka so interested consumers know the job (and its notifications) now exist.
5. **User interaction** - the frontend polls `GET /api/notifications/me`; acknowledging or acting on a notification (`/ack`, `/action`) updates that recipient's row and publishes `notification-opened` / `notification-action-taken` events, so the *originating* service can react to the outcome (e.g. procurement-service learning its purchase order was approved) without polling this service.

The REST API additionally supports a manual two-step flow (`POST /jobs` to create a DRAFT, then `POST /jobs/{jobId}/publish` to resolve recipients and fan out) for callers that want to review a job before it goes out, rather than firing immediately via Kafka.

## Database

- **Database**: the shared `cerp` PostgreSQL database — the same Postgres instance used by identity-service-java, project-service-java, and workflow-service-java. There is no dedicated/standalone database for this service.
- **Schema**: `notification` (Flyway-managed) — isolation is schema-per-service within the shared database, not database-per-service.
- **Tables**:
  - `notification.notification_jobs` - Notification jobs (batches)
  - `notification.notifications` - Individual notifications per recipient
  - `notification.outbox_events` - Outbox table for reliable event publishing (present in the schema; current event publishing goes directly through `KafkaTemplate` at request time rather than being routed through this table)

## Kafka Topics

| Topic | Direction | Description |
|-------|-----------|-------------|
| `notification-job-create-request` | Inbound | Other services publish here to create notifications |
| `notification-job-created` | Outbound | Published when a job is created |
| `notification-job-published` | Outbound | Published when a job is published |
| `notification-opened` | Outbound | Published when a user opens a notification |
| `notification-action-taken` | Outbound | Published when a user takes action |

## How to Create Notifications from Other Services

### 1. Publish to Kafka

```java
// Import the event record
import io.cerp.notification.event.NotificationJobCreateRequest;

// Send to Kafka
kafkaTemplate.send("notification-job-create-request", requestId, 
    new NotificationJobCreateRequest(
        requestId,                              // Unique request ID
        "purchase-order-review",                // Job code
        "Review new purchase order #123",       // Title
        "Please review the purchase order...",  // Description
        "REVIEW",                               // Action type: REVIEW, APPROVE, REJECT, ACKNOWLEDGE, EXECUTE, INPUT
        List.of("user@example.com"),            // Recipient emails (optional)
        List.of("PROCUREMENT_MANAGER"),         // Recipient roles (optional)
        UUID.fromString("tenant-id"),           // Tenant ID
        "po-123",                               // Source entity ID (string)
        "purchase-order",                       // Source entity type
        Map.of("orderId", "123", "amount", "1500.00"),  // Payload
        "HIGH",                                 // Priority: LOW, MEDIUM, HIGH, URGENT
        LocalDateTime.now().plusHours(24),      // Due date
        60,                                     // Reminder interval (minutes)
        "procurement-service"                   // Source service name
    )
);
```

### 2. Event Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `requestId` | String | Yes | Unique request ID for deduplication |
| `code` | String | No | Job code (auto-generated if not provided) |
| `title` | String | Yes | Notification title |
| `description` | String | Yes | Notification description |
| `actionType` | String | No | Action type (defaults to ACKNOWLEDGE) |
| `recipientEmails` | List\<String\> | No | Recipient emails (at least one of emails/roles required) |
| `recipientRoles` | List\<String\> | No | Recipient roles |
| `tenantId` | UUID | Yes | Tenant ID |
| `sourceEntityId` | String | No | Source entity ID for linking back |
| `sourceEntityType` | String | No | Source entity type |
| `payload` | Map | No | Additional JSON payload |
| `priority` | String | No | Priority level (defaults to MEDIUM) |
| `dueAt` | LocalDateTime | No | Expiration date |
| `reminderIntervalMinutes` | Integer | No | Reminder interval |
| `sourceService` | String | No | Name of the source service |

### 3. Example: Procurement Service Integration

```java
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IdentityServiceClient identityServiceClient;
    
    public void approvePurchaseOrder(UUID orderId, UUID userId) {
        // ... approval logic ...
        
        // Create notification for procurement manager to review
        List<UUID> managerIds = identityServiceClient.resolveUsersByRoles(
            List.of("PROCUREMENT_MANAGER"), tenantId);
        
        List<String> managerEmails = managerIds.stream()
            .map(id -> userIdentityService.getEmail(id))
            .collect(Collectors.toList());
        
        kafkaTemplate.send("notification-job-create-request", 
            UUID.randomUUID().toString(),
            new NotificationJobCreateRequest(
                UUID.randomUUID().toString(),
                "po-review-" + orderId.toString().substring(0, 8),
                "Review new purchase order #" + orderId,
                "A new purchase order has been submitted and requires your review.",
                "REVIEW",
                managerEmails,
                null,  // or List.of("PROCUREMENT_MANAGER")
                tenantId,
                orderId.toString(),
                "purchase-order",
                Map.of("orderId", orderId.toString(), "amount", "1500.00"),
                "HIGH",
                LocalDateTime.now().plusHours(24),
                60,
                "procurement-service"
            )
        );
    }
}
```

## REST API

### Notification Jobs

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/notifications/jobs` | Create a notification job (DRAFT) |
| GET | `/api/notifications/jobs` | List jobs with pagination |
| GET | `/api/notifications/jobs/{jobId}` | Get job details |
| POST | `/api/notifications/jobs/{jobId}/publish` | Publish a job |
| POST | `/api/notifications/jobs/{jobId}/cancel` | Cancel a job |
| GET | `/api/notifications/jobs/{jobId}/notifications` | Get job notifications |

### User Notifications

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/notifications/me` | Get my notifications |
| GET | `/api/notifications/me/unread-count` | Get unread count |
| POST | `/api/notifications/me/{notificationId}/ack` | Acknowledge notification |
| POST | `/api/notifications/me/{notificationId}/action` | Take action |
| GET | `/api/notifications/me/{notificationId}` | Get specific notification |

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | localhost | Database host (`postgres` in docker-compose) |
| `DB_PORT` | 5432 | Database port |
| `DB_NAME` | cerp | Database name — the shared `cerp` database, not a dedicated one (see Database section) |
| `DB_USERNAME` | postgres | Database username |
| `DB_PASSWORD` | postgres | Database password |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `IDENTITY_SERVICE_URL` | http://identity-service-java:8090 | Identity service URL, used to resolve recipients by email/role |
| `NOTIFICATION_INBOUND_TOPIC` | notification-job-create-request | Kafka inbound topic |
| `NOTIFICATION_INBOUND_GROUP_ID` | notification-service | Kafka consumer group |
| `LOG_LEVEL` | INFO | Log level for the `io.cerp.notification` package |
| `SERVER_PORT` | 8091 | Server port. docker-compose sets this explicitly to `8091`; the application's own built-in fallback is `8085` if the variable is unset entirely (e.g. running the jar standalone) |

## Running

The service listens on port `8091` in the docker-compose stack (see [Configuration](#configuration) for the standalone fallback).

```bash
# Build
mvn clean package

# Run standalone, matching the rest of the stack
SERVER_PORT=8091 java -jar target/notification-service-0.0.1-SNAPSHOT.jar

# Or with Docker
docker build -t notification-service .
docker run -p 8091:8091 -e SERVER_PORT=8091 notification-service

# Or as part of the full stack (wires up Postgres, Kafka, identity-service-java)
docker compose up --build notification-service-java
```

## Frontend Integration

The frontend uses the NotificationProvider to:
1. Poll for new notifications
2. Display unread count badge
3. Show notification dropdown
4. Allow users to acknowledge/take action

```tsx
import { NotificationProvider } from '@cerp/core-web/notifications';

// Wrap your app or page
export default function Layout({ children }) {
  return (
    <NotificationProvider tenantId={tenantId} userId={userId} refreshInterval={30000}>
      {children}
    </NotificationProvider>
  );
}
```

## Event Flow

1. **Service publishes** `NotificationJobCreateRequest` to Kafka
2. **Notification service consumes** the event
3. **Resolves recipients** via IdentityService (by email or role)
4. **Creates NotificationJob** with status DRAFT or PUBLISHED
5. **Creates individual Notification** records for each recipient
6. **Publishes** `NotificationJobCreated` event to Kafka
7. **User sees** notifications in the frontend
8. **User acknowledges/takes action** via REST API
9. **Notification service publishes** `NotificationActionTaken` event
10. **Other services can listen** for action events to trigger downstream processes