'use client';

import { useState } from 'react';
import type { Project, ConstructionSite } from '@cerp/shared-types';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Modal } from '../components/ui/modal';

export interface ProjectListProps {
  projects: Project[];
  constructionSites: ConstructionSite[];
  createProjectAction: (formData: FormData) => Promise<void>;
  createSubProjectAction: (formData: FormData) => Promise<void>;
  deleteProjectAction: (projectId: string) => Promise<void>;
  deleteSubProjectAction: (formData: FormData) => Promise<void>;
  onCreateWorkOrder: (project: Project, subProject?: { id: string; name: string }) => void;
}

const statusVariant: Record<string, 'default' | 'secondary' | 'success' | 'destructive' | 'outline'> = {
  PLANNED: 'secondary',
  ACTIVE: 'default',
  ON_HOLD: 'secondary',
  COMPLETED: 'success',
  CANCELLED: 'destructive',
};

export function ProjectList({ projects, constructionSites, createProjectAction, createSubProjectAction, deleteProjectAction, deleteSubProjectAction, onCreateWorkOrder }: ProjectListProps) {
  const [createProjectOpen, setCreateProjectOpen] = useState(false);
  const [createSubProjectOpen, setCreateSubProjectOpen] = useState<string | null>(null);
  const [selectedConSiteId, setSelectedConSiteId] = useState<string>('');
  const [selectedBuildingId, setSelectedBuildingId] = useState<string>('');

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold">Projects & SubProjects</h3>
        <Button onClick={() => setCreateProjectOpen(true)}>+ Add Project</Button>
      </div>

      {projects.length === 0 ? (
        <p className="py-6 text-center text-sm text-muted-foreground">No projects yet.</p>
      ) : (
        <div className="flex flex-col gap-3">
          {projects.map((project) => (
            <Card key={project.id}>
              <CardHeader className="flex flex-row items-start justify-between gap-4 pb-3">
                <div className="flex flex-col gap-1">
                  <CardTitle className="text-base flex items-center gap-2">
                    {project.name}
                    <Badge variant={statusVariant[project.status] ?? 'secondary'}>{project.status}</Badge>
                  </CardTitle>
                  {project.code && <span className="text-xs text-muted-foreground font-mono">{project.code}</span>}
                  {project.description && <p className="text-sm text-muted-foreground">{project.description}</p>}
                  <div className="flex items-center gap-3 text-xs text-muted-foreground">
                    {project.constructionSiteId && (() => {
                      const site = constructionSites.find((s) => s.id === project.constructionSiteId);
                      return site ? <span>Site: {site.name}</span> : null;
                    })()}
                    {project.city && <span>{project.city}{project.country ? `, ${project.country}` : ''}</span>}
                    {project.startDate && <span>Start: {new Date(project.startDate).toLocaleDateString()}</span>}
                    {project.endDate && <span>End: {new Date(project.endDate).toLocaleDateString()}</span>}
                    {project.budget && <span>Budget: {project.budget} {project.currencyCode}</span>}
                  </div>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => onCreateWorkOrder(project)}
                  >
                    Create Work Order
                  </Button>
                  <Button
                    size="sm"
                    variant="destructive"
                    onClick={async () => {
                      await deleteProjectAction(project.id);
                      window.location.reload();
                    }}
                  >
                    Delete
                  </Button>
                  <Button size="sm" onClick={() => setCreateSubProjectOpen(project.id)}>
                    + SubProject
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="pt-0">
                {project.subProjects.length === 0 ? (
                  <p className="pb-4 text-sm text-muted-foreground">No subprojects yet.</p>
                ) : (
                  <div className="flex flex-col gap-2 pb-4">
                    {project.subProjects.map((subProject) => (
                      <div key={subProject.id} className="flex items-center justify-between rounded-md border px-3 py-2">
                        <div className="flex flex-col gap-0.5">
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-medium">{subProject.name}</span>
                            <Badge variant="secondary">{subProject.code}</Badge>
                            <Badge variant={statusVariant[subProject.status] ?? 'secondary'}>{subProject.status}</Badge>
                          </div>
                          <div className="flex items-center gap-3 text-xs text-muted-foreground">
                            {subProject.description && <span className="truncate max-w-xs">{subProject.description}</span>}
                            {subProject.constructionPhase && <span className="font-mono">{subProject.constructionPhase}</span>}
                            {subProject.startDate && <span>{new Date(subProject.startDate).toLocaleDateString()}</span>}
                            {subProject.budget && <span>{subProject.budget} {subProject.currencyCode}</span>}
                          </div>
                          {subProject.floors?.length > 0 && (
                            <span className="text-xs text-muted-foreground">
                              {subProject.floors?.length} floor(s)
                            </span>
                          )}
                        </div>
                        <div className="flex shrink-0 items-center gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => onCreateWorkOrder(project, subProject)}
                          >
                            Create Work Order
                          </Button>
                          <form action={deleteSubProjectAction}>
                            <input type="hidden" name="projectId" value={project.id} />
                            <input type="hidden" name="subProjectId" value={subProject.id} />
                            <Button type="submit" size="sm" variant="destructive">
                              Delete
                            </Button>
                          </form>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
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
                <option key={site.id} value={site.id}>{site.name}</option>
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
              <select id="project-type" name="type" defaultValue="OTHER" className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1">
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
              <select id="project-status" name="status" defaultValue="PLANNED" className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1">
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
              <Input id="project-budget" name="budget" type="number" min="0" step="0.01" />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="project-currencyCode">Currency</Label>
              <Input id="project-currencyCode" name="currencyCode" defaultValue="EUR" />
            </div>
          </div>
          <div className="grid gap-2">
            <Label htmlFor="project-addressLine1">Address Line 1</Label>
            <Input id="project-addressLine1" name="addressLine1" placeholder="123 Main Street" />
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

      {/* Create SubProject Modal */}
      <Modal open={!!createSubProjectOpen} onClose={() => { setCreateSubProjectOpen(null); setSelectedBuildingId(''); }} title="Create subproject">
        {createSubProjectOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('projectId', createSubProjectOpen);
              formData.set('buildingId', selectedBuildingId);
              await createSubProjectAction(formData);
              setCreateSubProjectOpen(null);
              setSelectedBuildingId('');
              window.location.reload();
            }}
            className="flex min-w-0 max-h-[65vh] flex-col gap-4"
          >
            <input type="hidden" name="buildingId" value={selectedBuildingId} />
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
              <Label htmlFor="subproject-building">Building (from Construction Site)</Label>
              {(() => {
                // Get buildings from the construction site selected in the project form
                const selectedSite = constructionSites.find((site) => site.id === selectedConSiteId);
                const buildings = selectedSite?.buildings ?? [];
                return (
                  <>
                    <select
                      id="subproject-building"
                      name="buildingId"
                      value={selectedBuildingId}
                      onChange={(e) => setSelectedBuildingId(e.target.value)}
                      className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1"
                      disabled={buildings.length === 0}
                    >
                      <option value="">Select a building (optional)</option>
                      {buildings.map((building) => (
                        <option key={building.id} value={building.id}>{building.name}</option>
                      ))}
                    </select>
                    {selectedConSiteId && buildings.length === 0 && (
                      <p className="text-xs text-muted-foreground">The selected construction site has no buildings. Add buildings to the construction site first.</p>
                    )}
                    {!selectedConSiteId && (
                      <p className="text-xs text-muted-foreground">Select a construction site in the project form, then close and reopen this modal.</p>
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
                <select id="subproject-buildingType" name="buildingType" defaultValue="OTHER" className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1">
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
                <select id="subproject-constructionPhase" name="constructionPhase" defaultValue="PRE_CONSTRUCTION" className="flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1">
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
              <Button type="submit" className="w-full">Create SubProject</Button>
            </div>
          </form>
        ) : null}
      </Modal>
    </div>
  );
}