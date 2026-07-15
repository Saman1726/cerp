package io.cerp.identity.repo;

import io.cerp.identity.domain.Membership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    List<Membership> findByTenantId(UUID tenantId);

    List<Membership> findByUserId(UUID userId);

    Optional<Membership> findByTenantIdAndUserId(UUID tenantId, UUID userId);
}
