'use client';

import { useState, useTransition } from 'react';
import { useRouter } from 'next/navigation';
import type { ConstructionSite } from '@cerp/shared-types';
import { Building2, FileUp, FileText, Home, Layers, MapPin, MoreHorizontal, Plus, Copy } from 'lucide-react';
import {
  Button,
  Input,
  Label,
  Modal,
  Badge,
  Tree,
  TreeNode,
  DetailPopup,
  DetailFieldGrid,
  DetailField,
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../components/ui';
import { IfcFileManager } from './IfcFileManager';
import type { ConstructionSitesActions, SimpleFormAction } from './types';

type Building = ConstructionSite['buildings'][number];
type Floor = Building['floors'][number];
type Unit = Floor['units'][number];

type DetailState =
  | { kind: 'site'; site: ConstructionSite }
  | { kind: 'building'; site: ConstructionSite; building: Building }
  | { kind: 'floor'; site: ConstructionSite; building: Building; floor: Floor }
  | { kind: 'unit'; site: ConstructionSite; building: Building; floor: Floor; unit: Unit };

export interface ConstructionSitesTreeProps {
  sites: ConstructionSite[];
  actions: ConstructionSitesActions;
  onSiteSelect?: (siteId: string, siteName: string) => void;
}

function toFormData(fields: Record<string, string>): FormData {
  const formData = new FormData();
  for (const [key, value] of Object.entries(fields)) formData.set(key, value);
  return formData;
}

export function ConstructionSitesTree({ sites, actions, onSiteSelect }: ConstructionSitesTreeProps) {
  const router = useRouter();
  const [, startTransition] = useTransition();

  // Modal state
  const [createSiteOpen, setCreateSiteOpen] = useState(false);
  const [importIfcOpen, setImportIfcOpen] = useState<{ siteId: string; siteName: string } | null>(null);
  const [uploadIfcOpen, setUploadIfcOpen] = useState<{ siteId: string; siteName: string } | null>(null);
  const [manageIfcOpen, setManageIfcOpen] = useState<{ siteId: string; siteName: string } | null>(null);
  const [addBuildingOpen, setAddBuildingOpen] = useState<{ siteId: string } | null>(null);
  const [addFloorOpen, setAddFloorOpen] = useState<{ siteId: string; buildingId: string } | null>(null);
  const [addUnitOpen, setAddUnitOpen] = useState<{ siteId: string; buildingId: string; floorId: string } | null>(null);

  // Edit modals state
  const [editSiteOpen, setEditSiteOpen] = useState<{ id: string; name: string; address: string } | null>(null);
  const [duplicateSiteOpen, setDuplicateSiteOpen] = useState<{ id: string; name: string; address: string } | null>(null);
  const [editBuildingOpen, setEditBuildingOpen] = useState<{ siteId: string; buildingId: string; name: string } | null>(null);
  const [duplicateBuildingOpen, setDuplicateBuildingOpen] = useState<{ siteId: string; buildingId: string; name: string } | null>(null);
  const [editFloorOpen, setEditFloorOpen] = useState<{ siteId: string; buildingId: string; floorId: string; type: string; usableArea: number; totalArea: number } | null>(null);
  const [duplicateFloorOpen, setDuplicateFloorOpen] = useState<{ siteId: string; buildingId: string; floorId: string; type: string; usableArea: number; totalArea: number } | null>(null);
  const [editUnitOpen, setEditUnitOpen] = useState<{ siteId: string; buildingId: string; floorId: string; unitId: string; label: string; usableArea: number; bedrooms: number; bathrooms: number; balconies: number; toilets: number } | null>(null);
  const [duplicateUnitOpen, setDuplicateUnitOpen] = useState<{ siteId: string; buildingId: string; floorId: string; unitId: string; label: string; usableArea: number; bedrooms: number; bathrooms: number; balconies: number; toilets: number } | null>(null);

  // Detail popup state
  const [detail, setDetail] = useState<DetailState | null>(null);

  const handleAction = async (action: SimpleFormAction, formData: FormData, onClose: () => void) => {
    try {
      await action(formData);
      onClose();
      startTransition(() => {
        router.refresh();
      });
    } catch (error) {
      alert(error instanceof Error ? error.message : 'Something went wrong. Please try again.');
    }
  };

  const handleDelete = async (action: SimpleFormAction, fields: Record<string, string>, confirmMessage: string) => {
    if (!confirm(confirmMessage)) return;
    try {
      await action(toFormData(fields));
      setDetail(null);
      startTransition(() => {
        router.refresh();
      });
    } catch (error) {
      alert(error instanceof Error ? error.message : 'Something went wrong. Please try again.');
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold">Construction Sites</h3>
        <Button onClick={() => setCreateSiteOpen(true)}>+ Add Site</Button>
      </div>

      <Tree isEmpty={sites.length === 0} emptyMessage="No construction sites yet.">
        {sites.map((site) => (
          <TreeNode
            key={site.id}
            icon={<MapPin className="size-4 text-blue-500" />}
            label={site.name}
            meta={<span className="truncate">{site.address}</span>}
            onSelect={() => {
              setDetail({ kind: 'site', site });
              onSiteSelect?.(site.id, site.name);
            }}
            actions={
              <>
                <Button
                  size="sm"
                  variant="ghost"
                  className="size-7 p-0"
                  title="Add building"
                  onClick={() => setAddBuildingOpen({ siteId: site.id })}
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
                    <DropdownMenuItem onClick={() => setEditSiteOpen({ id: site.id, name: site.name, address: site.address })}>
                      Edit
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => setDuplicateSiteOpen({ id: site.id, name: site.name, address: site.address })}>
                      <Copy className="mr-2 size-4" />
                      Duplicate
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => setUploadIfcOpen({ siteId: site.id, siteName: site.name })}>
                      <FileUp className="mr-2 size-4" />
                      Upload IFC File
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => setManageIfcOpen({ siteId: site.id, siteName: site.name })}>
                      <FileText className="mr-2 size-4" />
                      Manage IFC Files
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => setImportIfcOpen({ siteId: site.id, siteName: site.name })}>
                      <FileUp className="mr-2 size-4" />
                      Import IFC (parse &amp; build)
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem
                      className="text-destructive"
                      onClick={() => handleDelete(actions.deleteSite, { siteId: site.id }, `Delete site "${site.name}"?`)}
                    >
                      Delete
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </>
            }
          >
            {site.buildings.length === 0 ? (
              <p className="py-2 text-sm text-muted-foreground">No buildings yet.</p>
            ) : (
              site.buildings.map((building) => (
                <TreeNode
                  key={building.id}
                  icon={<Building2 className="size-4 text-violet-500" />}
                  label={building.name}
                  meta={
                    <span className="truncate">
                      {building.usableArea} m² usable · {building.totalArea} m² total
                    </span>
                  }
                  onSelect={() => setDetail({ kind: 'building', site, building })}
                  actions={
                    <>
                      <Button
                        size="sm"
                        variant="ghost"
                        className="size-7 p-0"
                        title="Add floor"
                        onClick={() => setAddFloorOpen({ siteId: site.id, buildingId: building.id })}
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
                          <DropdownMenuItem onClick={() => setEditBuildingOpen({ siteId: site.id, buildingId: building.id, name: building.name })}>
                            Edit
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => setDuplicateBuildingOpen({ siteId: site.id, buildingId: building.id, name: building.name })}>
                            Duplicate
                          </DropdownMenuItem>
                          <DropdownMenuSeparator />
                          <DropdownMenuItem
                            className="text-destructive"
                            onClick={() =>
                              handleDelete(
                                actions.deleteBuilding,
                                { siteId: site.id, buildingId: building.id },
                                `Delete building "${building.name}"?`,
                              )
                            }
                          >
                            Delete
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </>
                  }
                >
                  {building.floors.length === 0 ? (
                    <p className="py-2 text-sm text-muted-foreground">No floors yet.</p>
                  ) : (
                    building.floors.map((floor) => (
                      <TreeNode
                        key={floor.id}
                        icon={<Layers className="size-4 text-amber-500" />}
                        label={floor.type}
                        meta={
                          <>
                            <Badge variant="secondary">{floor.usableArea} m² usable</Badge>
                            <Badge variant="secondary">{floor.totalArea} m² total</Badge>
                            <Badge variant="outline">{floor.units.length} units</Badge>
                          </>
                        }
                        onSelect={() => setDetail({ kind: 'floor', site, building, floor })}
                        actions={
                          <>
                            <Button
                              size="sm"
                              variant="ghost"
                              className="size-7 p-0"
                              title="Add unit"
                              onClick={() => setAddUnitOpen({ siteId: site.id, buildingId: building.id, floorId: floor.id })}
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
                                  onClick={() =>
                                    setEditFloorOpen({
                                      siteId: site.id,
                                      buildingId: building.id,
                                      floorId: floor.id,
                                      type: floor.type,
                                      usableArea: floor.usableArea,
                                      totalArea: floor.totalArea,
                                    })
                                  }
                                >
                                  Edit
                                </DropdownMenuItem>
                                <DropdownMenuItem
                                  onClick={() =>
                                    setDuplicateFloorOpen({
                                      siteId: site.id,
                                      buildingId: building.id,
                                      floorId: floor.id,
                                      type: floor.type,
                                      usableArea: floor.usableArea,
                                      totalArea: floor.totalArea,
                                    })
                                  }
                                >
                                  Duplicate
                                </DropdownMenuItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuItem
                                  className="text-destructive"
                                  onClick={() =>
                                    handleDelete(
                                      actions.deleteFloor,
                                      { siteId: site.id, buildingId: building.id, floorId: floor.id },
                                      `Delete floor "${floor.type}"?`,
                                    )
                                  }
                                >
                                  Delete
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </>
                        }
                      >
                        {floor.units.length === 0 ? (
                          <p className="py-2 text-sm text-muted-foreground">No units yet.</p>
                        ) : (
                          floor.units.map((unit) => (
                            <TreeNode
                              key={unit.id}
                              icon={<Home className="size-4 text-emerald-500" />}
                              label={unit.label}
                              meta={
                                <span className="truncate">
                                  {unit.usableArea} m² · {unit.bedrooms} bd · {unit.bathrooms} ba · {unit.balconies} balc · {unit.toilets} wc
                                </span>
                              }
                              onSelect={() => setDetail({ kind: 'unit', site, building, floor, unit })}
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
                                        setEditUnitOpen({
                                          siteId: site.id,
                                          buildingId: building.id,
                                          floorId: floor.id,
                                          unitId: unit.id,
                                          label: unit.label,
                                          usableArea: unit.usableArea,
                                          bedrooms: unit.bedrooms,
                                          bathrooms: unit.bathrooms,
                                          balconies: unit.balconies,
                                          toilets: unit.toilets,
                                        })
                                      }
                                    >
                                      Edit
                                    </DropdownMenuItem>
                                    <DropdownMenuItem
                                      onClick={() =>
                                        setDuplicateUnitOpen({
                                          siteId: site.id,
                                          buildingId: building.id,
                                          floorId: floor.id,
                                          unitId: unit.id,
                                          label: unit.label,
                                          usableArea: unit.usableArea,
                                          bedrooms: unit.bedrooms,
                                          bathrooms: unit.bathrooms,
                                          balconies: unit.balconies,
                                          toilets: unit.toilets,
                                        })
                                      }
                                    >
                                      Duplicate
                                    </DropdownMenuItem>
                                    <DropdownMenuSeparator />
                                    <DropdownMenuItem
                                      className="text-destructive"
                                      onClick={() =>
                                        handleDelete(
                                          actions.deleteUnit,
                                          { siteId: site.id, buildingId: building.id, floorId: floor.id, unitId: unit.id },
                                          `Delete unit "${unit.label}"?`,
                                        )
                                      }
                                    >
                                      Delete
                                    </DropdownMenuItem>
                                  </DropdownMenuContent>
                                </DropdownMenu>
                              }
                            />
                          ))
                        )}
                      </TreeNode>
                    ))
                  )}
                </TreeNode>
              ))
            )}
          </TreeNode>
        ))}
      </Tree>

      {/* Detail Popup */}
      <DetailPopup
        open={detail !== null}
        onOpenChange={(open) => { if (!open) setDetail(null); }}
        title={
          detail?.kind === 'site' ? detail.site.name :
          detail?.kind === 'building' ? detail.building.name :
          detail?.kind === 'floor' ? detail.floor.type :
          detail?.kind === 'unit' ? detail.unit.label : ''
        }
      >
        {detail?.kind === 'site' && (
          <DetailFieldGrid>
            <DetailField label="Name" value={detail.site.name} />
            <DetailField label="Address" value={detail.site.address} />
            <DetailField label="Buildings" value={detail.site.buildings.length} />
          </DetailFieldGrid>
        )}
        {detail?.kind === 'building' && (
          <DetailFieldGrid>
            <DetailField label="Name" value={detail.building.name} />
            <DetailField label="Usable area" value={`${detail.building.usableArea} m²`} />
            <DetailField label="Total area" value={`${detail.building.totalArea} m²`} />
            <DetailField label="Floors" value={detail.building.floors.length} />
            <DetailField label="Site" value={detail.site.name} />
          </DetailFieldGrid>
        )}
        {detail?.kind === 'floor' && (
          <DetailFieldGrid>
            <DetailField label="Type" value={detail.floor.type} />
            <DetailField label="Usable area" value={`${detail.floor.usableArea} m²`} />
            <DetailField label="Total area" value={`${detail.floor.totalArea} m²`} />
            <DetailField label="Units" value={detail.floor.units.length} />
            <DetailField label="Building" value={detail.building.name} />
          </DetailFieldGrid>
        )}
        {detail?.kind === 'unit' && (
          <DetailFieldGrid>
            <DetailField label="Label" value={detail.unit.label} />
            <DetailField label="Usable area" value={`${detail.unit.usableArea} m²`} />
            <DetailField label="Bedrooms" value={detail.unit.bedrooms} />
            <DetailField label="Bathrooms" value={detail.unit.bathrooms} />
            <DetailField label="Balconies" value={detail.unit.balconies} />
            <DetailField label="Toilets" value={detail.unit.toilets} />
            <DetailField label="Floor" value={detail.floor.type} />
          </DetailFieldGrid>
        )}
      </DetailPopup>

      {/* Create Site Modal */}
      <Modal open={createSiteOpen} onClose={() => setCreateSiteOpen(false)} title="Create construction site">
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            const form = e.currentTarget;
            const formData = new FormData(form);
            await handleAction(actions.createSite, formData, () => setCreateSiteOpen(false));
          }}
          className="flex flex-col gap-4"
        >
          <div className="grid gap-3">
            <div className="grid gap-2">
              <Label htmlFor="site-name">Name</Label>
              <Input id="site-name" name="name" placeholder="Site name" required />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="site-address">Address</Label>
              <Input id="site-address" name="address" placeholder="Address" required />
            </div>
          </div>
          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="outline" onClick={() => setCreateSiteOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">Create Site</Button>
          </div>
        </form>
      </Modal>

      {/* Edit Site Modal */}
      <Modal open={!!editSiteOpen} onClose={() => setEditSiteOpen(null)} title="Edit construction site">
        {editSiteOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('siteId', editSiteOpen.id);
              await handleAction(actions.updateSite, formData, () => setEditSiteOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="edit-site-name">Name</Label>
                <Input id="edit-site-name" name="name" defaultValue={editSiteOpen.name} required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-site-address">Address</Label>
                <Input id="edit-site-address" name="address" defaultValue={editSiteOpen.address} required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setEditSiteOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Save Changes</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Duplicate Site Modal */}
      <Modal open={!!duplicateSiteOpen} onClose={() => setDuplicateSiteOpen(null)} title="Duplicate construction site">
        {duplicateSiteOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('siteId', duplicateSiteOpen.id);
              await handleAction(actions.duplicateSite, formData, () => setDuplicateSiteOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="dup-site-name">Name (copy will be named like this)</Label>
                <Input id="dup-site-name" name="name" placeholder={`${duplicateSiteOpen.name} (Copy)`} defaultValue={`${duplicateSiteOpen.name} (Copy)`} required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setDuplicateSiteOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Duplicate Site</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Add Building Modal */}
      <Modal
        open={!!addBuildingOpen}
        onClose={() => setAddBuildingOpen(null)}
        title="Add building to site"
      >
        {addBuildingOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              await handleAction(actions.addBuilding, formData, () => setAddBuildingOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <input type="hidden" name="siteId" value={addBuildingOpen.siteId} />
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="building-name">Building name</Label>
                <Input id="building-name" name="name" placeholder="Name" required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setAddBuildingOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Add Building</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Edit Building Modal */}
      <Modal open={!!editBuildingOpen} onClose={() => setEditBuildingOpen(null)} title="Edit building">
        {editBuildingOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('siteId', editBuildingOpen.siteId);
              formData.set('buildingId', editBuildingOpen.buildingId);
              await handleAction(actions.updateBuilding, formData, () => setEditBuildingOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="edit-building-name">Building name</Label>
                <Input id="edit-building-name" name="name" defaultValue={editBuildingOpen.name} required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setEditBuildingOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Save Changes</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Duplicate Building Modal */}
      <Modal open={!!duplicateBuildingOpen} onClose={() => setDuplicateBuildingOpen(null)} title="Duplicate building">
        {duplicateBuildingOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('siteId', duplicateBuildingOpen.siteId);
              formData.set('buildingId', duplicateBuildingOpen.buildingId);
              await handleAction(actions.duplicateBuilding, formData, () => setDuplicateBuildingOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="dup-building-name">Name (copy will be named like this)</Label>
                <Input id="dup-building-name" name="name" placeholder={`${duplicateBuildingOpen.name} (Copy)`} defaultValue={`${duplicateBuildingOpen.name} (Copy)`} required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setDuplicateBuildingOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Duplicate Building</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Add Floor Modal */}
      <Modal open={!!addFloorOpen} onClose={() => setAddFloorOpen(null)} title="Add floor">
        {addFloorOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              await handleAction(actions.addFloor, formData, () => setAddFloorOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <input type="hidden" name="siteId" value={addFloorOpen.siteId} />
            <input type="hidden" name="buildingId" value={addFloorOpen.buildingId} />
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="floor-type">Type</Label>
                <Input id="floor-type" name="type" placeholder="e.g. Residential" required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="floor-usable">Usable area (m²)</Label>
                <Input id="floor-usable" name="usableArea" type="number" min="0" step="0.01" required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="floor-total">Total area (m²)</Label>
                <Input id="floor-total" name="totalArea" type="number" min="0" step="0.01" required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setAddFloorOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Add Floor</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Edit Floor Modal */}
      <Modal open={!!editFloorOpen} onClose={() => setEditFloorOpen(null)} title="Edit floor">
        {editFloorOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('siteId', editFloorOpen.siteId);
              formData.set('buildingId', editFloorOpen.buildingId);
              formData.set('floorId', editFloorOpen.floorId);
              await handleAction(actions.updateFloor, formData, () => setEditFloorOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="edit-floor-type">Type</Label>
                <Input id="edit-floor-type" name="type" defaultValue={editFloorOpen.type} required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-floor-usable">Usable area (m²)</Label>
                <Input id="edit-floor-usable" name="usableArea" type="number" min="0" step="0.01" defaultValue={editFloorOpen.usableArea} required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-floor-total">Total area (m²)</Label>
                <Input id="edit-floor-total" name="totalArea" type="number" min="0" step="0.01" defaultValue={editFloorOpen.totalArea} required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setEditFloorOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Save Changes</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Duplicate Floor Modal */}
      <Modal open={!!duplicateFloorOpen} onClose={() => setDuplicateFloorOpen(null)} title="Duplicate floor">
        {duplicateFloorOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('siteId', duplicateFloorOpen.siteId);
              formData.set('buildingId', duplicateFloorOpen.buildingId);
              formData.set('floorId', duplicateFloorOpen.floorId);
              await handleAction(actions.duplicateFloor, formData, () => setDuplicateFloorOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="dup-floor-name">Name (copy will be named like this)</Label>
                <Input id="dup-floor-name" name="name" placeholder={`${duplicateFloorOpen.type} (Copy)`} defaultValue={`${duplicateFloorOpen.type} (Copy)`} required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setDuplicateFloorOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Duplicate Floor</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Add Unit Modal */}
      <Modal open={!!addUnitOpen} onClose={() => setAddUnitOpen(null)} title="Add unit">
        {addUnitOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              await handleAction(actions.addUnit, formData, () => setAddUnitOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <input type="hidden" name="siteId" value={addUnitOpen.siteId} />
            <input type="hidden" name="buildingId" value={addUnitOpen.buildingId} />
            <input type="hidden" name="floorId" value={addUnitOpen.floorId} />
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="unit-label">Label</Label>
                <Input id="unit-label" name="label" placeholder="e.g. A-101" required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="unit-usable">Usable area (m²)</Label>
                <Input id="unit-usable" name="usableArea" type="number" min="0" step="0.01" required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="unit-bedrooms">Bedrooms</Label>
                <Input id="unit-bedrooms" name="bedrooms" type="number" min="0" step="1" required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="unit-bathrooms">Bathrooms</Label>
                <Input id="unit-bathrooms" name="bathrooms" type="number" min="0" step="1" required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="unit-balconies">Balconies</Label>
                <Input id="unit-balconies" name="balconies" type="number" min="0" step="1" required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="unit-toilets">Toilets</Label>
                <Input id="unit-toilets" name="toilets" type="number" min="0" step="1" required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setAddUnitOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Add Unit</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Edit Unit Modal */}
      <Modal open={!!editUnitOpen} onClose={() => setEditUnitOpen(null)} title="Edit unit">
        {editUnitOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('siteId', editUnitOpen.siteId);
              formData.set('buildingId', editUnitOpen.buildingId);
              formData.set('floorId', editUnitOpen.floorId);
              formData.set('unitId', editUnitOpen.unitId);
              await handleAction(actions.updateUnit, formData, () => setEditUnitOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="edit-unit-label">Label</Label>
                <Input id="edit-unit-label" name="label" defaultValue={editUnitOpen.label} required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-unit-usable">Usable area (m²)</Label>
                <Input id="edit-unit-usable" name="usableArea" type="number" min="0" step="0.01" defaultValue={editUnitOpen.usableArea} required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-unit-bedrooms">Bedrooms</Label>
                <Input id="edit-unit-bedrooms" name="bedrooms" type="number" min="0" step="1" defaultValue={editUnitOpen.bedrooms} required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-unit-bathrooms">Bathrooms</Label>
                <Input id="edit-unit-bathrooms" name="bathrooms" type="number" min="0" step="1" defaultValue={editUnitOpen.bathrooms} required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-unit-balconies">Balconies</Label>
                <Input id="edit-unit-balconies" name="balconies" type="number" min="0" step="1" defaultValue={editUnitOpen.balconies} required />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-unit-toilets">Toilets</Label>
                <Input id="edit-unit-toilets" name="toilets" type="number" min="0" step="1" defaultValue={editUnitOpen.toilets} required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setEditUnitOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Save Changes</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Duplicate Unit Modal */}
      <Modal open={!!duplicateUnitOpen} onClose={() => setDuplicateUnitOpen(null)} title="Duplicate unit">
        {duplicateUnitOpen ? (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const form = e.currentTarget;
              const formData = new FormData(form);
              formData.set('siteId', duplicateUnitOpen.siteId);
              formData.set('buildingId', duplicateUnitOpen.buildingId);
              formData.set('floorId', duplicateUnitOpen.floorId);
              formData.set('unitId', duplicateUnitOpen.unitId);
              await handleAction(actions.duplicateUnit, formData, () => setDuplicateUnitOpen(null));
            }}
            className="flex flex-col gap-4"
          >
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="dup-unit-name">Label (copy will be named like this)</Label>
                <Input id="dup-unit-name" name="name" placeholder={`${duplicateUnitOpen.label} (Copy)`} defaultValue={`${duplicateUnitOpen.label} (Copy)`} required />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button type="button" variant="outline" onClick={() => setDuplicateUnitOpen(null)}>
                Cancel
              </Button>
              <Button type="submit">Duplicate Unit</Button>
            </div>
          </form>
        ) : null}
      </Modal>

      {/* Import IFC Modal */}
      <Modal open={!!importIfcOpen} onClose={() => setImportIfcOpen(null)} title="Import IFC file">
        {importIfcOpen ? (
          <ImportIfcModalContent
            siteId={importIfcOpen.siteId}
            siteName={importIfcOpen.siteName}
            onClose={() => setImportIfcOpen(null)}
            onSubmit={async (file) => {
              const formData = new FormData();
              formData.set('siteId', importIfcOpen.siteId);
              formData.set('ifcFile', file);
              await handleAction(actions.importIfc, formData, () => {
                setImportIfcOpen(null);
                startTransition(() => {
                  router.refresh();
                });
              });
            }}
          />
        ) : null}
      </Modal>

      {/* Upload IFC File Modal (register only, no parsing) */}
      <Modal open={!!uploadIfcOpen} onClose={() => setUploadIfcOpen(null)} title="Upload IFC File">
        {uploadIfcOpen ? (
          <UploadIfcModalContent
            siteId={uploadIfcOpen.siteId}
            siteName={uploadIfcOpen.siteName}
            onClose={() => setUploadIfcOpen(null)}
            onSubmit={async (file) => {
              const formData = new FormData();
              formData.set('siteId', uploadIfcOpen.siteId);
              formData.set('ifcFile', file);
              await handleAction(actions.uploadFile, formData, () => {
                setUploadIfcOpen(null);
                setManageIfcOpen({ siteId: uploadIfcOpen.siteId, siteName: uploadIfcOpen.siteName });
              });
            }}
          />
        ) : null}
      </Modal>

      {/* Manage IFC Files Modal */}
      <Modal open={!!manageIfcOpen} onClose={() => setManageIfcOpen(null)} title="Manage IFC Files" size="lg">
        {manageIfcOpen ? (
          <IfcFileManager
            siteId={manageIfcOpen.siteId}
            siteName={manageIfcOpen.siteName}
            actions={actions}
          />
        ) : null}
      </Modal>
    </div>
  );
}
function ImportIfcModalContent({
  siteId: _siteId,
  siteName,
  onClose,
  onSubmit,
}: {
  siteId: string;
  siteName: string;
  onClose: () => void;
  onSubmit: (file: File) => Promise<void>;
}) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!selectedFile) return;
    setUploading(true);
    setUploadProgress('Uploading file...');
    setError(null);

    try {
      await onSubmit(selectedFile);
      setUploadProgress('File uploaded. Processing IFC data...');
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to import IFC file');
    } finally {
      setUploading(false);
      setUploadProgress('');
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        Importing an IFC file for <strong>{siteName}</strong> will parse the building/floor/unit hierarchy and populate the site.
      </p>
      <div className="grid gap-2">
        <Label htmlFor="ifc-file">IFC File</Label>
        <Input
          id="ifc-file"
          name="ifcFile"
          type="file"
          accept=".ifc"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) {
              setSelectedFile(file);
              setError(null);
              setUploadProgress('');
            }
          }}
          disabled={uploading}
        />
        <p className="text-xs text-muted-foreground">
          {selectedFile ? `${selectedFile.name} (${(selectedFile.size / 1024 / 1024).toFixed(2)} MB)` : 'No file selected'}
        </p>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
      {uploadProgress && (
        <div className="flex items-center gap-2">
          <div className="size-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          <span className="text-sm text-muted-foreground">{uploadProgress}</span>
        </div>
      )}
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="outline" onClick={onClose} disabled={uploading}>
          Cancel
        </Button>
        <Button onClick={handleSubmit} disabled={!selectedFile || uploading}>
          {uploading ? 'Processing...' : 'Import'}
        </Button>
      </div>
    </div>
  );
}


function UploadIfcModalContent({
  siteId: _siteId,
  siteName,
  onClose,
  onSubmit,
}: {
  siteId: string;
  siteName: string;
  onClose: () => void;
  onSubmit: (file: File) => Promise<void>;
}) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!selectedFile) return;
    setUploading(true);
    setUploadProgress('Uploading file...');
    setError(null);

    try {
      await onSubmit(selectedFile);
      setUploadProgress('File uploaded. Queued for processing.');
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to upload IFC file');
    } finally {
      setUploading(false);
      setUploadProgress('');
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">
        Uploading an IFC file for <strong>{siteName}</strong> will register the file and queue it for background processing.
      </p>
      <div className="grid gap-2">
        <Label htmlFor="upload-ifc-file">IFC File</Label>
        <Input
          id="upload-ifc-file"
          name="ifcFile"
          type="file"
          accept=".ifc"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) {
              setSelectedFile(file);
              setError(null);
              setUploadProgress('');
            }
          }}
          disabled={uploading}
        />
        <p className="text-xs text-muted-foreground">
          {selectedFile ? `${selectedFile.name} (${(selectedFile.size / 1024 / 1024).toFixed(2)} MB)` : "No file selected"}
        </p>
      </div>
      {error && <p className="text-sm text-destructive">{error}</p>}
      {uploadProgress && (
        <div className="flex items-center gap-2">
          <div className="size-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          <span className="text-sm text-muted-foreground">{uploadProgress}</span>
        </div>
      )}
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="outline" onClick={onClose} disabled={uploading}>
          Cancel
        </Button>
        <Button onClick={handleSubmit} disabled={!selectedFile || uploading}>
          {uploading ? "Uploading..." : "Upload"}
        </Button>
      </div>
    </div>
  );
}
