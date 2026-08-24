'use server';

import { revalidatePath } from 'next/cache';
import {
  createSite,
  deleteSite,
  addBuilding,
  deleteBuilding,
  addFloor,
  deleteFloor,
  addUnit,
  deleteUnit,
  updateSite,
  duplicateSite,
  updateBuilding,
  duplicateBuilding,
  updateFloor,
  duplicateFloor,
  updateUnit,
  duplicateUnit,
  importIfc,
} from '@cerp/core-web/construction-sites';

function revalidateOrThrow(result: { error?: string }, path: string): void {
  if (result.error) {
    throw new Error(result.error);
  }
  revalidatePath(path);
}

export async function createSiteAction(formData: FormData): Promise<void> {
  const result = await createSite(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function deleteSiteAction(formData: FormData): Promise<void> {
  const result = await deleteSite(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function updateSiteAction(formData: FormData): Promise<void> {
  const result = await updateSite(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function duplicateSiteAction(formData: FormData): Promise<void> {
  const result = await duplicateSite(formData);
  revalidateOrThrow(result, '/construction-sites');
}
export async function importIfcAction(formData: FormData): Promise<void> {
  const result = await importIfc(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function addBuildingAction(formData: FormData): Promise<void> {
  const result = await addBuilding(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function deleteBuildingAction(formData: FormData): Promise<void> {
  const result = await deleteBuilding(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function updateBuildingAction(formData: FormData): Promise<void> {
  const result = await updateBuilding(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function duplicateBuildingAction(formData: FormData): Promise<void> {
  const result = await duplicateBuilding(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function addFloorAction(formData: FormData): Promise<void> {
  const result = await addFloor(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function deleteFloorAction(formData: FormData): Promise<void> {
  const result = await deleteFloor(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function updateFloorAction(formData: FormData): Promise<void> {
  const result = await updateFloor(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function duplicateFloorAction(formData: FormData): Promise<void> {
  const result = await duplicateFloor(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function addUnitAction(formData: FormData): Promise<void> {
  const result = await addUnit(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function deleteUnitAction(formData: FormData): Promise<void> {
  const result = await deleteUnit(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function updateUnitAction(formData: FormData): Promise<void> {
  const result = await updateUnit(formData);
  revalidateOrThrow(result, '/construction-sites');
}

export async function duplicateUnitAction(formData: FormData): Promise<void> {
  const result = await duplicateUnit(formData);
  revalidateOrThrow(result, '/construction-sites');
}

// ── IFC File Actions ───────────────────────────────────────────────────

import {
  uploadIfcFile as uploadIfcFileLogic,
  deleteIfcFile as deleteIfcFileLogic,
  triggerPreview as triggerPreviewLogic,
  triggerBlenderRender as triggerBlenderRenderLogic,
  listIfcFiles as listIfcFilesLogic,
} from '@cerp/core-web/construction-sites';

export async function uploadIfcFileAction(formData: FormData): Promise<void> {
  const siteId = formData.get('siteId')?.toString();
  const file = formData.get('ifcFile') as File | null;
  if (!siteId) {
    throw new Error('Missing siteId');
  }
  if (!file) {
    throw new Error('No IFC file selected');
  }

  const result = await uploadIfcFileLogic(siteId, file);
  if (result.error) {
    throw new Error(result.error);
  }
  revalidatePath('/construction-sites');
}

export async function deleteIfcFileAction(formData: FormData): Promise<void> {
  const siteId = formData.get('siteId')?.toString();
  const fileId = formData.get('fileId')?.toString();
  if (!siteId || !fileId) {
    throw new Error('Missing siteId or fileId');
  }
  await deleteIfcFileLogic(siteId, fileId);
  revalidatePath('/construction-sites');
}

export async function triggerPreviewAction(siteId: string, fileId: string): Promise<void> {
  await triggerPreviewLogic(siteId, fileId);
}

export async function triggerBlenderRenderAction(siteId: string, fileId: string, view?: string): Promise<void> {
  await triggerBlenderRenderLogic(siteId, fileId, view);
}

export async function listIfcFilesAction(siteId: string): Promise<import('@cerp/core-web/construction-sites').IFCFile[]> {
  return listIfcFilesLogic(siteId);
}
