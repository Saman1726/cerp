'use client';

import { useOfflineSync, type UseOfflineSyncResult } from '../offline-sync/use-offline-sync';
import { listQueuedCaptures } from './offline-queue';
import { processQueue, type SyncEndpoints } from './sync-engine';

export type UseBackgroundSyncResult = UseOfflineSyncResult;

/**
 * Drains the offline capture queue whenever the network comes back, and
 * exposes queue state for UI feedback (pending badge, sync spinner, errors).
 */
export function useBackgroundSync(endpoints?: SyncEndpoints): UseBackgroundSyncResult {
  return useOfflineSync({
    listPendingCount: async () => (await listQueuedCaptures()).length,
    sync: () => processQueue(endpoints),
  });
}
