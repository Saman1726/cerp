'use server';

import { revalidatePath } from 'next/cache';
import { createPropertyRequestSchema } from '@cerp/shared-types';
import { apiClient } from '@/lib/api-client';

export interface CreatePropertyActionState {
  error?: string;
}

export async function createPropertyAction(
  _prevState: CreatePropertyActionState,
  formData: FormData,
): Promise<CreatePropertyActionState> {
  const parsed = createPropertyRequestSchema.safeParse({
    name: formData.get('name'),
    address: formData.get('address'),
  });

  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? 'Invalid input' };
  }

  try {
    await apiClient.post('/properties', parsed.data);
  } catch {
    return { error: 'Failed to create property' };
  }

  revalidatePath('/properties');
  return {};
}
