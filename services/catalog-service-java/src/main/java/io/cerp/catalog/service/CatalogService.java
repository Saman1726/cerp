package io.cerp.catalog.service;

import io.cerp.catalog.dto.CatalogItemResponse;
import io.cerp.catalog.dto.CreateCatalogItemRequest;
import io.cerp.catalog.dto.CreatePriceRequest;
import io.cerp.catalog.dto.UpdateCatalogItemRequest;
import java.util.List;
import java.util.UUID;

public interface CatalogService {

    CatalogItemResponse createItem(CreateCatalogItemRequest req);

    CatalogItemResponse updateItem(UUID id, UpdateCatalogItemRequest req);

    CatalogItemResponse getItem(UUID id);

    List<CatalogItemResponse> listItems(String category);

    CatalogItemResponse createPrice(CreatePriceRequest req);
}
