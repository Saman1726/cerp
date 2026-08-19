package io.cerp.vendor.repository;

import io.cerp.vendor.domain.CategoryGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryGroupRepository extends JpaRepository<CategoryGroup, UUID> {
    @Query(value = """
                      SELECT cg.id, cg.name, cg.description, cg.display_order, cg.is_active,
                             cg.created_at, cg.updated_at,
                             COUNT(cd.id) FILTER (WHERE cd.is_active = TRUE) as category_count
                      FROM vendor.category_groups cg
                      LEFT JOIN vendor.category_definitions cd ON cd.group_id = cg.id
                      WHERE cg.is_active = TRUE
                      GROUP BY cg.id, cg.name, cg.description, cg.display_order, cg.is_active,
                               cg.created_at, cg.updated_at
                      ORDER BY cg.display_order
                      """, nativeQuery = true)
    List<Object[]> findAllGroupsWithActiveCounts();
}