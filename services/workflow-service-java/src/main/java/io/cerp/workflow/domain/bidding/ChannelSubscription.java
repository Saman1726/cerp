package io.cerp.workflow.domain.bidding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a vendor's subscription to a bidding channel (e.g. "open_tenders").
 * Subscribed vendors receive push notifications when new tenders are published to the channel.
 * Unsubscribed vendors can still browse the channel but receive no notifications.
 */
@Entity
@Table(
    name = "channel_subscriptions",
    schema = "bidding",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "user_id", "channel_code"})
)
public class ChannelSubscription {

    @Id
    @Column(columnDefinition = "uuid DEFAULT gen_random_uuid()")
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "channel_code", nullable = false)
    private String channelCode;

    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt;

    public ChannelSubscription() {}

    public ChannelSubscription(UUID id, UUID tenantId, UUID userId, String channelCode, LocalDateTime subscribedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.channelCode = channelCode;
        this.subscribedAt = subscribedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
    public LocalDateTime getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(LocalDateTime subscribedAt) { this.subscribedAt = subscribedAt; }
}
