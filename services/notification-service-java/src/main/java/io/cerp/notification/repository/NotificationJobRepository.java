package io.cerp.notification.repository;

import io.cerp.notification.domain.NotificationJob;
import io.cerp.notification.domain.NotificationJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationJobRepository extends JpaRepository<NotificationJob, UUID> {

    List<NotificationJob> findByTenantId(String tenantId);

    List<NotificationJob> findByTenantIdAndStatus(String tenantId, NotificationJobStatus status);

    Page<NotificationJob> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<NotificationJob> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, NotificationJobStatus status, Pageable pageable);

    Page<NotificationJob> findByTenantId(String tenantId, Pageable pageable);

    Page<NotificationJob> findByTenantIdAndStatus(String tenantId, NotificationJobStatus status, Pageable pageable);

    @Query("SELECT j FROM NotificationJob j WHERE j.status = :status AND j.dueAt IS NOT NULL AND j.dueAt <= :now")
    List<NotificationJob> findExpiredJobs(@Param("status") NotificationJobStatus status, @Param("now") String now);

    @Query("SELECT j FROM NotificationJob j WHERE j.sourceService = :service AND j.sourceEntityId = :entityId")
    Optional<NotificationJob> findBySourceEntity(@Param("service") String service, @Param("entityId") UUID entityId);

    List<NotificationJob> findByTenantIdAndCreatedBy(String tenantId, UUID createdBy);

    List<NotificationJob> findByTenantIdAndSourceService(String tenantId, String sourceService);
}