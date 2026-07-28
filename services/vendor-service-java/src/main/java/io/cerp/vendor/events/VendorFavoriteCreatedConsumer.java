package io.cerp.vendor.events;

import io.cerp.vendor.dto.VendorEventEnvelope;
import io.cerp.vendor.model.VendorFavorite;
import io.cerp.vendor.repository.VendorFavoriteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes vendor favorite events from Kafka and persists the favorite relation to the database. This ensures eventual consistency between the event log and the
 * favorites table.
 */
@Component
public class VendorFavoriteCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(VendorFavoriteCreatedConsumer.class);

    private static final String FAVORITE_CREATED_TYPE = "vendor.favorite.created";
    private static final String FAVORITE_REMOVED_TYPE = "vendor.favorite.removed";

    private final VendorFavoriteRepository vendorFavoriteRepository;

    public VendorFavoriteCreatedConsumer(VendorFavoriteRepository vendorFavoriteRepository) {
        this.vendorFavoriteRepository = vendorFavoriteRepository;
    }

    @KafkaListener(topics = "${app.topics.vendor:supply.vendor.v1}", groupId = "${app.kafka.consumer.group-id:vendor-service}", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleVendorFavoriteEvent(VendorEventEnvelope<Object> envelope) {
        if (envelope == null || envelope.getPayload() == null) {
            log.warn("Received invalid vendor event envelope: {}", envelope);
            return;
        }

        Object payload = envelope.getPayload();
        String eventType = envelope.getType();

        if (FAVORITE_CREATED_TYPE.equals(eventType)) {
            handleFavoriteCreated(payload);
        } else if (FAVORITE_REMOVED_TYPE.equals(eventType)) {
            handleFavoriteRemoved(payload);
        } else {
            log.warn("Unknown vendor event type: {}", eventType);
        }
    }

    private void handleFavoriteCreated(Object payload) {
        if (!(payload instanceof io.cerp.vendor.dto.VendorFavoriteEvent event)) {
            log.warn("Expected VendorFavoriteEvent for CREATE, got: {}", payload.getClass().getName());
            return;
        }
        if (event.userId() == null || event.vendorId() == null) {
            log.warn("Received invalid vendor favorite CREATE event: userId={}, vendorId={}", event.userId(), event.vendorId());
            return;
        }
        if (vendorFavoriteRepository.existsByUserIdAndVendorId(event.userId(), event.vendorId())) {
            log.info("Favorite already exists, skipping: userId={}, vendorId={}", event.userId(), event.vendorId());
            return;
        }
        VendorFavorite favorite = new VendorFavorite(event.userId(), event.vendorId());
        vendorFavoriteRepository.save(favorite);
        log.info("Persisted vendor favorite from event: userId={}, vendorId={}", event.userId(), event.vendorId());
    }

    private void handleFavoriteRemoved(Object payload) {
        if (!(payload instanceof io.cerp.vendor.dto.VendorFavoriteEvent event)) {
            log.warn("Expected VendorFavoriteEvent for REMOVE, got: {}", payload.getClass().getName());
            return;
        }
        if (event.userId() == null || event.vendorId() == null) {
            log.warn("Received invalid vendor favorite REMOVE event: userId={}, vendorId={}", event.userId(), event.vendorId());
            return;
        }
        if (!vendorFavoriteRepository.existsByUserIdAndVendorId(event.userId(), event.vendorId())) {
            log.info("Favorite does not exist, skipping removal: userId={}, vendorId={}", event.userId(), event.vendorId());
            return;
        }
        vendorFavoriteRepository.deleteByUserIdAndVendorId(event.userId(), event.vendorId());
        log.info("Persisted vendor favorite removal from event: userId={}, vendorId={}", event.userId(), event.vendorId());
    }
}
