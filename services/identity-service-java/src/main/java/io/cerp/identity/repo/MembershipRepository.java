package io.cerp.identity.repo;

import io.cerp.identity.domain.Membership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    List<Membership> findByTenantId(UUID tenantId);

    List<Membership> findByUserId(UUID userId);

    Optional<Membership> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * Find all roles for a user across all memberships. Uses JSONB array functions to extract roles.
     */
    @Query(value = "SELECT DISTINCT UNNEST(m.roles) AS role FROM identity.memberships m WHERE m.user_id = :userId", nativeQuery = true)
    List<String> findRolesByUserId(@Param("userId") UUID userId);
}
