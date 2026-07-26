package io.cerp.identity.service;

import io.cerp.identity.domain.Tenant;
import io.cerp.identity.events.IdentityEventPublisher;
import io.cerp.identity.repo.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final @Nullable IdentityEventPublisher eventPublisher;

    @Autowired
    public TenantService(TenantRepository tenantRepository, @Autowired(required = false) IdentityEventPublisher eventPublisher) {
        this.tenantRepository = tenantRepository;
        this.eventPublisher = eventPublisher;
    }

    public Tenant create(String name, String region) {
        Tenant t = new Tenant(UUID.randomUUID(), name);
        t.setRegion(region);
        Tenant saved = tenantRepository.save(t);
        if (eventPublisher != null) {
            eventPublisher.publishTenantCreated(saved);
        }
        return saved;
    }

    public void delete(UUID id) {
        Optional<Tenant> tenant = tenantRepository.findById(id);
        if (tenant.isPresent()) {
            // Cascade delete is handled by ON DELETE CASCADE in memberships
            tenantRepository.delete(tenant.get());
        }
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> findById(UUID id) {
        return tenantRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }
}
