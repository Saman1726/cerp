'use client';

import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import type {
  Project,
  ConstructionSite,
  ProjectUserRole,
  ProjectAssignedUser,
  SubProject,
  SubProjectFloor,
  SubProjectBuildingPart,
  SubProjectBuildingType,
  SubProjectConstructionPhase,
  SubProjectStatus,
  SubProjectWorkflowResponse,
  Workflow,
  WorkflowInstanceStep,
  WorkflowStatus,
  WorkflowStepStatus,
  WorkflowStepType,
} from '@cerp/shared-types';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Modal } from '../components/ui/modal';
import { Tree, TreeNode } from '../components/ui/tree';
import { DetailPopup, DetailFieldGrid, DetailField } from '../components/ui/detail-popup';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { apiClient } from '../lib/api-client';
import {
  Blocks,
  Building2,
  FolderKanban,
  Layers,
  ListChecks,
  MoreHorizontal,
  Plus,
  Users,
  Workflow as WorkflowIcon,
} from 'lucide-react';

const statusVariant: Record<string, 'default' | 'secondary' | 'success' | 'destructive' | 'outline'> = {
  PLANNED: 'secondary',
  ACTIVE: 'default',
  ON_HOLD: 'secondary',
  COMPLETED: 'success',
  CANCELLED: 'destructive',
};

const WORKFLOW_STATUS_VARIANTS: Record<WorkflowStatus, 'default' | 'secondary' | 'success' | 'destructive' | 'outline'> = {
  PLANNED: 'outline',
  ACTIVE: 'default',
  PAUSED: 'secondary',
  COMPLETED: 'success',
  APPROVED: 'success',
  REJECTED: 'destructive',
  CANCELLED: 'destructive',
};

const WORKFLOW_STEP_STATUS_VARIANTS: Record<WorkflowStepStatus, 'default' | 'secondary' | 'success' | 'destructive' | 'outline'> = {
  PENDING: 'secondary',
  IN_PROGRESS: 'default',
  BLOCKED: 'outline',
  COMPLETED: 'success',
  APPROVED: 'success',
  REJECTED: 'destructive',
};

const STEP_TYPE_LABELS: Record<WorkflowStepType, string> = {
  TASK: 'Task',
  MILESTONE: 'Milestone',
  INSPECTION: 'Inspection',
  APPROVAL: 'Approval',
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

function formatDate(value: string | null | undefined): string | undefined {
  return value ? new Date(value).toLocaleDateString() : undefined;
}

function formatBudget(budget: number | null | undefined, currencyCode: string | null | undefined): string | undefined {
  return budget != null ? `${budget} ${currencyCode ?? ''}`.trim() : undefined;
}

function formatAddress(project: Project): string | undefined {
  return (
    [project.addressLine1, project.addressLine2, project.city, project.countryCode].filter(Boolean).join(', ') ||
    undefined
  );
}

/** Reusable role dropdown component */
function RoleDropdown({ value, onChange, size }: { value: ProjectUserRole; onChange: (value: ProjectUserRole) => void; size?: 'sm' | 'md' }) {
  const ROLE_OPTIONS: { value: ProjectUserRole; label: string }[] = [
    { value: 'VIEWER', label: 'Viewer' },
    { value: 'SUPERVISOR', label: 'Supervisor' },
    { value: 'MANAGER', label: 'Manager' },
    { value: 'OWNER', label: 'Owner' },
  ];
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value as ProjectUserRole)}
      className={`flex h-9 rounded-md border border-input bg-transparent px-2 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring ${size === 'sm' ? 'w-24' : 'w-32'}`}
    >
      {ROLE_OPTIONS.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
}

export interface ProjectListProps {
  projects: Project[];
  constructionSites: ConstructionSite[];
  createProjectAction: (formData: FormData) => Promise<{ error?: string }>;
  createSubProjectAction: (formData: FormData) => Promise<{ error?: string }>;
  deleteProjectAction: (projectId: string) => Promise<void>;
  deleteSubProjectAction: (formData: FormData) => Promise<void>;
  /** Assign users with roles: [{ userId, role }] */
  assignUsersAction?: (projectId: string, assignments: Array<{ userId: string; role: ProjectUserRole }>) => Promise<{ error?: string }>;
  removeUserAction?: (projectId: string, userId: string) => Promise<{ error?: string }>;
  /** Resolves assignedUserIds (UUID-keyed) to human-readable emails for the Assign Users popup. */
  getAssignedUsersAction?: (projectId: string) => Promise<ProjectAssignedUser[]>;
  currentUserEmail?: string;
}

type DetailState =
  | { kind: 'project'; project: Project }
  | { kind: 'subProject'; project: Project; subProject: SubProject }
  | { kind: 'floor'; project: Project; subProject: SubProject; floor: SubProjectFloor }
  | { kind: 'buildingPart'; project: Project; subProject: SubProject; part: SubProjectBuildingPart }
  | { kind: 'workflow'; project: Project; subProject: SubProject; workflow: Workflow }
  | { kind: 'step'; project: Project; subProject: SubProject; workflow: Workflow; step: WorkflowInstanceStep };

function getDetailTitle(detail: DetailState): string {
  switch (detail.kind) {
    case 'project':
      return `${detail.project.code} · ${detail.project.name}`;
    case 'subProject':
      return `${detail.subProject.code} · ${detail.subProject.name}`;
    case 'floor':
      return detail.floor.name;
    case 'buildingPart':
      return detail.part.name;
    case 'workflow':
      return detail.workflow.name;
    case 'step':
      return detail.step.name;
  }
}

function getDetailDescription(detail: DetailState): string | undefined {
  switch (detail.kind) {
    case 'project':
      return detail.project.description ?? undefined;
    case 'subProject':
      return detail.subProject.description ?? undefined;
    case 'workflow':
      return detail.workflow.description ?? undefined;
    case 'step':
      return detail.step.description ?? undefined;
    default:
      return undefined;
  }
}

function getManageWorkflowsHref(detail: DetailState): string | null {
  switch (detail.kind) {
    case 'subProject':
    case 'workflow':
    case 'step':
      return `/construction/projects/${detail.project.id}/subprojects/${detail.subProject.id}`;
    default:
      return null;
  }
}

function DetailContent({ detail, constructionSites }: { detail: DetailState; constructionSites: ConstructionSite[] }) {
  switch (detail.kind) {
    case 'project': {
      const { project } = detail;
      const site = constructionSites.find((s) => s.id === project.constructionSiteId);
      return (
        <DetailFieldGrid>
          <DetailField label="Status" value={project.status} />
          <DetailField label="Type" value={project.type} />
          <DetailField label="Construction Site" value={site?.name} />
          <DetailField label="Start Date" value={formatDate(project.startDate)} />
          <DetailField label="End Date" value={formatDate(project.endDate)} />
          <DetailField label="Budget" value={formatBudget(project.budget, project.currencyCode)} />
          <DetailField label="Address" value={formatAddress(project)} />
        </DetailFieldGrid>
      );
    }
    case 'subProject': {
      const { subProject } = detail;
      return (
        <DetailFieldGrid>
          <DetailField label="Status" value={subProject.status} />
          <DetailField label="Building Type" value={subProject.buildingType} />
          <DetailField label="Construction Phase" value={subProject.constructionPhase?.replace(/_/g, ' ')} />
          <DetailField label="Start Date" value={formatDate(subProject.startDate)} />
          <DetailField label="End Date" value={formatDate(subProject.endDate)} />
          <DetailField
            label="Progress"
            value={subProject.progress != null ? `${Math.round(subProject.progress * 100)}%` : undefined}
          />
          <DetailField label="Budget" value={formatBudget(subProject.budget, subProject.currencyCode)} />
        </DetailFieldGrid>
      );
    }
    case 'floor': {
      const { floor } = detail;
      return (
        <DetailFieldGrid>
          <DetailField label="Code" value={floor.code} />
          <DetailField label="Level" value={floor.level} />
          <DetailField label="Area" value={floor.area != null ? `${floor.area} m²` : undefined} />
        </DetailFieldGrid>
      );
    }
    case 'buildingPart': {
      const { part } = detail;
      return (
        <DetailFieldGrid>
          <DetailField label="Code" value={part.code} />
          <DetailField label="Scope Level" value={part.scopeLevel} />
        </DetailFieldGrid>
      );
    }
    case 'workflow': {
      const { workflow } = detail;
      return (
        <DetailFieldGrid>
          <DetailField label="Status" value={workflow.status} />
          <DetailField label="Phase" value={workflow.phaseType?.replace(/_/g, ' ')} />
          <DetailField label="Progress" value={`${workflow.progress}%`} />
          <DetailField label="Steps" value={`${workflow.completedSteps}/${workflow.totalSteps} complete`} />
          <DetailField label="Template" value={workflow.templateCode ?? undefined} />
        </DetailFieldGrid>
      );
    }
    case 'step': {
      const { step } = detail;
      return (
        <DetailFieldGrid>
          <DetailField label="Type" value={STEP_TYPE_LABELS[step.type]} />
          <DetailField label="Status" value={step.status.replace(/_/g, ' ')} />
          <DetailField label="Assigned To" value={
            step.assigneeName && step.assigneeEmail
              ? `${step.assigneeName} (${step.assigneeEmail})`
              : step.assigneeEmail ?? step.assigneeName ?? step.assignedToName ?? undefined
          } />
          <DetailField label="Due Date" value={formatDate(step.dueAt)} />
        </DetailFieldGrid>
      );
    }
  }
}

interface EditSubProjectState {
  projectId: string;
  subProjectId: string;
  code: string;
  name: string;
  description: string;
  buildingType: SubProjectBuildingType;
  constructionPhase: SubProjectConstructionPhase;
  status: SubProjectStatus;
}

export function ProjectList({
  projects,
  constructionSites,
  createProjectAction,
  createSubProjectAction,
  deleteProjectAction,
  deleteSubProjectAction,
  assignUsersAction,
  removeUserAction,
  getAssignedUsersAction,
}: ProjectListProps) {
  const [createProjectOpen, setCreateProjectOpen] = useState(false);
  const [createProjectError, setCreateProjectError] = useState<string | null>(null);
  const [createSubProjectOpen, setCreateSubProjectOpen] = useState<string | null>(null);
  const [createSubProjectError, setCreateSubProjectError] = useState<string | null>(null);
  const [assignUsersOpen, setAssignUsersOpen] = useState<string | null>(null);
  const [assignedUsersDetail, setAssignedUsersDetail] = useState<ProjectAssignedUser[]>([]);
  const [loadingAssignedUsers, setLoadingAssignedUsers] = useState(false);
  const [selectedConSiteId, setSelectedConSiteId] = useState<string>('');
  const [selectedBuildingId, setSelectedBuildingId] = useState<string>('');
  const [subProjectProjectId, setSubProjectProjectId] = useState<string>('');
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);
  const [userRoles, setUserRoles] = useState<Record<string, ProjectUserRole>>({});
  const [defaultRoleForNewUsers, setDefaultRoleForNewUsers] = useState<ProjectUserRole>('VIEWER');

  const [detail, setDetail] = useState<DetailState | null>(null);
  const [editSubProjectOpen, setEditSubProjectOpen] = useState<EditSubProjectState | null>(null);
  const [savingSubProject, setSavingSubProject] = useState(false);

  const [deleteImpact, setDeleteImpact] = useState<{
    projectId: string;
    projectName: string;
    subProjectCount: number;
    workflowCount: number;
    sagaCount: number;
    loading: boolean;
  } | null>(null);

  const [workflowsBySubProject, setWorkflowsBySubProject] = useState<Record<string, Workflow[]>>({});
  const loadedOrLoadingWorkflows = useRef<Set<string>>(new Set());

  const loadWorkflows = (subProjectId: string) => {
    if (loadedOrLoadingWorkflows.current.has(subProjectId)) return;
    loadedOrLoadingWorkflows.current.add(subProjectId);
    fetch(`/api/workflows/subprojects/${subProjectId}`)
      .then((res) => {
        if (!res.ok) throw new Error('Failed to fetch workflows');
        return res.json() as Promise<SubProjectWorkflowResponse>;
      })
      .then((data) => {
        setWorkflowsBySubProject((prev) => ({ ...prev, [subProjectId]: data.workflows ?? [] }));
      })
      .catch(() => {
        setWorkflowsBySubProject((prev) => ({ ...prev, [subProjectId]: [] }));
      });
  };

  // Get users currently assigned to the project
  const getAssignedUsers = (projectId: string) => {
    const project = projects.find((p) => p.id === projectId);
    return project?.assignedUserIds || [];
  };

  // Get roles for users assigned to the project
  const getAssignedUserRoles = (projectId: string): Record<string, ProjectUserRole> => {
    const project = projects.find((p) => p.id === projectId);
    return (project?.assignedUserRoles as Record<string, ProjectUserRole>) || {};
  };

  // assignedUserIds is UUID-keyed and not human-readable on its own - resolve to emails whenever
  // the Assign Users popup opens for a project.
  useEffect(() => {
    if (!assignUsersOpen || !getAssignedUsersAction) {
      return;
    }
    let cancelled = false;
    const loadAssignedUsers = async () => {
      setLoadingAssignedUsers(true);
      try {
        const users = await getAssignedUsersAction(assignUsersOpen);
        if (!cancelled) setAssignedUsersDetail(users);
      } finally {
        if (!cancelled) setLoadingAssignedUsers(false);
      }
    };
    loadAssignedUsers();
    return () => {
      cancelled = true;
    };
  }, [assignUsersOpen, getAssignedUsersAction]);

  // Falls back to the raw stored ID while emails are loading, or if none was resolved for it
  // (e.g. getAssignedUsersAction isn't wired up, or the lookup failed for that one entry).
  const getDisplayEmail = (userId: string) => {
    const resolved = assignedUsersDetail.find((u) => u.userId === userId);
    return resolved?.email ?? userId;
  };

  const handleDeleteProject = (projectId: string) => {
    const project = projects.find((p) => p.id === projectId);
    setDeleteImpact({
      projectId,
      projectName: project?.name ?? 'this project',
      subProjectCount: project?.subProjects?.length ?? 0,
      workflowCount: 0,
      sagaCount: 0,
      loading: true,
    });
    Promise.all([
      apiClient.get<unknown[]>(`/api/workflows?projectId=${projectId}`).catch(() => []),
      apiClient.get<number>(`/api/sagas/count-by-project/${projectId}`).catch(() => 0),
    ]).then(([workflows, sagaCount]) => {
      // Guard against a slower first request resolving after the user closed this
      // modal and opened it again for a different project - only apply the result
      // if it still belongs to the project currently being previewed.
      setDeleteImpact((prev) =>
        prev && prev.projectId === projectId
          ? { ...prev, workflowCount: workflows.length, sagaCount, loading: false }
          : prev,
      );
    });
  };

  const confirmDeleteProject = async () => {
    if (!deleteImpact) return;
    await deleteProjectAction(deleteImpact.projectId);
    setDeleteImpact(null);
    window.location.reload();
  };

  const handleDeleteSubProject = async (projectId: string, subProjectId: string) => {
    if (!confirm('Are you sure you want to delete this subproject?')) return;
    const formData = new FormData();
    formData.set('projectId', projectId);
    formData.set('subProjectId', subProjectId);
    await deleteSubProjectAction(formData);
    window.location.reload();
  };

  const handleUpdateSubProject = async (edit: EditSubProjectState) => {
    try {
      setSavingSubProject(true);
      const response = await fetch(`/api/v1/projects/${edit.projectId}/subprojects/${edit.subProjectId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: edit.code,
          name: edit.name,
          description: edit.description || undefined,
          buildingType: edit.buildingType,
          constructionPhase: edit.constructionPhase,
          status: edit.status,
        }),
      });
      if (!response.ok) throw new Error('Failed to update subproject');
      setEditSubProjectOpen(null);
      window.location.reload();
    } catch {
      alert('Failed to update subproject');
    } finally {
      setSavingSubProject(false);
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between pb-2">
        <h3 className="text-base font-semibold ml-1">Projects</h3>
        <div className="flex gap-2">
          <Button onClick={() => setCreateProjectOpen(true)}>+ Add Project</Button>
          {projects.length > 0 && projects[0] && (
            <Button
              variant="outline"
              onClick={() => {
                setSelectedUserIds([]);
                setAssignUsersOpen(projects[0]!.id);
              }}
            >
              <Users className="mr-2 h-4 w-4" />
              Assign Users
            </Button>
          )}
        </div>
      </div>

      <Tree isEmpty={projects.length === 0} emptyMessage="No projects yet.">
        {projects.map((project) => {
          return (
            <TreeNode
              key={project.id}
              icon={<FolderKanban className="size-4 text-blue-500" />}
              label={`${project.code} · ${project.name}`}
              meta={
                <>
                  <Badge variant={statusVariant[project.status] ?? 'secondary'}>{project.status}</Badge>
                  <span className="truncate">{project.type}</span>
                </>
              }
              onSelect={() => setDetail({ kind: 'project', project })}
              actions={
                <>
                  <Button
                    size="sm"
                    variant="ghost"
                    className="size-7 p-0"
                    title="Add subproject"
                    onClick={() => {
                      setSubProjectProjectId(project.id);
                      setCreateSubProjectOpen(project.id);
                    }}
                  >
                    <Plus className="size-4" />
                  </Button>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button size="sm" variant="ghost" className="size-7 p-0">
                        <MoreHorizontal className="size-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem
                        onClick={() => {
                          const roles = getAssignedUserRoles(project.id);
                          setUserRoles(roles);
                          setSelectedUserIds(getAssignedUsers(project.id));
                          setAssignUsersOpen(project.id);
                        }}
                      >
                        <Users className="mr-2 h-4 w-4" />
                        Assign Users
                      </DropdownMenuItem>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem className="text-destructive" onClick={() => handleDeleteProject(project.id)}>
                        Delete Project
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </>
              }
            >
              {project.subProjects.length === 0 ? (
                <p className="py-2 text-sm text-muted-foreground">No subprojects yet.</p>
              ) : (
                project.subProjects.map((subProject) => {
                  const floors = subProject.floors ?? [];
                  const buildingParts = subProject.buildingParts ?? [];
                  return (
                  <TreeNode
                    key={subProject.id}
                    icon={<Building2 className="size-4 text-violet-500" />}
                    label={`${subProject.code} · ${subProject.name}`}
                    meta={
                      <>
                        <Badge variant={statusVariant[subProject.status] ?? 'secondary'}>{subProject.status}</Badge>
                        {subProject.constructionPhase ? (
                          <span className="truncate">{subProject.constructionPhase.replace(/_/g, ' ')}</span>
                        ) : null}
                      </>
                    }
                    onSelect={() => setDetail({ kind: 'subProject', project, subProject })}
                    actions={
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button size="sm" variant="ghost" className="size-7 p-0">
                            <MoreHorizontal className="size-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem
                            onClick={() =>
                              setEditSubProjectOpen({
                                projectId: project.id,
                                subProjectId: subProject.id,
                                code: subProject.code,
                                name: subProject.name,
                                description: subProject.description ?? '',
                                buildingType: subProject.buildingType,
                                constructionPhase: subProject.constructionPhase ?? 'PRE_CONSTRUCTION',
                                status: subProject.status,
                              })
                            }
                          >
                            Edit
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            className="text-destructive"
                            onClick={() => handleDeleteSubProject(project.id, subProject.id)}
                          >
                            Delete SubProject
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    }
                  >
                    <TreeNode key="floors" label={`Floors (${floors.length})`}>
                      {floors.length === 0 ? (
                        <p className="py-2 text-sm text-muted-foreground">No floors yet.</p>
                      ) : (
                        floors.map((floor) => (
                          <TreeNode
                            key={floor.id}
                            icon={<Layers className="size-4 text-amber-500" />}
                            label={floor.name}
                            meta={
                              <span className="truncate">
                                Level {floor.level}
                                {floor.area ? ` · ${floor.area} m²` : ''}
                              </span>
                            }
                            onSelect={() => setDetail({ kind: 'floor', project, subProject, floor })}
                          />
                        ))
                      )}
                    </TreeNode>
                    <TreeNode key="buildingParts" label={`Building Parts (${buildingParts.length})`}>
                      {buildingParts.length === 0 ? (
                        <p className="py-2 text-sm text-muted-foreground">No building parts yet.</p>
                      ) : (
                        buildingParts.map((part) => (
                          <TreeNode
                            key={part.id}
                            icon={<Blocks className="size-4 text-amber-500" />}
                            label={part.name}
                            meta={<Badge variant="outline">{part.scopeLevel}</Badge>}
                            onSelect={() => setDetail({ kind: 'buildingPart', project, subProject, part })}
                          />
                        ))
                      )}
                    </TreeNode>
                    <TreeNode
                      key="workflows"
                      label={`Workflows${workflowsBySubProject[subProject.id] ? ` (${workflowsBySubProject[subProject.id]!.length})` : ''}`}
                      onExpand={() => loadWorkflows(subProject.id)}
                    >
                      {workflowsBySubProject[subProject.id] === undefined ? (
                        <p className="py-2 text-sm text-muted-foreground">Loading workflows…</p>
                      ) : workflowsBySubProject[subProject.id]!.length === 0 ? (
                        <p className="py-2 text-sm text-muted-foreground">No workflows yet.</p>
                      ) : (
                        workflowsBySubProject[subProject.id]!.map((workflow) => {
                          const steps = workflow.steps ?? [];
                          return (
                          <TreeNode
                            key={workflow.id}
                            icon={<WorkflowIcon className="size-4 text-amber-500" />}
                            label={workflow.name}
                            meta={
                              <Badge variant={WORKFLOW_STATUS_VARIANTS[workflow.status] ?? 'secondary'}>
                                {workflow.status}
                              </Badge>
                            }
                            onSelect={() => setDetail({ kind: 'workflow', project, subProject, workflow })}
                          >
                            {steps.length === 0
                              ? undefined
                              : [...steps]
                                  .sort((a, b) => a.order - b.order)
                                  .map((step) => (
                                    <TreeNode
                                      key={step.id}
                                      icon={<ListChecks className="size-4 text-emerald-500" />}
                                      label={step.name}
                                      meta={
                                        <Badge variant={WORKFLOW_STEP_STATUS_VARIANTS[step.status] ?? 'secondary'}>
                                          {step.status.replace(/_/g, ' ')}
                                        </Badge>
                                      }
                                      onSelect={() => setDetail({ kind: 'step', project, subProject, workflow, step })}
                                    />
                                  ))}
                          </TreeNode>
                          );
                        })
                      )}
                    </TreeNode>
                  </TreeNode>
                  );
                })
              )}
            </TreeNode>
          );
        })}
      </Tree>

      {/* Detail Popup */}
      <DetailPopup
        open={detail !== null}
        onOpenChange={(open) => {
          if (!open) setDetail(null);
        }}
        title={detail ? getDetailTitle(detail) : ''}
        description={detail ? getDetailDescription(detail) : undefined}
        footer={
          detail && getManageWorkflowsHref(detail) ? (
            <Button asChild variant="outline" size="sm">
              <Link href={getManageWorkflowsHref(detail)!}>Manage Workflows →</Link>
            </Button>
          ) : undefined
        }
      >
        {detail ? <DetailContent detail={detail} constructionSites={constructionSites} /> : null}
      </DetailPopup>

      {/* Create Project Modal */}
      <Modal open={createProjectOpen} onClose={() => { setCreateProjectOpen(false); setCreateProjectError(null); }} title="Create project">
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            const form = e.currentTarget;
            const formData = new FormData(form);
            formData.set('constructionSiteId', selectedConSiteId);
            setCreateProjectError(null);
            const result = await createProjectAction(formData);
            if (result.error) {
              setCreateProjectError(result.error);
              return;
            }
            setCreateProjectOpen(false);
            window.location.reload();
          }}
          className="flex min-w-0 max-h-[65vh] flex-col gap-4"
        >
          <div className="min-w-0 flex-1 overflow-y-auto">
            <div className="grid gap-2">
              <Label htmlFor="project-code">Code</Label>
              <Input id="project-code" name="code" placeholder="PRJ-001" required />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="project-name">Name</Label>
              <Input id="project-name" name="name" placeholder="Residential Complex" required />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="project-construction-site">Construction Site</Label>
              <select
                id="project-construction-site"
                name="constructionSiteId"
                value={selectedConSiteId}
                onChange={(e) => setSelectedConSiteId(e.target.value)}
                className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
              >
                <option value="">Select a construction site (optional)</option>
                {constructionSites.map((site) => (
                  <option key={site.id} value={site.id}>
                    {site.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="project-description">Description</Label>
              <textarea
                id="project-description"
                name="description"
                rows={2}
                placeholder="Optional description..."
                className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="grid gap-2">
                <Label htmlFor="project-type">Type</Label>
                <select
                  id="project-type"
                  name="type"
                  defaultValue="OTHER"
                  className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
                >
                  <option value="RESIDENTIAL">Residential</option>
                  <option value="COMMERCIAL">Commercial</option>
                  <option value="INDUSTRIAL">Industrial</option>
                  <option value="INFRASTRUCTURE">Infrastructure</option>
                  <option value="MIXED_USE">Mixed Use</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div className="grid gap-2">
                <Label htmlFor="project-status">Status</Label>
                <select
                  id="project-status"
                  name="status"
                  defaultValue="PLANNED"
                  className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
                >
                  <option value="PLANNED">Planned</option>
                  <option value="ACTIVE">Active</option>
                  <option value="ON_HOLD">On Hold</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="grid gap-2">
                <Label htmlFor="project-startDate">Start Date</Label>
                <Input id="project-startDate" name="startDate" type="date" />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="project-endDate">End Date</Label>
                <Input id="project-endDate" name="endDate" type="date" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="grid gap-2">
                <Label htmlFor="project-budget">Budget</Label>
                <Input
                  id="project-budget"
                  name="budget"
                  type="number"
                  min="0"
                  step="0.01"
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="project-currencyCode">Currency</Label>
                <Input id="project-currencyCode" name="currencyCode" defaultValue="EUR" />
              </div>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="project-addressLine1">Address Line 1</Label>
              <Input
                id="project-addressLine1"
                name="addressLine1"
                placeholder="123 Main Street"
              />
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div className="grid gap-2">
                <Label htmlFor="project-city">City</Label>
                <Input id="project-city" name="city" placeholder="Berlin" />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="project-postalCode">Postal Code</Label>
                <Input id="project-postalCode" name="postalCode" placeholder="10115" />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="project-countryCode">Country Code</Label>
                <Input id="project-countryCode" name="countryCode" placeholder="DE" />
              </div>
            </div>
          </div>
          <div className="shrink-0 pt-2">
            {createProjectError && (
              <p className="mb-2 text-sm text-destructive">{createProjectError}</p>
            )}
            <Button type="submit" className="w-full">Create Project</Button>
          </div>
        </form>
      </Modal>

      {/* Assign Users Modal */}
      {assignUsersOpen && (
        <Modal
          open={!!assignUsersOpen}
          onClose={() => {
            setAssignUsersOpen(null);
            setSelectedUserIds([]);
            setUserRoles({});
            setAssignedUsersDetail([]);
          }}
          title={`Assign users to ${projects.find(p => p.id === assignUsersOpen)?.name ?? 'project'}`}
        >
          <div className="flex flex-col gap-4">
            <div className="rounded-lg border p-4">
              <div className="flex items-center justify-between mb-3">
                <p className="text-sm text-muted-foreground">
                  {getAssignedUsers(assignUsersOpen).length === 0
                    ? 'No users assigned yet.'
                    : `Assigned users:`
                  }
                </p>
                <div className="flex items-center gap-2">
                  <label className="text-xs font-medium text-muted-foreground">Default role:</label>
                  <RoleDropdown
                    value={defaultRoleForNewUsers}
                    onChange={setDefaultRoleForNewUsers}
                    size="sm"
                  />
                </div>
              </div>
              {getAssignedUsers(assignUsersOpen).length > 0 && (
                <div className="flex flex-col gap-2">
                  {getAssignedUsers(assignUsersOpen).map((userId) => {
                    // Use userRoles state for reactive display of selected role
                    const selectedRole = userRoles[userId] || getAssignedUserRoles(assignUsersOpen)[userId] || 'VIEWER';
                    return (
                      <div key={userId} className="flex items-center justify-between gap-2">
                        <div className="flex items-center gap-2">
                          <Badge variant="secondary">{loadingAssignedUsers ? '…' : getDisplayEmail(userId)}</Badge>
                          <Badge variant={selectedRole === 'OWNER' ? 'destructive' : selectedRole === 'MANAGER' ? 'default' : selectedRole === 'SUPERVISOR' ? 'outline' : 'secondary'}>{selectedRole}</Badge>
                        </div>
                        <div className="flex items-center gap-1">
                          <RoleDropdown
                            value={selectedRole}
                            onChange={(newRole) => {
                              setUserRoles({ ...userRoles, [userId]: newRole });
                            }}
                            size="sm"
                          />
                          {removeUserAction && (
                            <Button
                              size="sm"
                              variant="ghost"
                              className="h-6 w-6 p-0 hover:bg-destructive/20"
                              type="button"
                              onClick={async () => {
                                const result = await removeUserAction(assignUsersOpen, userId);
                                if (!result.error) {
                                  window.location.reload();
                                }
                              }}
                            >
                              ×
                            </Button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="grid gap-2">
              <Label htmlFor="assign-user-input">Add user by email</Label>
              <div className="flex flex-col gap-2">
                <div className="flex gap-2">
                  <Input
                    id="assign-user-input"
                    placeholder="Enter user email (or comma-separated emails)"
                    value={selectedUserIds.join(', ')}
                    onChange={(e) => {
                      const emails = e.target.value.split(',').map(s => s.trim()).filter(Boolean);
                      setSelectedUserIds(emails);
                      // Initialize default role for new emails based on current default selection
                      const newRoles = { ...userRoles };
                      emails.forEach(email => {
                        if (!newRoles[email]) {
                          newRoles[email] = defaultRoleForNewUsers;
                        }
                      });
                      setUserRoles(newRoles);
                    }}
                  />
                  {assignUsersAction && (
                    <Button
                      type="button"
                      onClick={async () => {
                        if (selectedUserIds.length === 0) {
                          alert('Please enter at least one email');
                          return;
                        }
                        // Build assignments array with roles from userRoles state
                        const assignments: Array<{ userId: string; role: ProjectUserRole }> = selectedUserIds.map(email => ({
                          userId: email,
                          role: userRoles[email] || defaultRoleForNewUsers,
                        }));
                        const result = await assignUsersAction(assignUsersOpen, assignments);
                        if (result.error) {
                          alert(result.error);
                        } else {
                          // Keep modal open, clear selected users and reset default role
                          setSelectedUserIds([]);
                          setDefaultRoleForNewUsers('VIEWER');
                        }
                      }}
                    >
                      Assign
                    </Button>
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  Enter one or more email addresses separated by commas. Use the role selector above to set the default role for new assignments.
                </p>
              </div>
            </div>

            {/* Modal footer with Cancel/Done buttons */}
            <div className="flex items-center justify-end gap-2 pt-2 border-t">
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  setAssignUsersOpen(null);
                  setSelectedUserIds([]);
                  setUserRoles({});
                  setDefaultRoleForNewUsers('VIEWER');
                  setAssignedUsersDetail([]);
                }}
              >
                Cancel
              </Button>
              <Button
                type="button"
                onClick={async () => {
                  // Apply any pending role changes to existing assigned users
                  if (assignUsersAction) {
                    const assignedUsers = getAssignedUsers(assignUsersOpen);
                    const pendingAssignments: Array<{ userId: string; role: ProjectUserRole }> = assignedUsers.map(userId => ({
                      userId,
                      role: userRoles[userId] || getAssignedUserRoles(assignUsersOpen)[userId] || 'VIEWER',
                    }));
                    if (pendingAssignments.length > 0) {
                      const result = await assignUsersAction(assignUsersOpen, pendingAssignments);
                      if (result.error) {
                        alert(result.error);
                        return;
                      }
                    }
                  }
                  // Close modal on success
                  setAssignUsersOpen(null);
                  setSelectedUserIds([]);
                  setUserRoles({});
                  setDefaultRoleForNewUsers('VIEWER');
                  window.location.reload();
                }}
              >
                Done
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Create SubProject Modal */}
      <Modal
        open={!!createSubProjectOpen}
        onClose={() => {
          setCreateSubProjectOpen(null);
          setSelectedBuildingId('');
          setCreateSubProjectError(null);
        }}
        title="Create subproject"
      >
        {createSubProjectOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('projectId', subProjectProjectId);
              if (!selectedBuildingId) {
                alert('Please select a building');
                return;
              }
              setCreateSubProjectError(null);
              const result = await createSubProjectAction(formData);
              if (result.error) {
                setCreateSubProjectError(result.error);
                return;
              }
              setCreateSubProjectOpen(null);
              setSelectedBuildingId('');
              setSubProjectProjectId('');
              window.location.reload();
            }}
            className="flex min-w-0 max-h-[65vh] flex-col gap-4"
          >
            <div className="min-w-0 flex-1 overflow-y-auto">
              <div className="grid gap-2">
                <Label htmlFor="subproject-code">Code</Label>
                <Input id="subproject-code" name="code" placeholder="BLD-A" required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="subproject-name">Name</Label>
                <Input id="subproject-name" name="name" placeholder="Building A" required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="subproject-building">Building (required - 1 Building = 1 SubProject)</Label>
                {(() => {
                  const project = projects.find((p) => p.id === subProjectProjectId);
                  const constructionSiteId = project?.constructionSiteId || '';
                  const selectedSite = constructionSites.find(
                    (site) => site.id === constructionSiteId,
                  );
                  const buildings = selectedSite?.buildings ?? [];
                  return (
                    <>
                      <select
                        id="subproject-building"
                        name="buildingId"
                        value={selectedBuildingId}
                        onChange={(e) => setSelectedBuildingId(e.target.value)}
                        className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
                        disabled={!constructionSiteId}
                      >
                        {!constructionSiteId && <option value="">No construction site assigned</option>}
                        {constructionSiteId && buildings.length === 0 && <option value="">No buildings available</option>}
                        {constructionSiteId && buildings.length > 0 && (
                          <>
                            <option value="">Select a building</option>
                            {buildings.map((building) => (
                              <option key={building.id} value={building.id}>
                                {building.name}
                              </option>
                            ))}
                          </>
                        )}
                      </select>
                      {!constructionSiteId && (
                        <p className="text-xs text-muted-foreground">
                          This project has no construction site assigned. Please assign a construction site to the project first.
                        </p>
                      )}
                      {constructionSiteId && buildings.length === 0 && (
                        <p className="text-xs text-destructive">
                          ⚠️ No buildings exist for this construction site. You must create at least one building before creating a subProject.
                        </p>
                      )}
                    </>
                  );
                })()}
              </div>
              <div className="grid gap-2">
                <Label htmlFor="subproject-description">Description</Label>
                <textarea
                  id="subproject-description"
                  name="description"
                  rows={2}
                  placeholder="Optional description..."
                  className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="grid gap-2">
                  <Label htmlFor="subproject-buildingType">Building Type</Label>
                  <select
                    id="subproject-buildingType"
                    name="buildingType"
                    defaultValue="OTHER"
                    className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
                  >
                    <option value="RESIDENTIAL">Residential</option>
                    <option value="COMMERCIAL">Commercial</option>
                    <option value="INDUSTRIAL">Industrial</option>
                    <option value="MIXED_USE">Mixed Use</option>
                    <option value="INFRASTRUCTURE">Infrastructure</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="subproject-constructionPhase">Construction Phase</Label>
                  <select
                    id="subproject-constructionPhase"
                    name="constructionPhase"
                    defaultValue="PRE_CONSTRUCTION"
                    className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
                  >
                    <option value="PRE_CONSTRUCTION">Pre-Construction</option>
                    <option value="SITE_PREP">Site Prep</option>
                    <option value="FOUNDATION">Foundation</option>
                    <option value="BASEMENT">Basement</option>
                    <option value="SUPERSTRUCTURE">Superstructure</option>
                    <option value="ROOF">Roof</option>
                    <option value="BUILDING_ENVELOPE">Building Envelope</option>
                    <option value="MEP_ROUGHIN">MEP Rough-in</option>
                    <option value="INTERIOR_FINISH">Interior Finish</option>
                    <option value="EXTERIOR_WORKS">Exterior Works</option>
                    <option value="HANDOVER">Handover</option>
                  </select>
                </div>
              </div>
            </div>
            <div className="shrink-0 pt-2">
            {createSubProjectError && (
              <p className="mb-2 text-sm text-destructive">{createSubProjectError}</p>
            )}
            <Button type="submit" className="w-full" disabled={!(() => {
                const project = projects.find((p) => p.id === subProjectProjectId);
                const constructionSiteId = project?.constructionSiteId || '';
                const selectedSite = constructionSites.find(
                  (site) => site.id === constructionSiteId,
                );
                const buildings = selectedSite?.buildings ?? [];
                return buildings.length > 0 && selectedBuildingId !== '';
              })()}>
                Create SubProject
              </Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Edit SubProject Modal */}
      <Modal
        open={!!editSubProjectOpen}
        onClose={() => setEditSubProjectOpen(null)}
        title="Edit subproject"
      >
        {editSubProjectOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              await handleUpdateSubProject(editSubProjectOpen);
            }}
            className="flex min-w-0 max-h-[65vh] flex-col gap-4"
          >
            <div className="min-w-0 flex-1 overflow-y-auto">
              <div className="grid gap-2">
                <Label htmlFor="edit-subproject-code">Code</Label>
                <Input
                  id="edit-subproject-code"
                  value={editSubProjectOpen.code}
                  onChange={(e) => setEditSubProjectOpen({ ...editSubProjectOpen, code: e.target.value })}
                  required
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-subproject-name">Name</Label>
                <Input
                  id="edit-subproject-name"
                  value={editSubProjectOpen.name}
                  onChange={(e) => setEditSubProjectOpen({ ...editSubProjectOpen, name: e.target.value })}
                  required
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-subproject-description">Description</Label>
                <textarea
                  id="edit-subproject-description"
                  rows={2}
                  className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  value={editSubProjectOpen.description}
                  onChange={(e) => setEditSubProjectOpen({ ...editSubProjectOpen, description: e.target.value })}
                  placeholder="Optional description..."
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="grid gap-2">
                  <Label htmlFor="edit-subproject-buildingType">Building Type</Label>
                  <select
                    id="edit-subproject-buildingType"
                    value={editSubProjectOpen.buildingType}
                    onChange={(e) =>
                      setEditSubProjectOpen({ ...editSubProjectOpen, buildingType: e.target.value as SubProjectBuildingType })
                    }
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
                  <Label htmlFor="edit-subproject-constructionPhase">Construction Phase</Label>
                  <select
                    id="edit-subproject-constructionPhase"
                    value={editSubProjectOpen.constructionPhase}
                    onChange={(e) =>
                      setEditSubProjectOpen({
                        ...editSubProjectOpen,
                        constructionPhase: e.target.value as SubProjectConstructionPhase,
                      })
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
                <Label htmlFor="edit-subproject-status">Status</Label>
                <select
                  id="edit-subproject-status"
                  value={editSubProjectOpen.status}
                  onChange={(e) => setEditSubProjectOpen({ ...editSubProjectOpen, status: e.target.value as SubProjectStatus })}
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
            <div className="shrink-0 pt-2">
              <Button type="submit" className="w-full" disabled={savingSubProject}>
                {savingSubProject ? 'Saving...' : 'Save Changes'}
              </Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Delete Project Impact Modal */}
      {deleteImpact && (
        <Modal
          open={!!deleteImpact}
          onClose={() => setDeleteImpact(null)}
          title={`Delete ${deleteImpact.projectName}?`}
        >
          <div className="flex flex-col gap-4">
            <p className="text-sm text-muted-foreground">
              The project will be deleted. Everything below will be archived (hidden, and kept for
              audit purposes - not permanently erased):
            </p>
            <ul className="flex flex-col gap-1 text-sm">
              <li>{deleteImpact.loading ? '…' : deleteImpact.subProjectCount} SubProject(s)</li>
              <li>
                {deleteImpact.loading ? '…' : deleteImpact.workflowCount} Workflow(s) and their steps
              </li>
              <li>{deleteImpact.loading ? '…' : deleteImpact.sagaCount} Bidding/Procurement record(s)</li>
            </ul>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setDeleteImpact(null)}>
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={confirmDeleteProject}
                disabled={deleteImpact.loading}
              >
                Delete Project
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
