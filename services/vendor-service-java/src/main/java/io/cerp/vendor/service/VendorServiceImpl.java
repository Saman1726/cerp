package io.cerp.vendor.service;

import io.cerp.vendor.dto.AddRatingRequest;
import io.cerp.vendor.dto.CreateVendorRequest;
import io.cerp.vendor.dto.UpdateVendorRequest;
import io.cerp.vendor.dto.VendorResponse;
import io.cerp.vendor.events.VendorEventsPublisher;
import io.cerp.vendor.model.Vendor;
import io.cerp.vendor.model.VendorDocument;
import io.cerp.vendor.model.VendorRating;
import io.cerp.vendor.repo.VendorDocumentRepository;
import io.cerp.vendor.repo.VendorRatingRepository;
import io.cerp.vendor.repo.VendorRepository;
import io.cerp.vendor.security.TenantContext;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final VendorDocumentRepository vendorDocumentRepository;
    private final VendorRatingRepository vendorRatingRepository;
    private final VendorEventsPublisher events;

    public VendorServiceImpl(
            VendorRepository vendorRepository,
            VendorDocumentRepository vendorDocumentRepository,
            VendorRatingRepository vendorRatingRepository,
            VendorEventsPublisher events) {
        this.vendorRepository = vendorRepository;
        this.vendorDocumentRepository = vendorDocumentRepository;
        this.vendorRatingRepository = vendorRatingRepository;
        this.events = events;
    }

    @Override
    @Transactional
    public VendorResponse createVendor(CreateVendorRequest req) {
        UUID tenantId = requireTenant();
        Vendor v = new Vendor();
        v.setId(UUID.randomUUID());
        v.setTenantId(tenantId);
        v.setLegalName(req.getLegalName());
        if (req.getCategories() != null) {
            v.setCategories(new ArrayList<>(req.getCategories()));
        }
        v.setTaxProfile(req.getTaxProfile());
        v.setStatus("pending");
        v.setCreatedAt(OffsetDateTime.now());
        Vendor saved = vendorRepository.save(v);

        if (req.getDocuments() != null && !req.getDocuments().isEmpty()) {
            List<VendorDocument> docs = new ArrayList<>();
            for (CreateVendorRequest.CreateDocument d : req.getDocuments()) {
                VendorDocument vd = new VendorDocument();
                vd.setId(UUID.randomUUID());
                vd.setVendor(saved);
                vd.setDocType(d.getDocType());
                vd.setDocRef(d.getDocRef());
                vd.setStatus("pending");
                vd.setExpiresAt(d.getExpiresAt());
                vd.setCreatedAt(OffsetDateTime.now());
                docs.add(vd);
            }
            vendorDocumentRepository.saveAll(docs);
        }

        VendorResponse resp = VendorResponse.from(saved);
        events.publish("VendorCreated", resp);
        return resp;
    }

    @Override
    @Transactional
    public VendorResponse updateVendor(UUID id, UpdateVendorRequest req) {
        requireTenant(); // presence ensures RLS tag is set later by aspect at service layer
        Vendor v = vendorRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
        boolean statusChangedToApproved = false;

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            String old = v.getStatus();
            v.setStatus(req.getStatus());
            statusChangedToApproved = !"approved".equalsIgnoreCase(old) && "approved".equalsIgnoreCase(v.getStatus());
        }
        if (req.getCategories() != null) {
            v.setCategories(new ArrayList<>(req.getCategories()));
        }
        if (req.getTaxProfile() != null) {
            v.setTaxProfile(req.getTaxProfile());
        }
        v.setUpdatedAt(OffsetDateTime.now());
        Vendor saved = vendorRepository.save(v);

        VendorResponse resp = VendorResponse.from(saved);
        events.publish(statusChangedToApproved ? "VendorApproved" : "VendorUpdated", resp);
        return resp;
    }

    @Override
    @Transactional
    public VendorResponse addRating(UUID vendorId, AddRatingRequest req) {
        requireTenant();
        Vendor v = vendorRepository
            .findById(vendorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));

        VendorRating r = new VendorRating();
        r.setId(UUID.randomUUID());
        r.setVendor(v);
        r.setScore(Optional.ofNullable(req.getScore()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "score is required")));
        r.setRationale(req.getRationale());
        r.setCreatedAt(OffsetDateTime.now());
        vendorRatingRepository.save(r);

        VendorResponse resp = VendorResponse.from(v);
        events.publish("RatingUpdated", resp);
        return resp;
    }

    @Override
    public VendorResponse getVendor(UUID id) {
        requireTenant();
        Vendor v = vendorRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
        return VendorResponse.from(v);
    }

    @Override
    public List<VendorResponse> listVendors(String category, String name, String city) {
        requireTenant();
        List<Vendor> vendors = vendorRepository.findAllBySearchCriteria(category, name, city);
        List<VendorResponse> out = new ArrayList<>(vendors.size());
        for (Vendor v : vendors) {
            out.add(VendorResponse.from(v));
        }
        return out;
    }

    private UUID requireTenant() {
        return TenantContext
            .get()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant id is required"));
    }
}
