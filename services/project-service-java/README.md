# Project Service

Spring Boot 3 service managing construction projects and project portfolios within the CERP platform.

## Responsibilities (Phase 1)

- CRUD operations for construction projects
- Project code uniqueness per tenant/org
- Tenant-scoped data isolation (via `X-Tenant-Id` header)
- Reference to properties/organizations
- Budget and progress tracking
- Status lifecycle management (PLANNING -> ACTIVE -> COMPLETED)
- OAuth2/JWT authentication
- Kafka event publishing for project lifecycle changes
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
│             Project Service                           │
│                                                      │
│  REST Controller → Service Layer → JPA Repository    │
│       ↓                    ↓                  ↓      │
│   DTOs Entities        Business          PostgreSQL  │
│                      Validation                         │
│       ↓                                                  │
│    Kafka Events (created/updated/deleted)                │
└──────────────────────────────────────────────────────┘
```

## API Endpoints

| Method | Path                                       | Description                  | Auth |
| ------ | ------------------------------------------ | ---------------------------- | ---- |
| GET    | `/api/projects/v1/projects`                | List all projects for tenant | JWT  |
| GET    | `/api/projects/v1/projects/by-org/{orgId}` | List projects for tenant/org | JWT  |
| GET    | `/api/projects/v1/projects/{id}`           | Get project by ID            | JWT  |
| POST   | `/api/projects/v1/projects`                | Create new project           | JWT  |
| PATCH  | `/api/projects/v1/projects/{id}`           | Update project (partial)     | JWT  |
| DELETE | `/api/projects/v1/projects/{id}`           | Delete project               | JWT  |

### Headers

| Header             | Required | Description                          |
| ------------------ | -------- | ------------------------------------ |
| `Authorization`    | Yes      | Bearer JWT token                     |
| `X-Tenant-Id`      | Yes      | Tenant context (injected by gateway) |
| `X-User-Id`        | No       | User identity (default: "system")    |
| `X-Correlation-Id` | No       | Request tracing ID                   |
| `Content-Type`     | Yes      | application/json (POST/PATCH)        |

### Request/Response Examples

**Create Project:**

```json
POST /api/projects/v1/projects
{
  "code": "PRJ-2026-001",
  "name": "Riverside Commercial Complex",
  "orgId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "description": "Multi-story commercial building with retail and office spaces",
  "propertyId": "f1e2d3c4-b5a6-7890-dcba-f01234567890",
  "addressLine1": "123 Riverside Drive",
  "postalCode": "10115",
  "city": "Berlin",
  "countryCode": "DE",
  "projectType": "COMMERCIAL",
  "startDate": "2026-09-01",
  "endDate": "2027-12-31",
  "budget": 15000000.00,
  "currencyCode": "EUR",
  "clientName": "Riverside Developments Ltd.",
  "clientEmail": "projects@riverside-dev.example",
  "status": "PLANNING"
}
```

**Response:**

```json
{
  "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
  "tenantId": "tenant-123",
  "orgId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "code": "PRJ-2026-001",
  "name": "Riverside Commercial Complex",
  "description": "Multi-story commercial building with retail and office spaces",
  "status": "PLANNING",
  "projectType": "COMMERCIAL",
  "progress": 0.0,
  "budget": 15000000.0,
  "currencyCode": "EUR",
  "syncStatus": "LOCAL",
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
java -jar target/project-service-java-0.1.0-SNAPSHOT.jar
```

## Docker

```bash
docker build -t cerp/project-service .
docker run -p 8085:8085 \
  -e DB_HOST=postgres \
  -e KEYCLOAK_ISSUER_URI=http://keycloak:8081/realms/cerp \
  cerp/project-service
```

## Health Check

```bash
# Liveness
curl http://localhost:8085/actuator/health/liveness

# Readiness
curl http://localhost:8085/actuator/health/readiness

# Info
curl http://localhost:8085/actuator/info
```

## Observability

- Prometheus metrics at `/actuator/prometheus`
- OpenTelemetry tracing supported
- Correlation ID in logs: `traceId`, `spanId`, `correlationId`
- Mapped tenant ID in logs: `tenantId` (from `X-Tenant-Id` header)

## Database Migrations

Flyway manages schema changes under `src/main/resources/db/migration/`:

- `V1__create_projects_table.sql` - Initial schema with tenant isolation indexes

## Data Model

```
projects
├── id (UUID) - Primary key
├── tenant_id (VARCHAR) - Tenant scope for RLS
├── org_id (UUID) - Organization reference
├── external_id (VARCHAR) - External system reference
├── code (VARCHAR 64) - Project code (unique per tenant)
├── name (VARCHAR 255) - Project name
├── property_id (UUID) - Property reference (nullable)
├── address_* (VARCHAR) - Address fields
├── status (ENUM) - PLANNING, ACTIVE, ON_HOLD, COMPLETED, CANCELLED, SUSPENDED
├── project_type (ENUM) - NEW_CONSTRUCTION, RENOVATION, MAINTENANCE, INFRASTRUCTURE, COMMERCIAL, RESIDENTIAL, INDUSTRIAL
├── start_date, end_date, actual_start_date, actual_end_date - Date fields
├── progress (NUMERIC) - 0.00 to 100.00
├── budget (NUMERIC) - Total project budget
├── currency_code (VARCHAR) - ISO 4217
├── client_name, client_email, client_phone - Client info
├── sync_status (ENUM) - LOCAL, REMOTE, CONFLICT, PENDING_SYNC
├── created_at, updated_at - Audit timestamps
└── created_by, updated_by - Audit users
```

## Kafka Topics

| Topic             | Direction | Description             |
| ----------------- | --------- | ----------------------- |
| `project.created` | Publish   | New project created     |
| `project.updated` | Publish   | Project details changed |
| `project.deleted` | Publish   | Project was deleted     |

## Project Status Lifecycle

```
PLANNING → ACTIVE → COMPLETED
   │          │
   ├───────► ON_HOLD ────────► ACTIVE
   │
   ├───────► CANCELLED
   │
   └───────► SUSPENDED
```
