'use client';

import { useOfflineSync, type UseOfflineSyncResult } from '../../offline-sync/use-offline-sync';
import { listQueuedSagaAttachments } from './offline-queue';
import { processQueue, type SyncEndpoints } from './sync-engine';

export type UseSagaAttachmentSyncResult = UseOfflineSyncResult;

/**
 * Drains the offline saga-attachment queue whenever the network comes back, and exposes queue
 * state for UI feedback (spinner, errors) -- same shape as the workflow-step attachment queue's
 * useAttachmentSync.
 */
export function useSagaAttachmentSync(endpoints?: SyncEndpoints): UseSagaAttachmentSyncResult {
  return useOfflineSync({
    listPendingCount: async () => (await listQueuedSagaAttachments()).length,
    sync: () => processQueue(endpoints),
  });
}
