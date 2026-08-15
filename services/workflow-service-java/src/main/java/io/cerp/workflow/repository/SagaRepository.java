package io.cerp.workflow.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
 *
 * <p>The primary key (id) is the saga identifier (sagaId), which ensures JPA
 * performs UPDATE instead of INSERT when saving existing saga state.
 */
@Repository
public interface SagaRepository extends JpaRepository<SagaEntity, String> {

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

        /**
         * Find sagas by user_id (legacy field).
         */
        List<SagaEntity> findByUserId(String userId);

        /**
         * Find sagas created by a specific user.
         */
        List<SagaEntity> findByCreatedBy(String createdBy);

        /**
         * Find sagas assigned to a specific user.
         */
        List<SagaEntity> findByAssignedTo(String assignedTo);

        /**
         * Find sagas created by or assigned to a user.
         */
        List<SagaEntity> findByCreatedByOrAssignedTo(String createdBy, String assignedTo);

        /**
         * Find sagas where partyIds JSON array contains the given party ID.
         */
        @Query("SELECT s FROM SagaEntity s WHERE s.partyIds LIKE %:partyId% AND s.deleted = false")
        List<SagaEntity> findByPartyIdsContaining(@Param("partyId") String partyId);

        /**
         * Find sagas where user is creator, assignee, or party member.
         */
        @Query("SELECT s FROM SagaEntity s WHERE " +
                "(s.createdBy = :userId OR s.assignedTo = :userId OR s.partyIds LIKE %:partyId%) AND s.deleted = false")
        List<SagaEntity> findByUserAccess(@Param("userId") String userId,
                                           @Param("partyId") String partyId);

        /**
         * Find sagas by project_id.
         */
        List<SagaEntity> findByProjectIdAndDeletedFalse(String projectId);

        /**
         * Find saga by its ID (saga identifier).
         */
        Optional<SagaEntity> findById(String sagaId);

        /**
         * Find saga by ID and tenant.
         */
        Optional<SagaEntity> findByIdAndTenantId(String sagaId, String tenantId);

        /**
         * Update only the status and updatedAt fields for a saga by ID.
         * This preserves all other fields including user_id, project_id, steps_json, current_step, and data_json.
         *
         * @param sagaId    the saga ID (primary key)
         * @param status    the new status value
         * @param updatedAt the new updated timestamp
         * @return number of rows affected
         */
        @Modifying
        @Transactional
        @Query("UPDATE SagaEntity s SET s.status = :status, s.updatedAt = :updatedAt WHERE s.id = :sagaId")
        int updateStatusOnlyById(@Param("sagaId") String sagaId,
                                 @Param("status") String status,
                                 @Param("updatedAt") LocalDateTime updatedAt);

        /**
         * Archive (soft-delete) every saga tied to a Project - called when the Project itself is
         * deleted. Does not touch bidding.bid_wallets or bidding.bid_transactions.
         *
         * @param projectId the project ID
         * @return number of rows affected
         */
        @Modifying
        @Transactional
        @Query("UPDATE SagaEntity s SET s.deleted = true WHERE s.projectId = :projectId")
        int archiveByProjectId(@Param("projectId") String projectId);

        /**
         * Find delivery sagas linked to a specific procurement saga by checking data_json.
         */
        @Query(value = "SELECT * FROM workflow.saga_events s WHERE s.saga_type = 'DeliverySaga' AND s.data_json LIKE :procurementSagaId AND s.deleted = false AND s.status != 'COMPLETED'", nativeQuery = true)
        List<SagaEntity> findDeliverySagasByProcurementSagaId(@Param("procurementSagaId") String procurementSagaId);

        /**
         * Count non-archived sagas tied to a Project.
         */
        long countByProjectIdAndDeletedFalse(String projectId);
}