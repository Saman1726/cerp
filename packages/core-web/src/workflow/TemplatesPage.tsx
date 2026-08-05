'use client';

import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import { Modal } from '../components/ui/modal';
import { workflowApi } from './api';

// ── Internal types matching the API transformation layer ──

interface TemplateListItem {
  templateId: string;
  code: string;
  name: string;
  description: string | null;
  version: number;
  status: 'Draft' | 'Active' | 'Deprecated';
  steps: ApiStep[];
  metadata: {
    createdBy: string;
    createdAt: string;
    updatedAt: string;
    applicableCategories?: string[];
    applicableRegions?: string[];
  };
}

interface ApiStep {
  id?: string;
  templateId?: string;
  code: string;
  name: string;
  description: string | null;
  type: string;
  order: number;
  weight?: number;
  requiresEvidence: boolean;
  requiresApproval: boolean;
  approverRole?: string;
  slaHours?: number;
  checklists?: unknown[];
  gatingCondition?: string;
  assigneeRoles?: string[];
  dueOffsetDays?: number;
  createdAt: string;
  updatedAt: string;
}

/** Step editor form item - mirrors backend WorkflowTemplateStepRequest */
interface StepFormState {
  stepOrder: number;
  code: string;
  name: string;
  type: 'TASK' | 'INSPECTION' | 'APPROVAL' | 'MILESTONE';
  assigneeRoles: string;
  requiresEvidence: boolean;
  requiresApproval: boolean;
  dueOffsetDays: string;
  slaHours: string;
  weight: string;
  checklists: string;
  gatingCondition: string;
}

const DEFAULT_STEP_FORM: StepFormState = {
  stepOrder: 1,
  code: '',
  name: '',
  type: 'TASK',
  assigneeRoles: '',
  requiresEvidence: false,
  requiresApproval: false,
  dueOffsetDays: '',
  slaHours: '',
  weight: '1',
  checklists: '',
  gatingCondition: '',
};

const STEP_TYPES: { value: 'TASK' | 'INSPECTION' | 'APPROVAL' | 'MILESTONE'; label: string }[] = [
  { value: 'TASK', label: 'Task' },
  { value: 'INSPECTION', label: 'Inspection' },
  { value: 'APPROVAL', label: 'Approval' },
  { value: 'MILESTONE', label: 'Milestone' },
];

const STEP_STATUS_COLORS: Record<string, string> = {
  TASK: 'bg-blue-100 text-blue-800',
  INSPECTION: 'bg-amber-100 text-amber-800',
  APPROVAL: 'bg-purple-100 text-purple-800',
  MILESTONE: 'bg-green-100 text-green-800',
};

export function TemplatesPage() {
  const [templates, setTemplates] = useState<TemplateListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Detail modal state
  const [detailTemplate, setDetailTemplate] = useState<TemplateListItem | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [showDetail, setShowDetail] = useState(false);

  // Create/Edit modal state
  const [showFormModal, setShowFormModal] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editCode, setEditCode] = useState('');
  const [formName, setFormName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formPhaseType, setFormPhaseType] = useState('');
  const [formSteps, setFormSteps] = useState<StepFormState[]>([]);
  const [formLoading, setFormLoading] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const fetchTemplates = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await workflowApi.listTemplates();
      const mapped = (result as unknown as TemplateListItem[]).map((t) => ({
        ...t,
        steps: t.steps ?? [],
      }));
      setTemplates(mapped);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load templates');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let mounted = true;
    void (async () => {
      setLoading(true);
      setError(null);
      try {
        const result = await workflowApi.listTemplates();
        if (!mounted) return;
        const mapped = (result as unknown as TemplateListItem[]).map((t) => ({
          ...t,
          steps: t.steps ?? [],
        }));
        setTemplates(mapped);
      } catch (err) {
        if (mounted) setError(err instanceof Error ? err.message : 'Failed to load templates');
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, []);

  // ── Detail Modal ──

  const handleViewDetails = async (template: TemplateListItem) => {
    setDetailLoading(true);
    setDetailTemplate(null);
    try {
      const result = await workflowApi.getTemplateWithSteps(template.code);
      const mapped = result as unknown as TemplateListItem;
      setDetailTemplate(mapped);
      setShowDetail(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load template details');
    } finally {
      setDetailLoading(false);
    }
  };

  // ── Create / Edit Modal ──

  const openCreateModal = () => {
    setEditCode('');
    setFormName('');
    setFormDescription('');
    setFormPhaseType('');
    setFormSteps([{ ...DEFAULT_STEP_FORM, stepOrder: 1 }]);
    setIsEditing(false);
    setFormError(null);
    setShowFormModal(true);
  };

  const openEditModal = async (template: TemplateListItem) => {
    setFormError(null);
    setIsEditing(true);

    try {
      const result = await workflowApi.getTemplateWithSteps(template.code);
      const raw = result as unknown as TemplateListItem;

      setEditCode(raw.code);
      setFormName(raw.name);
      setFormDescription(raw.description ?? '');
      // Phase type is not stored in the frontend format, keep empty for now
      setFormPhaseType('');

      const apiSteps = raw.steps as ApiStep[];
      setFormSteps(
        apiSteps.map((s, idx) => ({
          stepOrder: s.order ?? idx + 1,
          code: s.code,
          name: s.name,
          type: (s.type as StepFormState['type']) ?? 'TASK',
          assigneeRoles: (s.assigneeRoles ?? []).join(', '),
          requiresEvidence: s.requiresEvidence ?? false,
          requiresApproval: s.requiresApproval ?? false,
          dueOffsetDays: (s as { dueOffsetDays?: number }).dueOffsetDays?.toString() ?? '',
          slaHours: (s.slaHours?.toString()) ?? '',
          weight: ((s.weight ?? 1) as number).toString(),
          checklists: JSON.stringify((s.checklists ?? [])),
          gatingCondition: s.gatingCondition ?? '',
        }))
      );
      setShowFormModal(true);
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Failed to load template for editing');
    }
  };

  const handleSaveTemplate = async () => {
    if (!editCode.trim() || !formName.trim() || !formPhaseType.trim()) {
      setFormError('Code, name, and phase type are required.');
      return;
    }

    const invalidStep = formSteps.filter((s): s is StepFormState => s != null).find((s) => !s.code.trim() || !s.name.trim());
    if (invalidStep) {
      setFormError('Each step must have a code and name.');
      return;
    }

    setFormLoading(true);
    setFormError(null);

    try {
      const apiSteps = formSteps.map((s) => {
        const roles = s.assigneeRoles.trim()
          ? s.assigneeRoles.split(',').map((r) => r.trim()).filter(Boolean)
          : [];

        return {
          stepOrder: s.stepOrder,
          code: s.code.trim(),
          name: s.name.trim(),
          type: s.type,
          assigneeRoles: roles,
          requiresEvidence: s.requiresEvidence,
          requiresApproval: s.requiresApproval,
          dueOffsetDays: s.dueOffsetDays ? parseInt(s.dueOffsetDays, 10) : undefined,
          slaHours: s.slaHours ? parseInt(s.slaHours, 10) : undefined,
          weight: s.weight ? parseFloat(s.weight) : 1,
          checklists: s.checklists.trim() || undefined,
          gatingCondition: s.gatingCondition.trim() || undefined,
        };
      });

      const requestBody = {
        code: editCode.trim().toUpperCase(),
        name: formName.trim(),
        description: formDescription.trim() || undefined,
        phaseType: formPhaseType.trim().toUpperCase(),
        steps: apiSteps,
      };

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const anyRequest = requestBody as any;
      if (isEditing) {
        await workflowApi.updateTemplate(editCode, anyRequest);
      } else {
        await workflowApi.createTemplate(anyRequest);
      }

      setShowFormModal(false);
      await fetchTemplates();
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Failed to save template');
    } finally {
      setFormLoading(false);
    }
  };

  // Step management
  const addStep = () => {
    setFormSteps([
      ...formSteps,
      { ...DEFAULT_STEP_FORM, stepOrder: formSteps.length + 1 },
    ]);
  };

  const removeStep = (index: number) => {
    setFormSteps(
      formSteps
        .filter((_, i) => i !== index)
        .map((s, i) => ({ ...s, stepOrder: i + 1 }))
    );
  };

  const updateStep = (index: number, field: keyof StepFormState, value: string | boolean) => {
    setFormSteps(
      formSteps.map((s, i) => (i === index ? { ...s, [field]: value } : s))
    );
  };

  const moveStep = (index: number, direction: 'up' | 'down') => {
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= formSteps.length) return;
    const newSteps: StepFormState[] = [...formSteps];
    const itemA = newSteps[index];
    const itemB = newSteps[newIndex];
    if (itemA == null || itemB == null) return;
    newSteps[newIndex] = itemA;
    newSteps[index] = itemB;
    setFormSteps(newSteps.map((s, i) => ({ ...s, stepOrder: i + 1 })));
  };

  // ── Delete ──

  const handleDelete = async (code: string) => {
    if (!confirm(`Are you sure you want to delete template ${JSON.stringify(code)}?`)) return;
    try {
      await workflowApi.deleteTemplate(code);
      setTemplates((prev) => prev.filter((t) => t.code !== code));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete template');
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-muted-foreground">Loading templates...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Workflow Templates</h1>
          <p className="text-muted-foreground mt-1">
            Manage orchestration template configurations
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={fetchTemplates}>
            Refresh
          </Button>
          <Button onClick={openCreateModal}>Create Template</Button>
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {/* Templates Grid */}
      {templates.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">
            No templates found. Create your first template to get started.
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {templates.map((template) => (
            <Card key={template.templateId} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between">
                  <div>
                    <CardTitle className="text-lg">{template.name}</CardTitle>
                    <p className="text-sm text-muted-foreground font-mono">{template.code}</p>
                  </div>
                  <Badge
                    variant="secondary"
                    className={
                      template.status === 'Active'
                        ? 'bg-green-100 text-green-800'
                        : template.status === 'Draft'
                          ? 'bg-blue-100 text-blue-800'
                          : 'bg-gray-100 text-gray-800'
                    }
                  >
                    {template.status}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-3">
                <p className="text-sm text-muted-foreground">{template.description}</p>

                <div className="flex items-center gap-4 text-sm text-muted-foreground">
                  <span>Version: {template.version}</span>
                </div>

                <div className="flex gap-2 pt-2 border-t">
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1"
                    onClick={() => handleViewDetails(template)}
                  >
                    View Details
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="flex-1"
                    onClick={() => openEditModal(template)}
                  >
                    Edit
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    className="text-red-600 hover:text-red-700"
                    onClick={() => handleDelete(template.code)}
                  >
                    Delete
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* ── Detail Modal ── */}
      <Modal
        open={showDetail}
        onClose={() => setShowDetail(false)}
        title={detailTemplate?.name ?? 'Template Details'}
      >
        {detailLoading ? (
          <div className="flex justify-center py-8">
            <p className="text-muted-foreground">Loading...</p>
          </div>
        ) : detailTemplate ? (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="font-medium text-muted-foreground">Code:</span>
                <p className="font-mono">{detailTemplate.code}</p>
              </div>
              <div>
                <span className="font-medium text-muted-foreground">Version:</span>
                <p>{detailTemplate.version}</p>
              </div>
              <div>
                <span className="font-medium text-muted-foreground">Status:</span>
                <Badge variant="secondary" className="ml-2">
                  {detailTemplate.status}
                </Badge>
              </div>
            </div>

            {detailTemplate.description && (
              <div>
                <span className="font-medium text-muted-foreground">Description:</span>
                <p className="mt-1 text-sm">{detailTemplate.description}</p>
              </div>
            )}

            {/* Steps */}
            <div>
              <h3 className="font-semibold text-sm mb-2">
                Steps ({detailTemplate.steps.length})
              </h3>
              {detailTemplate.steps.length === 0 ? (
                <p className="text-sm text-muted-foreground py-4 text-center">
                  No steps defined for this template.
                </p>
              ) : (
                <div className="space-y-2 max-h-96 overflow-y-auto">
                  {(detailTemplate.steps as ApiStep[])
                    .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
                    .map((step, idx) => {
                      const stepNum = step.order ?? idx + 1;
                      const stepType = step.type ?? 'TASK';
                      return (
                        <Card key={step.id ?? `step-${idx}`} className="px-3 py-2">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                              <span className="font-mono text-xs text-muted-foreground w-6">
                                {stepNum}
                              </span>
                              <div>
                                <p className="text-sm font-medium">{step.name}</p>
                                <p className="text-xs text-muted-foreground font-mono">{step.code}</p>
                              </div>
                            </div>
                            <div className="flex items-center gap-2">
                              <Badge
                                variant="secondary"
                                className={STEP_STATUS_COLORS[stepType] ?? 'bg-gray-100 text-gray-800'}
                              >
                                {stepType}
                              </Badge>
                              {step.requiresEvidence && (
                                <Badge variant="outline" className="text-xs">evidence</Badge>
                              )}
                              {step.requiresApproval && (
                                <Badge variant="outline" className="text-xs">approval</Badge>
                              )}
                            </div>
                          </div>
                          {step.slaHours && (
                            <p className="text-xs text-muted-foreground mt-1">
                              SLA: {step.slaHours}h
                            </p>
                          )}
                        </Card>
                      );
                    })}
                </div>
              )}
            </div>
          </div>
        ) : null}
      </Modal>

      {/* ── Create/Edit Modal ── */}
      <Modal
        open={showFormModal}
        onClose={() => setShowFormModal(false)}
        title={isEditing ? 'Edit Template' : 'Create Template'}
        className="max-w-2xl"
      >
        <div className="space-y-4">
          {formError && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-2 rounded text-sm">
              {formError}
            </div>
          )}

          {/* Basic Fields */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="template-code">Code *</Label>
              <Input
                id="template-code"
                value={editCode}
                onChange={(e) => setEditCode(isEditing ? editCode : e.target.value.toUpperCase())}
                placeholder="FOUNDATION"
                disabled={isEditing}
                readOnly={isEditing}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="template-phase">Phase Type *</Label>
              <Input
                id="template-phase"
                value={formPhaseType}
                onChange={(e) => setFormPhaseType(e.target.value.toUpperCase())}
                placeholder="FOUNDATION"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="template-name">Name *</Label>
            <Input
              id="template-name"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              placeholder="Foundation Workflow"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="template-desc">Description</Label>
            <Textarea
              id="template-desc"
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
              placeholder="Description of this workflow template..."
              rows={2}
            />
          </div>

          {/* Steps Section */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <Label>Steps</Label>
              <Button variant="outline" size="sm" type="button" onClick={addStep}>
                + Add Step
              </Button>
            </div>

            <div className="space-y-3 max-h-96 overflow-y-auto pr-2">
              {formSteps.map((step, index) => (
                <Card key={index} className="px-3 py-3">
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-sm font-medium">Step {index + 1}</span>
                    <div className="flex gap-1">
                      <Button
                        variant="ghost"
                        size="sm"
                        type="button"
                        onClick={() => moveStep(index, 'up')}
                        disabled={index === 0}
                        className="h-6 px-2 text-xs"
                      >
                        &uarr;
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        type="button"
                        onClick={() => moveStep(index, 'down')}
                        disabled={index === formSteps.length - 1}
                        className="h-6 px-2 text-xs"
                      >
                        &darr;
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        type="button"
                        onClick={() => removeStep(index)}
                        className="h-6 px-2 text-xs text-red-600 hover:text-red-700"
                      >
                        &times;
                      </Button>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-2">
                      <Label htmlFor={`step-code-${index}`} className="text-xs">Code *</Label>
                      <Input
                        id={`step-code-${index}`}
                        value={step.code}
                        onChange={(e) => updateStep(index, 'code', e.target.value.toUpperCase())}
                        placeholder="INSPECT_01"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor={`step-name-${index}`} className="text-xs">Name *</Label>
                      <Input
                        id={`step-name-${index}`}
                        value={step.name}
                        onChange={(e) => updateStep(index, 'name', e.target.value)}
                        placeholder="Foundation Inspection"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor={`step-type-${index}`} className="text-xs">Type *</Label>
                      <select
                        id={`step-type-${index}`}
                        className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                        value={step.type}
                        onChange={(e) => updateStep(index, 'type', e.target.value as StepFormState['type'])}
                      >
                        {STEP_TYPES.map((t) => (
                          <option key={t.value} value={t.value}>{t.label}</option>
                        ))}
                      </select>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor={`step-roles-${index}`} className="text-xs">Assignee Roles</Label>
                      <Input
                        id={`step-roles-${index}`}
                        value={step.assigneeRoles}
                        onChange={(e) => updateStep(index, 'assigneeRoles', e.target.value)}
                        placeholder="Engineer, Manager"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor={`step-sla-${index}`} className="text-xs">SLA Hours</Label>
                      <Input
                        id={`step-sla-${index}`}
                        type="number"
                        min="0"
                        value={step.slaHours}
                        onChange={(e) => updateStep(index, 'slaHours', e.target.value)}
                        placeholder="24"
                      />
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor={`step-weight-${index}`} className="text-xs">Weight</Label>
                      <Input
                        id={`step-weight-${index}`}
                        type="number"
                        step="0.01"
                        min="0"
                        value={step.weight}
                        onChange={(e) => updateStep(index, 'weight', e.target.value)}
                        placeholder="1"
                      />
                    </div>
                  </div>

                  <div className="flex gap-4 mt-3">
                    <label className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        checked={step.requiresEvidence}
                        onChange={(e) => updateStep(index, 'requiresEvidence', e.target.checked)}
                      />
                      Requires Evidence
                    </label>
                    <label className="flex items-center gap-2 text-sm">
                      <input
                        type="checkbox"
                        checked={step.requiresApproval}
                        onChange={(e) => updateStep(index, 'requiresApproval', e.target.checked)}
                      />
                      Requires Approval
                    </label>
                  </div>
                </Card>
              ))}

              {formSteps.length === 0 && (
                <p className="text-sm text-muted-foreground text-center py-4">
                  No steps added yet.
                </p>
              )}
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex justify-end gap-2 pt-4 border-t">
            <Button variant="outline" onClick={() => setShowFormModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleSaveTemplate} disabled={formLoading}>
              {formLoading ? 'Saving...' : isEditing ? 'Update' : 'Create'}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}