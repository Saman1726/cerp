package io.cerp.vendor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.cerp.vendor.dto.AddRatingRequest;
import io.cerp.vendor.dto.CreateVendorRequest;
import io.cerp.vendor.dto.VendorResponse;
import io.cerp.vendor.events.VendorEventsPublisher;
import io.cerp.vendor.model.Vendor;
import io.cerp.vendor.model.VendorRating;
import io.cerp.vendor.repo.VendorDocumentRepository;
import io.cerp.vendor.repo.VendorRatingRepository;
import io.cerp.vendor.repo.VendorRepository;
import io.cerp.vendor.security.TenantContext;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VendorServiceImplTest {

    // Simple stub to avoid Mockito/ByteBuddy issues on JDK 26
    static class StubPublisher extends io.cerp.vendor.events.VendorEventsPublisher {
        java.util.List<java.util.Map<String, Object>> published = new java.util.ArrayList<>();

        StubPublisher() {
            super(null, "test-topic");
        }

        @Override
        public void publish(String type, Object payload) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("type", type);
            m.put("payload", payload);
            published.add(m);
        }
    }

    @Mock
    VendorRepository vendorRepository;
    @Mock
    VendorDocumentRepository vendorDocumentRepository;
    @Mock
    VendorRatingRepository vendorRatingRepository;
    StubPublisher events;

    VendorServiceImpl service;

    @BeforeEach
    void setUp() {
        events = new StubPublisher();
        service = new VendorServiceImpl(vendorRepository, vendorDocumentRepository, vendorRatingRepository, events);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createVendor_success() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);

        CreateVendorRequest req = new CreateVendorRequest();
        req.setLegalName("Acme Suppliers");
        req.setCategories(List.of("electrical", "tools"));
        req.setTaxProfile(Map.of("vat", "DE123456789"));

        CreateVendorRequest.CreateDocument doc = new CreateVendorRequest.CreateDocument();
        doc.setDocType("ins_certificate");
        doc.setDocRef("DMS-123");
        doc.setExpiresAt(OffsetDateTime.now().plusDays(30));
        List<CreateVendorRequest.CreateDocument> docs = new ArrayList<>();
        docs.add(doc);
        req.setDocuments(docs);

        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vendorDocumentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        VendorResponse resp = service.createVendor(req);

        assertNotNull(resp.getId());
        assertEquals("Acme Suppliers", resp.getLegalName());
        assertEquals(tenantId, resp.getTenantId());
        assertTrue(resp.getCategories().contains("electrical"));
        assertEquals("pending", resp.getStatus());
        assertNotNull(resp.getCreatedAt());

        assertEquals(1, events.published.size());
        assertEquals("VendorCreated", events.published.get(0).get("type"));
    }

    @Test
    void addRating_success() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
        UUID vendorId = UUID.randomUUID();

        Vendor v = new Vendor();
        v.setId(vendorId);
        v.setTenantId(tenantId);
        v.setLegalName("Acme Suppliers");
        v.setStatus("pending");
        v.setCreatedAt(OffsetDateTime.now().minusDays(1));

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(v));
        when(vendorRatingRepository.save(any(VendorRating.class))).thenAnswer(inv -> inv.getArgument(0));

        AddRatingRequest req = new AddRatingRequest();
        req.setScore(5);
        req.setRationale("Excellent delivery performance");

        VendorResponse resp = service.addRating(vendorId, req);

        assertEquals(vendorId, resp.getId());
        verify(vendorRatingRepository).save(any(VendorRating.class));
        assertEquals(1, events.published.size());
        assertEquals("RatingUpdated", events.published.get(0).get("type"));
    }

    @Test
    void getVendor_notFound_throws() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.set(tenantId);
        when(vendorRepository.findById(any())).thenReturn(Optional.empty());

        ResponseStatusException ex =
                assertThrows(ResponseStatusException.class, () -> service.getVendor(UUID.randomUUID()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
