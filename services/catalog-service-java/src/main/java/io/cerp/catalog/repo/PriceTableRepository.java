package io.cerp.catalog.repo;

import io.cerp.catalog.model.PriceTable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceTableRepository extends JpaRepository<PriceTable, UUID> {

    List<PriceTable> findByMaterialServiceId(UUID materialServiceId);

    List<PriceTable> findByMaterialServiceIdAndTenantId(UUID materialServiceId, UUID tenantId);

    Optional<PriceTable> findByMaterialServiceIdAndTenantId(UUID materialServiceId, UUID tenantId);

    Optional<PriceTable> findFirstByMaterialServiceIdOrderByValidFromDesc(UUID materialServiceId);

    Optional<PriceTable> findFirstByMaterialServiceIdAndTenantIdOrderByValidFromDesc(
            UUID materialServiceId, UUID tenantId);

    Optional<PriceTable> findFirstByMaterialServiceIdAndTenantIdOrderByValidFromAsc(
            UUID materialServiceId, UUID tenantId);

    Optional<PriceTable> findFirstByMaterialServiceIdAndValidToIsNullOrderByValidFromDesc(UUID materialServiceId);

    Optional<PriceTable> findByLastPriceId(UUID lastPriceId);

    Optional<PriceTable> findByNextPriceId(UUID nextPriceId);

    @Query("SELECT p FROM catalog.price_table p WHERE p.materialService.id = :materialServiceId " +
           "AND p.validFrom <= :now AND (p.validTo IS NULL OR p.validTo >= :now)")
    Optional<PriceTable> findActivePriceForMaterial(
            @Param("materialServiceId") UUID materialServiceId,
            @Param("now") java.time.OffsetDateTime now);

    List<PriceTable> findByTenantId(UUID tenantId);
}