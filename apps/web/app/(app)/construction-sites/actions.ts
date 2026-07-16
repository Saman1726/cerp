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
} from '@cerp/core-web/construction-sites';

export async function createSiteAction(formData: FormData): Promise<void> {
  const result = await createSite(formData);
  if (!result.error) {
    revalidatePath('/construction-sites');
  }
}

export async function deleteSiteAction(formData: FormData): Promise<void> {
  const result = await deleteSite(formData);
  if (!result.error) {
    revalidatePath('/construction-sites');
  }
}

export async function addBuildingAction(formData: FormData): Promise<void> {
  const result = await addBuilding(formData);
  if (!result.error) {
    revalidatePath('/construction-sites');
  }
}

export async function deleteBuildingAction(formData: FormData): Promise<void> {
  const result = await deleteBuilding(formData);
  if (!result.error) {
    revalidatePath('/construction-sites');
  }
}

export async function addFloorAction(formData: FormData): Promise<void> {
  const result = await addFloor(formData);
  if (!result.error) {
    revalidatePath('/construction-sites');
  }
}

export async function deleteFloorAction(formData: FormData): Promise<void> {
  const result = await deleteFloor(formData);
  if (!result.error) {
    revalidatePath('/construction-sites');
  }
}

export async function addUnitAction(formData: FormData): Promise<void> {
  const result = await addUnit(formData);
  if (!result.error) {
    revalidatePath('/construction-sites');
  }
}

export async function deleteUnitAction(formData: FormData): Promise<void> {
  const result = await deleteUnit(formData);
  if (!result.error) {
    revalidatePath('/construction-sites');
  }
}