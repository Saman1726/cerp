# Java backend migration

This repository now defaults to Java for backend services:

- Core API: services/core-api-java (Spring Boot, PostgreSQL, Flyway)
- Partner System: services/partner-system-java (Spring Boot)

The previous Node.js backends remain available under a “legacy” profile for local development and manual CI runs, but they are no longer the default.

## Local development

Prerequisites:

- Java 21 (Temurin recommended)
- Docker
- Optional: Node 24 (only if running legacy stack)

Build Java services locally (optional; Docker builds on the fly):

```bash
(cd services/core-api-java && mvn -B -ntp -DskipTests package)
(cd services/partner-system-java && mvn -B -ntp -DskipTests package)
```

Bring up Java backend + Postgres with Docker Compose:

```bash
docker compose up -d postgres partner-system-java api-java
```

Service endpoints:

- Core API (Java)
  - Health: http://localhost:8080/actuator/health
  - OpenAPI: http://localhost:8080/api-docs
  - Swagger UI: http://localhost:8080/swagger
- Partner System (Java)
  - Health: http://localhost:3020/actuator/health

Notes:

- api-java uses DB_* env vars; defaults are set in docker-compose.yml. PARTNER_SYSTEM_URL defaults to http://partner-system-java:3020.
- No frontend changes are required at this stage. The frontend remains unchanged until we explicitly switch it to use the Java API.

### Running the legacy Node stack locally (optional)

The Node API and partner-system are gated behind the “legacy” profile:

```bash
docker compose --profile legacy up -d
```

## Kubernetes

Updated base manifests live under infra/k8s/base:

- api-java-deployment.yaml / api-java-service.yaml
- partner-system-java-deployment.yaml / partner-system-java-service.yaml
- ingress.yaml now routes api.cerp.example.com to api-java:8080

Important:

- api-java Deployment expects a Secret api-secrets with POSTGRES_USER and POSTGRES_PASSWORD keys. See infra/k8s/base/secret.example.yaml for guidance.
- Ingress routes:
  - api.cerp.example.com -> Service api-java:8080
  - app.cerp.example.com -> Service web:80 (unchanged)

Render and apply an overlay (once your cluster/registry access exist):

```bash
kubectl kustomize infra/k8s/overlays/dev
kubectl apply -k infra/k8s/overlays/dev
```

### Images and registry

By default, base manifests reference placeholder/local images:

- cerp/core-api-java:local
- cerp/partner-system-java:local

Use the GHCR publish workflow to build and push images, then patch overlays to point at GHCR images and tags.

Example overlay kustomization snippet:

```yaml
images:
  - name: cerp/core-api-java
    newName: ghcr.io/OWNER/core-api-java
    newTag: latest
  - name: cerp/partner-system-java
    newName: ghcr.io/OWNER/partner-system-java
    newTag: latest
```

Replace OWNER with your GitHub org or user.

## CI/CD

- Java CI (build/test):
  - .github/workflows/api-java-ci.yml
  - .github/workflows/partner-system-java-ci.yml
- Java Docker publish (GHCR):
  - .github/workflows/java-docker-publish.yml
  - Triggers on releases and manual runs; pushes ghcr.io/OWNER/core-api-java and ghcr.io/OWNER/partner-system-java with appropriate tags
- Legacy Node CI (manual only):
  - .github/workflows/api-ci.yml (legacy)
  - .github/workflows/partner-system-ci.yml (legacy)

## Migration status and next steps

- Default local/dev stack uses Java backends. Node backends are available via the legacy profile and manual CI runs.
- Frontend is intentionally unchanged for now and continues to work against the existing API contracts. When we are ready to switch the frontend to the Java API, we will update the frontend’s server-side calls and any typed clients to point at api-java.
- Overlays should patch images to GHCR in their respective kustomization overlays.
- After the frontend switch is complete and stable, the Node backends and their CI can be fully retired.
