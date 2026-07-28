package io.cerp.vendor.service;

import io.cerp.vendor.dto.VendorFavoriteEvent;
import io.cerp.vendor.model.VendorFavorite;
import io.cerp.vendor.repository.VendorFavoriteRepository;
import io.cerp.vendor.events.VendorEventsPublisher;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorFavoriteService {

    private static final Logger log = LoggerFactory.getLogger(VendorFavoriteService.class);

    private final VendorFavoriteRepository vendorFavoriteRepository;
    private final VendorEventsPublisher vendorEventsPublisher;

    public VendorFavoriteService(VendorFavoriteRepository vendorFavoriteRepository,
            VendorEventsPublisher vendorEventsPublisher) {
        this.vendorFavoriteRepository = vendorFavoriteRepository;
        this.vendorEventsPublisher = vendorEventsPublisher;
    }

    @Transactional
    public VendorFavorite addFavorite(UUID userId, UUID vendorId) {
        if (vendorFavoriteRepository.existsByUserIdAndVendorId(userId, vendorId)) {
            throw new IllegalArgumentException("Vendor is already favorited by this user");
        }
        VendorFavorite favorite = new VendorFavorite(userId, vendorId);
        VendorFavorite saved = vendorFavoriteRepository.save(favorite);

        // Publish event to Kafka
        vendorEventsPublisher.publish("vendor.favorite.created",
            new VendorFavoriteEvent(userId, vendorId, "CREATE"));

        log.info("Vendor favorite created: userId={}, vendorId={}", userId, vendorId);
        return saved;
    }

    @Transactional
    public void removeFavorite(UUID userId, UUID vendorId) {
        if (!vendorFavoriteRepository.existsByUserIdAndVendorId(userId, vendorId)) {
            throw new IllegalArgumentException("Vendor is not favorited by this user");
        }
        vendorFavoriteRepository.deleteByUserIdAndVendorId(userId, vendorId);

        // Publish event to Kafka
        vendorEventsPublisher.publish("vendor.favorite.removed",
            new VendorFavoriteEvent(userId, vendorId, "REMOVE"));

        log.info("Vendor favorite removed: userId={}, vendorId={}", userId, vendorId);
    }

    @Transactional(readOnly = true)
    public List<VendorFavorite> getMyFavorites(UUID userId) {
        return vendorFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<UUID> getFavoriteVendorIds(UUID userId) {
        return vendorFavoriteRepository.findVendorIdsByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(UUID userId, UUID vendorId) {
        return vendorFavoriteRepository.existsByUserIdAndVendorId(userId, vendorId);
    }
}
