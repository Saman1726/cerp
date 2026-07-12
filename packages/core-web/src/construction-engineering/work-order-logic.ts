import { createWorkOrderRequestSchema } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';
import type { IssueWorkOrderActionState } from './types';

// Deliberately NOT a 'use server' function - see properties/property-logic.ts
// for why. Each consuming app wraps this in its own thin 'use server' action.
export async function issueWorkOrder(formData: FormData): Promise<IssueWorkOrderActionState> {
  const parsed = createWorkOrderRequestSchema.safeParse({
    projectId: formData.get('projectId'),
    title: formData.get('title'),
    description: formData.get('description') || undefined,
    contractorPartnerId: formData.get('contractorPartnerId'),
  });

  if (!parsed.success) {
    return { error: parsed.error.issues[0]?.message ?? 'Invalid input' };
  }

  try {
    await apiClient.post('/work-orders', parsed.data);
  } catch {
    return { error: 'Failed to issue work order' };
  }

  return {};
}
