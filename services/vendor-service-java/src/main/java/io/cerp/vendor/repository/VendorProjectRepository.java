package io.cerp.vendor.repository;

import io.cerp.vendor.entity.VendorProject;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorProjectRepository extends JpaRepository<VendorProject, UUID> {
    List<VendorProject> findByVendorId(UUID vendorId);

    List<VendorProject> findByVendorIdAndProjectType(UUID vendorId, String projectType);
}