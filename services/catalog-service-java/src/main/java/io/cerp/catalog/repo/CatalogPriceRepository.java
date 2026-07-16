package io.cerp.catalog.repo;

import io.cerp.catalog.model.CatalogPrice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatalogPriceRepository extends JpaRepository<CatalogPrice, UUID> {

    List<CatalogPrice> findByItem_Id(UUID itemId);

    Optional<CatalogPrice> findFirstByItem_IdAndValidToIsNullOrderByValidFromDesc(UUID itemId);

    Optional<CatalogPrice> findTopByItem_IdOrderByValidFromDesc(UUID itemId);
}
