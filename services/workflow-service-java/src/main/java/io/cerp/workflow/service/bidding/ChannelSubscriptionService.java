package io.cerp.workflow.service.bidding;

import io.cerp.workflow.domain.bidding.ChannelSubscription;
import io.cerp.workflow.repository.ChannelSubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Manages vendor subscriptions to bidding channels.
 * Subscribing to a channel (e.g. "open_tenders") opts the vendor in to receive
 * push notifications when new tenders are published. Unsubscribed vendors can
 * still browse the channel freely — no notification is sent to them.
 */
@Service
public class ChannelSubscriptionService {

    public static final String CHANNEL_OPEN_TENDERS = "open_tenders";

    private final ChannelSubscriptionRepository repository;

    public ChannelSubscriptionService(ChannelSubscriptionRepository repository) {
        this.repository = repository;
    }

    public boolean isSubscribed(UUID tenantId, UUID userId, String channelCode) {
        return repository.existsByTenantIdAndUserIdAndChannelCode(tenantId, userId, channelCode);
    }

    public ChannelSubscription subscribe(UUID tenantId, UUID userId, String channelCode) {
        return repository.findByTenantIdAndUserIdAndChannelCode(tenantId, userId, channelCode)
                .orElseGet(() -> {
                    ChannelSubscription sub = new ChannelSubscription(
                            UUID.randomUUID(), tenantId, userId, channelCode, LocalDateTime.now());
                    return repository.save(sub);
                });
    }

    public void unsubscribe(UUID tenantId, UUID userId, String channelCode) {
        repository.deleteByTenantIdAndUserIdAndChannelCode(tenantId, userId, channelCode);
    }

    public List<ChannelSubscription> getSubscribers(UUID tenantId, String channelCode) {
        return repository.findByTenantIdAndChannelCode(tenantId, channelCode);
    }
}
