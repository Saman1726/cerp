package io.cerp.vendor.service;

import io.cerp.vendor.dto.AddRatingRequest;
import io.cerp.vendor.dto.CreateVendorRequest;
import io.cerp.vendor.dto.UpdateVendorRequest;
import io.cerp.vendor.dto.VendorResponse;
import java.util.List;
import java.util.UUID;

public interface VendorService {

    VendorResponse createVendor(CreateVendorRequest req);

    VendorResponse updateVendor(UUID id, UpdateVendorRequest req);

    VendorResponse addRating(UUID vendorId, AddRatingRequest req);

    VendorResponse getVendor(UUID id);

    List<VendorResponse> listVendors(String category);
}
