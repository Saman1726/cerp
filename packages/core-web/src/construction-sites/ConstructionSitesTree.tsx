'use client';

import { useState, useTransition } from 'react';
import { useRouter } from 'next/navigation';
import type { ConstructionSite } from '@cerp/shared-types';
import { Button, Card, CardContent, CardHeader, CardTitle, Input, Label, Modal, Badge } from '../components/ui';
import type { ConstructionSitesActions, SimpleFormAction } from './types';

type ToggleMap = Record<string, boolean>;

export interface ConstructionSitesTreeProps {
  sites: ConstructionSite[];
  actions: ConstructionSitesActions;
}

export function ConstructionSitesTree({ sites, actions }: ConstructionSitesTreeProps) {
  const router = useRouter();
  const [, startTransition] = useTransition();

  // Expansion state
  const [openBuildings, setOpenBuildings] = useState<ToggleMap>({});
  const [openFloors, setOpenFloors] = useState<ToggleMap>({});

  // Modal state
  const [createSiteOpen, setCreateSiteOpen] = useState(false);
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

  // Helpers
  const toggleBuilding = (buildingId: string) =>
    setOpenBuildings((m) => ({ ...m, [buildingId]: !m[buildingId] }));
  const toggleFloor = (floorId: string) => setOpenFloors((m) => ({ ...m, [floorId]: !m[floorId] }));

  const handleAction = async (action: SimpleFormAction, formData: FormData, onClose: () => void) => {
    await action(formData);
    onClose();
    startTransition(() => {
      router.refresh();
    });
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h3 className="text-base font-semibold">Construction Sites</h3>
        <Button onClick={() => setCreateSiteOpen(true)}>+ Add Site</Button>
      </div>

      {sites.length === 0 ? (
        <p className="py-6 text-center text-sm text-muted-foreground">No construction sites yet.</p>
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {sites.map((site) => (
            <Card key={site.id} className="overflow-hidden">
              <CardHeader className="flex flex-row items-start justify-between gap-4">
                <div>
                  <CardTitle className="text-base">{site.name}</CardTitle>
                  <p className="text-sm text-muted-foreground">{site.address}</p>
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setDuplicateSiteOpen({ id: site.id, name: site.name, address: site.address })}
                  >
                    Duplicate
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setEditSiteOpen({ id: site.id, name: site.name, address: site.address })}
                  >
                    Edit
                  </Button>
                  <form action={actions.deleteSite}>
                    <input type="hidden" name="siteId" value={site.id} />
                    <Button type="submit" variant="destructive" size="sm">
                      Delete
                    </Button>
                  </form>
                  <Button size="sm" onClick={() => setAddBuildingOpen({ siteId: site.id })}>
                    + Building
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="flex flex-col gap-3">
                {site.buildings.length === 0 ? (
                  <p className="text-sm text-muted-foreground">No buildings yet.</p>
                ) : (
                  site.buildings.map((b) => (
                    <div key={b.id} className="rounded-md border p-3">
                      <div className="flex items-center justify-between">
                        <button
                          type="button"
                          className="w-full text-left"
                          onClick={() => toggleBuilding(b.id)}
                          aria-expanded={openBuildings[b.id] ? 'true' : 'false'}
                        >
                          <div className="flex items-center justify-between">
                            <div className="flex flex-col">
                              <span className="text-sm font-medium">{b.name}</span>
                              <span className="text-xs text-muted-foreground">
                                Usable {b.usableArea} m² • Total {b.totalArea} m²
                              </span>
                            </div>
                            <span className="text-xs text-muted-foreground">
                              {openBuildings[b.id] ? '▾' : '▸'}
                            </span>
                          </div>
                        </button>
                        <div className="ml-2 flex items-center gap-2">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setDuplicateBuildingOpen({ siteId: site.id, buildingId: b.id, name: b.name })}
                          >
                            Duplicate
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setEditBuildingOpen({ siteId: site.id, buildingId: b.id, name: b.name })}
                          >
                            Edit
                          </Button>
                          <form action={actions.deleteBuilding}>
                            <input type="hidden" name="siteId" value={site.id} />
                            <input type="hidden" name="buildingId" value={b.id} />
                            <Button type="submit" variant="destructive" size="sm">
                              Delete
                            </Button>
                          </form>
                          <Button
                            size="sm"
                            onClick={() => setAddFloorOpen({ siteId: site.id, buildingId: b.id })}
                          >
                            + Floor
                          </Button>
                        </div>
                      </div>

                      {openBuildings[b.id] && (
                        <div className="mt-3 flex flex-col gap-2">
                          {b.floors.length === 0 ? (
                            <p className="text-sm text-muted-foreground">No floors yet.</p>
                          ) : (
                            b.floors.map((f) => (
                              <div key={f.id} className="rounded-md border p-3">
                                <div className="flex items-center justify-between">
                                  <button
                                    type="button"
                                    className="w-full text-left"
                                    onClick={() => toggleFloor(f.id)}
                                    aria-expanded={openFloors[f.id] ? 'true' : 'false'}
                                  >
                                    <div className="flex items-center justify-between">
                                      <div className="flex flex-wrap items-center gap-2">
                                        <span className="text-sm font-medium">{f.type}</span>
                                        <Badge variant="secondary">{f.usableArea} m² usable</Badge>
                                        <Badge variant="secondary">{f.totalArea} m² total</Badge>
                                        <Badge variant="outline">{f.units.length} units</Badge>
                                      </div>
                                      <span className="text-xs text-muted-foreground">
                                        {openFloors[f.id] ? '▾' : '▸'}
                                      </span>
                                    </div>
                                  </button>
                                  <div className="ml-2 flex items-center gap-2">
                                    <Button
                                      size="sm"
                                      variant="outline"
                                      onClick={() => setDuplicateFloorOpen({ siteId: site.id, buildingId: b.id, floorId: f.id, type: f.type, usableArea: f.usableArea, totalArea: f.totalArea })}
                                    >
                                      Duplicate
                                    </Button>
                                    <Button
                                      size="sm"
                                      variant="outline"
                                      onClick={() => setEditFloorOpen({ siteId: site.id, buildingId: b.id, floorId: f.id, type: f.type, usableArea: f.usableArea, totalArea: f.totalArea })}
                                    >
                                      Edit
                                    </Button>
                                    <form action={actions.deleteFloor}>
                                      <input type="hidden" name="siteId" value={site.id} />
                                      <input type="hidden" name="buildingId" value={b.id} />
                                      <input type="hidden" name="floorId" value={f.id} />
                                      <Button type="submit" variant="destructive" size="sm">
                                        Delete
                                      </Button>
                                    </form>
                                    <Button
                                      size="sm"
                                      onClick={() =>
                                        setAddUnitOpen({ siteId: site.id, buildingId: b.id, floorId: f.id })
                                      }
                                    >
                                      + Unit
                                    </Button>
                                  </div>
                                </div>

                                {openFloors[f.id] && (
                                  <div className="mt-3 grid gap-2 sm:grid-cols-2">
                                    {f.units.length === 0 ? (
                                      <p className="text-sm text-muted-foreground sm:col-span-2">
                                        No units yet.
                                      </p>
                                    ) : (
                                      f.units.map((u) => (
                                        <div key={u.id} className="rounded-md border p-3">
                                          <div className="flex items-start justify-between gap-2">
                                            <div className="flex flex-col">
                                              <span className="text-sm font-medium">{u.label}</span>
                                              <span className="text-xs text-muted-foreground">
                                                {u.usableArea} m² • {u.bedrooms} bd • {u.bathrooms} ba •{' '}
                                                {u.balconies} balc • {u.toilets} wc
                                              </span>
                                            </div>
                                            <div className="flex flex-col gap-1">
                                              <div className="flex gap-1">
                                                <Button
                                                  size="sm"
                                                  variant="outline"
                                                  onClick={() => setEditUnitOpen({ siteId: site.id, buildingId: b.id, floorId: f.id, unitId: u.id, label: u.label, usableArea: u.usableArea, bedrooms: u.bedrooms, bathrooms: u.bathrooms, balconies: u.balconies, toilets: u.toilets })}
                                                >
                                                  Edit
                                                </Button>
                                                <Button
                                                  size="sm"
                                                  variant="outline"
                                                  onClick={() => setDuplicateUnitOpen({ siteId: site.id, buildingId: b.id, floorId: f.id, unitId: u.id, label: u.label, usableArea: u.usableArea, bedrooms: u.bedrooms, bathrooms: u.bathrooms, balconies: u.balconies, toilets: u.toilets })}
                                                >
                                                  Duplicate
                                                </Button>
                                              </div>
                                              <form action={actions.deleteUnit}>
                                                <input type="hidden" name="siteId" value={site.id} />
                                                <input type="hidden" name="buildingId" value={b.id} />
                                                <input type="hidden" name="floorId" value={f.id} />
                                                <input type="hidden" name="unitId" value={u.id} />
                                                <Button type="submit" variant="destructive" size="sm">
                                                  Delete
                                                </Button>
                                              </form>
                                            </div>
                                          </div>
                                        </div>
                                      ))
                                    )}
                                  </div>
                                )}
                              </div>
                            ))
                          )}
                        </div>
                      )}
                    </div>
                  ))
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

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
    </div>
  );
}