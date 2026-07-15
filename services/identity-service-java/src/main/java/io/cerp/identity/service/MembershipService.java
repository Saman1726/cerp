package io.cerp.identity.service;

import io.cerp.identity.domain.Membership;
import io.cerp.identity.events.IdentityEventPublisher;
import io.cerp.identity.repo.MembershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final @Nullable IdentityEventPublisher eventPublisher;

    @Autowired
    public MembershipService(
            MembershipRepository membershipRepository,
            @Autowired(required = false) IdentityEventPublisher eventPublisher) {
        this.membershipRepository = membershipRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Create or update a membership for (tenantId, userId) with given roles and scopes. Emits RoleAssigned event after persistence (idempotent on update).
     */
    public Membership assign(UUID tenantId, UUID userId, List<String> roles, Map<String, Object> scopes) {
        Membership m = membershipRepository
            .findByTenantIdAndUserId(tenantId, userId)
            .map(existing -> existing.setRoles(roles).setScopes(scopes))
            .orElseGet(() -> new Membership(null, tenantId, userId, roles, scopes));

        Membership saved = membershipRepository.save(m);
        if (eventPublisher != null) {
            eventPublisher.publishRoleAssigned(saved);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Membership> findByTenantAndUser(UUID tenantId, UUID userId) {
        return membershipRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    @Transactional(readOnly = true)
    public List<Membership> listByTenant(UUID tenantId) {
        return membershipRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Membership> listByUser(UUID userId) {
        return membershipRepository.findByUserId(userId);
    }
}
