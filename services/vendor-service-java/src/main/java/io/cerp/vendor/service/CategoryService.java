package io.cerp.vendor.service;

import io.cerp.vendor.domain.CategoryDefinition;
import io.cerp.vendor.dto.CategoryResponse;
import io.cerp.vendor.repository.CategoryDefinitionRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryDefinitionRepository categoryDefinitionRepository;

    public CategoryService(CategoryDefinitionRepository categoryDefinitionRepository) {
        this.categoryDefinitionRepository = categoryDefinitionRepository;
    }

    public List<CategoryResponse> getAllCategories() {
        List<CategoryDefinition> categories = categoryDefinitionRepository.findAllActiveCategories();
        return categories.stream().map(this::toResponse).toList();
    }

    public List<CategoryResponse> getCategoriesByGroup(String groupId) {
        List<CategoryDefinition> categories = categoryDefinitionRepository.findByGroupId(groupId);
        return categories.stream().map(this::toResponse).toList();
    }

    private CategoryResponse toResponse(CategoryDefinition entity) {
        return new CategoryResponse(
            entity.getId().toString(),
            entity.getCode(),
            entity.getName(),
            entity.getDescription(),
            entity.getIcon(),
            entity.getDisplayOrder(),
            entity.getGroupId() != null ? entity.getGroupId().toString() : null,
            entity.getGroupName());
    }
}
