package io.cerp.procurement.service;

import io.cerp.procurement.dto.CreateProcurementRequest;
import io.cerp.procurement.dto.ProcurementRequestResponse;
import io.cerp.procurement.dto.UpdateProcurementRequest;
import java.util.List;
import java.util.UUID;

public interface ProcurementService {

    ProcurementRequestResponse createRequest(CreateProcurementRequest req);

    ProcurementRequestResponse updateRequest(UUID id, UpdateProcurementRequest req);

    ProcurementRequestResponse getRequest(UUID id);

    List<ProcurementRequestResponse> listRequests(String status);
}
