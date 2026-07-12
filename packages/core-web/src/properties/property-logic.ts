import { createPropertyRequestSchema } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';
import type { CreatePropertyActionState } from './types';

// Deliberately NOT a 'use server' function - cross-package Server Action
// resolution is fragile/undocumented in Next.js. Each consuming app wraps
// this in its own thin 'use server' action (and calls revalidatePath itself,
// since the path a consumer mounts properties at isn't core's to assume).
export async function createProperty(formData: FormData): Promise<CreatePropertyActionState> {
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

  return {};
}
