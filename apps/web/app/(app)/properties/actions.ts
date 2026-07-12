'use server';

import { revalidatePath } from 'next/cache';
import { createProperty } from '@cerp/core-web/properties';
import type { CreatePropertyActionState } from '@cerp/core-web/properties';

export async function createPropertyAction(
  _prevState: CreatePropertyActionState,
  formData: FormData,
): Promise<CreatePropertyActionState> {
  const result = await createProperty(formData);
  if (!result.error) {
    revalidatePath('/properties');
  }
  return result;
}
