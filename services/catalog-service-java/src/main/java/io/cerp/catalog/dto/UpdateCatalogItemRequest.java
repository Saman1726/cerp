package io.cerp.catalog.dto;

import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Partial update payload for CatalogItem. All fields are optional; when provided they will overwrite the existing values.
 */
public class UpdateCatalogItemRequest {

    // Optional new name
    @Size(min = 1, max = 255)
    private String name;

    // Optional new description
    private String description;

    // Optional replacement for attributes
    private Map<String, Object> attributes;

    // Optional replacement for categories (tags)
    private List<@Size(min = 1, max = 64) String> categories = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }
}
