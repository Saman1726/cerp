import { apiClient } from '../lib/api-client';
import type { SimpleActionState } from './types';

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