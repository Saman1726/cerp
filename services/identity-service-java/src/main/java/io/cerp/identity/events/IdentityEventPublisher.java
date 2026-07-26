package io.cerp.identity.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cerp.identity.domain.Membership;
import io.cerp.identity.domain.Tenant;
import io.cerp.identity.domain.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.events", name = "enabled", havingValue = "true")
public class IdentityEventPublisher {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TENANT_TOPIC = "identity.tenant.v1";
    private static final String USER_TOPIC = "identity.user.v1";
    private static final String MEMBERSHIP_TOPIC = "identity.membership.v1";
    private static final String VENDOR_TOPIC = "identity.vendor.v1";

    @Autowired
    public IdentityEventPublisher(ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishTenantCreated(Tenant tenant) {
        Map<String, Object> envelope = baseEnvelope("identity.tenant.created", "v1");
        Map<String, Object> data = new HashMap<>();
        data.put("id", tenant.getId().toString());
        data.put("name", tenant.getName());
        envelope.put("data", data);
        send(TENANT_TOPIC, tenant.getId().toString(), envelope);
    }

    public void publishUserCreated(UserAccount user) {
        Map<String, Object> envelope = baseEnvelope("identity.user.created", "v1");
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId().toString());
        data.put("email", user.getEmail());
        data.put("name", user.getName());
        data.put("status", user.getStatus());
        envelope.put("data", data);
        send(USER_TOPIC, user.getId().toString(), envelope);
    }

    public void publishRoleAssigned(Membership membership) {
        Map<String, Object> envelope = baseEnvelope("identity.membership.role_assigned", "v1");
        Map<String, Object> data = new HashMap<>();
        data.put("id", membership.getId().toString());
        data.put("tenantId", membership.getTenantId().toString());
        data.put("userId", membership.getUserId().toString());
        data.put("roles", membership.getRoles() != null ? membership.getRoles() : List.of());
        data.put("scopes", membership.getScopes() != null ? membership.getScopes() : Map.of());
        envelope.put("data", data);
        send(MEMBERSHIP_TOPIC, membership.getUserId().toString(), envelope);
    }

    /**
     * Publish event when vendor identity is created, so vendor-service can link tenantId/userId.
     */
    public void publishVendorIdentityCreated(UUID vendorId, UUID tenantId, UUID userId) {
        Map<String, Object> envelope = baseEnvelope("identity.vendor.identity_created", "v1");
        Map<String, Object> data = new HashMap<>();
        data.put("vendorId", vendorId.toString());
        data.put("tenantId", tenantId.toString());
        data.put("userId", userId.toString());
        envelope.put("data", data);
        send(VENDOR_TOPIC, vendorId.toString(), envelope);
    }

    private Map<String, Object> baseEnvelope(String type, String version) {
        Map<String, Object> env = new HashMap<>();
        env.put("type", type);
        env.put("version", version);
        env.put("occurredAt", OffsetDateTime.now().toString());
        return env;
    }

    private void send(String topic, String key, Map<String, Object> envelope) {
        try {
            String payload = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(topic, key, payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event for topic " + topic, e);
        }
    }
}
