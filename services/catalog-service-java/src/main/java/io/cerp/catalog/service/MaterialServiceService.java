package io.cerp.catalog.service;

import io.cerp.catalog.dto.CreateMaterialServiceRequest;
import io.cerp.catalog.dto.CreatePriceTableRequest;
import io.cerp.catalog.dto.MaterialServiceResponse;
import io.cerp.catalog.dto.PriceTableResponse;
import io.cerp.catalog.dto.UpdateMaterialServiceRequest;
import io.cerp.catalog.dto.UpdatePriceTableRequest;
import java.util.List;
import java.util.UUID;

public interface MaterialServiceService {

    MaterialServiceResponse createMaterialService(CreateMaterialServiceRequest request);

    MaterialServiceResponse updateMaterialService(UUID id, UpdateMaterialServiceRequest request);

    MaterialServiceResponse getMaterialService(UUID id);

    List<MaterialServiceResponse> listMaterialServices();

    List<MaterialServiceResponse> listMaterialServicesByType(String type);

    PriceTableResponse createPriceTable(CreatePriceTableRequest request);

    PriceTableResponse updatePriceTable(UUID id, UpdatePriceTableRequest request);

    PriceTableResponse getPriceTable(UUID id);

    List<PriceTableResponse> listPriceTables();

    List<PriceTableResponse> listPriceTablesByMaterialService(UUID materialServiceId);

    List<MaterialServiceResponse> searchMaterials(String type, String query);
}