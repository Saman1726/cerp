import { apiClient, ApiClientError } from '@cerp/core-web/lib/api-client';
import type { CreatePropertyActionState } from '@cerp/core-web/properties';

// This overlay's own extension logic: posts to /demo/properties (not core's
// /properties) so the extra inspectorNotes field reaches the satellite
// table. Core's PropertyForm component is still reused as-is via its
// extraFields slot - only the submission logic is overlay-specific.
export async function createDemoProperty(formData: FormData): Promise<CreatePropertyActionState> {
  const name = formData.get('name');
  const address = formData.get('address');
  const inspectorNotes = formData.get('inspectorNotes');

  if (typeof name !== 'string' || !name || typeof address !== 'string' || !address) {
    return { error: 'Name and address are required' };
  }

  try {
    await apiClient.post('/demo/properties', {
      name,
      address,
      inspectorNotes: typeof inspectorNotes === 'string' && inspectorNotes ? inspectorNotes : undefined,
    });
  } catch (error) {
    if (error instanceof ApiClientError) {
      return { error: error.message };
    }
    return { error: 'Failed to create property' };
  }

  return {};
}
