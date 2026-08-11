package io.cerp.workflow.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SagaAttachmentRepository extends JpaRepository<SagaAttachmentEntity, UUID> {

    List<SagaAttachmentEntity> findBySagaIdOrderByCreatedAtDesc(String sagaId);

    Optional<SagaAttachmentEntity> findByIdAndSagaId(UUID id, String sagaId);

    long countBySagaId(String sagaId);

    /** Batched count for decorating a list of sagas at once, avoiding N+1. */
    @Query("SELECT a.sagaId, COUNT(a) FROM SagaAttachmentEntity a WHERE a.sagaId IN :sagaIds GROUP BY a.sagaId")
    List<Object[]> countBySagaIdIn(@Param("sagaIds") List<String> sagaIds);
}
