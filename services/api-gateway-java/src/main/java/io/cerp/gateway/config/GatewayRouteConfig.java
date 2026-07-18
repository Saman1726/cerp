package io.cerp.gateway.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway route configuration.
 *
 * <p>
 * Maps API path prefixes to backend services via Spring Cloud Gateway. All routes inherit tenant context and correlation ID headers automatically via global filters.
 *
 * Route mapping (Section 21, API Gateway/BFF):
 * <ul>
 * <li>/api/identity/* → Identity Service</li>
 * <li>/api/orgs/* → Organization Service</li>
 * <li>/api/projects/* → Project Service</li>
 * <li>/api/documents/* → DMS</li>
 * <li>/api/reports/* → Projections/Reporting</li>
 * <li>/api/audit/* → Audit/Event Catalog</li>
 * <li>/api/procurement/* → Procurement Service</li>
 * <li>/api/vendors/* → Vendor Service</li>
 * <li>/api/catalog/* → Catalog Service</li>
 * </ul>
 */
@Configuration
public class GatewayRouteConfig {

    @Value("${cerp.services.identity.url:http://localhost:8083}")
    private String identityServiceUrl;

    @Value("${cerp.services.organization.url:http://localhost:8084}")
    private String organizationServiceUrl;

    @Value("${cerp.services.project.url:http://localhost:8085}")
    private String projectServiceUrl;

    @Value("${cerp.services.dms.url:http://localhost:8086}")
    private String dmsServiceUrl;

    @Value("${cerp.services.projections.url:http://localhost:8087}")
    private String projectionsServiceUrl;

    @Value("${cerp.services.audit.url:http://localhost:8088}")
    private String auditServiceUrl;

    @Value("${cerp.services.procurement.url:http://localhost:9091}")
    private String procurementServiceUrl;

    @Value("${cerp.services.vendor.url:http://localhost:9092}")
    private String vendorServiceUrl;

    @Value("${cerp.services.catalog.url:http://localhost:9093}")
    private String catalogServiceUrl;

    @Value("${cerp.services.workflow.url:http://localhost:8089}")
    private String workflowServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // ── Identity Service ──
            .route("identity-service", r -> r
                .path("/api/identity/**")
                .filters(f -> f.stripPrefix(1))
                .uri(identityServiceUrl))

            // ── Organization Service ──
            .route("organization-service", r -> r
                .path("/api/orgs/**")
                .filters(f -> f.stripPrefix(1))
                .uri(organizationServiceUrl))

            // ── Project Service ──
            .route("project-service", r -> r
                .path("/api/projects/**")
                .filters(f -> f.stripPrefix(1))
                .uri(projectServiceUrl))

            // ── Document Management Service ──
            .route("dms-service", r -> r
                .path("/api/documents/**")
                .filters(f -> f.stripPrefix(1))
                .uri(dmsServiceUrl))

            // ── Projections/Reporting Service ──
            .route("projections-service", r -> r
                .path("/api/reports/**")
                .filters(f -> f.stripPrefix(1))
                .uri(projectionsServiceUrl))

            // ── Audit/Event Catalog Service ──
            .route("audit-service", r -> r
                .path("/api/audit/**")
                .filters(f -> f.stripPrefix(1))
                .uri(auditServiceUrl))

            // ── Procurement Service ──
            .route("procurement-service", r -> r
                .path("/api/procurement/**")
                .filters(f -> f.stripPrefix(1))
                .uri(procurementServiceUrl))

            // ── Vendor Service ──
            .route("vendor-service", r -> r
                .path("/api/vendors/**")
                .filters(f -> f.stripPrefix(1))
                .uri(vendorServiceUrl))

            // ── Catalog Service ──
            .route("catalog-service", r -> r
                .path("/api/catalog/**")
                .filters(f -> f.stripPrefix(1))
                .uri(catalogServiceUrl))

            // ── Workflow Service ──
            // Routes: /api/v1/{tenantId}/workflows/... and /api/workflow-templates/...
            // stripPrefix(1) removes "/api", keeping "/v1/{tenantId}/workflows/..."
            // rewritePath rewrites /v1/{segment} back to /api/v1/{segment}
            .route("workflow-service", r -> r
                .path("/api/v1/**", "/api/workflows/**", "/api/workflow-templates/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .rewritePath("/v1/(?<segment>.*)", "/api/v1/${segment}"))
                .uri(workflowServiceUrl))

            .build();
    }
}
