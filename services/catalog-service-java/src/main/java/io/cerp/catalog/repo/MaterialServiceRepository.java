package io.cerp.catalog.repo;

import io.cerp.catalog.model.MaterialService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MaterialServiceRepository extends JpaRepository<MaterialService, UUID> {

    List<MaterialService> findByTenantId(UUID tenantId);

    Optional<MaterialService> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<MaterialService> findByCodeAndTenantId(String code, UUID tenantId);

    List<MaterialService> findByTypeAndTenantId(String type, UUID tenantId);

    List<MaterialService> findByTenantIdAndType(UUID tenantId, String type);

    @Query("SELECT ms FROM catalog.material_services ms WHERE ms.tenantId = :tenantId " +
           "AND (:type IS NULL OR ms.type = :type)")
    List<MaterialService> findAllByTenantAndOptionalType(
            @Param("tenantId") UUID tenantId,
            @Param("type") String type);

    boolean existsByCodeAndTenantId(String code, UUID tenantId);
}