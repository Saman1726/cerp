'use client';

import { useState } from 'react';
import type { ConstructionSite } from '@cerp/shared-types';
import { Button, Card, CardContent, CardHeader, CardTitle, Input, Label, Modal, Badge } from '../components/ui';
import type { ConstructionSitesActions, SimpleFormAction } from './types';

type ToggleMap = Record<string, boolean>;

export interface ConstructionSitesTreeProps {
  sites: ConstructionSite[];
  actions: ConstructionSitesActions;
}

export function ConstructionSitesTree({ sites, actions }: ConstructionSitesTreeProps) {
  // Expansion state
  const [openBuildings, setOpenBuildings] = useState<ToggleMap>({});
  const [openFloors, setOpenFloors] = useState<ToggleMap>({});

  // Modal state
  const [createSiteOpen, setCreateSiteOpen] = useState(false);
  const [addBuildingOpen, setAddBuildingOpen] = useState<{ siteId: string } | null>(null);
  const [addFloorOpen, setAddFloorOpen] = useState<{ siteId: string; buildingId: string } | null>(null);
  const [addUnitOpen, setAddUnitOpen] = useState<{ siteId: string; buildingId: string; floorId: string } | null>(null);

  // Helpers
  const toggleBuilding = (buildingId: string) =>
    setOpenBuildings((m) => ({ ...m, [buildingId]: !m[buildingId] }));
  const toggleFloor = (floorId: string) => setOpenFloors((m) => ({ ...m, [floorId]: !m[floorId] }));

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
        <SimpleActionForm action={actions.createSite} submitLabel="Create">
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
        </SimpleActionForm>
      </Modal>

      {/* Add Building Modal */}
      <Modal
        open={!!addBuildingOpen}
        onClose={() => setAddBuildingOpen(null)}
        title="Add building to site"
      >
        {addBuildingOpen ? (
          <SimpleActionForm action={actions.addBuilding} submitLabel="Add building">
            <input type="hidden" name="siteId" value={addBuildingOpen.siteId} />
            <div className="grid gap-3">
              <div className="grid gap-2">
                <Label htmlFor="building-name">Building name</Label>
                <Input id="building-name" name="name" placeholder="Name" required />
              </div>
            </div>
          </SimpleActionForm>
        ) : null}
      </Modal>

      {/* Add Floor Modal */}
      <Modal open={!!addFloorOpen} onClose={() => setAddFloorOpen(null)} title="Add floor">
        {addFloorOpen ? (
          <SimpleActionForm action={actions.addFloor} submitLabel="Add floor">
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
          </SimpleActionForm>
        ) : null}
      </Modal>

      {/* Add Unit Modal */}
      <Modal open={!!addUnitOpen} onClose={() => setAddUnitOpen(null)} title="Add unit">
        {addUnitOpen ? (
          <SimpleActionForm action={actions.addUnit} submitLabel="Add unit">
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
          </SimpleActionForm>
        ) : null}
      </Modal>
    </div>
  );
}

function SimpleActionForm({
  action,
  submitLabel,
  children,
}: {
  action: SimpleFormAction;
  submitLabel: string;
  children: React.ReactNode;
}) {
  // Note: we intentionally avoid useActionState here to keep code compact.
  // The server action is expected to revalidate the /construction path on success and show errors by redirect or message.
  return (
    <form action={action} className="flex flex-col gap-4">
      {children}
      <div className="flex items-center justify-end gap-2">
        <Button type="submit">{submitLabel}</Button>
      </div>
    </form>
  );
}