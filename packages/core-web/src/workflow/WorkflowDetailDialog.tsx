'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { CheckCircle, Clock, AlertCircle, X, FileText, Loader2, Play, CheckSquare, AlertTriangle } from 'lucide-react';
import { Badge } from '../components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '../components/ui/modal/dialog';
import { Button } from '../components/ui/button';
import { workflowApi } from './api';
import { updateWorkflowStep } from '../construction-engineering/workflow-client';
import type { WorkflowNote, WorkflowInstanceStep } from '@cerp/shared-types';
import { StepRowActions } from './StepRowActions';
import { processQueue } from './step-attachments';

type WorkflowDetailTabId = 'overview' | 'steps' | 'notes' | 'actions';

const WORKFLOW_DETAIL_TABS: { id: WorkflowDetailTabId; label: string }[] = [
  { id: 'overview', label: 'Overview' },
  { id: 'steps', label: 'Steps' },
  { id: 'notes', label: 'Notes' },
  { id: 'actions', label: 'Actions' },
];

function isTerminal(status: string): boolean {
  return ['COMPLETED', 'APPROVED', 'REJECTED', 'CANCELLED'].includes(status.toUpperCase());
}

function statusIcon(status: string | undefined) {
  const s = status?.toUpperCase() || '';
  if (s === 'COMPLETED' || s === 'APPROVED') return <CheckCircle className="h-5 w-5 text-green-600" />;
  if (s === 'REJECTED') return <AlertCircle className="h-5 w-5 text-destructive" />;
  return <Clock className="h-5 w-5 text-muted-foreground" />;
}

function statusBadgeVariant(status: string | undefined): 'default' | 'success' | 'destructive' | 'secondary' | 'outline' {
  const s = (status ?? '').toUpperCase();
  if (s === 'COMPLETED' || s === 'APPROVED') return 'success';
  if (s === 'REJECTED') return 'destructive';
  if (s === 'ACTIVE' || s === 'IN_PROGRESS') return 'secondary';
  if (s === 'PLANNED') return 'outline';
  return 'secondary';
}

function fmtDate(d: string | null | undefined): string {
  if (!d) return '\u2014';
  return new Date(d).toLocaleString();
}

function stepStatusBadge(status: string | undefined): 'default' | 'success' | 'destructive' | 'secondary' | 'outline' {
  const s = (status ?? '').toUpperCase();
  if (s === 'COMPLETED' || s === 'APPROVED') return 'success';
  if (s === 'REJECTED') return 'destructive';
  if (s === 'IN_PROGRESS') return 'default';
  if (s === 'BLOCKED') return 'outline';
  return 'secondary';
}

export interface WorkflowDetailDialogProps {
  workflowId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onRefresh?: () => void;
  /** Project ID for attachment uploads (photos/documents). */
  projectId?: string;
  /** Sub-project ID for attachment uploads. */
  subProjectId?: string;
}

export function WorkflowDetailDialog({
  workflowId, open, onOpenChange, onRefresh, projectId, subProjectId,
}: WorkflowDetailDialogProps) {
  const [workflow, setWorkflow] = useState<Record<string, unknown> | null>(null);
  const [steps, setSteps] = useState<Record<string, unknown>[]>([]);
  const [notes, setNotes] = useState<WorkflowNote[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<WorkflowDetailTabId>('overview');
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [assignLoading, setAssignLoading] = useState<string | null>(null);
  const [assignValues, setAssignValues] = useState<Record<string, string>>({});

  // Fetches data and sets state; used by event handlers (not in effects)
  const refetch = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await workflowApi.getWorkflow(workflowId);
      setWorkflow(data.workflow);
      setSteps((data.steps ?? []) as Record<string, unknown>[]);
      setNotes((data.notes ?? []) as unknown as WorkflowNote[]);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load workflow details');
    } finally {
      setLoading(false);
    }
  }, [workflowId]);

  // Track previous open state to reset when dialog closes
  const prevOpenRef = useRef(open);

  /* eslint-disable react-hooks/set-state-in-effect */
  useEffect(() => {
    if (!open && prevOpenRef.current) {
      setWorkflow(null);
      setSteps([]);
      setNotes([]);
      setLoading(false);
      setError(null);
    }
    prevOpenRef.current = open;

    if (!open) return;

    let cancelled = false;
    setLoading(true);
    setError(null);

    workflowApi
      .getWorkflow(workflowId)
      .then((data) => {
        if (!cancelled) {
          setWorkflow(data.workflow);
          setSteps((data.steps ?? []) as Record<string, unknown>[]);
          setNotes((data.notes ?? []) as unknown as WorkflowNote[]);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load workflow details');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [open, workflowId]);
  /* eslint-enable react-hooks/set-state-in-effect */

  const handleAction = useCallback(async (command: 'start' | 'complete' | 'pause' | 'resume' | 'cancel') => {
    setActionLoading(command);
    setError(null);
    try {
      const endpointMap: Record<string, string> = {
        start: 'start', complete: 'complete', pause: 'pause', resume: 'resume', cancel: 'cancel',
      };
      const res = await fetch(`/api/workflows/${workflowId}/${endpointMap[command]}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body?.message ?? 'Action failed');
      }
      await refetch();
      onRefresh?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to execute: ${command}`);
    } finally {
      setActionLoading(null);
    }
  }, [workflowId, refetch, onRefresh]);

  const handleAssignStep = useCallback(async (stepId: string) => {
    const email = assignValues[stepId]?.trim();
    if (!email) return;
    setAssignLoading(stepId);
    setError(null);
    try {
      await updateWorkflowStep(workflowId, stepId, { email });
      setAssignValues((prev) => {
        const next = { ...prev };
        delete next[stepId];
        return next;
      });
      await refetch();
      onRefresh?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to assign step');
    } finally {
      setAssignLoading(null);
    }
  }, [workflowId, assignValues, refetch, onRefresh]);

  // ── Step-level actions ──

  const handleStartStep = useCallback(async (stepId: string) => {
    setActionLoading(`start:${stepId}`);
    setError(null);
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/start`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body?.message ?? 'Failed to start step');
      }
      await refetch();
      onRefresh?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start step');
    } finally {
      setActionLoading(null);
    }
  }, [workflowId, refetch, onRefresh]);

  const handleCompleteStep = useCallback(async (stepId: string) => {
    setActionLoading(`complete:${stepId}`);
    setError(null);
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/complete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body?.message ?? 'Failed to complete step');
      }
      await refetch();
      onRefresh?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to complete step');
    } finally {
      setActionLoading(null);
    }
  }, [workflowId, refetch, onRefresh]);

  const handleApproveStep = useCallback(async (stepId: string) => {
    setActionLoading(`approve:${stepId}`);
    setError(null);
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body?.message ?? 'Failed to approve step');
      }
      await refetch();
      onRefresh?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to approve step');
    } finally {
      setActionLoading(null);
    }
  }, [workflowId, refetch, onRefresh]);

  const handleRejectStep = useCallback(async (stepId: string) => {
    setActionLoading(`reject:${stepId}`);
    setError(null);
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body?.message ?? 'Failed to reject step');
      }
      await refetch();
      onRefresh?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reject step');
    } finally {
      setActionLoading(null);
    }
  }, [workflowId, refetch, onRefresh]);

  const wfStatus = (workflow?.status as string) ?? '';
  const currentStepId = (workflow?.currentStepId as string) ?? null;
  const terminal = isTerminal(wfStatus);

  const renderOverviewContent = () => {
    if (!workflow) return null;
    return (
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <Badge variant={statusBadgeVariant(wfStatus)} className="capitalize text-xs">{wfStatus}</Badge>
          {(workflow.code as string) && <Badge variant="outline" className="text-xs font-mono">{String(workflow.code)}</Badge>}
        </div>
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><FileText className="h-4 w-4" />Workflow Info</CardTitle></CardHeader>
          <CardContent>
            <dl className="grid grid-cols-2 gap-3 text-sm">
              {(workflow.name as string) && <div className="col-span-2"><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Name</dt><dd className="font-medium">{String(workflow.name)}</dd></div>}
              {(workflow.templateCode as string) && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Template</dt><dd className="font-mono text-xs">{String(workflow.templateCode)}</dd></div>}
              {((workflow.subprojectIds as string[]) ?? []).length > 0 && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Subproject</dt><dd className="font-mono text-xs">{String(((workflow.subprojectIds as string[])[0] ?? '').slice(0, 8))}</dd></div>}
              <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Completed Steps</dt><dd className="text-xs font-medium">{String(workflow.completedSteps ?? 0)}/{String(workflow.totalSteps ?? 0)}</dd></div>
              <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Progress</dt><dd className="text-xs font-medium">{Math.round((workflow.progress as number) ?? 0)}%</dd></div>
              <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Started</dt><dd className="text-xs">{fmtDate(workflow.startedAt as string | null | undefined)}</dd></div>
              <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Completed</dt><dd className="text-xs">{fmtDate(workflow.completedAt as string | null | undefined)}</dd></div>
              <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Created</dt><dd className="text-xs">{fmtDate(workflow.createdAt as string | null | undefined)}</dd></div>
              <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Updated</dt><dd className="text-xs">{fmtDate(workflow.updatedAt as string | null | undefined)}</dd></div>
            </dl>
          </CardContent>
        </Card>
        <div>
          <div className="flex items-center justify-between text-xs text-muted-foreground mb-1">
            <span>Progress</span>
            <span>{Math.round((workflow.progress as number) ?? 0)}%</span>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
            <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${Math.round((workflow.progress as number) ?? 0)}%` }} />
          </div>
        </div>
      </div>
    );
  };

  const renderStepsContent = () => {
    if (steps.length === 0) return <div className="py-4 text-center text-sm text-muted-foreground">No steps yet.</div>;
    return (
      <div className="space-y-2">
        {[...steps].sort((a, b) => ((a.order as number) ?? 0) - ((b.order as number) ?? 0)).map((step, i) => {
          const sStatus = (step.status as string) ?? 'PENDING';
          const isCurrent = step.id === currentStepId;
          const stepIdStr = String(step.id);
          const currentAssignee = ((step.assigneeEmail as string) ?? (step.assigneeName as string) ?? (step.assignedToName as string)) ?? '';
          const currentAssigneeName = (step.assigneeName as string) ?? (step.assignedToName as string);
          const currentAssigneeEmail = (step.assigneeEmail as string);
          const currentAssigneeDisplay = currentAssigneeName && currentAssigneeEmail
            ? `${currentAssigneeName} (${currentAssigneeEmail})`
            : currentAssignee;
          return (
            <div key={stepIdStr} className={`flex items-start gap-3 rounded-lg border p-3 ${isCurrent ? 'border-primary bg-primary/5' : ''}`}>
              <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold bg-muted text-muted-foreground">{i + 1}</div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-medium">{step.name as string}</span>
                  <Badge variant={stepStatusBadge(sStatus)} className="capitalize text-[10px]">{sStatus}</Badge>
                  {isCurrent && <Badge variant="default" className="text-[10px]">Current</Badge>}
                </div>
                <div className="flex items-center gap-3 mt-1 text-xs text-muted-foreground flex-wrap">
                  {currentAssigneeDisplay && <span>Assigned: {currentAssigneeDisplay}</span>}
                  {((step.dueAt as string) ?? '') && <span>Due: {fmtDate(step.dueAt as string | null | undefined)}</span>}
                </div>
                <div className="flex items-center gap-2 mt-2">
                  <input
                    type="email"
                    placeholder="Assign by email..."
                    value={assignValues[stepIdStr] ?? ''}
                    onChange={(e) => setAssignValues((prev) => ({ ...prev, [stepIdStr]: e.target.value }))}
                    className="h-7 rounded-md border border-input bg-background px-2 text-xs placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring w-48"
                    onKeyDown={(e) => { if (e.key === 'Enter') handleAssignStep(stepIdStr); }}
                  />
                  <button
                    onClick={() => handleAssignStep(stepIdStr)}
                    disabled={!assignValues[stepIdStr]?.trim() || assignLoading === stepIdStr}
                    className="h-7 px-2.5 text-xs rounded-md bg-primary text-primary-foreground hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {assignLoading === stepIdStr ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : 'Assign'}
                  </button>
                </div>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                {step.status === 'PENDING' && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-7 gap-1 text-xs"
                    disabled={actionLoading === `start:${stepIdStr}`}
                    onClick={() => handleStartStep(stepIdStr)}
                  >
                    {actionLoading === `start:${stepIdStr}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <Play className="h-3 w-3" />}
                    Start
                  </Button>
                )}
                {step.status === 'IN_PROGRESS' && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-7 gap-1 text-xs"
                    disabled={actionLoading === `complete:${stepIdStr}`}
                    onClick={() => handleCompleteStep(stepIdStr)}
                  >
                    {actionLoading === `complete:${stepIdStr}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <CheckSquare className="h-3 w-3" />}
                    Complete
                  </Button>
                )}
                {step.status === 'COMPLETED' && (
                  <>
                    <Button
                      size="sm"
                      variant="default"
                      className="h-7 gap-1 text-xs"
                      disabled={actionLoading === `approve:${stepIdStr}`}
                      onClick={() => handleApproveStep(stepIdStr)}
                    >
                      {actionLoading === `approve:${stepIdStr}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <CheckCircle className="h-3 w-3" />}
                      Approve
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="h-7 gap-1 text-xs"
                      disabled={actionLoading === `reject:${stepIdStr}`}
                      onClick={() => handleRejectStep(stepIdStr)}
                    >
                      {actionLoading === `reject:${stepIdStr}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <AlertTriangle className="h-3 w-3" />}
                      Reject
                    </Button>
                  </>
                )}
                {step.status === 'REJECTED' && (
                  <Button
                    size="sm"
                    variant="outline"
                    className="h-7 gap-1 text-xs"
                    disabled={actionLoading === `start:${stepIdStr}`}
                    onClick={() => handleStartStep(stepIdStr)}
                  >
                    {actionLoading === `start:${stepIdStr}` ? <Loader2 className="h-3 w-3 animate-spin" /> : <Play className="h-3 w-3" />}
                    Restart
                  </Button>
                )}
                {step.status === 'APPROVED' && (
                  <span className="text-xs text-muted-foreground">Approved</span>
                )}
                {step.status === 'BLOCKED' && (
                  <span className="text-xs text-muted-foreground">Blocked</span>
                )}
                <StepRowActions
                  workflowId={workflowId}
                  projectId={projectId}
                  subProjectId={subProjectId}
                  step={step as unknown as WorkflowInstanceStep}
                  onStepUpdated={() => { void refetch(); onRefresh?.(); }}
                  onAttachmentQueued={() => { try { processQueue(); } catch { /* queue drain is best-effort */ } }}
                />
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  const renderNotesContent = () => {
    if (notes.length === 0) return <div className="py-4 text-center text-sm text-muted-foreground">No notes yet.</div>;
    return (
      <div className="space-y-2">
        {notes.map((note) => (
          <div key={note.id} className="rounded-lg border p-3">
            <div className="flex items-center gap-2 text-xs text-muted-foreground mb-1">
              <span className="font-medium">{note.createdBy || 'Unknown'}</span>
              <span>\u2022</span>
              <span>{fmtDate(note.createdAt)}</span>
            </div>
            <p className="text-sm">{note.content}</p>
          </div>
        ))}
      </div>
    );
  };

  const renderActionsContent = () => {
    if (terminal) return <div className="py-4 text-center text-sm text-muted-foreground">Workflow is terminal ({wfStatus}). No actions available.</div>;
    return (
      <div className="space-y-2">
        {wfStatus === 'PLANNED' && (
          <Button onClick={() => handleAction('start')} disabled={!!actionLoading} className="w-full">{actionLoading === 'start' ? 'Starting...' : 'Start Workflow'}</Button>
        )}
        {wfStatus === 'ACTIVE' && (
          <>
            <Button onClick={() => handleAction('complete')} disabled={!!actionLoading} className="w-full">{actionLoading === 'complete' ? 'Completing...' : 'Complete Workflow'}</Button>
            <Button onClick={() => handleAction('pause')} variant="outline" disabled={!!actionLoading} className="w-full">{actionLoading === 'pause' ? 'Pausing...' : 'Pause Workflow'}</Button>
          </>
        )}
        {wfStatus === 'PAUSED' && (
          <Button onClick={() => handleAction('resume')} variant="outline" disabled={!!actionLoading} className="w-full">{actionLoading === 'resume' ? 'Resuming...' : 'Resume Workflow'}</Button>
        )}
        {(wfStatus === 'PLANNED' || wfStatus === 'ACTIVE' || wfStatus === 'PAUSED') && (
          <Button onClick={() => handleAction('cancel')} variant="destructive" disabled={!!actionLoading} className="w-full">{actionLoading === 'cancel' ? 'Cancelling...' : 'Cancel Workflow'}</Button>
        )}
      </div>
    );
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              {statusIcon(wfStatus)}
              <DialogTitle className="text-lg">Workflow Details</DialogTitle>
            </div>
            <Button size="sm" variant="ghost" className="h-6 w-6 p-0" onClick={() => onOpenChange(false)}><X className="h-4 w-4" /></Button>
          </div>
          <DialogDescription className="flex flex-wrap items-center gap-2">
            <Badge variant={statusBadgeVariant(wfStatus)} className="capitalize">{wfStatus || '\u2014'}</Badge>
            {workflow && (workflow.templateCode as string) && <Badge variant="outline" className="text-xs">{String(workflow.templateCode)}</Badge>}
            {workflow && (workflow.name as string) && <span className="text-xs text-muted-foreground truncate">{String(workflow.name)}</span>}
          </DialogDescription>
        </DialogHeader>
        {error && <div className="px-1 py-2 text-sm text-destructive bg-destructive/10 rounded">{error}</div>}
        {!loading && !error && workflow && (
          <div className="space-y-0">
            <div className="flex gap-0.5 mb-4 border-b pb-0">
              {WORKFLOW_DETAIL_TABS.map((tab) => (
                <button
                  key={tab.id}
                  className={`text-xs font-medium px-3 py-2 transition-colors border-b-2 ${
                    activeTab === tab.id ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground hover:border-muted'
                  }`}
                  onClick={() => setActiveTab(tab.id as WorkflowDetailTabId)}
                >
                  {tab.label}
                </button>
              ))}
            </div>
            {activeTab === 'overview' && renderOverviewContent()}
            {activeTab === 'steps' && renderStepsContent()}
            {activeTab === 'notes' && renderNotesContent()}
            {activeTab === 'actions' && renderActionsContent()}
          </div>
        )}
        {loading && <div className="py-8 text-center text-sm text-muted-foreground">Loading workflow details...</div>}
        <DialogFooter className="mt-4">
          <Button variant="outline" onClick={() => onOpenChange(false)}>Close</Button>
          <Button variant="default" onClick={() => { void refetch(); onRefresh?.(); }}>Refresh</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}