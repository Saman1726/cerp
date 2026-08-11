package io.cerp.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cerp.storage")
public record StorageProperties(
    String endpoint,
    String publicEndpoint,
    String accessKey,
    String secretKey,
    String region,
    String bucket,
    int presignTtlMinutes
) {}
