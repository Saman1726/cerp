'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import type { Project, ConstructionSite } from '@cerp/shared-types';
import { Badge } from '@cerp/core-web/components/ui/badge';
import { Button } from '@cerp/core-web/components/ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@cerp/core-web/components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@cerp/core-web/components/ui/dropdown-menu';
import { Modal } from '@cerp/core-web/components/ui/modal';
import { Input } from '@cerp/core-web/components/ui/input';
import { Label } from '@cerp/core-web/components/ui/label';
import { ChevronDown, ChevronRight, MoreHorizontal } from 'lucide-react';

const statusVariant: Record<string, 'default' | 'secondary' | 'success' | 'destructive' | 'outline'> = {
  PLANNED: 'secondary',
  ACTIVE: 'default',
  ON_HOLD: 'secondary',
  COMPLETED: 'success',
  CANCELLED: 'destructive',
};

export default function ProjectDetailClient({
  project,
  projectId,
  constructionSites,
}: {
  project: Project;
  projectId: string;
  constructionSites: ConstructionSite[];
}) {
  const router = useRouter();
  const [createSubProjectOpen, setCreateSubProjectOpen] = useState(false);
  const [expandedSubProjectId, setExpandedSubProjectId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [selectedBuildingId, setSelectedBuildingId] = useState<string>('');

  const site = constructionSites.find((s) => s.id === project.constructionSiteId);

  const handleSubProjectClick = (subProjectId: string) => {
    router.push(`/construction/projects/${projectId}/subprojects/${subProjectId}`);
  };

  const handleDeleteProject = async (pId: string) => {
    try {
      setLoading(true);
      const response = await fetch(`/api/projects/v1/projects/${pId}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) throw new Error('Failed to delete project');
      window.location.href = '/construction';
    } catch {
      alert('Failed to delete project');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteSubProject = async (projId: string, subProjId: string) => {
    try {
      setLoading(true);
      const response = await fetch(`/api/v1/projects/${projId}/subprojects/${subProjId}`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
      });
      if (!response.ok) throw new Error('Failed to delete subproject');
      window.location.reload();
    } catch {
      alert('Failed to delete subproject');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateSubProject = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!selectedBuildingId) {
      alert('Please select a building');
      return;
    }

    const formData = new FormData(e.currentTarget);

    try {
      setLoading(true);
      const response = await fetch(`/api/v1/projects/${projectId}/subprojects`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          code: formData.get('code'),
          name: formData.get('name'),
          buildingId: selectedBuildingId,
          description: formData.get('description')?.toString() || undefined,
          buildingType: formData.get('buildingType') || 'OTHER',
          constructionPhase: formData.get('constructionPhase') || 'PRE_CONSTRUCTION',
        }),
      });
      if (!response.ok) throw new Error('Failed to create subproject');
      setCreateSubProjectOpen(false);
      setSelectedBuildingId('');
      window.location.reload();
    } catch {
      alert('Failed to create subproject');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col gap-4">
        <Link
          href="/construction"
          className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        >
          <span className="mr-1">←</span> Back to Projects
        </Link>
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-semibold">{project.name}</h1>
              <Badge variant={statusVariant[project.status] ?? 'secondary'}>{project.status}</Badge>
            </div>
            {project.code && (
              <p className="mt-1 text-sm text-muted-foreground font-mono">{project.code}</p>
            )}
            {project.description && (
              <p className="mt-2 text-sm text-muted-foreground">{project.description}</p>
            )}
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="sm">
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem
                onClick={() => {
                  if (confirm('Are you sure you want to delete this project?')) {
                    handleDeleteProject(project.id);
                  }
                }}
                className="text-destructive"
              >
                Delete Project
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {/* Project Details Grid */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Type</p>
          <p className="mt-1 text-sm font-medium">{project.type}</p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Construction Site</p>
          <p className="mt-1 text-sm font-medium">{site?.name ?? '—'}</p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Start Date</p>
          <p className="mt-1 text-sm font-medium">
            {project.startDate ? new Date(project.startDate).toLocaleDateString() : '—'}
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">End Date</p>
          <p className="mt-1 text-sm font-medium">
            {project.endDate ? new Date(project.endDate).toLocaleDateString() : '—'}
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Budget</p>
          <p className="mt-1 text-sm font-medium">
            {project.budget ? `${project.budget} ${project.currencyCode}` : '—'}
          </p>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-xs font-medium text-muted-foreground">Address</p>
          <p className="mt-1 text-sm font-medium">
            {[project.address, project.city, project.country].filter(Boolean).join(', ') || '—'}
          </p>
        </div>
      </div>

      {/* SubProjects Section */}
      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">SubProjects</h2>
          <Button onClick={() => setCreateSubProjectOpen(true)}>+ Add SubProject</Button>
        </div>

        {project.subProjects.length === 0 ? (
          <p className="py-6 text-center text-sm text-muted-foreground">No subprojects yet.</p>
        ) : (
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[100px]">Code</TableHead>
                  <TableHead>Name</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Phase</TableHead>
                  <TableHead>Dates</TableHead>
                  <TableHead className="w-[60px] text-right">⋯</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {project.subProjects.map((subProject) => (
                  <SubProjectRow
                    key={subProject.id}
                    subProject={subProject}
                    projectId={project.id}
                    projectIdPath={projectId}
                    isExpanded={expandedSubProjectId === subProject.id}
                    onToggle={() =>
                      setExpandedSubProjectId(expandedSubProjectId === subProject.id ? null : subProject.id)
                    }
                    onDelete={handleDeleteSubProject}
                    onClick={handleSubProjectClick}
                  />
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      {/* Create SubProject Modal */}
      <Modal
        open={createSubProjectOpen}
        onClose={() => {
          setCreateSubProjectOpen(false);
          setSelectedBuildingId('');
        }}
        title="Create subproject"
      >
        <form onSubmit={handleCreateSubProject} className="flex min-w-0 max-h-[65vh] flex-col gap-4">
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
                const constructionSiteId = project.constructionSiteId;
                const selectedSite = constructionSites.find((site) => site.id === constructionSiteId);
                const buildings = selectedSite?.buildings ?? [];
                return (
                  <>
                    <select
                      id="subproject-building"
                      name="buildingId"
                      value={selectedBuildingId}
                      onChange={(e) => setSelectedBuildingId(e.target.value)}
                      className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
                className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="grid gap-2">
                <Label htmlFor="subproject-buildingType">Building Type</Label>
                <select
                  id="subproject-buildingType"
                  name="buildingType"
                  defaultValue="OTHER"
                  className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
                  className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
            <Button type="submit" className="w-full" disabled={loading || !selectedBuildingId || !project.constructionSiteId}>
              {loading ? 'Creating...' : 'Create SubProject'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

function SubProjectRow({
  subProject,
  projectId,
  projectIdPath,
  isExpanded,
  onToggle,
  onDelete,
  onClick,
}: {
  subProject: Project['subProjects'][number];
  projectId: string;
  projectIdPath: string;
  isExpanded: boolean;
  onToggle: () => void;
  onDelete: (projectId: string, subProjectId: string) => Promise<void>;
  onClick: (subProjectId: string) => void;
}) {
  return (
    <>
      <TableRow
        className="cursor-pointer hover:bg-muted/30"
        onClick={() => onClick(subProject.id)}
      >
        <TableCell
          onClick={(e) => {
            e.stopPropagation();
            onToggle();
          }}
        >
          <Button
            variant="ghost"
            size="sm"
            className="p-0 h-6 w-6 hover:bg-transparent"
            type="button"
          >
            {isExpanded ? (
              <ChevronDown className="h-4 w-4" />
            ) : (
              <ChevronRight className="h-4 w-4" />
            )}
          </Button>
          <Badge variant="secondary" className="ml-2">
            {subProject.code}
          </Badge>
        </TableCell>
        <TableCell>{subProject.name}</TableCell>
        <TableCell>
          <Badge variant={statusVariant[subProject.status] ?? 'secondary'}>{subProject.status}</Badge>
        </TableCell>
        <TableCell className="text-sm text-muted-foreground">
          {subProject.constructionPhase?.replace(/_/g, ' ') || '—'}
        </TableCell>
        <TableCell className="text-sm text-muted-foreground">
          {[subProject.startDate, subProject.endDate]
            .filter(Boolean)
            .map((d) => (d ? new Date(d!).toLocaleDateString() : null))
            .filter(Boolean)
            .join(' – ') || '—'}
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
              <DropdownMenuItem
                className="text-destructive"
                onClick={async () => {
                  if (confirm('Are you sure you want to delete this subproject?')) {
                    await onDelete(projectId, subProject.id);
                  }
                }}
              >
                Delete SubProject
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </TableCell>
      </TableRow>
      {isExpanded && subProject.floors && subProject.floors.length > 0 && (
        <TableRow>
          <TableCell colSpan={6} className="bg-muted/20">
            <div className="px-4 py-3">
              <div className="flex items-center gap-2 mb-2">
                <ChevronDown className="h-3 w-3 text-muted-foreground" />
                <span className="text-xs font-medium text-muted-foreground">Floors</span>
              </div>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-[100px]">Code</TableHead>
                    <TableHead>Name</TableHead>
                    <TableHead>Level</TableHead>
                    <TableHead className="text-right">Area</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {subProject.floors.map((floor) => (
                    <TableRow key={floor.id}>
                      <TableCell className="font-medium">{floor.code}</TableCell>
                      <TableCell>{floor.name}</TableCell>
                      <TableCell>{floor.level}</TableCell>
                      <TableCell className="text-right">{floor.area ? `${floor.area} m²` : '—'}</TableCell>
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