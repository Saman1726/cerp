package io.cerp.vendor.repository;

import io.cerp.vendor.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    Optional<Vendor> findByTenantId(UUID tenantId);

    Optional<Vendor> findByUserId(UUID userId);

    List<Vendor> findByStatus(String status);

    @Query(value = "SELECT * FROM supply.vendors v WHERE :category = ANY(v.categories)", nativeQuery = true)
    List<Vendor> findByCategoriesContains(@Param("category") String category);
}
