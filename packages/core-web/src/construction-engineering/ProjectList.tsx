'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import type { Project, ConstructionSite, ProjectUserRole } from '@cerp/shared-types';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Modal } from '../components/ui/modal';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { ChevronDown, ChevronRight, MoreHorizontal, Users } from 'lucide-react';

/** Reusable role dropdown component */
function RoleDropdown({ value, onChange, size }: { value: ProjectUserRole; onChange: (value: ProjectUserRole) => void; size?: 'sm' | 'md' }) {
  const ROLE_OPTIONS: { value: ProjectUserRole; label: string }[] = [
    { value: 'VIEWER', label: 'Viewer' },
    { value: 'EDITOR', label: 'Editor' },
    { value: 'ADMIN', label: 'Admin' },
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
  createProjectAction: (formData: FormData) => Promise<void>;
  createSubProjectAction: (formData: FormData) => Promise<void>;
  deleteProjectAction: (projectId: string) => Promise<void>;
  deleteSubProjectAction: (formData: FormData) => Promise<void>;
  onCreateWorkOrder: (project: Project, subProject?: { id: string; name: string }) => void;
  /** Assign users with roles: [{ userId, role }] */
  assignUsersAction?: (projectId: string, assignments: Array<{ userId: string; role: ProjectUserRole }>) => Promise<{ error?: string }>;
  removeUserAction?: (projectId: string, userId: string) => Promise<{ error?: string }>;
  currentUserEmail?: string;
}

const statusVariant: Record<string, 'default' | 'secondary' | 'success' | 'destructive' | 'outline'> = {
  PLANNED: 'secondary',
  ACTIVE: 'default',
  ON_HOLD: 'secondary',
  COMPLETED: 'success',
  CANCELLED: 'destructive',
};

function ProjectRow({
  project,
  constructionSites,
  isExpanded,
  onToggle,
  onCreateWorkOrder,
  onCreateSubProject,
  onDeleteProject,
  onDeleteSubProject,
  onAssignUsers,
}: {
  project: Project;
  constructionSites: ConstructionSite[];
  isExpanded: boolean;
  onToggle: () => void;
  onCreateWorkOrder: (project: Project, subProject?: { id: string; name: string }) => void;
  onCreateSubProject: (projectId: string) => void;
  onDeleteProject: (projectId: string) => Promise<void>;
  onDeleteSubProject: (formData: FormData) => Promise<void>;
  onAssignUsers: (projectId: string) => void;
}) {
  const router = useRouter();
  const site = constructionSites.find((s) => s.id === project.constructionSiteId);

  const handleSubProjectClick = (subProjectId: string) => {
    router.push(`/construction/projects/${project.id}/subprojects/${subProjectId}`);
  };

  return (
    <>
      <TableRow className="cursor-pointer hover:bg-muted/30">
        <TableCell className="font-medium">
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              className="p-0 h-6 w-6 hover:bg-transparent"
              onClick={(e) => {
                e.stopPropagation();
                onToggle();
              }}
            >
              {isExpanded ? (
                <ChevronDown className="h-4 w-4" />
              ) : (
                <ChevronRight className="h-4 w-4" />
              )}
            </Button>
            <Link
              href={`/construction/projects/${project.id}`}
              className="hover:underline text-blue-600 dark:text-blue-400"
              onClick={(e) => e.stopPropagation()}
            >
              {project.code}
            </Link>
          </div>
        </TableCell>
        <TableCell>
          <Link
            href={`/construction/projects/${project.id}`}
            className="hover:underline text-blue-600 dark:text-blue-400"
            onClick={(e) => e.stopPropagation()}
          >
            {project.name}
          </Link>
        </TableCell>
        <TableCell>
          <Badge variant={statusVariant[project.status] ?? 'secondary'}>{project.status}</Badge>
        </TableCell>
        <TableCell>{project.type}</TableCell>
        <TableCell>{site?.name ?? '—'}</TableCell>
        <TableCell className="text-sm text-muted-foreground">
          {project.startDate ? new Date(project.startDate).toLocaleDateString() : '—'}
        </TableCell>
        <TableCell className="text-sm text-muted-foreground">
          {project.endDate ? new Date(project.endDate).toLocaleDateString() : '—'}
        </TableCell>
        <TableCell onClick={(e) => e.stopPropagation()}>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                <span className="sr-only">Open menu</span>
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => onAssignUsers(project.id)}>
                <Users className="mr-2 h-4 w-4" />
                Assign Users
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onCreateWorkOrder(project)}>
                Create Work Order
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => onCreateSubProject(project.id)}>
                Create SubProject
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem
                className="text-destructive"
                onClick={async () => {
                  await onDeleteProject(project.id);
                  window.location.reload();
                }}
              >
                Delete Project
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </TableCell>
      </TableRow>
      {isExpanded && project.subProjects.length > 0 && (
        <TableRow>
          <TableCell colSpan={8} className="bg-muted/20">
            <div className="px-4 py-3">
              <div className="flex items-center gap-2 mb-2">
                <ChevronDown className="h-3 w-3 text-muted-foreground" />
                <span className="text-xs font-medium text-muted-foreground">SubProjects</span>
              </div>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[100px]">Code</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Phase</TableHead>
                    <TableHead className="text-right w-[120px]">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {project.subProjects.map((subProject) => (
                    <TableRow
                      key={subProject.id}
                      className="cursor-pointer hover:bg-muted/30"
                      onClick={() => handleSubProjectClick(subProject.id)}
                    >
                      <TableCell className="font-medium" onClick={(e) => e.stopPropagation()}>
                        <Badge variant="secondary">{subProject.code}</Badge>
                      </TableCell>
                      <TableCell onClick={(e) => e.stopPropagation()}>{subProject.name}</TableCell>
                      <TableCell onClick={(e) => e.stopPropagation()}>
                        <Badge variant={statusVariant[subProject.status] ?? 'secondary'}>
                          {subProject.status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground" onClick={(e) => e.stopPropagation()}>
                        {subProject.constructionPhase ? subProject.constructionPhase.replace(/_/g, ' ') : '—'}
                      </TableCell>
                      <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                        <div className="flex justify-end gap-1">
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-7 text-xs"
                            type="button"
                            onClick={() => onCreateWorkOrder(project, subProject)}
                          >
                            Work Order
                          </Button>
                          <form key={`delete-sp-${subProject.id}`} action={onDeleteSubProject}>
                            <input type="hidden" name="projectId" value={project.id} />
                            <input type="hidden" name="subProjectId" value={subProject.id} />
                            <Button
                              size="sm"
                              variant="destructive"
                              className="h-7 text-xs"
                              type="submit"
                            >
                              Delete
                            </Button>
                          </form>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </TableCell>
        </TableRow>
      )}
    </>
  );
}

export function ProjectList({
  projects,
  constructionSites,
  createProjectAction,
  createSubProjectAction,
  deleteProjectAction,
  deleteSubProjectAction,
  onCreateWorkOrder,
  assignUsersAction,
  removeUserAction,
  currentUserEmail,
}: ProjectListProps) {
  // Debug: log props to verify they are being passed
  console.log('[ProjectList DEBUG] projects:', projects?.length, 'assignUsersAction:', !!assignUsersAction, 'removeUserAction:', !!removeUserAction, 'currentUserEmail:', currentUserEmail);
  const [createProjectOpen, setCreateProjectOpen] = useState(false);
  const [createSubProjectOpen, setCreateSubProjectOpen] = useState<string | null>(null);
  const [assignUsersOpen, setAssignUsersOpen] = useState<string | null>(null);
  const [expandedProjectId, setExpandedProjectId] = useState<string | null>(null);
  const [selectedConSiteId, setSelectedConSiteId] = useState<string>('');
  const [selectedBuildingId, setSelectedBuildingId] = useState<string>('');
  const [subProjectProjectId, setSubProjectProjectId] = useState<string>('');
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);
  const [userRoles, setUserRoles] = useState<Record<string, ProjectUserRole>>({});
  const [defaultRoleForNewUsers, setDefaultRoleForNewUsers] = useState<ProjectUserRole>('VIEWER');

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

      {projects.length === 0 ? (
        <p className="py-6 text-center text-sm text-muted-foreground">No projects yet.</p>
      ) : (
        <div className="rounded-md border">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-[120px]">Code</TableHead>
                <TableHead className="min-w-[180px]">Name</TableHead>
                <TableHead className="w-[120px]">Status</TableHead>
                <TableHead className="w-[140px]">Type</TableHead>
                <TableHead className="w-[160px]">Construction Site</TableHead>
                <TableHead className="w-[110px]">Start Date</TableHead>
                <TableHead className="w-[110px]">End Date</TableHead>
                <TableHead className="w-[60px] text-right">⋯</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {projects.map((project) => (
                <ProjectRow
                  key={project.id}
                  project={project}
                  constructionSites={constructionSites}
                  isExpanded={expandedProjectId === project.id}
                  onToggle={() =>
                    setExpandedProjectId(expandedProjectId === project.id ? null : project.id)
                  }
                  onCreateWorkOrder={onCreateWorkOrder}
                  onCreateSubProject={(projectId) => {
                    setSubProjectProjectId(projectId);
                    setCreateSubProjectOpen(projectId);
                  }}
                  onDeleteProject={deleteProjectAction}
                  onDeleteSubProject={deleteSubProjectAction}
                  onAssignUsers={(projectId) => {
                    const roles = getAssignedUserRoles(projectId);
                    setUserRoles(roles);
                    setSelectedUserIds(getAssignedUsers(projectId));
                    setAssignUsersOpen(projectId);
                  }}
                />
              ))}
            </TableBody>
          </Table>
        </div>
      )}

      {/* Create Project Modal */}
      <Modal open={createProjectOpen} onClose={() => setCreateProjectOpen(false)} title="Create project">
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            const form = e.currentTarget;
            const formData = new FormData(form);
            formData.set('constructionSiteId', selectedConSiteId);
            await createProjectAction(formData);
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
                          <Badge variant="secondary">{userId}</Badge>
                          <Badge variant={selectedRole === 'ADMIN' ? 'destructive' : selectedRole === 'EDITOR' ? 'default' : 'secondary'}>{selectedRole}</Badge>
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
              await createSubProjectAction(formData);
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
    </div>
  );
}