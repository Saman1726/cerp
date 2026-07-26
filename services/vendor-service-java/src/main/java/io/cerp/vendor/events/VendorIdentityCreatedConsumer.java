package io.cerp.vendor.events;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cerp.vendor.service.VendorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Consumes vendor identity created events from identity-service and links the vendor record with the newly created tenantId and userId.
 *
 * Flow: 1. Listen to identity.vendor.v1 topic 2. Extract vendorId, tenantId, userId from event payload 3. Call vendorService.linkIdentity() to update vendor record
 */
@Component
@ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
public class VendorIdentityCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(VendorIdentityCreatedConsumer.class);

    private final VendorService vendorService;
    private final ObjectMapper objectMapper;

    @Autowired
    public VendorIdentityCreatedConsumer(VendorService vendorService, ObjectMapper objectMapper) {
        this.vendorService = vendorService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.topics.vendor-identity:identity.vendor.v1}", groupId = "${app.kafka.consumer.group-id:vendor-service}")
    public void handleVendorIdentityCreated(@Payload String payload) {
        try {
            // Simple JSON parsing - in production use a proper ObjectMapper
            Map<String, Object> data = parseJson(payload);
            String vendorIdStr = (String) data.get("vendorId");
            String tenantIdStr = (String) data.get("tenantId");
            String userIdStr = (String) data.get("userId");

            UUID vendorId = UUID.fromString(vendorIdStr);
            UUID tenantId = UUID.fromString(tenantIdStr);
            UUID userId = UUID.fromString(userIdStr);

            log.info("Received vendor identity created event for vendorId={}", vendorId);

            vendorService.linkIdentity(vendorId, tenantId, userId);
            log.info("Successfully linked identity for vendorId={}, tenantId={}, userId={}",
                vendorId, tenantId, userId);

        } catch (Exception e) {
            log.error("Failed to process vendor identity created event: {}", e.getMessage(), e);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            Map<String, Object> envelope = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            Object dataObj = envelope.get("data");
            if (dataObj instanceof Map) {
                return (Map<String, Object>) dataObj;
            }
            throw new IllegalArgumentException("Event payload missing 'data' field");
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse event payload: " + e.getMessage(), e);
        }
    }
}
