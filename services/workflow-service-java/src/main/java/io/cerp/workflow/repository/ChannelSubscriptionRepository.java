package io.cerp.workflow.repository;

import io.cerp.workflow.domain.bidding.ChannelSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChannelSubscriptionRepository extends JpaRepository<ChannelSubscription, UUID> {

    List<ChannelSubscription> findByTenantIdAndChannelCode(UUID tenantId, String channelCode);

    Optional<ChannelSubscription> findByTenantIdAndUserIdAndChannelCode(
            UUID tenantId, UUID userId, String channelCode);

    boolean existsByTenantIdAndUserIdAndChannelCode(UUID tenantId, UUID userId, String channelCode);

    @Transactional
    void deleteByTenantIdAndUserIdAndChannelCode(UUID tenantId, UUID userId, String channelCode);
}
