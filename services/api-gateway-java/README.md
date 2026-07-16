# API Gateway / BFF

Spring Cloud Gateway-based API Gateway and Backend-for-Frontend for CERP.

## Responsibilities (Phase 1)

- Single ingress for all clients
- OIDC authentication via Keycloak
- JWT validation
- Tenant scoping and header propagation
- Route mapping to backend services
- Basic rate limiting
- Correlation ID injection

## Route Mapping

| Path Prefix           | Backend Service             | Port |
| --------------------- | --------------------------- | ---- |
| `/api/identity/**`    | Identity Service            | 8083 |
| `/api/orgs/**`        | Organization Service        | 8084 |
| `/api/projects/**`    | Project Service             | 8085 |
| `/api/documents/**`   | Document Management Service | 8086 |
| `/api/reports/**`     | Projections/Reporting       | 8087 |
| `/api/audit/**`       | Audit/Event Catalog         | 8088 |
| `/api/procurement/**` | Procurement Service         | 9091 |
| `/api/vendors/**`     | Vendor Service              | 9092 |
| `/api/catalog/**`     | Catalog Service             | 9093 |

## Configuration

| Environment Variable            | Default                                         | Description              |
| ------------------------------- | ----------------------------------------------- | ------------------------ |
| `KEYCLOAK_ISSUER_URI`           | `http://localhost:8081/realms/cerp`             | Keycloak issuer URI      |
| `KEYCLOAK_CLIENT_ID`            | `cerp-gateway`                                  | OAuth2 client ID         |
| `KEYCLOAK_CLIENT_SECRET`        | _(empty)_                                       | OAuth2 client secret     |
| `CORS_ALLOWED_ORIGINS`          | `https://localhost:3001,https://localhost:3000` | Allowed CORS origins     |
| `CERP_IDENTITY_SERVICE_URL`     | `http://localhost:8083`                         | Identity Service URL     |
| `CERP_ORGANIZATION_SERVICE_URL` | `http://localhost:8084`                         | Organization Service URL |
| `CERP_PROJECT_SERVICE_URL`      | `http://localhost:8085`                         | Project Service URL      |
| `CERP_DMS_SERVICE_URL`          | `http://localhost:8086`                         | DMS Service URL          |
| `CERP_PROJECTIONS_SERVICE_URL`  | `http://localhost:8087`                         | Projections Service URL  |
| `CERP_AUDIT_SERVICE_URL`        | `http://localhost:8088`                         | Audit Service URL        |
| `CERP_PROCUREMENT_SERVICE_URL`  | `http://localhost:9091`                         | Procurement Service URL  |
| `CERP_VENDOR_SERVICE_URL`       | `http://localhost:9092`                         | Vendor Service URL       |
| `CERP_CATALOG_SERVICE_URL`      | `http://localhost:9093`                         | Catalog Service URL      |

## Build

```bash
mvn clean package -DskipTests
```

## Run

```bash
java -jar target/api-gateway-java-0.1.0-SNAPSHOT.jar
```

## Docker

```bash
docker build -t cerp/api-gateway .
docker run -p 8080:8080 \
  -e KEYCLOAK_ISSUER_URI=http://keycloak:8081/realms/cerp \
  cerp/api-gateway
```

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

## Observability

- Prometheus metrics at `/actuator/prometheus`
- OpenTelemetry auto-instrumentation supported
- Correlation ID in request/response headers: `X-Correlation-Id`
- Tenant ID propagated via `X-Tenant-Id` header
