package io.cerp.workflow.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SagaNoteRepository extends JpaRepository<SagaNoteEntity, UUID> {

    List<SagaNoteEntity> findBySagaIdOrderByCreatedAtDesc(String sagaId);

    long countBySagaId(String sagaId);

    /** Batched count for decorating a list of sagas at once, avoiding N+1. */
    @Query("SELECT n.sagaId, COUNT(n) FROM SagaNoteEntity n WHERE n.sagaId IN :sagaIds GROUP BY n.sagaId")
    List<Object[]> countBySagaIdIn(@Param("sagaIds") List<String> sagaIds);
}
