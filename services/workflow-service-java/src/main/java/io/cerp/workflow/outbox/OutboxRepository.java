package io.cerp.workflow.outbox;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA repository for the OutboxEvent entity.
 *
 * <p>
 * Provides methods to persist and query outbox events. The outbox processor uses {@link #findPendingEvents(int)} to retrieve events that need to be published to the
 * message broker.
 */
@Repository
public class OutboxRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Save a new outbox event.
     */
    public void save(OutboxEvent event) {
        entityManager.persist(event);
    }

    /**
     * Find pending events that need to be published.
     *
     * @param batchSize
     *            maximum number of events to retrieve
     * @return list of pending outbox events
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPendingEvents(int batchSize) {
        Query query = entityManager.createQuery("SELECT e FROM OutboxEvent e WHERE e.status = OutboxEvent.OutboxStatus.PENDING");
        query.setMaxResults(batchSize);
        @SuppressWarnings("unchecked")
        List<OutboxEvent> events = query.getResultList();
        return events != null ? events : List.of();
    }

    /**
     * Find stale pending events that have not been processed within the timeout.
     *
     * @param timeoutMinutes
     *            minutes before considering an event stale
     * @param batchSize
     *            maximum number of events to retrieve
     * @return list of stale outbox events
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findStalePendingEvents(int timeoutMinutes, int batchSize) {
        Instant cutoff = Instant.now().minusMillis(timeoutMinutes * 60_000L);
        Query query = entityManager.createQuery(
            "SELECT e FROM OutboxEvent e WHERE e.status = OutboxEvent.OutboxStatus.PENDING AND e.createdAt < :cutoff");
        query.setParameter("cutoff", cutoff);
        query.setMaxResults(batchSize);
        @SuppressWarnings("unchecked")
        List<OutboxEvent> events = query.getResultList();
        return events != null ? events : List.of();
    }

    /**
     * Mark an event as published.
     */
    @Transactional
    public void markAsPublished(OutboxEvent event) {
        event.markAsPublished();
        entityManager.merge(event);
    }

    /**
     * Mark an event as failed.
     */
    @Transactional
    public void markAsFailed(OutboxEvent event, String errorMessage) {
        event.markAsFailed(errorMessage);
        entityManager.merge(event);
    }

    /**
     * Increment the retry count for an event.
     */
    @Transactional
    public void incrementRetryCount(OutboxEvent event) {
        event.incrementRetryCount();
        entityManager.merge(event);
    }

    /**
     * Get the count of pending events.
     */
    @Transactional(readOnly = true)
    public long countPendingEvents() {
        Query query = entityManager.createQuery("SELECT COUNT(e) FROM OutboxEvent e WHERE e.status = OutboxEvent.OutboxStatus.PENDING");
        return (Long) query.getSingleResult();
    }
}
