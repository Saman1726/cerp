package io.cerp.vendor.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event payload for vendor registration events published to Kafka.
 */
public class VendorRegistrationEvent {

    private UUID vendorId;
    private String email;
    private String password;
    private String legalName;
    private String contactName;
    private String phone;
    private String website;
    private String address;
    private String city;
    private String country;
    private String region;
    private String tenantName;
    private List<String> vendorTypes;
    private List<UUID> categoryGroupIds;
    private List<UUID> capabilityIds;
    private List<String> projectTypes;
    private Integer yearsInBusiness;
    private String description;
    private String logoUrl;
    private String registrationNumber;
    private List<String> regions;
    private List<String> categories;
    private OffsetDateTime timestamp;

    public VendorRegistrationEvent() {
    }

    public UUID getVendorId() {
        return vendorId;
    }

    public void setVendorId(UUID vendorId) {
        this.vendorId = vendorId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public List<String> getVendorTypes() {
        return vendorTypes;
    }

    public void setVendorTypes(List<String> vendorTypes) {
        this.vendorTypes = vendorTypes;
    }

    public List<UUID> getCategoryGroupIds() {
        return categoryGroupIds;
    }

    public void setCategoryGroupIds(List<UUID> categoryGroupIds) {
        this.categoryGroupIds = categoryGroupIds;
    }

    public List<UUID> getCapabilityIds() {
        return capabilityIds;
    }

    public void setCapabilityIds(List<UUID> capabilityIds) {
        this.capabilityIds = capabilityIds;
    }

    public List<String> getProjectTypes() {
        return projectTypes;
    }

    public void setProjectTypes(List<String> projectTypes) {
        this.projectTypes = projectTypes;
    }

    public Integer getYearsInBusiness() {
        return yearsInBusiness;
    }

    public void setYearsInBusiness(Integer yearsInBusiness) {
        this.yearsInBusiness = yearsInBusiness;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}