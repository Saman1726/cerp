package io.cerp.vendor.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.cerp.vendor.entity.Vendor;
import io.cerp.vendor.repository.VendorCertificationRepository;
import io.cerp.vendor.repository.VendorProjectRepository;
import io.cerp.vendor.repository.VendorRepository;
import io.cerp.vendor.repository.CategoryGroupRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class VendorServiceImplTest {

    @Mock
    VendorRepository vendorRepository;

    @Mock
    VendorCertificationRepository vendorCertificationRepository;

    @Mock
    VendorProjectRepository vendorProjectRepository;

    @Mock
    CategoryGroupRepository categoryGroupRepository;

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    private VendorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new VendorServiceImpl(vendorRepository, vendorCertificationRepository,
            vendorProjectRepository, categoryGroupRepository, kafkaTemplate, "test-topic");
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    @Test
    void createVendor_success() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> {
            Vendor v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        Vendor vendor = service.create(tenantId, userId, "Acme Suppliers", "John",
            "555-1234", "https://acme.com", "123 Main St", "Berlin", "DE", List.of("electrical"));

        assertNotNull(vendor.getId());
        assertEquals("Acme Suppliers", vendor.getLegalName());
        assertEquals(tenantId, vendor.getTenantId());
        assertEquals(userId, vendor.getUserId());
        assertEquals("pending", vendor.getStatus());
        assertNotNull(vendor.getCreatedAt());
        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void register_success() {
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> {
            Vendor v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        doReturn(java.util.concurrent.CompletableFuture.completedFuture(null))
            .when(kafkaTemplate).send(any(String.class), any(Message.class));

        Vendor vendor = service.register("vendor@example.com", "password123",
            "Acme Suppliers", "John", "555-1234", "https://acme.com",
            "123 Main St", "Berlin", "DE", "BE", "Acme Tenant", List.of("electrical"),
            List.of(), List.of(), List.of(), null, null, null, null, List.of());

        assertNotNull(vendor.getId());
        assertEquals("Acme Suppliers", vendor.getLegalName());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), vendor.getTenantId());
        assertNull(vendor.getUserId());
        assertEquals("pending", vendor.getStatus());
        verify(vendorRepository).save(any(Vendor.class));
        verify(kafkaTemplate).send(any(String.class), any(Message.class));
    }

    @Test
    void register_failureToPublish_doesNotFail() {
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> {
            Vendor v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        doThrow(new RuntimeException("kafka error")).when(kafkaTemplate).send(any(String.class), any(Message.class));

        assertDoesNotThrow(() -> service.register("vendor@example.com", "password123",
            "Acme Suppliers", "John", "555-1234", "https://acme.com",
            "123 Main St", "Berlin", "DE", "BE", "Acme Tenant", List.of("electrical"),
            List.of(), List.of(), List.of(), null, null, null, null, List.of()));

        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void linkIdentity_success() {
        UUID vendorId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setLegalName("Acme Suppliers");
        vendor.setTenantId(null);

        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        Vendor result = service.linkIdentity(vendorId, tenantId, userId);

        assertEquals(tenantId, result.getTenantId());
        assertEquals(userId, result.getUserId());
        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void findById_success() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setLegalName("Acme Suppliers");
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));

        Optional<Vendor> found = service.findById(vendorId);

        assertTrue(found.isPresent());
        assertEquals("Acme Suppliers", found.get().getLegalName());
    }

    @Test
    void findById_notFound() {
        UUID vendorId = UUID.randomUUID();
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.empty());

        Optional<Vendor> found = service.findById(vendorId);

        assertTrue(found.isEmpty());
    }

    @Test
    void findByTenantId_success() {
        UUID tenantId = UUID.randomUUID();
        Vendor vendor = new Vendor();
        vendor.setId(UUID.randomUUID());
        vendor.setTenantId(tenantId);
        when(vendorRepository.findByTenantId(tenantId)).thenReturn(Optional.of(vendor));

        Optional<Vendor> found = service.findByTenantId(tenantId);

        assertTrue(found.isPresent());
        assertEquals(tenantId, found.get().getTenantId());
    }

    @Test
    void findByStatus_success() {
        List<Vendor> vendors = List.of(new Vendor(), new Vendor());
        when(vendorRepository.findByStatus("pending")).thenReturn(vendors);

        List<Vendor> found = service.findByStatus("pending");

        assertEquals(2, found.size());
    }

    @Test
    void findAll_success() {
        List<Vendor> vendors = List.of(new Vendor());
        when(vendorRepository.findAll()).thenReturn(vendors);

        List<Vendor> found = service.findAll();

        assertEquals(1, found.size());
    }

    @Test
    void updateStatus_success() {
        UUID vendorId = UUID.randomUUID();
        Vendor vendor = new Vendor();
        vendor.setId(vendorId);
        vendor.setStatus("pending");
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> inv.getArgument(0));

        Vendor updated = service.updateStatus(vendorId, "approved");

        assertEquals("approved", updated.getStatus());
        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void updateStatus_notFound() {
        UUID vendorId = UUID.randomUUID();
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.updateStatus(vendorId, "approved"));
    }

    @Test
    void findByCategoriesContains_success() {
        List<Vendor> vendors = List.of(new Vendor());
        when(vendorRepository.findByCategoriesContains("electrical")).thenReturn(vendors);

        List<Vendor> found = service.findByCategoriesContains("electrical");

        assertEquals(1, found.size());
    }

    @Test
    void createVendor_emptyCategories() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> {
            Vendor v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        Vendor vendor = service.create(tenantId, userId, "Acme Suppliers", null,
            null, null, null, null, null, null);

        assertNotNull(vendor.getId());
        assertEquals(List.of(), vendor.getCategories());
    }

    @Test
    void register_emptyCategories() {
        when(vendorRepository.save(any(Vendor.class))).thenAnswer(inv -> {
            Vendor v = inv.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        doReturn(java.util.concurrent.CompletableFuture.completedFuture(null))
            .when(kafkaTemplate).send(any(String.class), any(Message.class));

        Vendor vendor = service.register("vendor@example.com", "password123",
            "Acme Suppliers", null, null, null,
            null, null, null, null, null, null,
            List.of(), List.of(), List.of(), null, null, null, null, List.of());

        assertNotNull(vendor.getId());
        assertEquals(List.of(), vendor.getCategories());
    }
}
