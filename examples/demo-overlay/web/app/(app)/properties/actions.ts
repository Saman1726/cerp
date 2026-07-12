'use server';

import { revalidatePath } from 'next/cache';
import type { CreatePropertyActionState } from '@cerp/core-web/properties';
import { createDemoProperty } from '@/lib/demo-property-logic';

export async function createDemoPropertyAction(
  _prevState: CreatePropertyActionState,
  formData: FormData,
): Promise<CreatePropertyActionState> {
  const result = await createDemoProperty(formData);
  if (!result.error) {
    revalidatePath('/properties');
  }
  return result;
}
