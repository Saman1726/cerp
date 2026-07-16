package io.cerp.vendor.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpdateVendorRequest {

    // Optional: pending|approved|rejected
    @Pattern(regexp = "^(pending|approved|rejected)$")
    private String status;

    // Optional categories (tags)
    private List<@Size(max = 64) String> categories = new ArrayList<>();

    // Optional tax profile update
    private Map<String, Object> taxProfile;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public Map<String, Object> getTaxProfile() {
        return taxProfile;
    }

    public void setTaxProfile(Map<String, Object> taxProfile) {
        this.taxProfile = taxProfile;
    }
}
