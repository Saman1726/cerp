'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import type {
  SubProjectResponse,
  SubProjectFloor,
  SubProjectBuildingPart,
  SubProjectBuildingType,
  SubProjectConstructionPhase,
  SubProjectStatus,
} from '@cerp/shared-types';
import { Badge } from '@cerp/core-web/components/ui/badge';
import { Button } from '@cerp/core-web/components/ui/button';
import { Modal } from '@cerp/core-web/components/ui/modal';
import { Input } from '@cerp/core-web/components/ui/input';
import { Label } from '@cerp/core-web/components/ui/label';

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

export default function SubProjectDetailClient({
  subProject,
  projectId,
}: {
  subProject: SubProjectResponse;
  projectId: string;
}) {
  const router = useRouter();
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
          <p className="mt-1 text-sm font-medium">
            {subProject.externalId ?? '—'}
          </p>
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
                <Badge variant="outline" className="mt-2 text-xs">
                  {part.scopeLevel}
                </Badge>
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