package io.cerp.catalog.dto;

import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpdateMaterialServiceRequest {

    @Size(max = 255)
    private String name;

    private String description;

    @Size(max = 20)
    private String unit;

    private String documentUrl;

    @Size(max = 10)
    private String type;

    private List<UUID> categoryGroupIds = new ArrayList<>();

    private List<UUID> categoryIds = new ArrayList<>();

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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<UUID> getCategoryGroupIds() {
        return categoryGroupIds;
    }

    public void setCategoryGroupIds(List<UUID> categoryGroupIds) {
        this.categoryGroupIds = categoryGroupIds;
    }

    public List<UUID> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<UUID> categoryIds) {
        this.categoryIds = categoryIds;
    }
}