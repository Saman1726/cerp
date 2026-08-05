package io.cerp.workflow.event;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import io.cerp.workflow.config.KafkaTopicConfig;
import io.cerp.workflow.domain.bidding.ChannelSubscription;
import io.cerp.workflow.service.NotificationServiceClient;
import io.cerp.workflow.service.bidding.ChannelSubscriptionService;

/**
 * Kafka consumer for open-tender published events.
 *
 * <p>When a new open tender is published, sends a push notification only to vendors
 * who subscribed to the {@code open_tenders} channel. Unsubscribed vendors can still
 * browse the channel — they just receive no notification.
 */
@Service
public class OpenTenderPublishedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OpenTenderPublishedConsumer.class);

    private final ChannelSubscriptionService subscriptionService;
    private final NotificationServiceClient notificationServiceClient;

    public OpenTenderPublishedConsumer(ChannelSubscriptionService subscriptionService,
                                        NotificationServiceClient notificationServiceClient) {
        this.subscriptionService = subscriptionService;
        this.notificationServiceClient = notificationServiceClient;
    }

    @KafkaListener(
        topics = KafkaTopicConfig.TOPIC_OPEN_TENDER_PUBLISHED,
        groupId = "workflow-service-open-tender-subscription-group"
    )
    public void onOpenTenderPublished(Map<String, Object> event) {
        final Object tenantIdObj = event.get("tenantId");
        final Object tenderIdObj = event.get("tenderId");

        if (tenantIdObj == null || tenderIdObj == null) {
            log.warn("Received open-tender event with missing tenantId or tenderId, skipping");
            return;
        }

        final String tenderId = tenderIdObj.toString();
        final UUID tenantId;
        try {
            tenantId = UUID.fromString(tenantIdObj.toString());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tenantId in open-tender event: {}", tenantIdObj);
            return;
        }

        final List<ChannelSubscription> subscribers =
                subscriptionService.getSubscribers(tenantId, ChannelSubscriptionService.CHANNEL_OPEN_TENDERS);

        if (subscribers.isEmpty()) {
            log.debug("No subscribers for open_tenders channel in tenant={}, tenderId={}", tenantId, tenderId);
            return;
        }

        final String shortId = tenderId.length() > 8 ? tenderId.substring(0, 8) : tenderId;

        for (final ChannelSubscription sub : subscribers) {
            try {
                notificationServiceClient.sendNotification(
                        sub.getUserId().toString(),
                        tenantId,
                        "New Open Tender Available",
                        "Open tender " + shortId + " is accepting bids. Submit your offer in the channel.",
                        "open_tender_" + tenderId,
                        new java.util.HashMap<>(event));
            } catch (Exception e) {
                log.warn("Failed to notify subscriber userId={} for tenderId={}: {}",
                        sub.getUserId(), tenderId, e.getMessage());
            }
        }

        log.info("Notified {} subscriber(s) for open tender: tenderId={}", subscribers.size(), tenderId);
    }
}
