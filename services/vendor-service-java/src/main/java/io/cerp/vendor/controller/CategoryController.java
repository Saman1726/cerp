package io.cerp.vendor.controller;

import io.cerp.vendor.dto.CategoryResponse;
import io.cerp.vendor.service.CategoryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vendors/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<CategoryResponse>> getCategoriesByGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(categoryService.getCategoriesByGroup(groupId));
    }
}
