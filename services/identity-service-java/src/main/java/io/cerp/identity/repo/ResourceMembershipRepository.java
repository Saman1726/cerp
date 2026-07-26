package io.cerp.identity.repo;

import io.cerp.identity.domain.ResourceMembership;
import io.cerp.identity.domain.ResourceMembership.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing ResourceMembership entities. Provides queries for finding user access to specific resources, and finding all resources a user has access to.
 */
@Repository
public interface ResourceMembershipRepository extends JpaRepository<ResourceMembership, UUID> {

    /**
     * Find all resource memberships for a specific user.
     */
    List<ResourceMembership> findByUserId(UUID userId);

    /**
     * Find all resource memberships for a specific resource.
     */
    List<ResourceMembership> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    /**
     * Find a specific membership for a user on a resource.
     */
    Optional<ResourceMembership> findByResourceTypeAndResourceIdAndUserId(ResourceType resourceType, UUID resourceId, UUID userId);

    /**
     * Check if a user has any membership on a resource.
     */
    boolean existsByResourceTypeAndResourceIdAndUserId(ResourceType resourceType, UUID resourceId, UUID userId);

    /**
     * Find all resource IDs of a given type that a user has access to.
     */
    @Query("SELECT rm.resourceId FROM ResourceMembership rm WHERE rm.userId = :userId AND rm.resourceType = :resourceType")
    List<UUID> findResourceIdsByUserIdAndType(@Param("userId") UUID userId, @Param("resourceType") ResourceType resourceType);

    /**
     * Delete all memberships for a specific resource.
     */
    void deleteByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    /**
     * Delete all memberships for a specific user.
     */
    void deleteByUserId(UUID userId);

    /**
     * Find all distinct resource IDs a user has access to (across all resource types).
     */
    @Query("SELECT DISTINCT rm.resourceId FROM ResourceMembership rm WHERE rm.userId = :userId")
    List<UUID> findDistinctResourceIdsByUserId(@Param("userId") UUID userId);
}
