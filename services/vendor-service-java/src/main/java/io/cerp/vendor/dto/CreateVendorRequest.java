package io.cerp.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateVendorRequest {

    @NotBlank
    private String legalName;

    // Optional categories (tags)
    private List<@NotBlank @Size(max = 64) String> categories = new ArrayList<>();

    // Arbitrary tax profile structure (e.g., tax ids, registration numbers)
    private Map<String, Object> taxProfile;

    // Optional initial documents to register for the vendor
    private List<CreateDocument> documents = new ArrayList<>();

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
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

    public List<CreateDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<CreateDocument> documents) {
        this.documents = documents;
    }

    public static class CreateDocument {
        @NotBlank
        private String docType;

        @NotBlank
        private String docRef;

        private OffsetDateTime expiresAt;

        public String getDocType() {
            return docType;
        }

        public void setDocType(String docType) {
            this.docType = docType;
        }

        public String getDocRef() {
            return docRef;
        }

        public void setDocRef(String docRef) {
            this.docRef = docRef;
        }

        public OffsetDateTime getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(OffsetDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
