package io.cerp.vendor.repository;

import io.cerp.vendor.model.VendorFavorite;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorFavoriteRepository extends JpaRepository<VendorFavorite, UUID> {

    List<VendorFavorite> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndVendorId(UUID userId, UUID vendorId);

    void deleteByUserIdAndVendorId(UUID userId, UUID vendorId);

    @Query("SELECT vf.vendorId FROM VendorFavorite vf WHERE vf.userId = :userId")
    List<UUID> findVendorIdsByUserId(@Param("userId") UUID userId);
}
