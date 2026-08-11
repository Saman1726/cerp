package io.cerp.workflow.saga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.cerp.workflow.repository.SagaAttachmentEntity;
import io.cerp.workflow.repository.SagaAttachmentRepository;
import io.cerp.workflow.repository.SagaNoteEntity;
import io.cerp.workflow.repository.SagaNoteRepository;
import io.cerp.workflow.repository.SagaRepository;
import io.cerp.workflow.service.EventPublisher;

/**
 * Covers only the saga-level notes/attachments methods added to {@link DefaultSagaOrchestrator}
 * -- the pre-existing orchestration/retry/compensation logic on this class has no test coverage
 * today and rewriting that is out of scope here.
 */
class DefaultSagaOrchestratorTest {

    private SagaRepository sagaRepository;
    private SagaNoteRepository sagaNoteRepository;
    private SagaAttachmentRepository sagaAttachmentRepository;
    private DefaultSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        sagaRepository = mock(SagaRepository.class);
        EventPublisher eventPublisher = mock(EventPublisher.class);
        sagaNoteRepository = mock(SagaNoteRepository.class);
        sagaAttachmentRepository = mock(SagaAttachmentRepository.class);
        orchestrator = new DefaultSagaOrchestrator(sagaRepository, eventPublisher, sagaNoteRepository, sagaAttachmentRepository);
        when(sagaNoteRepository.save(any(SagaNoteEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sagaAttachmentRepository.save(any(SagaAttachmentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── Notes ──

    @Test
    void addNote_persistsNote() {
        String sagaId = UUID.randomUUID().toString();
        UUID authorId = UUID.randomUUID();
        when(sagaRepository.existsById(sagaId)).thenReturn(true);

        SagaNoteEntity result = orchestrator.addNote(sagaId, authorId, "materials delayed", SagaNoteEntity.NoteType.RISK);

        assertEquals(sagaId, result.getSagaId());
        assertEquals(authorId, result.getAuthorId());
        assertEquals(SagaNoteEntity.NoteType.RISK, result.getType());
        verify(sagaNoteRepository, times(1)).save(any(SagaNoteEntity.class));
    }

    @Test
    void addNote_sagaNotFound_throws() {
        String sagaId = UUID.randomUUID().toString();
        when(sagaRepository.existsById(sagaId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> orchestrator.addNote(sagaId, UUID.randomUUID(), "note", SagaNoteEntity.NoteType.GENERAL));
        verify(sagaNoteRepository, never()).save(any(SagaNoteEntity.class));
    }

    // ── Attachments ──

    @Test
    void addAttachment_persistsMetadata() {
        String sagaId = UUID.randomUUID().toString();
        UUID attachmentId = UUID.randomUUID();
        when(sagaAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());
        when(sagaRepository.existsById(sagaId)).thenReturn(true);

        SagaAttachmentEntity result = orchestrator.addAttachment(sagaId, attachmentId, UUID.randomUUID(),
            SagaAttachmentEntity.AttachmentKind.PHOTO, "tenants/t/projects/p/site.jpg", "site.jpg", "image/jpeg", 245_000L);

        assertEquals(attachmentId, result.getId());
        assertEquals(sagaId, result.getSagaId());
        verify(sagaAttachmentRepository, times(1)).save(any(SagaAttachmentEntity.class));
    }

    @Test
    void addAttachment_retriedWithSameId_isIdempotent() {
        String sagaId = UUID.randomUUID().toString();
        UUID attachmentId = UUID.randomUUID();
        UUID uploadedBy = UUID.randomUUID();
        when(sagaAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());
        when(sagaRepository.existsById(sagaId)).thenReturn(true);

        SagaAttachmentEntity first = orchestrator.addAttachment(sagaId, attachmentId, uploadedBy,
            SagaAttachmentEntity.AttachmentKind.DOCUMENT, "tenants/t/projects/p/spec.pdf", "spec.pdf", "application/pdf", 900_000L);

        // Simulate the offline sync queue retrying after a dropped connection: the row now exists.
        when(sagaAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(first));

        SagaAttachmentEntity second = orchestrator.addAttachment(sagaId, attachmentId, uploadedBy,
            SagaAttachmentEntity.AttachmentKind.DOCUMENT, "tenants/t/projects/p/spec.pdf", "spec.pdf", "application/pdf", 900_000L);

        assertSame(first, second);
        verify(sagaAttachmentRepository, times(1)).save(any(SagaAttachmentEntity.class));
    }

    @Test
    void addAttachment_sagaNotFound_throws() {
        String sagaId = UUID.randomUUID().toString();
        UUID attachmentId = UUID.randomUUID();
        when(sagaAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());
        when(sagaRepository.existsById(sagaId)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> orchestrator.addAttachment(sagaId, attachmentId, UUID.randomUUID(),
                SagaAttachmentEntity.AttachmentKind.PHOTO, "key", "file.jpg", "image/jpeg", 1L));
        verify(sagaAttachmentRepository, never()).save(any(SagaAttachmentEntity.class));
    }

    @Test
    void deleteAttachment_removesRowAndReturnsObjectKey() {
        String sagaId = UUID.randomUUID().toString();
        UUID attachmentId = UUID.randomUUID();
        SagaAttachmentEntity attachment = new SagaAttachmentEntity(attachmentId, sagaId, UUID.randomUUID(),
            SagaAttachmentEntity.AttachmentKind.PHOTO, "the-key", "a.jpg", "image/jpeg", 1L);
        when(sagaAttachmentRepository.findByIdAndSagaId(attachmentId, sagaId)).thenReturn(Optional.of(attachment));

        String objectKey = orchestrator.deleteAttachment(sagaId, attachmentId);

        assertEquals("the-key", objectKey);
        verify(sagaAttachmentRepository, times(1)).delete(attachment);
    }

    @Test
    void deleteAttachment_notFound_throws() {
        String sagaId = UUID.randomUUID().toString();
        UUID attachmentId = UUID.randomUUID();
        when(sagaAttachmentRepository.findByIdAndSagaId(attachmentId, sagaId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> orchestrator.deleteAttachment(sagaId, attachmentId));
        verify(sagaAttachmentRepository, never()).delete(any(SagaAttachmentEntity.class));
    }
}
