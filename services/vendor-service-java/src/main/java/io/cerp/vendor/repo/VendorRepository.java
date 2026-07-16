package io.cerp.vendor.repo;

import io.cerp.vendor.model.Vendor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findById(UUID id);

    // RLS in DB scopes by tenant automatically. Use native query to check membership in Postgres text[].
    @Query(value = """
            select v.*
            from supply.vendors v
            where (:category is null or :category = '' or exists (
              select 1 from unnest(v.categories) c where c = :category
            ))
            order by v.created_at desc
            """, nativeQuery = true)
    List<Vendor> findAllByOptionalCategory(@Param("category") String category);
}
