package io.cerp.catalog.service;

import io.cerp.catalog.dto.CatalogItemResponse;
import io.cerp.catalog.dto.CreateCatalogItemRequest;
import io.cerp.catalog.dto.CreatePriceRequest;
import io.cerp.catalog.dto.UpdateCatalogItemRequest;
import io.cerp.catalog.events.CatalogEventsPublisher;
import io.cerp.catalog.model.CatalogItem;
import io.cerp.catalog.model.CatalogPrice;
import io.cerp.catalog.repo.CatalogItemRepository;
import io.cerp.catalog.repo.CatalogPriceRepository;
import io.cerp.catalog.security.TenantContext;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogServiceImpl implements CatalogService {

    private final CatalogItemRepository itemRepository;
    private final CatalogPriceRepository priceRepository;
    private final CatalogEventsPublisher eventsPublisher;

    public CatalogServiceImpl(
            CatalogItemRepository itemRepository,
            CatalogPriceRepository priceRepository,
            CatalogEventsPublisher eventsPublisher) {
        this.itemRepository = itemRepository;
        this.priceRepository = priceRepository;
        this.eventsPublisher = eventsPublisher;
    }

    @Override
    @Transactional
    public CatalogItemResponse createItem(CreateCatalogItemRequest req) {
        UUID tenantId = requireTenant();

        CatalogItem item = new CatalogItem();
        item.setTenantId(tenantId);
        item.setSku(req.getSku());
        item.setName(req.getName());
        item.setDescription(req.getDescription());
        item.setAttributes(req.getAttributes());
        if (req.getCategories() != null) {
            item.setCategories(new ArrayList<>(req.getCategories()));
        }

        CatalogItem saved = itemRepository.save(item);

        // Publish event
        eventsPublisher.publish("catalog.item.created", toEventPayload(saved));

        return toResponse(saved, latestPrice(saved.getId()));
    }

    @Override
    @Transactional
    public CatalogItemResponse updateItem(UUID id, UpdateCatalogItemRequest req) {
        UUID tenantId = requireTenant();

        CatalogItem item = itemRepository
            .findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Catalog item not found"));

        if (req.getName() != null) {
            item.setName(req.getName());
        }
        if (req.getDescription() != null) {
            item.setDescription(req.getDescription());
        }
        if (req.getAttributes() != null) {
            item.setAttributes(req.getAttributes());
        }
        if (req.getCategories() != null && !req.getCategories().isEmpty()) {
            item.setCategories(new ArrayList<>(req.getCategories()));
        }

        CatalogItem saved = itemRepository.save(item);

        // Publish event
        eventsPublisher.publish("catalog.item.updated", toEventPayload(saved));

        return toResponse(saved, latestPrice(saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogItemResponse getItem(UUID id) {
        UUID tenantId = requireTenant();

        CatalogItem item = itemRepository
            .findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Catalog item not found"));

        return toResponse(item, latestPrice(item.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItemResponse> listItems(String category) {
        UUID tenantId = requireTenant();

        List<CatalogItem> items = itemRepository.findAllByTenantAndOptionalCategory(tenantId, category);
        List<CatalogItemResponse> responses = new ArrayList<>(items.size());
        for (CatalogItem item : items) {
            responses.add(toResponse(item, latestPrice(item.getId())));
        }
        return responses;
    }

    @Override
    @Transactional
    public CatalogItemResponse createPrice(CreatePriceRequest req) {
        UUID tenantId = requireTenant();

        // Ensure item exists in tenant
        CatalogItem item = itemRepository
            .findById(req.getItemId())
            .orElseThrow(() -> new IllegalArgumentException("Catalog item not found"));
        if (!tenantId.equals(item.getTenantId())) {
            throw new IllegalArgumentException("Catalog item does not belong to current tenant");
        }

        // Close previous active price if exists
        Optional<CatalogPrice> current = priceRepository.findFirstByItem_IdAndValidToIsNullOrderByValidFromDesc(item.getId());
        OffsetDateTime newValidFrom = req.getValidFrom() != null ? req.getValidFrom() : OffsetDateTime.now();
        if (current.isPresent()) {
            CatalogPrice prev = current.get();
            if (prev.getValidTo() == null || prev.getValidTo().isAfter(newValidFrom)) {
                prev.setValidTo(newValidFrom);
            }
            priceRepository.save(prev);
        }

        // Create new price
        CatalogPrice price = new CatalogPrice();
        price.setItem(item);
        price.setCurrency(req.getCurrency());
        price.setAmount(req.getAmount());
        price.setValidFrom(newValidFrom);
        CatalogPrice savedPrice = priceRepository.save(price);

        // Publish event
        eventsPublisher.publish("catalog.price.created", toPriceEventPayload(savedPrice));

        return toResponse(item, Optional.of(savedPrice));
    }

    private UUID requireTenant() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("Tenant id is required"));
    }

    private Optional<CatalogPrice> latestPrice(UUID itemId) {
        // Prefer active price; if none, fall back to latest by validFrom
        Optional<CatalogPrice> active = priceRepository.findFirstByItem_IdAndValidToIsNullOrderByValidFromDesc(itemId);
        if (active.isPresent()) {
            return active;
        }
        return priceRepository.findTopByItem_IdOrderByValidFromDesc(itemId);
    }

    private CatalogItemResponse toResponse(CatalogItem item, Optional<CatalogPrice> latest) {
        CatalogItemResponse dto = new CatalogItemResponse();
        dto.setId(item.getId());
        dto.setSku(item.getSku());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setAttributes(item.getAttributes());
        dto.setCategories(item.getCategories());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());

        latest.ifPresent(p -> {
            CatalogItemResponse.PriceView pv = new CatalogItemResponse.PriceView();
            pv.setCurrency(p.getCurrency());
            pv.setAmount(p.getAmount());
            pv.setValidFrom(p.getValidFrom());
            pv.setValidTo(p.getValidTo());
            dto.setLatestPrice(pv);
        });

        return dto;
    }

    private Object toEventPayload(CatalogItem item) {
        // Lightweight payload for item events
        return java.util.Map.of(
            "id", item.getId().toString(),
            "tenantId", item.getTenantId().toString(),
            "sku", item.getSku(),
            "name", item.getName());
    }

    private Object toPriceEventPayload(CatalogPrice price) {
        return java.util.Map.of(
            "id", price.getId().toString(),
            "itemId", price.getItem().getId().toString(),
            "currency", price.getCurrency(),
            "amount", price.getAmount(),
            "validFrom", price.getValidFrom().toString(),
            "validTo", price.getValidTo() != null ? price.getValidTo().toString() : null);
    }
}
