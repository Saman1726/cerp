# Notification Service

A centralized notification service that creates jobs and notifications for users to act upon. Other services create notifications by publishing events to Kafka.

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

## Database

- **Database**: `notification_service` (dedicated database)
- **Schema**: `notification` (Flyway-managed)
- **Tables**:
  - `notification.notification_jobs` - Notification jobs (batches)
  - `notification.notifications` - Individual notifications per recipient
  - `notification.outbox_events` - Outbox for reliable event publishing

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
| `DB_HOST` | localhost | Database host |
| `DB_PORT` | 5432 | Database port |
| `DB_NAME` | notification_service | Database name |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka brokers |
| `IDENTITY_SERVICE_URL` | http://identity-service-java:8090 | Identity service URL |
| `NOTIFICATION_INBOUND_TOPIC` | notification-job-create-request | Kafka inbound topic |
| `NOTIFICATION_INBOUND_GROUP_ID` | notification-service | Kafka consumer group |
| `SERVER_PORT` | 8085 | Server port |

## Running

```bash
# Build
mvn clean package

# Run
java -jar target/notification-service-0.0.1-SNAPSHOT.jar

# Or with Docker
docker build -t notification-service .
docker run -p 8085:8085 notification-service
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
    <NotificationProvider pollingInterval={30000}>
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