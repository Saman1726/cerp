package io.cerp.catalog.repo;

import io.cerp.catalog.model.CatalogItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {

    List<CatalogItem> findByTenantId(UUID tenantId);

    Optional<CatalogItem> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<CatalogItem> findBySkuAndTenantId(String sku, UUID tenantId);

    List<CatalogItem> findByVendorId(UUID vendorId);

    List<CatalogItem> findByVendorIdAndVendorType(UUID vendorId, String vendorType);

    @Query(value = """
            select ci.*
            from supply.catalog_items ci
            where ci.tenant_id = :tenantId
              and (:category is null or :category = ANY(ci.categories))
            order by ci.created_at desc
            """, nativeQuery = true)
    List<CatalogItem> findAllByTenantAndOptionalCategory(
            @Param("tenantId") UUID tenantId,
            @Param("category") String category);
}
