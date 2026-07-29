package io.cerp.workflow.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for saga state persistence.
 *
 * <p>
 * Stores saga execution state including:
 * <ul>
 * <li>Saga metadata (type, tenant, status)</li>
 * <li>Step execution history</li>
 * <li>Last error information</li>
 * </ul>
 */
@Repository
public interface SagaRepository extends JpaRepository<SagaEntity, UUID> {

    /**
     * Find all active (non-completed, non-aborted) sagas for a tenant.
     */
    List<SagaEntity> findByTenantIdAndStatusIn(String tenantId, List<String> statuses);

    /**
     * Find sagas by type and status.
     */
    List<SagaEntity> findBySagaTypeAndStatus(String sagaType, String status);

    /**
     * Find sagas by type, status, and tenant.
     */
    @Query("SELECT s FROM SagaEntity s WHERE s.tenantId = :tenantId AND s.sagaType = :sagaType AND s.status IN :statuses")
    List<SagaEntity> findByTenantIdAndSagaTypeAndStatus(
            @Param("tenantId") String tenantId,
            @Param("sagaType") String sagaType,
            @Param("statuses") List<String> statuses);

    /**
     * Find sagas older than the given timestamp with given statuses. Used for finding stuck or timed-out sagas.
     */
    @Query("SELECT s FROM SagaEntity s WHERE s.updatedAt < :before AND s.status IN :statuses")
    List<SagaEntity> findStaleSagas(
            @Param("before") LocalDateTime before,
            @Param("statuses") List<String> statuses);

    /**
     * Count sagas by type.
     */
    @Query("SELECT s.sagaType, COUNT(s) FROM SagaEntity s WHERE s.tenantId = :tenantId GROUP BY s.sagaType")
    List<Object[]> countSagasByTypeForTenant(@Param("tenantId") String tenantId);
}
