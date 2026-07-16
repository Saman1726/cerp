package io.cerp.project.config;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Servlet filter that extracts tenant context from headers.
 */
@Component
public class TenantContextFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private static final ThreadLocal<String> tenantIdContext = new ThreadLocal<>();
    private static final ThreadLocal<String> userIdContext = new ThreadLocal<>();
    private static final ThreadLocal<String> correlationIdContext = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String tenantId = httpRequest.getHeader(TENANT_ID_HEADER);
        String userId = httpRequest.getHeader(USER_ID_HEADER);
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);

        if (correlationId != null) {
            org.slf4j.MDC.put("correlationId", correlationId);
        }

        try {
            tenantIdContext.set(tenantId);
            userIdContext.set(userId != null ? userId : "anonymous");
            correlationIdContext.set(correlationId);

            log.debug("Processing request for tenant: {}, user: {}", tenantId, userId);
            chain.doFilter(request, response);
        } finally {
            tenantIdContext.remove();
            userIdContext.remove();
            correlationIdContext.remove();
            org.slf4j.MDC.clear();
        }
    }

    public static String getTenantId() {
        return tenantIdContext.get();
    }

    public static String getUserId() {
        return userIdContext.get();
    }

    public static String getCorrelationId() {
        return correlationIdContext.get();
    }
}
