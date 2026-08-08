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
