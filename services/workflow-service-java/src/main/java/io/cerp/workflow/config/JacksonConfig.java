package io.cerp.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson configuration for proper Hibernate lazy-loading support.
 *
 * <p>
 * Registers {@link Hibernate6Module} which allows Jackson to safely initialize Hibernate proxies and lazy collections during JSON serialization even after the Hibernate
 * Session has closed. This prevents LazyInitializationException when serializing @OneToMany/@ManyToOne relationships that use FetchType.LAZY.
 * </p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper mapper = builder.build();

        // Register Hibernate6 module to handle lazy proxies gracefully
        mapper.registerModule(new Hibernate6Module());

        // Register Java Time module for LocalDateTime serialization
        mapper.registerModule(new JavaTimeModule());

        return mapper;
    }
}
