package io.cerp.vendor.repository;

import io.cerp.vendor.entity.VendorCertification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorCertificationRepository extends JpaRepository<VendorCertification, UUID> {
    List<VendorCertification> findByVendorId(UUID vendorId);

    List<VendorCertification> findByVendorIdAndActive(UUID vendorId, Boolean active);
}