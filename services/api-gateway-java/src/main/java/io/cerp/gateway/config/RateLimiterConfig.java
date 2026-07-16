package io.cerp.gateway.config;

import java.net.InetSocketAddress;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import reactor.core.publisher.Mono;

/**
 * Rate limiting configuration for the API Gateway.
 *
 * <p>
 * Uses per-client rate limiting with IP/client-ID based keys. Default limits are conservative for MVP; tune based on pilot load.
 *
 * <p>
 * Per Section 21: "basic rate limiting"
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Key resolver that uses client IP address as the rate-limit key.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            if (remoteAddress != null && remoteAddress.getAddress() != null) {
                return Mono.just(remoteAddress.getAddress().getHostAddress());
            }
            return Mono.just("unknown");
        };
    }
}
