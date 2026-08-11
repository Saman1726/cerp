'use client';

import { useOfflineSync, type UseOfflineSyncResult } from '../../offline-sync/use-offline-sync';
import { listQueuedStepAttachments } from './offline-queue';
import { processQueue, type SyncEndpoints } from './sync-engine';

export type UseAttachmentSyncResult = UseOfflineSyncResult;

/**
 * Drains the offline step-attachment queue whenever the network comes back, and exposes queue
 * state for UI feedback (spinner, errors) -- same shape as Site Capture's useBackgroundSync.
 */
export function useAttachmentSync(endpoints?: SyncEndpoints): UseAttachmentSyncResult {
  return useOfflineSync({
    listPendingCount: async () => (await listQueuedStepAttachments()).length,
    sync: () => processQueue(endpoints),
  });
}
