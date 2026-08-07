package io.cerp.identity.service;

import io.cerp.identity.domain.Plan;
import io.cerp.identity.domain.PlanLimits;
import io.cerp.identity.domain.Subscription;
import io.cerp.identity.events.IdentityEventPublisher;
import io.cerp.identity.repo.PlanLimitsRepository;
import io.cerp.identity.repo.SubscriptionRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanLimitsRepository planLimitsRepository;
    private final @Nullable IdentityEventPublisher eventPublisher;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
            PlanLimitsRepository planLimitsRepository,
            @Autowired(required = false) IdentityEventPublisher eventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.planLimitsRepository = planLimitsRepository;
        this.eventPublisher = eventPublisher;
    }

    public Subscription create(UUID userId, Plan plan) {
        subscriptionRepository.findByUserId(userId).ifPresent(s -> {
            throw new IllegalArgumentException("Subscription already exists for user: " + userId);
        });

        Plan resolvedPlan = plan != null ? plan : Plan.FREEMIUM;
        PlanLimits limits = planLimitsRepository.findById(resolvedPlan)
            .orElseThrow(() -> new IllegalStateException("No plan_limits row for plan: " + resolvedPlan));

        Subscription saved = subscriptionRepository.save(new Subscription(userId, resolvedPlan));

        if (eventPublisher != null) {
            eventPublisher.publishSubscriptionCreated(saved, limits.getStartingBidGrant());
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> findByUserId(UUID userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Optional<PlanLimits> findLimits(Plan plan) {
        return planLimitsRepository.findById(plan);
    }
}
