package io.cerp.catalog.service;

import io.cerp.catalog.dto.CreateMaterialServiceRequest;
import io.cerp.catalog.dto.CreatePriceTableRequest;
import io.cerp.catalog.dto.MaterialServiceResponse;
import io.cerp.catalog.dto.PriceTableResponse;
import io.cerp.catalog.dto.UpdateMaterialServiceRequest;
import io.cerp.catalog.dto.UpdatePriceTableRequest;
import io.cerp.catalog.events.CatalogEventsPublisher;
import io.cerp.catalog.model.MaterialService;
import io.cerp.catalog.model.PriceTable;
import io.cerp.catalog.repo.MaterialServiceRepository;
import io.cerp.catalog.repo.PriceTableRepository;
import io.cerp.catalog.security.TenantContext;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialServiceImpl implements MaterialServiceService {

    private final MaterialServiceRepository materialServiceRepository;
    private final PriceTableRepository priceTableRepository;
    private final CatalogEventsPublisher eventsPublisher;

    public MaterialServiceImpl(
            MaterialServiceRepository materialServiceRepository,
            PriceTableRepository priceTableRepository,
            CatalogEventsPublisher eventsPublisher) {
        this.materialServiceRepository = materialServiceRepository;
        this.priceTableRepository = priceTableRepository;
        this.eventsPublisher = eventsPublisher;
    }

    @Override
    @Transactional
    public MaterialServiceResponse createMaterialService(CreateMaterialServiceRequest request) {
        UUID tenantId = requireTenant();

        // Check for duplicate code
        if (materialServiceRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new IllegalArgumentException("Material/Service with code " + request.getCode() + " already exists");
        }

        MaterialService entity = new MaterialService();
        entity.setTenantId(tenantId);
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setUnit(request.getUnit());
        entity.setDocumentUrl(request.getDocumentUrl());
        entity.setType(request.getTypeAsString());
        entity.setCategoryGroupIds(request.getCategoryGroupIds());
        entity.setCategoryIds(request.getCategoryIds());

        MaterialService saved = materialServiceRepository.save(entity);

        eventsPublisher.publish("material_service.created", toEventPayload(saved));

        return toMaterialServiceResponse(saved, null);
    }

    @Override
    @Transactional
    public MaterialServiceResponse updateMaterialService(UUID id, UpdateMaterialServiceRequest request) {
        UUID tenantId = requireTenant();

        MaterialService entity = materialServiceRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Material/Service not found"));

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getUnit() != null) {
            entity.setUnit(request.getUnit());
        }
        if (request.getDocumentUrl() != null) {
            entity.setDocumentUrl(request.getDocumentUrl());
        }
        if (request.getType() != null) {
            entity.setType(request.getType());
        }
        if (request.getCategoryGroupIds() != null) {
            entity.setCategoryGroupIds(request.getCategoryGroupIds());
        }
        if (request.getCategoryIds() != null) {
            entity.setCategoryIds(request.getCategoryIds());
        }

        MaterialService saved = materialServiceRepository.save(entity);

        eventsPublisher.publish("material_service.updated", toEventPayload(saved));

        return toMaterialServiceResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialServiceResponse getMaterialService(UUID id) {
        UUID tenantId = requireTenant();

        MaterialService entity = materialServiceRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Material/Service not found"));

        return toMaterialServiceResponse(entity, getLatestPrice(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialServiceResponse> listMaterialServices() {
        UUID tenantId = requireTenant();
        List<MaterialService> entities = materialServiceRepository.findByTenantId(tenantId);
        return entities.stream()
                .map(entity -> toMaterialServiceResponse(entity, getLatestPrice(entity.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialServiceResponse> listMaterialServicesByType(String type) {
        UUID tenantId = requireTenant();
        List<MaterialService> entities = materialServiceRepository.findByTypeAndTenantId(type, tenantId);
        return entities.stream()
                .map(entity -> toMaterialServiceResponse(entity, getLatestPrice(entity.getId())))
                .toList();
    }

    @Override
    @Transactional
    public PriceTableResponse createPriceTable(CreatePriceTableRequest request) {
        UUID tenantId = requireTenant();

        // Find the material/service
        MaterialService materialService = materialServiceRepository
                .findByIdAndTenantId(request.getMaterialServiceId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material/Service not found: " + request.getMaterialServiceId()));

        // Set the last price (most recent) as the previous price for this new entry
        Optional<PriceTable> lastPriceOpt = priceTableRepository
                .findFirstByMaterialServiceIdAndTenantIdOrderByValidFromDesc(
                        request.getMaterialServiceId(), tenantId);

        PriceTable entity = new PriceTable();
        entity.setTenantId(tenantId);
        entity.setMaterialService(materialService);
        entity.setCurrency(request.getCurrency());
        entity.setUnitPrice(request.getUnitPrice());
        entity.setValidFrom(request.getValidFrom() != null ? request.getValidFrom() : OffsetDateTime.now());
        entity.setValidTo(request.getValidTo());

        // Link the price chain
        if (lastPriceOpt.isPresent()) {
            PriceTable lastPrice = lastPriceOpt.get();
            entity.setLastPrice(lastPrice);
            // Set the next price on the last price
            lastPrice.setNextPrice(entity);
            priceTableRepository.save(lastPrice);
        }

        PriceTable saved = priceTableRepository.save(entity);

        eventsPublisher.publish("price_table.created", toPriceEventPayload(saved));

        return toPriceTableResponse(saved);
    }

    @Override
    @Transactional
    public PriceTableResponse updatePriceTable(UUID id, UpdatePriceTableRequest request) {
        UUID tenantId = requireTenant();

        PriceTable entity = priceTableRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Price record not found"));

        // Verify tenant ownership
        if (!tenantId.equals(entity.getTenantId())) {
            throw new IllegalArgumentException("Price record does not belong to current tenant");
        }

        if (request.getCurrency() != null) {
            entity.setCurrency(request.getCurrency());
        }
        if (request.getUnitPrice() != null) {
            entity.setUnitPrice(request.getUnitPrice());
        }
        if (request.getValidFrom() != null) {
            entity.setValidFrom(request.getValidFrom());
        }
        if (request.getValidTo() != null) {
            entity.setValidTo(request.getValidTo());
        }

        PriceTable saved = priceTableRepository.save(entity);

        eventsPublisher.publish("price_table.updated", toPriceEventPayload(saved));

        return toPriceTableResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PriceTableResponse getPriceTable(UUID id) {
        UUID tenantId = requireTenant();

        PriceTable entity = priceTableRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Price record not found"));

        if (!tenantId.equals(entity.getTenantId())) {
            throw new IllegalArgumentException("Price record does not belong to current tenant");
        }

        return toPriceTableResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceTableResponse> listPriceTables() {
        UUID tenantId = requireTenant();
        List<PriceTable> entities = priceTableRepository.findByTenantId(tenantId);
        return entities.stream().map(this::toPriceTableResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PriceTableResponse> listPriceTablesByMaterialService(UUID materialServiceId) {
        UUID tenantId = requireTenant();
        List<PriceTable> entities = priceTableRepository.findByMaterialServiceIdAndTenantId(materialServiceId, tenantId);
        return entities.stream().map(this::toPriceTableResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaterialServiceResponse> searchMaterials(String type, String query) {
        UUID tenantId = requireTenant();

        List<MaterialService> results;
        if (type != null && !type.isEmpty()) {
            results = materialServiceRepository.findByTypeAndTenantId(type, tenantId);
        } else {
            results = materialServiceRepository.findByTenantId(tenantId);
        }

        if (query != null && !query.isEmpty()) {
            String lowerQuery = query.toLowerCase();
            results = results.stream()
                    .filter(ms -> ms.getName().toLowerCase().contains(lowerQuery)
                            || ms.getCode().toLowerCase().contains(lowerQuery)
                            || (ms.getDescription() != null && ms.getDescription().toLowerCase().contains(lowerQuery)))
                    .toList();
        }

        return results.stream()
                .map(entity -> toMaterialServiceResponse(entity, getLatestPrice(entity.getId())))
                .toList();
    }

    private UUID requireTenant() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("Tenant id is required"));
    }

    private Optional<PriceTable> getLatestPrice(UUID materialServiceId) {
        // Try to find active price first
        Optional<PriceTable> active = priceTableRepository
                .findFirstByMaterialServiceIdAndValidToIsNullOrderByValidFromDesc(materialServiceId);
        if (active.isPresent()) {
            return active;
        }
        // Fall back to most recent price
        return priceTableRepository.findFirstByMaterialServiceIdOrderByValidFromDesc(materialServiceId);
    }

    private MaterialServiceResponse toMaterialServiceResponse(MaterialService entity, Optional<PriceTable> latestPrice) {
        MaterialServiceResponse response = new MaterialServiceResponse();
        response.setId(entity.getId());
        response.setType(entity.getType());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setUnit(entity.getUnit());
        response.setDocumentUrl(entity.getDocumentUrl());
        response.setCategoryGroupIds(entity.getCategoryGroupIds());
        response.setCategoryIds(entity.getCategoryIds());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        latestPrice.ifPresent(price -> {
            MaterialServiceResponse.LatestPriceView priceView = new MaterialServiceResponse.LatestPriceView();
            priceView.setCurrency(price.getCurrency());
            priceView.setUnitPrice(price.getUnitPrice());
            priceView.setValidFrom(price.getValidFrom());
            priceView.setValidTo(price.getValidTo());
            response.setLatestPrice(priceView);
        });

        return response;
    }

    private PriceTableResponse toPriceTableResponse(PriceTable entity) {
        PriceTableResponse response = new PriceTableResponse();
        response.setId(entity.getId());
        response.setMaterialServiceId(entity.getMaterialService().getId());
        response.setMaterialServiceName(entity.getMaterialService().getName());
        response.setMaterialServiceCode(entity.getMaterialService().getCode());
        response.setMaterialServiceType(entity.getMaterialService().getType());
        response.setCurrency(entity.getCurrency());
        response.setUnitPrice(entity.getUnitPrice());
        response.setValidFrom(entity.getValidFrom());
        response.setValidTo(entity.getValidTo());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());

        // Set last price if exists
        if (entity.getLastPrice() != null) {
            PriceTableResponse.LatestPriceView lastPriceView = new PriceTableResponse.LatestPriceView();
            lastPriceView.setId(entity.getLastPrice().getId());
            lastPriceView.setCurrency(entity.getLastPrice().getCurrency());
            lastPriceView.setUnitPrice(entity.getLastPrice().getUnitPrice());
            lastPriceView.setValidFrom(entity.getLastPrice().getValidFrom());
            lastPriceView.setValidTo(entity.getLastPrice().getValidTo());
            response.setLastPrice(lastPriceView);
        }

        // Set next price if exists
        if (entity.getNextPrice() != null) {
            PriceTableResponse.LatestPriceView nextPriceView = new PriceTableResponse.LatestPriceView();
            nextPriceView.setId(entity.getNextPrice().getId());
            nextPriceView.setCurrency(entity.getNextPrice().getCurrency());
            nextPriceView.setUnitPrice(entity.getNextPrice().getUnitPrice());
            nextPriceView.setValidFrom(entity.getNextPrice().getValidFrom());
            nextPriceView.setValidTo(entity.getNextPrice().getValidTo());
            response.setNextPrice(nextPriceView);
        }

        return response;
    }

    private Object toEventPayload(MaterialService entity) {
        return java.util.Map.of(
                "id", entity.getId().toString(),
                "tenantId", entity.getTenantId().toString(),
                "type", entity.getType(),
                "code", entity.getCode(),
                "name", entity.getName());
    }

    private Object toPriceEventPayload(PriceTable price) {
        return java.util.Map.of(
                "id", price.getId().toString(),
                "materialServiceId", price.getMaterialService().getId().toString(),
                "currency", price.getCurrency(),
                "unitPrice", price.getUnitPrice(),
                "validFrom", price.getValidFrom().toString(),
                "validTo", price.getValidTo() != null ? price.getValidTo().toString() : null);
    }
}
