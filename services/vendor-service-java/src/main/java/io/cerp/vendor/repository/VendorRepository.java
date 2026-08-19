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

    @Query(value = "SELECT * FROM vendor.vendors v WHERE :category = ANY(v.categories)", nativeQuery = true)
    List<Vendor> findByCategoriesContains(@Param("category") String category);
    
    @Query(value = "SELECT * FROM vendor.vendors v WHERE :vendorType = ANY(v.vendor_types)", nativeQuery = true)
    List<Vendor> findByVendorTypesContains(@Param("vendorType") String vendorType);
    
    @Query(value = "SELECT * FROM vendor.vendors v WHERE :capId = ANY(v.capability_ids)", nativeQuery = true)
    List<Vendor> findByCapabilityIdsContains(@Param("capId") UUID capId);
    
    @Query(value = "SELECT * FROM vendor.vendors v WHERE :groupId = ANY(v.category_group_ids)", nativeQuery = true)
    List<Vendor> findByCategoryGroupIdsContains(@Param("groupId") UUID groupId);
}
