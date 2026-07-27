package io.cerp.vendor.repository;

import io.cerp.vendor.domain.CategoryDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryDefinitionRepository extends JpaRepository<CategoryDefinition, String> {
    @Query(value = """
            SELECT cd.id as id, cd.code as code, cd.name as name, cd.description as description,
                   cd.icon as icon, cd.display_order as displayOrder,
                   cd.group_id as groupId, cg.name as groupName
            FROM supply.category_definitions cd
            LEFT JOIN supply.category_groups cg ON cd.group_id = cg.id
            WHERE cd.is_active = TRUE
            ORDER BY COALESCE((SELECT display_order FROM supply.category_groups WHERE id = cd.group_id), 999), cd.display_order
            """, nativeQuery = true)
    List<CategoryDefinition> findAllActiveCategories();

    @Query(value = """
            SELECT cd.id as id, cd.code as code, cd.name as name, cd.description as description,
                   cd.icon as icon, cd.display_order as displayOrder,
                   cd.group_id as groupId, cg.name as groupName
            FROM supply.category_definitions cd
            LEFT JOIN supply.category_groups cg ON cd.group_id = cg.id
            WHERE cd.is_active = TRUE AND cd.group_id = :groupId
            ORDER BY cd.display_order
            """, nativeQuery = true)
    List<CategoryDefinition> findByGroupId(@Param("groupId") String groupId);
}
