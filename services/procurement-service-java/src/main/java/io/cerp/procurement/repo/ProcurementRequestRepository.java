package io.cerp.procurement.repo;

import io.cerp.procurement.model.ProcurementRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcurementRequestRepository extends JpaRepository<ProcurementRequest, UUID> {
    List<ProcurementRequest> findByStatus(String status);
}
