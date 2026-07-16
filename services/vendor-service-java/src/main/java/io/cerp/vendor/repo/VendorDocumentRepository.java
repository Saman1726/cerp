package io.cerp.vendor.repo;

import io.cerp.vendor.model.VendorDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {

    @Query(value = """
            select d.*
            from supply.vendor_documents d
            where d.vendor_id = :vendorId
            order by d.created_at desc
            """, nativeQuery = true)
    List<VendorDocument> findByVendorId(@Param("vendorId") UUID vendorId);
}
