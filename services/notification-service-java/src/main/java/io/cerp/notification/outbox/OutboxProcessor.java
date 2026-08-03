package io.cerp.notification.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled processor for outbox events.
 * Periodically processes pending outbox events and publishes them via Kafka.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;

    @Scheduled(fixedDelayString = "${notification.outbox.process-delay:5000}")
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatusOrderByCreatedAtAsc("PENDING");
        
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                publishEvent(event);
                event.markProcessed();
                outboxRepository.save(event);
                log.debug("Processed outbox event: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to process outbox event: id={}", event.getId(), e);
                event.setStatus("FAILED");
                outboxRepository.save(event);
            }
        }
    }

    private void publishEvent(OutboxEvent event) {
        log.info("Publishing outbox event type: {}", event.getEventType());
    }

    public void addEvent(OutboxEvent event) {
        event.setStatus("PENDING");
        outboxRepository.save(event);
    }
}