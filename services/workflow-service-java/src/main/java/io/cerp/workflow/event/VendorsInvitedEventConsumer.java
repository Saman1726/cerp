package io.cerp.workflow.event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cerp.workflow.security.IdentityPermissionClient;
import io.cerp.workflow.service.NotificationServiceClient;
import io.cerp.workflow.service.VendorServiceClient;
import io.cerp.workflow.service.VendorServiceClient.VendorUserIdMapping;

/**
 * Kafka event consumer for vendor invitation events.
 *
 * <p>
 * When vendors are invited to a bidding saga, this consumer:
 * <ol>
 *   <li>Resolves vendor IDs to user IDs via the vendor service</li>
 *   <li>Sends a notification to each invited vendor</li>
 *   <li>Sends a confirmation notification to the project owner</li>
 * </ol>
 */
@Component
public class VendorsInvitedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(VendorsInvitedEventConsumer.class);

    /** Topic for bidding saga vendor invitation events */
    public static final String TOPIC_VENDORS_INVITED = "collaboration.bidding.vendors.invited.v1";

    private final VendorServiceClient vendorServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final IdentityPermissionClient identityPermissionClient;
    private final ObjectMapper objectMapper;

    public VendorsInvitedEventConsumer(
            VendorServiceClient vendorServiceClient,
            NotificationServiceClient notificationServiceClient,
            IdentityPermissionClient identityPermissionClient,
            ObjectMapper objectMapper) {
        this.vendorServiceClient = vendorServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.identityPermissionClient = identityPermissionClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Handle vendor invitation event via Kafka.
     *
     * @param eventJson the vendor invitation event JSON payload
     */
    @KafkaListener(topics = TOPIC_VENDORS_INVITED, groupId = "workflow-service-bidding-group")
    public void handleVendorsInvited(String eventJson) {
        try {
            VendorsInvitedEvent event = objectMapper.readValue(eventJson, VendorsInvitedEvent.class);
            processVendorsInvitedEvent(event);
        } catch (Exception parseEx) {
            log.error("Failed to parse vendors invited event: {}", eventJson, parseEx);
        }
    }

    private void processVendorsInvitedEvent(VendorsInvitedEvent event) {
        log.info("Processing vendor invitation event: tenderId={}, vendorCount={}, sagaId={}",
                event.tenderId(), event.vendorIds().size(), event.sagaId());

        // Resolve vendor IDs to user IDs
        List<UUID> vendorIdUUIDs = event.vendorIds().stream()
                .map(UUID::fromString)
                .toList();
        List<VendorUserIdMapping> vendorUserMappings = vendorServiceClient.resolveUserIds(vendorIdUUIDs, event.tenantId());

        if (vendorUserMappings.isEmpty()) {
            log.warn("No valid vendor-user mappings found for tender {}: no notifications will be sent",
                    event.tenderId());
            return;
        }

        log.info("Resolved {} vendor-user mappings for tender {}", vendorUserMappings.size(), event.tenderId());

        // Build notification data payload
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("tenderId", event.tenderId());
        notificationData.put("sagaId", event.sagaId());
        notificationData.put("projectId", event.projectId());
        notificationData.put("bidDeadline", event.bidDeadline());
        notificationData.put("invitedAt", Instant.now().toString());

        // Send notifications to each vendor
        final String shortTenderId = event.tenderId() != null
                ? (event.tenderId().length() > 8 ? event.tenderId().substring(0, 8) : event.tenderId())
                : "unknown";
        for (VendorUserIdMapping mapping : vendorUserMappings) {
            try {
                String title = "New Bidding Invitation";
                String message = String.format(
                        "You have been invited to submit a bid for Tender %s. View the invitation details in your dashboard.",
                        shortTenderId);

                Map<String, Object> vendorData = new HashMap<>(notificationData);
                vendorData.put("vendorId", mapping.vendorId());

                UUID vendorNotifTenantId = mapping.vendorTenantId() != null
                        ? mapping.vendorTenantId()
                        : UUID.fromString(event.tenantId());

                // Resolve actual recipient IDs via identity service membership (by tenantId).
                // vendor.userId can be stale; the membership table reflects who currently logs in.
                List<UUID> recipientIds = identityPermissionClient.resolveVendorUserIdsByTenantId(vendorNotifTenantId);
                if (recipientIds.isEmpty() && mapping.userId() != null) {
                    recipientIds = List.of(mapping.userId());
                }

                for (UUID recipientId : recipientIds) {
                    notificationServiceClient.sendNotification(
                            recipientId.toString(),
                            vendorNotifTenantId,
                            title,
                            message,
                            "vendor_invite_" + event.tenderId() + "_" + mapping.vendorId(),
                            vendorData);
                    log.info("Sent vendor invitation notification: recipientId={}, vendorId={}, tenderId={}, vendorTenantId={}",
                            recipientId, mapping.vendorId(), event.tenderId(), vendorNotifTenantId);
                }
            } catch (Exception e) {
                log.error("Failed to send notification to vendor vendorId={}, tenderId={}",
                        mapping.vendorId(), event.tenderId(), e);
            }
        }

        // Send confirmation notification to project owner (if available)
        if (event.bidderUserId() != null) {
            try {
                String title = "Vendors Invited";
                String message = String.format(
                        "%d vendor(s) have been invited to bid on your Tender %s.",
                        vendorUserMappings.size(),
                        shortTenderId);

                Map<String, Object> ownerData = new HashMap<>(notificationData);
                ownerData.put("vendorCount", vendorUserMappings.size());
                ownerData.put("invitedVendorIds", vendorUserMappings.stream()
                        .map(m -> m.vendorId().toString())
                        .toList());

                notificationServiceClient.sendNotification(
                        event.bidderUserId().toString(),
                        UUID.fromString(event.tenantId()),
                        title,
                        message,
                        "vendor_invite_confirm_" + event.tenderId(),
                        ownerData);

                log.info("Sent vendor invitation confirmation to project owner userId={}, tenderId={}",
                        event.bidderUserId(), event.tenderId());
            } catch (Exception e) {
                log.error("Failed to send invitation confirmation to project owner userId={}, tenderId={}",
                        event.bidderUserId(), event.tenderId(), e);
            }
        }

        log.info("Successfully processed vendor invitation notifications for tender {}", event.tenderId());
    }

    /**
     * Vendor invitation event payload published when vendors are invited to a bidding saga.
     */
    public record VendorsInvitedEvent(
            String tenderId,
            String sagaId,
            String tenantId,
            UUID bidderUserId,
            UUID projectId,
            List<String> vendorIds,
            Object bidDeadline,
            Map<String, Object> inviteConfig) {

        /**
         * Create from a raw map (deserialization helper).
         */
        @SuppressWarnings("unchecked")
        public static VendorsInvitedEvent fromMap(Map<String, Object> data) {
            List<String> vendorIds = new ArrayList<>();
            Object vIds = data.get("invitedVendorIds");
            if (vIds instanceof List<?> list) {
                for (Object id : list) {
                    vendorIds.add(id.toString());
                }
            }
            // Also check vendorIds as fallback
            if (vendorIds.isEmpty()) {
                Object vvIds = data.get("vendorIds");
                if (vvIds instanceof List<?> vlist) {
                    for (Object id : vlist) {
                        vendorIds.add(id.toString());
                    }
                }
            }

            return new VendorsInvitedEvent(
                    (String) data.get("tenderId"),
                    (String) data.get("sagaId"),
                    (String) data.get("tenantId"),
                    data.get("bidderUserId") != null ? UUID.fromString(data.get("bidderUserId").toString()) : null,
                    data.get("projectId") != null ? UUID.fromString(data.get("projectId").toString()) : null,
                    vendorIds,
                    data.get("bidDeadline"),
                    data.get("inviteConfig") instanceof Map<?, ?> map
                            ? new HashMap<>((Map<String, Object>) map)
                            : new HashMap<>());
        }
    }
}