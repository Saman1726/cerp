package io.cerp.identity.api;

import io.cerp.identity.domain.Plan;
import io.cerp.identity.domain.PlanLimits;
import io.cerp.identity.domain.Subscription;
import io.cerp.identity.service.SubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    public static record CreateSubscriptionRequest(
            @NotNull UUID userId,
            Plan plan) {
    }

    public static record PlanLimitsDto(
            String plan,
            Integer maxConstructionSites,
            Integer maxBuildings,
            Integer maxOwnersPerProject,
            Integer maxManagersPerProject,
            Integer maxMembersPerProject,
            Long startingBidGrant) {

        static PlanLimitsDto from(PlanLimits l) {
            return new PlanLimitsDto(l.getPlan().name(), l.getMaxConstructionSites(), l.getMaxBuildings(),
                l.getMaxOwnersPerProject(), l.getMaxManagersPerProject(), l.getMaxMembersPerProject(),
                l.getStartingBidGrant());
        }
    }

    public static record SubscriptionResponse(
            UUID id,
            UUID userId,
            String plan,
            String status,
            PlanLimitsDto limits,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    private SubscriptionResponse toResponse(Subscription s, PlanLimits limits) {
        return new SubscriptionResponse(s.getId(), s.getUserId(), s.getPlan().name(), s.getStatus(),
            limits != null ? PlanLimitsDto.from(limits) : null, s.getCreatedAt(), s.getUpdatedAt());
    }

    @PostMapping("/subscriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse create(@Valid @RequestBody CreateSubscriptionRequest body) {
        Subscription saved = subscriptionService.create(body.userId(), body.plan());
        return toResponse(saved, subscriptionService.findLimits(saved.getPlan()).orElse(null));
    }

    @GetMapping("/api/v1/users/{userId}/subscription")
    public SubscriptionResponse getByUserId(@PathVariable UUID userId) {
        Subscription sub = subscriptionService.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("No subscription for user: " + userId));
        return toResponse(sub, subscriptionService.findLimits(sub.getPlan()).orElse(null));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String msg) {
            super(msg);
        }
    }
}
