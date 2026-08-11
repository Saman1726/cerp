'use client';

import Link from 'next/link';
import { useEffect, useState, useCallback, useRef } from 'react';
import type {
  Project,
  Workflow,
  WorkflowInstanceStep,
  WorkflowResponse,
  WorkflowStatus,
  WorkflowStepStatus,
  WorkflowNote,
} from '@cerp/shared-types';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Modal } from '../components/ui/modal';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { StepRowActions } from './StepRowActions';
import { useAttachmentSync } from './step-attachments';

const WORKFLOW_STATUS_VARIANTS: Record<
  WorkflowStatus,
  'default' | 'secondary' | 'success' | 'destructive' | 'outline'
> = {
  PLANNED: 'outline',
  ACTIVE: 'default',
  PAUSED: 'secondary',
  COMPLETED: 'success',
  APPROVED: 'success',
  REJECTED: 'destructive',
  CANCELLED: 'destructive',
};

const WORKFLOW_STEP_STATUS_VARIANTS: Record<
  WorkflowStepStatus,
  'default' | 'secondary' | 'success' | 'destructive' | 'outline'
> = {
  PENDING: 'secondary',
  IN_PROGRESS: 'default',
  BLOCKED: 'outline',
  COMPLETED: 'success',
  APPROVED: 'success',
  REJECTED: 'destructive',
};

function formatStepStatus(status: WorkflowStepStatus): string {
  return status.replace(/_/g, ' ');
}

interface SubProjectContext {
  projectId: string;
  projectCode: string;
  projectName: string;
  subProjectCode: string;
  subProjectName: string;
}

export function ConstructionMonitoringPage() {
  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [contextBySubproject, setContextBySubproject] = useState<Record<string, SubProjectContext>>(
    {},
  );
  const [loading, setLoading] = useState(true);

  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string | null>(null);
  const [selectedWorkflow, setSelectedWorkflow] = useState<Workflow | null>(null);
  const [selectedWorkflowAllSteps, setSelectedWorkflowAllSteps] = useState<WorkflowInstanceStep[]>(
    [],
  );
  const [notes, setNotes] = useState<WorkflowNote[]>([]);
  const [noteContent, setNoteContent] = useState('');
  const [notesLoading, setNotesLoading] = useState(false);
  const [stepModalOpen, setStepModalOpen] = useState(false);
  const [currentStep, setCurrentStep] = useState<WorkflowInstanceStep | null>(null);
  const [evidenceUrls, setEvidenceUrls] = useState('');
  const [approvalNotes, setApprovalNotes] = useState('');

  // Owned once here (not per step row): the upload queue is one shared IndexedDB store across
  // every step on the page, so a single background-sync listener must drain it, not one per row.
  const { syncNow: syncAttachmentsNow, isSyncing: attachmentsSyncing } = useAttachmentSync();

  const fetchWorkflows = useCallback(async () => {
    try {
      setLoading(true);
      const res = await fetch('/api/workflows');
      if (!res.ok) throw new Error('Failed to fetch workflows');
      const data: Workflow[] = await res.json();
      setWorkflows(data);
    } catch {
      // Silently fail — matches the subproject workflows tab's tolerance for an empty result
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchProjectContext = useCallback(async () => {
    try {
      const res = await fetch('/api/projects');
      if (!res.ok) return;
      const projects: Project[] = await res.json();
      const map: Record<string, SubProjectContext> = {};
      for (const project of projects) {
        for (const subProject of project.subProjects ?? []) {
          map[subProject.id] = {
            projectId: project.id,
            projectCode: project.code,
            projectName: project.name,
            subProjectCode: subProject.code,
            subProjectName: subProject.name,
          };
        }
      }
      setContextBySubproject(map);
    } catch {
      // Silently fail — the list still renders, just without project/subproject labels
    }
  }, []);

  useEffect(() => {
    /* eslint-disable react-hooks/set-state-in-effect */
    fetchWorkflows();
    fetchProjectContext();
    /* eslint-enable react-hooks/set-state-in-effect */
  }, [fetchWorkflows, fetchProjectContext]);

  const fetchWorkflowDetail = useCallback(async (workflowId: string) => {
    try {
      const res = await fetch(`/api/workflows/${workflowId}`);
      if (!res.ok) throw new Error('Failed to fetch workflow detail');
      const data: WorkflowResponse = await res.json();
      setSelectedWorkflow(data.workflow);
      setSelectedWorkflowAllSteps(data.steps ?? []);
      setNotes(data.notes ?? []);
    } catch {
      alert('Failed to load workflow details');
    }
  }, []);

  // Refetches whenever a step-attachment sync round finishes (isSyncing true -> false), not just
  // right after enqueueing -- this also covers uploads that only complete later, e.g. once the
  // online-event listener fires after being offline, so the attachment icon/count update on
  // their own without the user needing to reload the page.
  const wasSyncingAttachmentsRef = useRef(false);
  useEffect(() => {
    if (wasSyncingAttachmentsRef.current && !attachmentsSyncing && selectedWorkflowId) {
      fetchWorkflowDetail(selectedWorkflowId);
    }
    wasSyncingAttachmentsRef.current = attachmentsSyncing;
  }, [attachmentsSyncing, selectedWorkflowId, fetchWorkflowDetail]);

  const fetchNotes = useCallback(async (workflowId: string) => {
    try {
      setNotesLoading(true);
      const res = await fetch(`/api/workflows/${workflowId}/notes`);
      if (!res.ok) throw new Error('Failed to fetch notes');
      const data: WorkflowNote[] = await res.json();
      setNotes(data);
    } catch {
      // Silently fail
    } finally {
      setNotesLoading(false);
    }
  }, []);

  const handleStartStep = async (workflowId: string, stepId: string) => {
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/start`, {
        method: 'POST',
      });
      if (!res.ok) throw new Error('Failed to start step');
      fetchWorkflows();
      if (selectedWorkflowId) fetchWorkflowDetail(selectedWorkflowId);
    } catch {
      alert('Failed to start step');
    }
  };

  const handleCompleteStepFinal = async (workflowId: string, stepId: string) => {
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/complete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          evidenceUrls: evidenceUrls
            .split(',')
            .map((u) => u.trim())
            .filter((u) => u),
          approvalNotes: approvalNotes || undefined,
        }),
      });
      if (!res.ok) throw new Error('Failed to complete step');
      fetchWorkflows();
      if (selectedWorkflowId) fetchWorkflowDetail(selectedWorkflowId);
      setStepModalOpen(false);
      setEvidenceUrls('');
      setApprovalNotes('');
    } catch {
      alert('Failed to complete step');
    }
  };

  const handleApproveStep = async (workflowId: string, stepId: string) => {
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ notes: approvalNotes || undefined }),
      });
      if (!res.ok) throw new Error('Failed to approve step');
      fetchWorkflows();
      if (selectedWorkflowId) fetchWorkflowDetail(selectedWorkflowId);
      setStepModalOpen(false);
      setApprovalNotes('');
    } catch {
      alert('Failed to approve step');
    }
  };

  const handleRejectStep = async (workflowId: string, stepId: string) => {
    if (!approvalNotes.trim()) {
      alert('Please provide a reason for rejecting this step.');
      return;
    }
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ notes: approvalNotes }),
      });
      if (!res.ok) throw new Error('Failed to reject step');
      fetchWorkflows();
      if (selectedWorkflowId) fetchWorkflowDetail(selectedWorkflowId);
      setStepModalOpen(false);
      setApprovalNotes('');
    } catch {
      alert('Failed to reject step');
    }
  };

  const handleRestartStep = async (workflowId: string, stepId: string) => {
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/restart`, {
        method: 'POST',
      });
      if (!res.ok) throw new Error('Failed to restart step');
      fetchWorkflows();
      if (selectedWorkflowId) fetchWorkflowDetail(selectedWorkflowId);
      setStepModalOpen(false);
    } catch {
      alert('Failed to restart step');
    }
  };

  const handleStartWorkflow = async (workflowId: string) => {
    try {
      const res = await fetch(`/api/workflows/${workflowId}/start`, { method: 'POST' });
      if (!res.ok) throw new Error('Failed to start workflow');
      fetchWorkflows();
      if (selectedWorkflowId === workflowId) fetchWorkflowDetail(workflowId);
    } catch {
      alert('Failed to start workflow');
    }
  };

  const handleCompleteWorkflow = async (workflowId: string) => {
    if (!confirm('Mark this workflow as complete?')) return;
    try {
      const res = await fetch(`/api/workflows/${workflowId}/complete`, { method: 'POST' });
      if (!res.ok) throw new Error('Failed to complete workflow');
      fetchWorkflows();
      if (selectedWorkflowId === workflowId) fetchWorkflowDetail(workflowId);
    } catch {
      alert('Failed to complete workflow');
    }
  };

  const handleCancelWorkflow = async (workflowId: string) => {
    if (!confirm('Are you sure you want to cancel this workflow?')) return;
    try {
      const res = await fetch(`/api/workflows/${workflowId}/cancel`, { method: 'POST' });
      if (!res.ok) throw new Error('Failed to cancel workflow');
      fetchWorkflows();
      if (selectedWorkflowId === workflowId) {
        setSelectedWorkflowId(null);
        setSelectedWorkflow(null);
        setSelectedWorkflowAllSteps([]);
      }
    } catch {
      alert('Failed to cancel workflow');
    }
  };

  const handlePauseWorkflow = async (workflowId: string) => {
    const reason = window.prompt('Reason for pausing this workflow:');
    if (!reason || !reason.trim()) return;
    try {
      const res = await fetch(`/api/workflows/${workflowId}/pause`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason }),
      });
      if (!res.ok) throw new Error('Failed to pause workflow');
      fetchWorkflows();
      if (selectedWorkflowId === workflowId) fetchWorkflowDetail(workflowId);
    } catch {
      alert('Failed to pause workflow');
    }
  };

  const handleResumeWorkflow = async (workflowId: string) => {
    try {
      const res = await fetch(`/api/workflows/${workflowId}/resume`, { method: 'POST' });
      if (!res.ok) throw new Error('Failed to resume workflow');
      fetchWorkflows();
      if (selectedWorkflowId === workflowId) fetchWorkflowDetail(workflowId);
    } catch {
      alert('Failed to resume workflow');
    }
  };

  const handleAddNote = async () => {
    if (!selectedWorkflowId || !noteContent.trim()) return;
    try {
      const res = await fetch(`/api/workflows/${selectedWorkflowId}/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: noteContent }),
      });
      if (!res.ok) throw new Error('Failed to add note');
      setNoteContent('');
      fetchWorkflowDetail(selectedWorkflowId);
    } catch {
      alert('Failed to add note');
    }
  };

  const getWorkflowProgress = (workflow: Workflow): number => {
    if (workflow.totalSteps === 0) return 0;
    return Math.round((workflow.completedSteps / workflow.totalSteps) * 100);
  };

  const getContext = (workflow: Workflow): SubProjectContext | undefined =>
    workflow.subprojectIds?.[0] ? contextBySubproject[workflow.subprojectIds[0]] : undefined;

  const openStep = (step: WorkflowInstanceStep) => {
    setCurrentStep(step);
    setEvidenceUrls('');
    setApprovalNotes('');
    setStepModalOpen(true);
  };

  // ── Selected workflow detail ──

  if (selectedWorkflowId && selectedWorkflow) {
    const context = getContext(selectedWorkflow);
    const steps = [...selectedWorkflowAllSteps].sort((a, b) => a.order - b.order);

    return (
      <div className="flex flex-col gap-6">
        <Button
          variant="ghost"
          size="sm"
          className="w-fit"
          onClick={() => {
            setSelectedWorkflowId(null);
            setSelectedWorkflow(null);
            setSelectedWorkflowAllSteps([]);
            setNotes([]);
          }}
        >
          ← Back to all workflows
        </Button>

        <div className="rounded-lg border p-4">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <div className="flex items-center gap-3">
                <h3 className="text-lg font-semibold">{selectedWorkflow.name}</h3>
                <Badge variant={WORKFLOW_STATUS_VARIANTS[selectedWorkflow.status] ?? 'secondary'}>
                  {selectedWorkflow.status}
                </Badge>
              </div>
              {context && (
                <p className="mt-1 text-sm text-muted-foreground">
                  {context.projectCode} · {context.projectName} &nbsp;/&nbsp;{' '}
                  {context.subProjectCode} · {context.subProjectName}
                  {' · '}
                  <Link
                    className="text-primary hover:underline"
                    href={`/construction/projects/${context.projectId}/subprojects/${selectedWorkflow.subprojectIds[0]}`}
                  >
                    Open subproject →
                  </Link>
                </p>
              )}
              {selectedWorkflow.description && (
                <p className="mt-1 text-sm text-muted-foreground">{selectedWorkflow.description}</p>
              )}
              <div className="mt-2 flex items-center gap-4 text-sm text-muted-foreground">
                <span>
                  {selectedWorkflow.completedSteps}/{selectedWorkflow.totalSteps} steps
                </span>
                <span>·</span>
                <span>{getWorkflowProgress(selectedWorkflow)}% complete</span>
              </div>
              <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full rounded-full bg-primary transition-all"
                  style={{ width: `${getWorkflowProgress(selectedWorkflow)}%` }}
                />
              </div>
            </div>
            <div className="flex gap-2">
              {selectedWorkflow.status === 'PLANNED' && (
                <Button onClick={() => handleStartWorkflow(selectedWorkflowId)} size="sm">
                  Start
                </Button>
              )}
              {selectedWorkflow.status === 'ACTIVE' && (
                <>
                  <Button
                    onClick={() => handleCompleteWorkflow(selectedWorkflowId)}
                    variant="outline"
                    size="sm"
                  >
                    Complete
                  </Button>
                  <Button
                    onClick={() => handlePauseWorkflow(selectedWorkflowId)}
                    variant="outline"
                    size="sm"
                  >
                    Pause
                  </Button>
                </>
              )}
              {selectedWorkflow.status === 'PAUSED' && (
                <Button
                  onClick={() => handleResumeWorkflow(selectedWorkflowId)}
                  variant="outline"
                  size="sm"
                >
                  Resume
                </Button>
              )}
              {(selectedWorkflow.status === 'PLANNED' ||
                selectedWorkflow.status === 'ACTIVE' ||
                selectedWorkflow.status === 'PAUSED') && (
                <Button
                  onClick={() => handleCancelWorkflow(selectedWorkflowId)}
                  variant="destructive"
                  size="sm"
                >
                  Cancel
                </Button>
              )}
            </div>
          </div>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b text-left text-muted-foreground">
                  <th className="pb-2 pr-4 font-medium">#</th>
                  <th className="pb-2 pr-4 font-medium">Name</th>
                  <th className="pb-2 pr-4 font-medium">Status</th>
                  <th className="pb-2 pr-4 font-medium">Assigned To</th>
                  <th className="pb-2 pr-4 font-medium">Due Date</th>
                  <th className="pb-2 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {steps.map((step) => (
                  <tr key={step.id} className="border-b last:border-b-0">
                    <td className="py-3 pr-4 text-muted-foreground">{step.order}</td>
                    <td className="py-3 pr-4 font-medium">
                      {step.name}
                      {step.approvalNotes && (
                        <p className="mt-0.5 text-xs font-normal text-muted-foreground">
                          {step.approvalNotes}
                        </p>
                      )}
                    </td>
                    <td className="py-3 pr-4">
                      <Badge variant={WORKFLOW_STEP_STATUS_VARIANTS[step.status] ?? 'secondary'}>
                        {formatStepStatus(step.status)}
                      </Badge>
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground">
                      {step.assignedToName ?? '—'}
                    </td>
                    <td className="py-3 pr-4 text-muted-foreground">
                      {step.dueAt ? new Date(step.dueAt).toLocaleDateString() : '—'}
                    </td>
                    <td className="py-3">
                      <div className="flex items-center gap-1">
                        {step.status === 'PENDING' && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleStartStep(selectedWorkflowId, step.id)}
                          >
                            Start
                          </Button>
                        )}
                        {step.status === 'IN_PROGRESS' && (
                          <Button variant="outline" size="sm" onClick={() => openStep(step)}>
                            Complete
                          </Button>
                        )}
                        {step.status === 'COMPLETED' && (
                          <Button variant="outline" size="sm" onClick={() => openStep(step)}>
                            Review
                          </Button>
                        )}
                        {step.status === 'APPROVED' && (
                          <span className="text-xs text-muted-foreground">Approved</span>
                        )}
                        {step.status === 'REJECTED' && (
                          <Button variant="outline" size="sm" onClick={() => openStep(step)}>
                            Restart
                          </Button>
                        )}
                        {step.status === 'BLOCKED' && (
                          <span className="text-xs text-muted-foreground">Blocked</span>
                        )}
                        <StepRowActions
                          workflowId={selectedWorkflowId}
                          projectId={context?.projectId}
                          subProjectId={selectedWorkflow.subprojectIds[0]}
                          step={step}
                          onStepUpdated={() => fetchWorkflowDetail(selectedWorkflowId)}
                          onAttachmentQueued={() => void syncAttachmentsNow()}
                        />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4">
            <h4 className="text-sm font-medium text-muted-foreground">Notes</h4>
            <div className="mt-2 flex gap-2">
              <Input
                placeholder="Add a note..."
                value={noteContent}
                onChange={(e) => setNoteContent(e.target.value)}
                className="flex-1"
              />
              <Button onClick={handleAddNote} size="sm" disabled={!noteContent.trim()}>
                Add
              </Button>
            </div>
            <div className="mt-2 flex flex-col gap-2">
              {notesLoading ? (
                <p className="text-sm text-muted-foreground">Loading notes...</p>
              ) : notes.length === 0 ? (
                <p className="text-sm text-muted-foreground">No notes yet.</p>
              ) : (
                notes.map((note) => (
                  <div key={note.id} className="rounded-md border p-2">
                    <p className="text-sm">{note.content}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {new Date(note.createdAt).toLocaleString()}
                    </p>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Step Action Modal */}
        <Modal
          open={stepModalOpen}
          onClose={() => setStepModalOpen(false)}
          title={currentStep ? `Step: ${currentStep.name}` : 'Step Details'}
        >
          {currentStep && (
            <div className="flex min-w-0 flex-col gap-4">
              <div>
                <p className="text-sm text-muted-foreground">
                  {currentStep.description || 'No description'}
                </p>
                <div className="mt-2 flex gap-2">
                  <Badge variant={WORKFLOW_STEP_STATUS_VARIANTS[currentStep.status] ?? 'secondary'}>
                    {formatStepStatus(currentStep.status)}
                  </Badge>
                </div>
              </div>

              {currentStep.status === 'IN_PROGRESS' && (
                <>
                  <div className="grid gap-2">
                    <Label htmlFor="evidence-urls">Evidence URLs (comma-separated)</Label>
                    <Input
                      id="evidence-urls"
                      value={evidenceUrls}
                      onChange={(e) => setEvidenceUrls(e.target.value)}
                      placeholder="https://example.com/evidence1.jpg, https://example.com/evidence2.pdf"
                    />
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="approval-notes">Notes</Label>
                    <textarea
                      id="approval-notes"
                      rows={2}
                      className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                      value={approvalNotes}
                      onChange={(e) => setApprovalNotes(e.target.value)}
                      placeholder="Optional notes..."
                    />
                  </div>
                </>
              )}

              {currentStep.status === 'COMPLETED' && (
                <div className="grid gap-2">
                  <Label htmlFor="approval-notes">Notes</Label>
                  <textarea
                    id="approval-notes"
                    rows={2}
                    className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    value={approvalNotes}
                    onChange={(e) => setApprovalNotes(e.target.value)}
                    placeholder="Notes (required if rejecting)..."
                  />
                </div>
              )}

              <div className="flex gap-2">
                {currentStep.status === 'PENDING' && (
                  <Button
                    onClick={() => handleStartStep(selectedWorkflowId, currentStep.id)}
                    className="flex-1"
                  >
                    Start Step
                  </Button>
                )}
                {currentStep.status === 'IN_PROGRESS' && (
                  <Button
                    onClick={() => handleCompleteStepFinal(selectedWorkflowId, currentStep.id)}
                    className="flex-1"
                  >
                    Complete Step
                  </Button>
                )}
                {currentStep.status === 'COMPLETED' && (
                  <>
                    <Button
                      onClick={() => handleApproveStep(selectedWorkflowId, currentStep.id)}
                      className="flex-1"
                    >
                      Approve
                    </Button>
                    <Button
                      variant="destructive"
                      onClick={() => handleRejectStep(selectedWorkflowId, currentStep.id)}
                      className="flex-1"
                    >
                      Reject
                    </Button>
                  </>
                )}
                {currentStep.status === 'REJECTED' && (
                  <Button
                    onClick={() => handleRestartStep(selectedWorkflowId, currentStep.id)}
                    className="flex-1"
                  >
                    Restart Step
                  </Button>
                )}
              </div>
            </div>
          )}
        </Modal>
      </div>
    );
  }

  // ── All-workflows list ──

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">Construction Monitoring</h2>
          <p className="text-sm text-muted-foreground">
            Workflows across every project and subproject.
          </p>
        </div>
        <Button variant="outline" onClick={fetchWorkflows}>
          Refresh
        </Button>
      </div>

      {loading ? (
        <p className="py-4 text-center text-sm text-muted-foreground">Loading workflows...</p>
      ) : workflows.length === 0 ? (
        <div className="rounded-lg border border-dashed p-8 text-center">
          <p className="text-sm text-muted-foreground">No construction workflows yet.</p>
          <p className="mt-1 text-xs text-muted-foreground">
            Workflows are created from a subproject&apos;s Workflows tab.
          </p>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {workflows.map((workflow) => {
            const progress = getWorkflowProgress(workflow);
            const context = getContext(workflow);
            const steps = workflow.steps ?? [];
            const activeStep = steps.find((s) => s.status === 'IN_PROGRESS');
            const pendingSteps = steps.filter((s) => s.status === 'PENDING').length;

            return (
              <div
                key={workflow.id}
                className="cursor-pointer rounded-lg border p-4 transition-colors hover:bg-muted/50"
                onClick={() => {
                  setSelectedWorkflowId(workflow.id);
                  fetchWorkflowDetail(workflow.id);
                  fetchNotes(workflow.id);
                }}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{workflow.name}</span>
                      <Badge variant={WORKFLOW_STATUS_VARIANTS[workflow.status] ?? 'secondary'}>
                        {workflow.status}
                      </Badge>
                    </div>
                    {context && (
                      <p className="mt-1 text-xs text-muted-foreground">
                        {context.projectCode} · {context.projectName} / {context.subProjectCode} ·{' '}
                        {context.subProjectName}
                      </p>
                    )}
                    <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                      <span>
                        {workflow.completedSteps}/{workflow.totalSteps} steps
                      </span>
                      <span>·</span>
                      <span>{progress}% complete</span>
                      {activeStep && (
                        <>
                          <span>·</span>
                          <span>Current: {activeStep.name}</span>
                        </>
                      )}
                      {pendingSteps > 0 && (
                        <>
                          <span>·</span>
                          <span>{pendingSteps} pending</span>
                        </>
                      )}
                    </div>
                    <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                      <div
                        className="h-full rounded-full bg-primary transition-all"
                        style={{ width: `${progress}%` }}
                      />
                    </div>
                  </div>
                  <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
                    {workflow.status === 'PLANNED' && (
                      <Button
                        onClick={() => handleStartWorkflow(workflow.id)}
                        variant="ghost"
                        size="sm"
                      >
                        Start
                      </Button>
                    )}
                    {workflow.status === 'ACTIVE' && (
                      <>
                        <Button
                          onClick={() => handleCompleteWorkflow(workflow.id)}
                          variant="ghost"
                          size="sm"
                        >
                          Complete
                        </Button>
                        <Button
                          onClick={() => handlePauseWorkflow(workflow.id)}
                          variant="ghost"
                          size="sm"
                        >
                          Pause
                        </Button>
                      </>
                    )}
                    {workflow.status === 'PAUSED' && (
                      <Button
                        onClick={() => handleResumeWorkflow(workflow.id)}
                        variant="ghost"
                        size="sm"
                      >
                        Resume
                      </Button>
                    )}
                    {(workflow.status === 'PLANNED' ||
                      workflow.status === 'ACTIVE' ||
                      workflow.status === 'PAUSED') && (
                      <Button
                        onClick={() => handleCancelWorkflow(workflow.id)}
                        variant="ghost"
                        size="sm"
                        className="text-destructive hover:text-destructive"
                      >
                        Cancel
                      </Button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
