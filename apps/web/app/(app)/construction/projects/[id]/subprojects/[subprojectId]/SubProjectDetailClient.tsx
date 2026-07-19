'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState, useEffect, useCallback } from 'react';
import type {
  SubProjectResponse,
  SubProjectFloor,
  SubProjectBuildingPart,
  SubProjectBuildingType,
  SubProjectConstructionPhase,
  SubProjectStatus,
  Workflow,
  WorkflowInstanceStep,
  WorkflowTemplate,
  WorkflowTemplateStep,
  WorkflowResponse,
  WorkflowTemplateResponse,
  WorkflowStatus,
  WorkflowStepStatus,
  WorkflowStepType,
  WorkflowNote,
} from '@cerp/shared-types';
import { Badge } from '@cerp/core-web/components/ui/badge';
import { Button } from '@cerp/core-web/components/ui/button';
import { Modal } from '@cerp/core-web/components/ui/modal';
import { Input } from '@cerp/core-web/components/ui/input';
import { Label } from '@cerp/core-web/components/ui/label';

// ── Workflow type helpers ──

const WORKFLOW_STATUS_VARIANTS: Record<WorkflowStatus, 'default' | 'secondary' | 'success' | 'destructive' | 'outline'> = {
  ACTIVE: 'default',
  COMPLETED: 'success',
  CANCELLED: 'destructive',
  PAUSED: 'secondary',
};

const WORKFLOW_STEP_STATUS_VARIANTS: Record<WorkflowStepStatus, 'default' | 'secondary' | 'success' | 'destructive' | 'outline'> = {
  PENDING: 'secondary',
  IN_PROGRESS: 'default',
  COMPLETED: 'success',
  SKIPPED: 'outline',
  FAILED: 'destructive',
};

const STEP_TYPE_ICONS: Record<WorkflowStepType, string> = {
  TASK: '📋',
  MILESTONE: '🏁',
  INSPECTION: '🔍',
  APPROVAL: '✅',
};

const STEP_TYPE_LABELS: Record<WorkflowStepType, string> = {
  TASK: 'Task',
  MILESTONE: 'Milestone',
  INSPECTION: 'Inspection',
  APPROVAL: 'Approval',
};

function formatPhaseType(phaseType: string | null): string {
  if (!phaseType) return '—';
  return phaseType.replace(/_/g, ' ');
}

function formatStepStatus(status: WorkflowStepStatus): string {
  return status.replace(/_/g, ' ');
}

const statusVariant: Record<SubProjectStatus, 'default' | 'secondary' | 'success' | 'destructive' | 'outline'> = {
  PLANNED: 'secondary',
  ACTIVE: 'default',
  ON_HOLD: 'secondary',
  COMPLETED: 'success',
  CANCELLED: 'destructive',
};

const buildingTypeOptions: SubProjectBuildingType[] = [
  'RESIDENTIAL',
  'COMMERCIAL',
  'INDUSTRIAL',
  'MIXED_USE',
  'INFRASTRUCTURE',
  'OTHER',
];

const constructionPhaseOptions: SubProjectConstructionPhase[] = [
  'PRE_CONSTRUCTION',
  'SITE_PREP',
  'FOUNDATION',
  'BASEMENT',
  'SUPERSTRUCTURE',
  'ROOF',
  'BUILDING_ENVELOPE',
  'MEP_ROUGHIN',
  'INTERIOR_FINISH',
  'EXTERIOR_WORKS',
  'HANDOVER',
];

const statusOptions: SubProjectStatus[] = ['PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];

type TabKey = 'overview' | 'workflows' | 'templates';

export default function SubProjectDetailClient({
  subProject,
  projectId,
}: {
  subProject: SubProjectResponse;
  projectId: string;
}): JSX.Element {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<TabKey>('overview');
  const [updateModalOpen, setUpdateModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const [formData, setFormData] = useState({
    code: subProject.code ?? '',
    name: subProject.name ?? '',
    description: subProject.description ?? '',
    buildingType: subProject.buildingType as SubProjectBuildingType,
    constructionPhase: subProject.constructionPhase as SubProjectConstructionPhase,
    status: subProject.status as SubProjectStatus,
  });

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      const response = await fetch(`/api/v1/projects/${projectId}/subprojects/${subProject.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: formData.code,
          name: formData.name,
          description: formData.description || undefined,
          buildingType: formData.buildingType,
          constructionPhase: formData.constructionPhase,
          status: formData.status,
        }),
      });
      if (!response.ok) throw new Error('Failed to update subproject');
      setUpdateModalOpen(false);
      window.location.reload();
    } catch {
      alert('Failed to update subproject');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this subproject?')) return;
    try {
      setLoading(true);
      const response = await fetch(`/api/v1/projects/${projectId}/subprojects/${subProject.id}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) throw new Error('Failed to delete subproject');
      router.push(`/construction/projects/${projectId}`);
    } catch {
      alert('Failed to delete subproject');
    } finally {
      setLoading(false);
    }
  };

  // ── Workflow state ──

  const [workflows, setWorkflows] = useState<Workflow[]>([]);
  const [workflowSteps, setWorkflowSteps] = useState<Record<string, WorkflowInstanceStep[]>>({});
  const [templates, setTemplates] = useState<WorkflowTemplate[]>([]);
  const [templateDetail, setTemplateDetail] = useState<WorkflowTemplateResponse | null>(null);
  const [templatesLoading, setTemplatesLoading] = useState(false);
  const [workflowLoading, setWorkflowLoading] = useState(false);
  const [createWorkflowModalOpen, setCreateWorkflowModalOpen] = useState(false);
  const [selectedTemplateCode, setSelectedTemplateCode] = useState('');
  const [selectedWorkflowId, setSelectedWorkflowId] = useState<string | null>(null);
  const [selectedWorkflow, setSelectedWorkflow] = useState<Workflow | null>(null);
  const [selectedWorkflowAllSteps, setSelectedWorkflowAllSteps] = useState<WorkflowInstanceStep[]>([]);
  const [notes, setNotes] = useState<WorkflowNote[]>([]);
  const [noteContent, setNoteContent] = useState('');
  const [notesLoading, setNotesLoading] = useState(false);
  const [stepModalOpen, setStepModalOpen] = useState(false);
  const [currentStep, setCurrentStep] = useState<WorkflowInstanceStep | null>(null);
  const [evidenceUrls, setEvidenceUrls] = useState('');
  const [approvalNotes, setApprovalNotes] = useState('');

  const subprojectId = subProject.id;

  const [initialized, setInitialized] = useState(false);

  const fetchWorkflows = useCallback(async () => {
    try {
      setWorkflowLoading(true);
      const res = await fetch(`/api/workflows/subprojects/${subprojectId}`);
      if (!res.ok) throw new Error('Failed to fetch workflows');
      const data = await res.json();
      setWorkflows(data.workflows ?? []);
      setWorkflowSteps(
        (data.steps ?? []).reduce((acc: Record<string, WorkflowInstanceStep[]>, step: WorkflowInstanceStep) => {
          if (!acc[step.workflowId]) acc[step.workflowId] = [];
          acc[step.workflowId].push(step);
          return acc;
        }, {})
      );
    } catch {
      // Silently fail — workflows are optional
    } finally {
      setWorkflowLoading(false);
    }
  }, [subprojectId]);

  const fetchTemplates = useCallback(async () => {
    try {
      setTemplatesLoading(true);
      const res = await fetch('/api/workflows/templates');
      if (!res.ok) throw new Error('Failed to fetch templates');
      const data: WorkflowTemplate[] = await res.json();
      setTemplates(data);
    } catch {
      // Silently fail — templates are optional
    } finally {
      setTemplatesLoading(false);
    }
  }, []);

  const fetchTemplateDetail = useCallback(async (templateCode: string) => {
    try {
      const res = await fetch(`/api/workflows/templates/${templateCode}`);
      if (!res.ok) throw new Error('Failed to fetch template detail');
      const data: WorkflowTemplateResponse = await res.json();
      setTemplateDetail(data);
    } catch {
      alert('Failed to load template details');
    }
  }, []);

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

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    if (!initialized) {
      fetchWorkflows();
      fetchTemplates();
      setInitialized(true);
    }
  }, [fetchWorkflows, fetchTemplates, initialized]);

  const handleCreateWorkflow = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTemplateCode) {
      alert('Please select a template');
      return;
    }
    try {
      const res = await fetch('/api/workflows', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          templateCode: selectedTemplateCode,
          subprojectId,
          phaseType: templates.find((t) => t.code === selectedTemplateCode)?.phaseType ?? '',
        }),
      });
      if (!res.ok) throw new Error('Failed to create workflow');
      setCreateWorkflowModalOpen(false);
      setSelectedTemplateCode('');
      fetchWorkflows();
    } catch {
      alert('Failed to create workflow');
    }
  };

  const handleCompleteStepFinal = async (workflowId: string, stepId: string) => {
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}/complete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          status: 'COMPLETED',
          evidenceUrls: evidenceUrls.split(',').map((u) => u.trim()).filter((u) => u),
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

  const handleStartStep = async (workflowId: string, stepId: string) => {
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'IN_PROGRESS' }),
      });
      if (!res.ok) throw new Error('Failed to start step');
      fetchWorkflows();
      if (selectedWorkflowId) fetchWorkflowDetail(selectedWorkflowId);
    } catch {
      alert('Failed to start step');
    }
  };

  const handleSkipStep = async (workflowId: string, stepId: string) => {
    if (!confirm('Mark this step as skipped?')) return;
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${stepId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ status: 'SKIPPED' }),
      });
      if (!res.ok) throw new Error('Failed to skip step');
      fetchWorkflows();
      if (selectedWorkflowId) fetchWorkflowDetail(selectedWorkflowId);
    } catch {
      alert('Failed to skip step');
    }
  };

  const handleCancelWorkflow = async (workflowId: string) => {
    if (!confirm('Are you sure you want to cancel this workflow?')) return;
    try {
      const res = await fetch(`/api/workflows/${workflowId}/cancel`, {
        method: 'POST',
      });
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
    try {
      const res = await fetch(`/api/workflows/${workflowId}/pause`, {
        method: 'POST',
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
      const res = await fetch(`/api/workflows/${workflowId}/resume`, {
        method: 'POST',
      });
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

  // ── Tab content helpers ──

  const renderOverviewTab = () => (
    <>
      {/* Details Grid */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Type</p>
          <p className="mt-1 text-sm font-medium">{subProject.buildingType}</p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Construction Phase</p>
          <p className="mt-1 text-sm font-medium">{subProject.constructionPhase?.replace(/_/g, ' ') ?? '—'}</p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Project ID</p>
          <p className="mt-1 text-sm font-medium font-mono">{subProject.projectId}</p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Start Date</p>
          <p className="mt-1 text-sm font-medium">
            {subProject.startDate ? new Date(subProject.startDate).toLocaleDateString() : '—'}
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">End Date</p>
          <p className="mt-1 text-sm font-medium">
            {subProject.endDate ? new Date(subProject.endDate).toLocaleDateString() : '—'}
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Actual Start Date</p>
          <p className="mt-1 text-sm font-medium">
            {subProject.actualStartDate ? new Date(subProject.actualStartDate).toLocaleDateString() : '—'}
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Actual End Date</p>
          <p className="mt-1 text-sm font-medium">
            {subProject.actualEndDate ? new Date(subProject.actualEndDate).toLocaleDateString() : '—'}
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Progress</p>
          <p className="mt-1 text-sm font-medium">
            {subProject.progress != null ? `${Math.round(subProject.progress * 100)}%` : '—'}
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Budget</p>
          <p className="mt-1 text-sm font-medium">
            {subProject.budget != null ? `${subProject.budget} ${subProject.currencyCode ?? ''}` : '—'}
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">External ID</p>
          <p className="mt-1 text-sm font-medium">{subProject.externalId ?? '—'}</p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Sync Status</p>
          <p className="mt-1 text-sm font-medium">
            {subProject.syncStatus ? (
              <Badge variant="outline">{subProject.syncStatus}</Badge>
            ) : (
              '—'
            )}
          </p>
        </div>
      </div>

      {/* Floors */}
      {subProject.floors && subProject.floors.length > 0 && (
        <div className="rounded-lg border p-4">
          <h3 className="mb-3 text-sm font-medium text-muted-foreground">Floors ({subProject.floors.length})</h3>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {subProject.floors.map((floor: SubProjectFloor) => (
              <div key={floor.id} className="rounded-md border p-3">
                <div className="flex items-center gap-2">
                  <Badge variant="secondary" className="font-mono text-xs">
                    {floor.code}
                  </Badge>
                  <span className="text-sm">{floor.name}</span>
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  Level {floor.level}
                  {floor.area ? ` · ${floor.area} m²` : ''}
                </p>
                {floor.description && (
                  <p className="mt-1 text-xs text-muted-foreground">{floor.description}</p>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Building Parts */}
      {subProject.buildingParts && subProject.buildingParts.length > 0 && (
        <div className="rounded-lg border p-4">
          <h3 className="mb-3 text-sm font-medium text-muted-foreground">
            Building Parts ({subProject.buildingParts.length})
          </h3>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {subProject.buildingParts.map((part: SubProjectBuildingPart) => (
              <div key={part.id} className="rounded-md border p-3">
                <div className="flex items-center gap-2">
                  <Badge variant="secondary" className="font-mono text-xs">
                    {part.code}
                  </Badge>
                  <span className="text-sm">{part.name}</span>
                </div>
                {part.description && (
                  <p className="mt-1 text-xs text-muted-foreground">{part.description}</p>
                )}
                <Badge variant="outline" className="mt-2 text-xs">{part.scopeLevel}</Badge>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Metadata */}
      <div className="rounded-lg border p-4">
        <h3 className="mb-3 text-sm font-medium text-muted-foreground">Metadata</h3>
        <div className="grid grid-cols-1 gap-2 text-sm md:grid-cols-2">
          <div>
            <span className="text-muted-foreground">Created: </span>
            {subProject.createdAt ? new Date(subProject.createdAt).toLocaleString() : '—'}
          </div>
          <div>
            <span className="text-muted-foreground">Updated: </span>
            {subProject.updatedAt ? new Date(subProject.updatedAt).toLocaleString() : '—'}
          </div>
          <div>
            <span className="text-muted-foreground">Created By: </span>
            {subProject.createdBy ?? '—'}
          </div>
          <div>
            <span className="text-muted-foreground">Updated By: </span>
            —
          </div>
          {subProject.externalId && (
            <div>
              <span className="text-muted-foreground">External ID: </span>
              <span className="font-mono">{subProject.externalId}</span>
            </div>
          )}
        </div>
      </div>
    </>
  );

  const renderWorkflowsTab = () => {
    // If a specific workflow is selected, show its details
    if (selectedWorkflowId) {
      const steps = selectedWorkflowAllSteps.sort((a, b) => a.order - b.order);
      return (
        <div className="flex flex-col gap-6">
          {/* Back button */}
          <Button variant="ghost" size="sm" onClick={() => { setSelectedWorkflowId(null); setSelectedWorkflow(null); setSelectedWorkflowAllSteps([]); setNotes([]); }}>
            ← Back to all workflows
          </Button>

          {/* Workflow header */}
          {selectedWorkflow && (
            <div className="rounded-lg border p-4">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <h3 className="text-lg font-semibold">{selectedWorkflow.name}</h3>
                    <Badge variant={WORKFLOW_STATUS_VARIANTS[selectedWorkflow.status] ?? 'secondary'}>
                      {selectedWorkflow.status}
                    </Badge>
                    {selectedWorkflow.templateCode && (
                      <Badge variant="outline" className="font-mono text-xs">{selectedWorkflow.templateCode}</Badge>
                    )}
                  </div>
                  {selectedWorkflow.description && (
                    <p className="mt-1 text-sm text-muted-foreground">{selectedWorkflow.description}</p>
                  )}
                  <div className="mt-2 flex items-center gap-4 text-sm text-muted-foreground">
                    <span>{formatPhaseType(selectedWorkflow.phaseType)}</span>
                    <span>·</span>
                    <span>{selectedWorkflow.completedSteps}/{selectedWorkflow.totalSteps} steps</span>
                    <span>·</span>
                    <span>{getWorkflowProgress(selectedWorkflow)}% complete</span>
                  </div>
                  {/* Progress bar */}
                  <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-muted">
                    <div
                      className="h-full rounded-full bg-primary transition-all"
                      style={{ width: `${getWorkflowProgress(selectedWorkflow)}%` }}
                    />
                  </div>
                </div>
                <div className="flex gap-2">
                  {selectedWorkflow.status === 'ACTIVE' && (
                    <Button onClick={() => handlePauseWorkflow(selectedWorkflowId)} variant="outline" size="sm">
                      Pause
                    </Button>
                  )}
                  {selectedWorkflow.status === 'PAUSED' && (
                    <Button onClick={() => handleResumeWorkflow(selectedWorkflowId)} variant="outline" size="sm">
                      Resume
                    </Button>
                  )}
                  {selectedWorkflow.status !== 'CANCELLED' && selectedWorkflow.status !== 'COMPLETED' && (
                    <Button onClick={() => handleCancelWorkflow(selectedWorkflowId)} variant="destructive" size="sm">
                      Cancel
                    </Button>
                  )}
                </div>
              </div>

              {/* Steps table */}
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-left text-muted-foreground">
                      <th className="pb-2 pr-4 font-medium">#</th>
                      <th className="pb-2 pr-4 font-medium">Type</th>
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
                        <td className="py-3 pr-4">
                          <div className="flex items-center gap-1">
                            <span>{STEP_TYPE_ICONS[step.type]}</span>
                            <span className="text-xs">{STEP_TYPE_LABELS[step.type]}</span>
                          </div>
                        </td>
                        <td className="py-3 pr-4 font-medium">{step.name}</td>
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
                          <div className="flex gap-1">
                            {step.status === 'PENDING' && (
                              <>
                                <Button variant="outline" size="sm" onClick={() => { setCurrentStep(step); setStepModalOpen(true); }}>
                                  Start
                                </Button>
                                <Button variant="ghost" size="sm" onClick={() => handleSkipStep(selectedWorkflowId, step.id)}>
                                  Skip
                                </Button>
                              </>
                            )}
                            {step.status === 'IN_PROGRESS' && (
                              <Button variant="outline" size="sm" onClick={() => { setCurrentStep(step); setStepModalOpen(true); }}>
                                Complete
                              </Button>
                            )}
                            {(step.status === 'COMPLETED' || step.status === 'SKIPPED') && (
                              <span className="text-xs text-muted-foreground">Done</span>
                            )}
                            {step.status === 'FAILED' && (
                              <Button variant="outline" size="sm" onClick={() => handleStartStep(selectedWorkflowId, step.id)}>
                                Retry
                              </Button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Notes section */}
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
          )}
        </div>
      );
    }

    // Show list of workflows
    return (
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-medium text-muted-foreground">
            Workflows ({workflows.length})
          </h3>
          <Button
            onClick={() => setCreateWorkflowModalOpen(true)}
            variant="outline"
            size="sm"
            disabled={templatesLoading || templates.length === 0}
          >
            + New Workflow
          </Button>
        </div>

        {workflowLoading ? (
          <p className="py-4 text-center text-sm text-muted-foreground">Loading workflows...</p>
        ) : workflows.length === 0 ? (
          <div className="rounded-lg border border-dashed p-8 text-center">
            <p className="text-sm text-muted-foreground">No workflows yet for this subproject.</p>
            <p className="mt-1 text-xs text-muted-foreground">
              Create a workflow from a template to get started.
            </p>
            <Button
              onClick={() => setCreateWorkflowModalOpen(true)}
              variant="outline"
              size="sm"
              className="mt-4"
              disabled={templatesLoading || templates.length === 0}
            >
              + Create Workflow
            </Button>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            {workflows.map((workflow) => {
              const steps = workflowSteps[workflow.id] ?? [];
              const progress = getWorkflowProgress(workflow);
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
                        {workflow.templateCode && (
                          <Badge variant="outline" className="font-mono text-xs">
                            {workflow.templateCode}
                          </Badge>
                        )}
                      </div>
                      {workflow.description && (
                        <p className="mt-1 text-xs text-muted-foreground">{workflow.description}</p>
                      )}
                      <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                        <span>{formatPhaseType(workflow.phaseType)}</span>
                        <span>·</span>
                        <span>{workflow.completedSteps}/{workflow.totalSteps} steps</span>
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
                      {/* Progress bar */}
                      <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-muted">
                        <div
                          className="h-full rounded-full bg-primary transition-all"
                          style={{ width: `${progress}%` }}
                        />
                      </div>
                    </div>
                    <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
                      {workflow.status === 'ACTIVE' && (
                        <Button onClick={() => handlePauseWorkflow(workflow.id)} variant="ghost" size="sm">
                          Pause
                        </Button>
                      )}
                      {workflow.status === 'PAUSED' && (
                        <Button onClick={() => handleResumeWorkflow(workflow.id)} variant="ghost" size="sm">
                          Resume
                        </Button>
                      )}
                      {workflow.status !== 'CANCELLED' && workflow.status !== 'COMPLETED' && (
                        <Button onClick={() => handleCancelWorkflow(workflow.id)} variant="ghost" size="sm" className="text-destructive hover:text-destructive">
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
  };

  const renderTemplatesTab = () => (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-medium text-muted-foreground">
          Workflow Templates ({templates.length})
        </h3>
      </div>

      {templatesLoading ? (
        <p className="py-4 text-center text-sm text-muted-foreground">Loading templates...</p>
      ) : templates.length === 0 ? (
        <p className="py-4 text-center text-sm text-muted-foreground">No templates available.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {templates.map((template) => {
            const templateSteps = (template as WorkflowTemplate & { steps?: WorkflowTemplateStep[] }).steps ?? [];
            return (
              <div key={template.id} className="rounded-lg border p-4">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{template.name}</span>
                      <Badge variant="outline" className="font-mono text-xs">
                        {template.code}
                      </Badge>
                      <Badge variant="secondary">
                        {formatStepStatus((template as { status?: string }).status ?? 'ACTIVE')}
                      </Badge>
                    </div>
                    {template.description && (
                      <p className="mt-1 text-xs text-muted-foreground">{template.description}</p>
                    )}
                    <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                      <span>{formatPhaseType(template.phaseType)}</span>
                      <span>·</span>
                      <span>{templateSteps.length} steps</span>
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setSelectedTemplateCode(template.code);
                        fetchTemplateDetail(template.code);
                      }}
                    >
                      View Steps
                    </Button>
                    <Button
                      size="sm"
                      onClick={() => {
                        setSelectedTemplateCode(template.code);
                        setCreateWorkflowModalOpen(true);
                      }}
                    >
                      Use for Subproject
                    </Button>
                  </div>
                </div>

                {/* Show first few steps inline */}
                {templateSteps.length > 0 && templateSteps.length <= 5 && (
                  <div className="mt-3 flex flex-col gap-1">
                    {templateSteps.sort((a, b) => a.order - b.order).map((step: WorkflowTemplateStep) => (
                      <div key={step.id} className="flex items-center gap-2 text-xs">
                        <span>{STEP_TYPE_ICONS[step.type]}</span>
                        <span className="text-muted-foreground">{step.order}.</span>
                        <span>{step.name}</span>
                        <Badge variant="outline" className="text-xs">{STEP_TYPE_LABELS[step.type]}</Badge>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Template detail modal */}
      {templateDetail && (
        <Modal
          open={!!templateDetail}
          onClose={() => setTemplateDetail(null)}
          title={`Template: ${templateDetail.template.name}`}
        >
          <div className="flex max-h-[70vh] flex-col gap-4 overflow-y-auto">
            <div>
              <p className="text-sm text-muted-foreground">{templateDetail.template.description}</p>
              <div className="mt-2 flex gap-2">
                <Badge variant="outline">{templateDetail.template.code}</Badge>
                <Badge variant="secondary">{formatPhaseType(templateDetail.template.phaseType)}</Badge>
              </div>
            </div>
            <div>
              <h4 className="text-sm font-medium">Steps ({templateDetail.steps?.length ?? 0})</h4>
              <div className="mt-2 flex flex-col gap-2">
                {(templateDetail.steps ?? [])
                  .sort((a, b) => a.order - b.order)
                  .map((step: WorkflowTemplateStep) => (
                    <div key={step.id} className="rounded-md border p-3">
                      <div className="flex items-center gap-2">
                        <span>{STEP_TYPE_ICONS[step.type]}</span>
                        <span className="font-medium">{step.name}</span>
                        <Badge variant="outline" className="text-xs">{STEP_TYPE_LABELS[step.type]}</Badge>
                      </div>
                      {step.description && (
                        <p className="mt-1 text-xs text-muted-foreground">{step.description}</p>
                      )}
                      <div className="mt-1 flex flex-wrap gap-2 text-xs text-muted-foreground">
                        {step.requiresEvidence && <Badge variant="secondary">Requires Evidence</Badge>}
                        {step.requiresApproval && <Badge variant="secondary">Requires Approval</Badge>}
                        {step.slaHours && <span>SLA: {step.slaHours}h</span>}
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col gap-4">
        <Link
          href={`/construction/projects/${projectId}`}
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <span className="mr-1">←</span> Back to Project
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-semibold">{subProject.name}</h1>
              <Badge variant={statusVariant[subProject.status] ?? 'secondary'}>{subProject.status}</Badge>
            </div>
            {subProject.code && (
              <p className="mt-1 text-sm text-muted-foreground font-mono">{subProject.code}</p>
            )}
            {subProject.description && (
              <p className="mt-2 text-sm text-muted-foreground">{subProject.description}</p>
            )}
          </div>
          <div className="flex gap-2">
            <Button onClick={() => setUpdateModalOpen(true)} variant="outline" size="sm">
              Edit
            </Button>
            <Button onClick={handleDelete} variant="destructive" size="sm" disabled={loading}>
              Delete
            </Button>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b">
        <nav className="flex gap-1">
          {([
            { key: 'overview' as TabKey, label: 'Overview' },
            { key: 'workflows' as TabKey, label: `Workflows (${workflows.length})` },
            { key: 'templates' as TabKey, label: `Templates (${templates.length})` },
          ]).map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
                activeTab === tab.key
                  ? 'border-primary text-foreground'
                  : 'border-transparent text-muted-foreground hover:text-foreground'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab content */}
      {activeTab === 'overview' && renderOverviewTab()}
      {activeTab === 'workflows' && renderWorkflowsTab()}
      {activeTab === 'templates' && renderTemplatesTab()}

      {/* Create Workflow Modal */}
      <Modal
        open={createWorkflowModalOpen}
        onClose={() => setCreateWorkflowModalOpen(false)}
        title="Create workflow"
      >
        <form onSubmit={handleCreateWorkflow} className="flex min-w-0 flex-col gap-4">
          <div className="grid gap-2">
            <Label htmlFor="workflow-template">Template</Label>
            <select
              id="workflow-template"
              value={selectedTemplateCode}
              onChange={(e) => setSelectedTemplateCode(e.target.value)}
              className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              required
              disabled={templatesLoading}
            >
              <option value="">Select a template...</option>
              {templates.map((t) => {
                const tWithSteps = t as WorkflowTemplate & { steps?: WorkflowTemplateStep[] };
                return (
                  <option key={t.id} value={t.code}>
                    {t.name} ({formatPhaseType(t.phaseType)}) — {tWithSteps.steps?.length ?? 0} steps
                  </option>
                );
              })}
            </select>
            {selectedTemplateCode && (
              <p className="text-xs text-muted-foreground">
                {templates
                  .find((t) => t.code === selectedTemplateCode)
                  ?.steps?.length?.toString() ?? 0}{' '}
                steps in this template
              </p>
            )}
          </div>
          <div className="shrink-0">
            <Button type="submit" className="w-full" disabled={loading || !selectedTemplateCode}>
              {loading ? 'Creating...' : 'Create Workflow'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Step Action Modal */}
      <Modal
        open={stepModalOpen}
        onClose={() => setStepModalOpen(false)}
        title={currentStep ? `Step: ${currentStep.name}` : 'Step Details'}
      >
        {currentStep && selectedWorkflowId && (
          <div className="flex min-w-0 flex-col gap-4">
            <div>
              <p className="text-sm text-muted-foreground">{currentStep.description || 'No description'}</p>
              <div className="mt-2 flex gap-2">
                <Badge variant={WORKFLOW_STEP_STATUS_VARIANTS[currentStep.status] ?? 'secondary'}>
                  {formatStepStatus(currentStep.status)}
                </Badge>
                <Badge variant="outline">{STEP_TYPE_LABELS[currentStep.type]}</Badge>
                {currentStep.requiresEvidence && <Badge variant="secondary">Requires Evidence</Badge>}
                {currentStep.requiresApproval && <Badge variant="secondary">Requires Approval</Badge>}
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
                  <Label htmlFor="approval-notes">Approval Notes</Label>
                  <textarea
                    id="approval-notes"
                    rows={2}
                    className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                    value={approvalNotes}
                    onChange={(e) => setApprovalNotes(e.target.value)}
                    placeholder="Optional approval notes..."
                  />
                </div>
              </>
            )}

            <div className="flex gap-2">
              {currentStep.status === 'PENDING' && (
                <Button onClick={() => handleStartStep(selectedWorkflowId, currentStep.id)} className="flex-1">
                  Start Step
                </Button>
              )}
              {currentStep.status === 'IN_PROGRESS' && (
                <Button onClick={() => handleCompleteStepFinal(selectedWorkflowId, currentStep.id)} className="flex-1">
                  Complete Step
                </Button>
              )}
              {(currentStep.status === 'PENDING' || currentStep.status === 'IN_PROGRESS') && (
                <Button variant="outline" onClick={() => handleSkipStep(selectedWorkflowId, currentStep.id)} className="flex-1">
                  Skip Step
                </Button>
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* Update Modal */}
      <Modal
        open={updateModalOpen}
        onClose={() => setUpdateModalOpen(false)}
        title="Edit subproject"
      >
        <form onSubmit={handleUpdate} className="flex min-w-0 max-h-[65vh] flex-col gap-4">
          <div className="min-w-0 flex-1 overflow-y-auto">
            <div className="grid gap-2">
              <Label htmlFor="code">Code</Label>
              <Input
                id="code"
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                placeholder="BLD-A"
                required
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="name">Name</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="Building A"
                required
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="description">Description</Label>
              <textarea
                id="description"
                rows={2}
                className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Optional description..."
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="grid gap-2">
                <Label htmlFor="buildingType">Building Type</Label>
                <select
                  id="buildingType"
                  value={formData.buildingType}
                  onChange={(e) => setFormData({ ...formData, buildingType: e.target.value as SubProjectBuildingType })}
                  className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {buildingTypeOptions.map((option) => (
                    <option key={option} value={option}>
                      {option.replace(/_/g, ' ')}
                    </option>
                  ))}
                </select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="constructionPhase">Construction Phase</Label>
                <select
                  id="constructionPhase"
                  value={formData.constructionPhase}
                  onChange={(e) =>
                    setFormData({ ...formData, constructionPhase: e.target.value as SubProjectConstructionPhase })
                  }
                  className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  {constructionPhaseOptions.map((option) => (
                    <option key={option} value={option}>
                      {option.replace(/_/g, ' ')}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="status">Status</Label>
              <select
                id="status"
                value={formData.status}
                onChange={(e) => setFormData({ ...formData, status: e.target.value as SubProjectStatus })}
                className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {statusOptions.map((option) => (
                  <option key={option} value={option}>
                    {option.replace(/_/g, ' ')}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="shrink-0">
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? 'Saving...' : 'Save Changes'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}