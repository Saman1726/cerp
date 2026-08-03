package io.cerp.notification.repository;

import io.cerp.notification.domain.Notification;
import io.cerp.notification.domain.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId")
    Page<Notification> findByRecipientId(@Param("tenantId") String tenantId, @Param("recipientId") UUID recipientId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.status = :status")
    Page<Notification> findByRecipientIdAndStatus(@Param("tenantId") String tenantId, @Param("recipientId") UUID recipientId, @Param("status") NotificationStatus status, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.job.id = :jobId")
    Optional<Notification> findByRecipientIdAndJobId(@Param("tenantId") String tenantId, @Param("recipientId") UUID recipientId, @Param("jobId") UUID jobId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.status = 'PENDING'")
    long countPendingNotifications(@Param("tenantId") String tenantId, @Param("recipientId") UUID recipientId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.status = 'READ'")
    long countReadNotifications(@Param("tenantId") String tenantId, @Param("recipientId") UUID recipientId);

    java.util.List<Notification> findByJobId(UUID jobId);

    java.util.List<Notification> findByTenantIdAndStatus(String tenantId, NotificationStatus status);

    Page<Notification> findByTenantIdAndRecipientId(String tenantId, UUID recipientId, Pageable pageable);

    Page<Notification> findByTenantId(String tenantId, Pageable pageable);
}