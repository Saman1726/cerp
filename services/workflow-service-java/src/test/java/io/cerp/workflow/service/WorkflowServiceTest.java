package io.cerp.workflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.cerp.workflow.domain.Workflow;
import io.cerp.workflow.domain.WorkflowNote;
import io.cerp.workflow.domain.WorkflowStep;
import io.cerp.workflow.domain.WorkflowStepAttachment;
import io.cerp.workflow.repository.WorkflowNoteRepository;
import io.cerp.workflow.repository.WorkflowRepository;
import io.cerp.workflow.repository.WorkflowStepAttachmentRepository;
import io.cerp.workflow.repository.WorkflowStepRepository;
import io.cerp.workflow.repository.WorkflowTemplateRepository;
import io.cerp.workflow.security.IdentityPermissionClient;
import io.cerp.workflow.state.StateMachineEngine;
import io.cerp.workflow.state.WorkflowStateMachines;
import io.cerp.workflow.state.WorkflowStateStore;
import io.cerp.workflow.state.WorkflowStepStateStore;

/**
 * Characterizes every (status x transition method) combination for Workflow/WorkflowStep.
 *
 * <p>
 * This is the parity oracle for the StateMachineEngine migration: it must pass unchanged before and after,
 * except for the deliberate IllegalStateException -&gt; StateMachineEngine.StateTransitionException swap.
 */
class WorkflowServiceTest {

    private WorkflowRepository workflowRepository;
    private WorkflowTemplateRepository templateRepository;
    private WorkflowStepRepository workflowStepRepository;
    private WorkflowNoteRepository workflowNoteRepository;
    private WorkflowStepAttachmentRepository workflowStepAttachmentRepository;
    private IdentityPermissionClient identityPermissionClient;
    private WorkflowService service;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);
        templateRepository = mock(WorkflowTemplateRepository.class);
        workflowStepRepository = mock(WorkflowStepRepository.class);
        workflowNoteRepository = mock(WorkflowNoteRepository.class);
        workflowStepAttachmentRepository = mock(WorkflowStepAttachmentRepository.class);
        StateMachineEngine stateMachineEngine = new StateMachineEngine(WorkflowStateMachines.allMachines());
        WorkflowStateStore workflowStateStore = new WorkflowStateStore(workflowRepository);
        WorkflowStepStateStore workflowStepStateStore = new WorkflowStepStateStore(workflowStepRepository);
        identityPermissionClient = mock(IdentityPermissionClient.class);
        service = new WorkflowService(workflowRepository, templateRepository, workflowStepRepository,
            stateMachineEngine, workflowStateStore, workflowStepStateStore, workflowNoteRepository,
            workflowStepAttachmentRepository, identityPermissionClient);
        when(workflowRepository.save(any(Workflow.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workflowStepRepository.save(any(WorkflowStep.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workflowNoteRepository.save(any(WorkflowNote.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workflowStepAttachmentRepository.save(any(WorkflowStepAttachment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private record Fixture(Workflow workflow, WorkflowStep step) {
    }

    private Workflow newWorkflow(Workflow.WorkflowStatus status) {
        Workflow workflow = new Workflow("tenant-1", UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), 1, "CODE-1", "Test Workflow");
        workflow.setId(UUID.randomUUID());
        workflow.setStatus(status);
        when(workflowRepository.findById(workflow.getId())).thenReturn(Optional.of(workflow));
        when(workflowRepository.findByIdWithSteps(workflow.getId())).thenReturn(Optional.of(workflow));
        return workflow;
    }

    private Fixture stepFixture(WorkflowStep.StepStatus stepStatus) {
        Workflow workflow = newWorkflow(Workflow.WorkflowStatus.IN_PROGRESS);
        WorkflowStep step = new WorkflowStep(UUID.randomUUID(), 1, "STEP-1", "Step One");
        step.setId(UUID.randomUUID());
        step.setStatus(stepStatus);
        workflow.addStep(step);
        when(workflowStepRepository.findById(step.getId())).thenReturn(Optional.of(step));
        return new Fixture(workflow, step);
    }

    // ── Workflow-level transitions ──

    @ParameterizedTest
    @EnumSource(Workflow.WorkflowStatus.class)
    void start_onlyValidFromPlanned(Workflow.WorkflowStatus initial) {
        Workflow workflow = newWorkflow(initial);
        if (initial == Workflow.WorkflowStatus.PLANNED) {
            Workflow result = service.startWorkflow(workflow.getId());
            assertEquals(Workflow.WorkflowStatus.IN_PROGRESS, result.getStatus());
            assertEquals(LocalDate.now(), result.getStartDate());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class, () -> service.startWorkflow(workflow.getId()));
            assertEquals(initial, workflow.getStatus());
        }
    }

    @ParameterizedTest
    @EnumSource(Workflow.WorkflowStatus.class)
    void block_onlyValidFromInProgress(Workflow.WorkflowStatus initial) {
        Workflow workflow = newWorkflow(initial);
        if (initial == Workflow.WorkflowStatus.IN_PROGRESS) {
            Workflow result = service.blockWorkflow(workflow.getId(), "materials delayed");
            assertEquals(Workflow.WorkflowStatus.BLOCKED, result.getStatus());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class,
                () -> service.blockWorkflow(workflow.getId(), "materials delayed"));
        }
    }

    @ParameterizedTest
    @EnumSource(Workflow.WorkflowStatus.class)
    void unblock_onlyValidFromBlocked(Workflow.WorkflowStatus initial) {
        Workflow workflow = newWorkflow(initial);
        if (initial == Workflow.WorkflowStatus.BLOCKED) {
            Workflow result = service.unblockWorkflow(workflow.getId());
            assertEquals(Workflow.WorkflowStatus.IN_PROGRESS, result.getStatus());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class, () -> service.unblockWorkflow(workflow.getId()));
        }
    }

    @ParameterizedTest
    @EnumSource(Workflow.WorkflowStatus.class)
    void complete_onlyValidFromInProgress(Workflow.WorkflowStatus initial) {
        Workflow workflow = newWorkflow(initial);
        if (initial == Workflow.WorkflowStatus.IN_PROGRESS) {
            Workflow result = service.completeWorkflow(workflow.getId());
            assertEquals(Workflow.WorkflowStatus.COMPLETED, result.getStatus());
            assertEquals(LocalDate.now(), result.getEndDate());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class, () -> service.completeWorkflow(workflow.getId()));
        }
    }

    @ParameterizedTest
    @EnumSource(Workflow.WorkflowStatus.class)
    void approve_onlyValidFromCompleted(Workflow.WorkflowStatus initial) {
        Workflow workflow = newWorkflow(initial);
        UUID userId = UUID.randomUUID();
        if (initial == Workflow.WorkflowStatus.COMPLETED) {
            Workflow result = service.approveWorkflow(workflow.getId(), userId);
            assertEquals(Workflow.WorkflowStatus.APPROVED, result.getStatus());
            assertEquals(userId, result.getWhoHasApproved());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class, () -> service.approveWorkflow(workflow.getId(), userId));
        }
    }

    @ParameterizedTest
    @EnumSource(Workflow.WorkflowStatus.class)
    void reject_onlyValidFromCompleted(Workflow.WorkflowStatus initial) {
        Workflow workflow = newWorkflow(initial);
        UUID userId = UUID.randomUUID();
        if (initial == Workflow.WorkflowStatus.COMPLETED) {
            Workflow result = service.rejectWorkflow(workflow.getId(), userId);
            assertEquals(Workflow.WorkflowStatus.REJECTED, result.getStatus());
            assertEquals(userId, result.getWhoHasApproved());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class, () -> service.rejectWorkflow(workflow.getId(), userId));
        }
    }

    @ParameterizedTest
    @EnumSource(Workflow.WorkflowStatus.class)
    void cancel_onlyValidFromPlannedOrInProgress(Workflow.WorkflowStatus initial) {
        Workflow workflow = newWorkflow(initial);
        UUID userId = UUID.randomUUID();
        if (initial == Workflow.WorkflowStatus.PLANNED || initial == Workflow.WorkflowStatus.IN_PROGRESS) {
            Workflow result = service.cancelWorkflow(workflow.getId(), userId);
            assertEquals(Workflow.WorkflowStatus.CANCELED, result.getStatus());
            assertEquals(LocalDate.now(), result.getEndDate());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class, () -> service.cancelWorkflow(workflow.getId(), userId));
        }
    }

    // ── WorkflowStep-level transitions ──

    @ParameterizedTest
    @EnumSource(WorkflowStep.StepStatus.class)
    void startStep_onlyValidFromNotStarted(WorkflowStep.StepStatus initial) {
        Fixture fx = stepFixture(initial);
        UUID userId = UUID.randomUUID();
        if (initial == WorkflowStep.StepStatus.NOT_STARTED) {
            WorkflowStep result = service.startStep(fx.workflow().getId(), fx.step().getId(), userId);
            assertEquals(WorkflowStep.StepStatus.IN_PROGRESS, result.getStatus());
            assertEquals(userId, result.getAssigneeUserId());
            assertNotNull(result.getStartedAt());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class,
                () -> service.startStep(fx.workflow().getId(), fx.step().getId(), userId));
        }
    }

    @ParameterizedTest
    @EnumSource(WorkflowStep.StepStatus.class)
    void completeStep_onlyValidFromInProgress(WorkflowStep.StepStatus initial) {
        Fixture fx = stepFixture(initial);
        if (initial == WorkflowStep.StepStatus.IN_PROGRESS) {
            WorkflowStep result = service.completeStep(fx.workflow().getId(), fx.step().getId(),
                "evidence.pdf", "done", UUID.randomUUID());
            assertEquals(WorkflowStep.StepStatus.COMPLETED, result.getStatus());
            assertNotNull(result.getCompletedAt());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class,
                () -> service.completeStep(fx.workflow().getId(), fx.step().getId(), "e", "n", UUID.randomUUID()));
        }
    }

    @ParameterizedTest
    @EnumSource(WorkflowStep.StepStatus.class)
    void approveStep_onlyValidFromCompleted(WorkflowStep.StepStatus initial) {
        Fixture fx = stepFixture(initial);
        UUID userId = UUID.randomUUID();
        if (initial == WorkflowStep.StepStatus.COMPLETED) {
            WorkflowStep result = service.approveStep(fx.workflow().getId(), fx.step().getId(), "ok", userId);
            assertEquals(WorkflowStep.StepStatus.APPROVED, result.getStatus());
            assertEquals(userId, result.getApprovedBy());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class,
                () -> service.approveStep(fx.workflow().getId(), fx.step().getId(), "ok", userId));
        }
    }

    @ParameterizedTest
    @EnumSource(WorkflowStep.StepStatus.class)
    void rejectStep_onlyValidFromCompleted(WorkflowStep.StepStatus initial) {
        Fixture fx = stepFixture(initial);
        UUID userId = UUID.randomUUID();
        if (initial == WorkflowStep.StepStatus.COMPLETED) {
            WorkflowStep result = service.rejectStep(fx.workflow().getId(), fx.step().getId(), "no", userId);
            assertEquals(WorkflowStep.StepStatus.REJECTED, result.getStatus());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class,
                () -> service.rejectStep(fx.workflow().getId(), fx.step().getId(), "no", userId));
        }
    }

    @ParameterizedTest
    @EnumSource(WorkflowStep.StepStatus.class)
    void blockStep_onlyValidFromInProgress(WorkflowStep.StepStatus initial) {
        Fixture fx = stepFixture(initial);
        if (initial == WorkflowStep.StepStatus.IN_PROGRESS) {
            WorkflowStep result = service.blockStep(fx.workflow().getId(), fx.step().getId(), "reason");
            assertEquals(WorkflowStep.StepStatus.BLOCKED, result.getStatus());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class,
                () -> service.blockStep(fx.workflow().getId(), fx.step().getId(), "reason"));
        }
    }

    @ParameterizedTest
    @EnumSource(WorkflowStep.StepStatus.class)
    void unblockStep_onlyValidFromBlocked(WorkflowStep.StepStatus initial) {
        Fixture fx = stepFixture(initial);
        if (initial == WorkflowStep.StepStatus.BLOCKED) {
            WorkflowStep result = service.unblockStep(fx.workflow().getId(), fx.step().getId());
            assertEquals(WorkflowStep.StepStatus.IN_PROGRESS, result.getStatus());
        } else {
            assertThrows(StateMachineEngine.StateTransitionException.class,
                () -> service.unblockStep(fx.workflow().getId(), fx.step().getId()));
        }
    }

    // ── Side effects worth locking in explicitly ──

    @Test
    void approveStep_appendsToExistingNotes() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.COMPLETED);
        fx.step().setNotes("original evidence notes");

        WorkflowStep result = service.approveStep(fx.workflow().getId(), fx.step().getId(), "looks good",
            UUID.randomUUID());

        assertEquals("original evidence notes\n[Approved: looks good]", result.getNotes());
    }

    @Test
    void rejectStep_overwritesNotes() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.COMPLETED);
        fx.step().setNotes("original evidence notes");

        WorkflowStep result = service.rejectStep(fx.workflow().getId(), fx.step().getId(), "not sufficient",
            UUID.randomUUID());

        assertEquals("not sufficient", result.getNotes());
    }

    @Test
    void blockStep_appendsToExistingNotes() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.IN_PROGRESS);
        fx.step().setNotes("existing note");

        WorkflowStep result = service.blockStep(fx.workflow().getId(), fx.step().getId(), "waiting on materials");

        assertEquals("existing note\n[Blocked: waiting on materials]", result.getNotes());
    }

    @Test
    void completeStep_recalculatesWorkflowProgress() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.IN_PROGRESS);

        service.completeStep(fx.workflow().getId(), fx.step().getId(), "evidence.pdf", "done", UUID.randomUUID());

        assertEquals(0, new BigDecimal("100.00").compareTo(fx.workflow().getProgressPercent()));
    }

    @Test
    void rejectStep_doesNotRecalculateWorkflowProgress() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.COMPLETED);
        fx.workflow().setProgressPercent(new BigDecimal("42.00"));

        service.rejectStep(fx.workflow().getId(), fx.step().getId(), "insufficient", UUID.randomUUID());

        assertEquals(0, new BigDecimal("42.00").compareTo(fx.workflow().getProgressPercent()));
    }

    // ── Notes ──

    @Test
    void addNote_workflowLevel_persistsWithNullStep() {
        Workflow workflow = newWorkflow(Workflow.WorkflowStatus.IN_PROGRESS);
        UUID authorId = UUID.randomUUID();

        WorkflowNote result = service.addNote(workflow.getId(), null, authorId, "materials on site",
            WorkflowNote.NoteType.GENERAL);

        assertNull(result.getStep());
        assertEquals(authorId, result.getAuthorId());
        assertEquals("materials on site", result.getNote());
        assertEquals(WorkflowNote.NoteType.GENERAL, result.getType());
    }

    @Test
    void addNote_stepLevel_persistsWithStep() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.IN_PROGRESS);
        UUID authorId = UUID.randomUUID();

        WorkflowNote result = service.addNote(fx.workflow().getId(), fx.step().getId(), authorId,
            "delayed by weather", WorkflowNote.NoteType.RISK);

        assertEquals(fx.step(), result.getStep());
        assertEquals(WorkflowNote.NoteType.RISK, result.getType());
    }

    @Test
    void addNote_stepNotInWorkflow_throws() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.IN_PROGRESS);
        UUID unrelatedStepId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class,
            () -> service.addNote(fx.workflow().getId(), unrelatedStepId, UUID.randomUUID(), "note",
                WorkflowNote.NoteType.GENERAL));
    }

    // ── Step Attachments ──

    @Test
    void addAttachment_persistsMetadataOnStep() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.IN_PROGRESS);
        UUID attachmentId = UUID.randomUUID();
        UUID uploadedBy = UUID.randomUUID();
        when(workflowStepAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        WorkflowStepAttachment result = service.addAttachment(fx.workflow().getId(), fx.step().getId(),
            attachmentId, uploadedBy, WorkflowStepAttachment.AttachmentKind.PHOTO,
            "tenants/t/projects/p/site.jpg", "site.jpg", "image/jpeg", 245_000L);

        assertEquals(attachmentId, result.getId());
        assertEquals(fx.step(), result.getStep());
        assertEquals(WorkflowStepAttachment.AttachmentKind.PHOTO, result.getKind());
        verify(workflowStepAttachmentRepository, times(1)).save(any(WorkflowStepAttachment.class));
    }

    @Test
    void addAttachment_retriedWithSameId_isIdempotent() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.IN_PROGRESS);
        UUID attachmentId = UUID.randomUUID();
        UUID uploadedBy = UUID.randomUUID();
        when(workflowStepAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        WorkflowStepAttachment first = service.addAttachment(fx.workflow().getId(), fx.step().getId(),
            attachmentId, uploadedBy, WorkflowStepAttachment.AttachmentKind.DOCUMENT,
            "tenants/t/projects/p/spec.pdf", "spec.pdf", "application/pdf", 900_000L);

        // Simulate the offline sync queue retrying after a dropped connection: the row now exists.
        when(workflowStepAttachmentRepository.findById(attachmentId)).thenReturn(Optional.of(first));

        WorkflowStepAttachment second = service.addAttachment(fx.workflow().getId(), fx.step().getId(),
            attachmentId, uploadedBy, WorkflowStepAttachment.AttachmentKind.DOCUMENT,
            "tenants/t/projects/p/spec.pdf", "spec.pdf", "application/pdf", 900_000L);

        assertSame(first, second);
        verify(workflowStepAttachmentRepository, times(1)).save(any(WorkflowStepAttachment.class));
    }

    @Test
    void addAttachment_stepNotInWorkflow_throws() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.IN_PROGRESS);
        UUID attachmentId = UUID.randomUUID();
        UUID unrelatedStepId = UUID.randomUUID();
        when(workflowStepAttachmentRepository.findById(attachmentId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.addAttachment(fx.workflow().getId(), unrelatedStepId, attachmentId,
                UUID.randomUUID(), WorkflowStepAttachment.AttachmentKind.PHOTO,
                "key", "file.jpg", "image/jpeg", 1L));
        verify(workflowStepAttachmentRepository, never()).save(any(WorkflowStepAttachment.class));
    }

    @Test
    void deleteAttachment_removesRowAndReturnsObjectKey() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.IN_PROGRESS);
        UUID attachmentId = UUID.randomUUID();
        WorkflowStepAttachment attachment = new WorkflowStepAttachment(attachmentId, fx.workflow(), fx.step(),
            UUID.randomUUID(), WorkflowStepAttachment.AttachmentKind.PHOTO, "the-key", "a.jpg", "image/jpeg", 1L);
        when(workflowStepAttachmentRepository.findByIdAndWorkflowIdAndStepId(
            attachmentId, fx.workflow().getId(), fx.step().getId())).thenReturn(Optional.of(attachment));

        String objectKey = service.deleteAttachment(fx.workflow().getId(), fx.step().getId(), attachmentId);

        assertEquals("the-key", objectKey);
        verify(workflowStepAttachmentRepository, times(1)).delete(attachment);
    }

    @Test
    void deleteAttachment_notFoundForThisStepOrWorkflow_throws() {
        Fixture fx = stepFixture(WorkflowStep.StepStatus.IN_PROGRESS);
        UUID attachmentId = UUID.randomUUID();
        when(workflowStepAttachmentRepository.findByIdAndWorkflowIdAndStepId(
            attachmentId, fx.workflow().getId(), fx.step().getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
            () -> service.deleteAttachment(fx.workflow().getId(), fx.step().getId(), attachmentId));
        verify(workflowStepAttachmentRepository, never()).delete(any(WorkflowStepAttachment.class));
    }
}
