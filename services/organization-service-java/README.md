# Organization Service

Spring Boot 3 service managing business organization profiles within the CERP platform.

## Responsibilities (Phase 1)

- CRUD operations for business organizations
- Tenant-scoped data isolation (via `X-Tenant-Id` header)
- Organization name uniqueness per tenant
- OAuth2/JWT authentication
- Kafka event publishing for organizational changes
- PostgreSQL persistence with Flyway migrations

## Architecture

```
┌──────────────────────────────────────────────────────┐
│                    API Gateway                        │
│              (X-Tenant-Id propagation)                │
└──────────────────────┬───────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────┐
│            Organization Service                       │
│                                                      │
│  REST Controller → Service Layer → JPA Repository    │
│       ↓                    ↓                  ↓      │
│   DTOs Entities        Business          PostgreSQL  │
│                      Validation                         │
│       ↓                                                  │
│    Kafka Events (Phase 2)                                │
└──────────────────────────────────────────────────────┘
```

## API Endpoints

| Method | Path                              | Description                   | Auth |
| ------ | --------------------------------- | ----------------------------- | ---- |
| GET    | `/api/orgs/v1/organizations`      | List all organizations        | JWT  |
| GET    | `/api/orgs/v1/organizations/{id}` | Get organization by ID        | JWT  |
| POST   | `/api/orgs/v1/organizations`      | Create new organization       | JWT  |
| PATCH  | `/api/orgs/v1/organizations/{id}` | Update organization (partial) | JWT  |
| DELETE | `/api/orgs/v1/organizations/{id}` | Delete organization           | JWT  |

### Headers

| Header             | Required | Description                          |
| ------------------ | -------- | ------------------------------------ |
| `Authorization`    | Yes      | Bearer JWT token                     |
| `X-Tenant-Id`      | Yes      | Tenant context (injected by gateway) |
| `X-User-Id`        | No       | User identity (default: "system")    |
| `X-Correlation-Id` | No       | Request tracing ID                   |
| `Content-Type`     | Yes      | application/json (POST/PATCH)        |

### Request/Response Examples

**Create Organization:**

```json
POST /api/orgs/v1/organizations
{
  "name": "Acme Construction GmbH",
  "legalForm": "GmbH",
  "taxId": "DE123456789",
  "commercialRegister": "HRB 12345",
  "addressLine1": "Hauptstrasse 1",
  "postalCode": "10115",
  "city": "Berlin",
  "countryCode": "DE",
  "contactEmail": "info@acme-construction.example",
  "currencyCode": "EUR"
}
```

**Response:**

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "tenantId": "tenant-123",
  "name": "Acme Construction GmbH",
  "legalForm": "GmbH",
  "taxId": "DE123456789",
  "status": "ACTIVE",
  "syncStatus": "LOCAL",
  "currencyCode": "EUR",
  "createdAt": "2026-07-16T10:30:00",
  "updatedAt": "2026-07-16T10:30:00"
}
```

## Configuration

| Environment Variable          | Default                             | Description                      |
| ----------------------------- | ----------------------------------- | -------------------------------- |
| `DB_HOST`                     | `localhost`                         | PostgreSQL host                  |
| `DB_PORT`                     | `5432`                              | PostgreSQL port                  |
| `DB_NAME`                     | `cerp`                              | Database name                    |
| `DB_USERNAME`                 | `postgres`                          | Database username                |
| `DB_PASSWORD`                 | `postgres`                          | Database password                |
| `KEYCLOAK_ISSUER_URI`         | `http://localhost:8081/realms/cerp` | Keycloak issuer URI              |
| `KAFKA_BOOTSTRAP_SERVERS`     | `localhost:9092`                    | Kafka brokers                    |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317`             | OpenTelemetry collector endpoint |

## Build

```bash
mvn clean package -DskipTests
```

## Run

```bash
java -jar target/organization-service-java-0.1.0-SNAPSHOT.jar
```

## Docker

```bash
docker build -t cerp/organization-service .
docker run -p 8084:8084 \
  -e DB_HOST=postgres \
  -e KEYCLOAK_ISSUER_URI=http://keycloak:8081/realms/cerp \
  cerp/organization-service
```

## Health Check

```bash
# Liveness
curl http://localhost:8084/actuator/health/liveness

# Readiness
curl http://localhost:8084/actuator/health/readiness

# Info
curl http://localhost:8084/actuator/info
```

## Observability

- Prometheus metrics at `/actuator/prometheus`
- OpenTelemetry tracing supported
- Correlation ID in logs: `traceId`, `spanId`, `correlationId`
- Mapped tenant ID in logs: `tenantId` (from `X-Tenant-Id` header)

## Database Migrations

Flyway manages schema changes under `src/main/resources/db/migration/`:

- `V1__create_organizations_table.sql` - Initial schema with tenant isolation indexes

## Data Model

```
organizations
├── id (UUID) - Primary key
├── tenant_id (VARCHAR) - Tenant scope for RLS
├── external_id (VARCHAR) - External system reference
├── name (VARCHAR 255) - Organization name
├── legal_form, tax_id, commercial_register - Legal info
├── address_* (VARCHAR) - Address fields
├── status (ENUM) - ACTIVE, INACTIVE, SUSPENDED, UNDER_REVIEW
├── sync_status (ENUM) - LOCAL, REMOTE, CONFLICT, PENDING_SYNC
├── currency_code (VARCHAR) - ISO 4217
├── created_at, updated_at - Audit timestamps
└── created_by, updated_by - Audit users
```
