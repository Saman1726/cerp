package io.cerp.notification.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for outbox events.
 */
public interface OutboxRepository extends JpaRepository<OutboxEvent, java.util.UUID> {
    
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status);
    
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.status = 'PROCESSED', o.processedAt = CURRENT_TIMESTAMP WHERE o.id IN :ids")
    void markAsProcessed(@Param("ids") List<java.util.UUID> ids);
    
    @Modifying
    @Query("UPDATE OutboxEvent o SET o.status = 'PROCESSING', o.updatedAt = CURRENT_TIMESTAMP WHERE o.status = 'PENDING'")
    int markProcessing();
}