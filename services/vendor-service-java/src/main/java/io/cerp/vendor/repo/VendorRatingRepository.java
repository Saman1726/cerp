package io.cerp.vendor.repo;

import io.cerp.vendor.model.VendorRating;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorRatingRepository extends JpaRepository<VendorRating, UUID> {

    @Query(value = """
            select r.*
            from supply.vendor_ratings r
            where r.vendor_id = :vendorId
            order by r.created_at desc
            """, nativeQuery = true)
    List<VendorRating> findByVendorId(@Param("vendorId") UUID vendorId);
}
