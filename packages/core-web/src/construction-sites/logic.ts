import { apiClient } from '../lib/api-client';
import type { SimpleActionState } from './types';
import type { Building, ConstructionSite, Floor, Unit, CreateFloorRequest, CreateUnitRequest, CreateBuildingRequest, CreateConstructionSiteRequest } from '@cerp/shared-types';

// Non `use server` logic functions. App layer wraps these as Server Actions and handles revalidatePath.

export async function createSite(formData: FormData): Promise<SimpleActionState> {
  const name = (formData.get('name') || '').toString().trim();
  const address = (formData.get('address') || '').toString().trim();
  if (!name || !address) return { error: 'Name and address are required' };

  try {
    await apiClient.post('/construction-sites', { name, address });
    return {};
  } catch {
    return { error: 'Failed to create site' };
  }
}

export async function deleteSite(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  if (!siteId) return { error: 'Missing siteId' };

  try {
    await apiClient.delete(`/construction-sites/${siteId}`);
    return {};
  } catch {
    return { error: 'Failed to delete site' };
  }
}

export async function addBuilding(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const name = (formData.get('name') || '').toString().trim();
  if (!siteId || !name) return { error: 'Missing siteId or name' };

  try {
    await apiClient.post(`/construction-sites/${siteId}/buildings`, { name });
    return {};
  } catch {
    return { error: 'Failed to add building' };
  }
}

export async function deleteBuilding(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  if (!siteId || !buildingId) return { error: 'Missing siteId or buildingId' };

  try {
    await apiClient.delete(`/construction-sites/${siteId}/buildings/${buildingId}`);
    return {};
  } catch {
    return { error: 'Failed to delete building' };
  }
}

export async function addFloor(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  const type = (formData.get('type') || '').toString().trim();
  const usableArea = Number.parseFloat((formData.get('usableArea') || '').toString());
  const totalArea = Number.parseFloat((formData.get('totalArea') || '').toString());

  if (!siteId || !buildingId || !type || Number.isNaN(usableArea) || Number.isNaN(totalArea))
    return { error: 'Missing required fields' };

  try {
    await apiClient.post(`/construction-sites/${siteId}/buildings/${buildingId}/floors`, {
      type,
      usableArea,
      totalArea,
    });
    return {};
  } catch {
    return { error: 'Failed to add floor' };
  }
}

export async function deleteFloor(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  const floorId = formData.get('floorId')?.toString();
  if (!siteId || !buildingId || !floorId) return { error: 'Missing ids' };

  try {
    await apiClient.delete(`/construction-sites/${siteId}/buildings/${buildingId}/floors/${floorId}`);
    return {};
  } catch {
    return { error: 'Failed to delete floor' };
  }
}

export async function addUnit(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  const floorId = formData.get('floorId')?.toString();

  const label = (formData.get('label') || '').toString().trim();
  const usableArea = Number.parseFloat((formData.get('usableArea') || '').toString());
  const bedrooms = Number.parseInt((formData.get('bedrooms') || '').toString(), 10);
  const bathrooms = Number.parseInt((formData.get('bathrooms') || '').toString(), 10);
  const balconies = Number.parseInt((formData.get('balconies') || '').toString(), 10);
  const toilets = Number.parseInt((formData.get('toilets') || '').toString(), 10);

  if (
    !siteId ||
    !buildingId ||
    !floorId ||
    !label ||
    Number.isNaN(usableArea) ||
    Number.isNaN(bedrooms) ||
    Number.isNaN(bathrooms) ||
    Number.isNaN(balconies) ||
    Number.isNaN(toilets)
  )
    return { error: 'Missing required fields' };

  try {
    await apiClient.post(`/construction-sites/${siteId}/buildings/${buildingId}/floors/${floorId}/units`, {
      label,
      usableArea,
      bedrooms,
      bathrooms,
      balconies,
      toilets,
    });
    return {};
  } catch {
    return { error: 'Failed to add unit' };
  }
}

export async function deleteUnit(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  const floorId = formData.get('floorId')?.toString();
  const unitId = formData.get('unitId')?.toString();
  if (!siteId || !buildingId || !floorId || !unitId) return { error: 'Missing ids' };

  try {
    await apiClient.delete(
      `/construction-sites/${siteId}/buildings/${buildingId}/floors/${floorId}/units/${unitId}`,
    );
    return {};
  } catch {
    return { error: 'Failed to delete unit' };
  }
}

// ── Update Actions ──

export async function updateSite(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const name = (formData.get('name') || '').toString().trim();
  const address = (formData.get('address') || '').toString().trim();
  if (!siteId || !name || !address) return { error: 'Name and address are required' };

  try {
    await apiClient.put(`/construction-sites/${siteId}`, { name, address });
    return {};
  } catch {
    return { error: 'Failed to update site' };
  }
}

export async function duplicateSite(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  if (!siteId) return { error: 'Missing siteId' };

  try {
    const site = await apiClient.get<ConstructionSite>(`/construction-sites/${siteId}`);
    const name = (formData.get('name') || `${site.name} (Copy)`).toString().trim();
    const newSite = await apiClient.post<CreateConstructionSiteRequest, ConstructionSite>('/construction-sites', { name, address: site.address });

    // Duplicate all buildings with their floors and units
    if (site.buildings && site.buildings.length > 0) {
      for (const building of site.buildings) {
        const newBuildingName = formData.get(`building_${building.id}_name`)?.toString()?.trim() || `${building.name} (Copy)`;
        const newBuilding = await apiClient.post<CreateBuildingRequest, Building>(`/construction-sites/${newSite.id}/buildings`, {
          name: newBuildingName,
        });

        // Duplicate all floors with their units
        if (building.floors && building.floors.length > 0) {
          for (const floor of building.floors) {
            const newFloorType = formData.get(`floor_${building.id}_${floor.id}_type`)?.toString()?.trim() || floor.type;
            const newFloor = await apiClient.post<CreateFloorRequest, Floor>(`/construction-sites/${newSite.id}/buildings/${newBuilding.id}/floors`, {
              type: newFloorType,
              usableArea: floor.usableArea,
              totalArea: floor.totalArea,
            });

            // Duplicate all units
            if (floor.units && floor.units.length > 0) {
              for (const unit of floor.units) {
                const newUnitLabel = formData.get(`unit_${building.id}_${floor.id}_${unit.id}_label`)?.toString()?.trim() || `${unit.label} (Copy)`;
                await apiClient.post<CreateUnitRequest, Unit>(`/construction-sites/${newSite.id}/buildings/${newBuilding.id}/floors/${newFloor.id}/units`, {
                  label: newUnitLabel,
                  usableArea: unit.usableArea,
                  bedrooms: unit.bedrooms,
                  bathrooms: unit.bathrooms,
                  balconies: unit.balconies,
                  toilets: unit.toilets,
                });
              }
            }
          }
        }
      }
    }

    return {};
  } catch {
    return { error: 'Failed to duplicate site' };
  }
}

export async function updateBuilding(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  const name = (formData.get('name') || '').toString().trim();
  if (!siteId || !buildingId || !name) return { error: 'Missing siteId, buildingId or name' };

  try {
    await apiClient.put(`/construction-sites/${siteId}/buildings/${buildingId}`, { name });
    return {};
  } catch {
    return { error: 'Failed to update building' };
  }
}

export async function duplicateBuilding(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  if (!siteId || !buildingId) return { error: 'Missing siteId or buildingId' };

  try {
    console.log(`[duplicateBuilding] Starting duplication for site=${siteId}, building=${buildingId}`);

    const site = await apiClient.get<ConstructionSite>(`/construction-sites/${siteId}`);
    const building = site.buildings?.find((b: Building) => b.id === buildingId);
    if (!building) return { error: 'Building not found' };

    console.log(`[duplicateBuilding] Found building: ${building.name} with ${building.floors?.length || 0} floors`);

    const name = (formData.get('name') || `${building.name} (Copy)`).toString().trim();
    const newBuilding = await apiClient.post<CreateBuildingRequest, Building>(`/construction-sites/${siteId}/buildings`, { name });
    console.log(`[duplicateBuilding] Created new building: ${newBuilding.id}`);

    // Duplicate all floors with their units
    if (building.floors && building.floors.length > 0) {
      for (const floor of building.floors) {
        console.log(`[duplicateBuilding] Duplicating floor: ${floor.id} (${floor.type})`);
        const newFloorType = formData.get(`floor_${floor.id}_type`)?.toString()?.trim() || floor.type;
        const newFloor = await apiClient.post<CreateFloorRequest, Floor>(`/construction-sites/${siteId}/buildings/${newBuilding.id}/floors`, {
          type: newFloorType,
          usableArea: floor.usableArea,
          totalArea: floor.totalArea,
        });
        console.log(`[duplicateBuilding] Created new floor: ${newFloor.id}`);

        // Duplicate all units
        if (floor.units && floor.units.length > 0) {
          console.log(`[duplicateBuilding] Duplicating ${floor.units.length} units from floor ${floor.id}`);
          for (const unit of floor.units) {
          const newUnitLabel = formData.get(`unit_${building.id}_${floor.id}_${unit.id}_label`)?.toString()?.trim() || `${unit.label} (Copy)`;
          await apiClient.post<CreateUnitRequest, Unit>(`/construction-sites/${siteId}/buildings/${newBuilding.id}/floors/${newFloor.id}/units`, {
              label: newUnitLabel,
              usableArea: unit.usableArea,
              bedrooms: unit.bedrooms,
              bathrooms: unit.bathrooms,
              balconies: unit.balconies,
              toilets: unit.toilets,
            });
            console.log(`[duplicateBuilding] Duplicated unit: ${unit.label} -> ${newUnitLabel}`);
          }
        }
      }
    }

    console.log('[duplicateBuilding] Duplication completed successfully');
    return {};
  } catch (error) {
    console.error('[duplicateBuilding] Failed to duplicate building:', error);
    return { error: 'Failed to duplicate building' };
  }
}

export async function updateFloor(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  const floorId = formData.get('floorId')?.toString();
  const type = (formData.get('type') || '').toString().trim();
  const usableArea = Number.parseFloat((formData.get('usableArea') || '').toString());
  const totalArea = Number.parseFloat((formData.get('totalArea') || '').toString());

  if (!siteId || !buildingId || !floorId || !type || Number.isNaN(usableArea) || Number.isNaN(totalArea)) {
    return { error: 'Missing required fields' };
  }

  try {
    await apiClient.put(`/construction-sites/${siteId}/buildings/${buildingId}/floors/${floorId}`, {
      type,
      usableArea,
      totalArea,
    });
    return {};
  } catch {
    return { error: 'Failed to update floor' };
  }
}

export async function duplicateFloor(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  const floorId = formData.get('floorId')?.toString();
  if (!siteId || !buildingId || !floorId) return { error: 'Missing ids' };

  try {
    console.log(`[duplicateFloor] Starting duplication for site=${siteId}, building=${buildingId}, floor=${floorId}`);

    const site = await apiClient.get<ConstructionSite>(`/construction-sites/${siteId}`);
    const building = site.buildings?.find((b: Building) => b.id === buildingId);
    const floor = building?.floors?.find((f: Floor) => f.id === floorId);
    if (!building || !floor) return { error: 'Floor not found' };

    console.log(`[duplicateFloor] Found floor: ${floor.id} (${floor.type}) with ${floor.units?.length || 0} units`);

    const newFloorType = (formData.get('name') || floor.type).toString().trim();
    const newFloor = await apiClient.post<CreateFloorRequest, Floor>(`/construction-sites/${siteId}/buildings/${buildingId}/floors`, {
      type: newFloorType,
      usableArea: floor.usableArea,
      totalArea: floor.totalArea,
    });
    console.log(`[duplicateFloor] Created new floor: ${newFloor.id}`);

    // Duplicate all units
    if (floor.units && floor.units.length > 0) {
      console.log(`[duplicateFloor] Duplicating ${floor.units.length} units from floor ${floor.id}`);
      for (const unit of floor.units) {
        const newUnitLabel = formData.get(`unit_${building.id}_${floor.id}_${unit.id}_label`)?.toString()?.trim() || `${unit.label} (Copy)`;
        await apiClient.post<CreateUnitRequest, Unit>(`/construction-sites/${siteId}/buildings/${buildingId}/floors/${newFloor.id}/units`, {
          label: newUnitLabel,
          usableArea: unit.usableArea,
          bedrooms: unit.bedrooms,
          bathrooms: unit.bathrooms,
          balconies: unit.balconies,
          toilets: unit.toilets,
        });
        console.log(`[duplicateFloor] Duplicated unit: ${unit.label} -> ${newUnitLabel}`);
      }
    }

    console.log('[duplicateFloor] Duplication completed successfully');
    return {};
  } catch (error) {
    console.error('[duplicateFloor] Failed to duplicate floor:', error);
    return { error: 'Failed to duplicate floor' };
  }
}

export async function updateUnit(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  const floorId = formData.get('floorId')?.toString();
  const unitId = formData.get('unitId')?.toString();

  const label = (formData.get('label') || '').toString().trim();
  const usableArea = Number.parseFloat((formData.get('usableArea') || '').toString());
  const bedrooms = Number.parseInt((formData.get('bedrooms') || '').toString(), 10);
  const bathrooms = Number.parseInt((formData.get('bathrooms') || '').toString(), 10);
  const balconies = Number.parseInt((formData.get('balconies') || '').toString(), 10);
  const toilets = Number.parseInt((formData.get('toilets') || '').toString(), 10);

  if (!siteId || !buildingId || !floorId || !unitId || !label
    || Number.isNaN(usableArea) || Number.isNaN(bedrooms) || Number.isNaN(bathrooms)
    || Number.isNaN(balconies) || Number.isNaN(toilets)) {
    return { error: 'Missing required fields' };
  }

  try {
    await apiClient.put(`/construction-sites/${siteId}/buildings/${buildingId}/floors/${floorId}/units/${unitId}`, {
      label,
      usableArea,
      bedrooms,
      bathrooms,
      balconies,
      toilets,
    });
    return {};
  } catch {
    return { error: 'Failed to update unit' };
  }
}

export async function duplicateUnit(formData: FormData): Promise<SimpleActionState> {
  const siteId = formData.get('siteId')?.toString();
  const buildingId = formData.get('buildingId')?.toString();
  const floorId = formData.get('floorId')?.toString();
  const unitId = formData.get('unitId')?.toString();
  if (!siteId || !buildingId || !floorId || !unitId) return { error: 'Missing ids' };

  try {
    const site = await apiClient.get<ConstructionSite>(`/construction-sites/${siteId}`);
    const building = site.buildings?.find((b: Building) => b.id === buildingId);
    const floor = building?.floors?.find((f: Floor) => f.id === floorId);
    const unit = floor?.units?.find((u: Unit) => u.id === unitId);
    if (!unit) return { error: 'Unit not found' };
    const name = (formData.get('name') || `${unit.label} (Copy)`).toString().trim();
    await apiClient.post(`/construction-sites/${siteId}/buildings/${buildingId}/floors/${floorId}/units`, {
      label: name,
      usableArea: unit.usableArea,
      bedrooms: unit.bedrooms,
      bathrooms: unit.bathrooms,
      balconies: unit.balconies,
      toilets: unit.toilets,
    });
    return {};
  } catch {
    return { error: 'Failed to duplicate unit' };
  }
}
