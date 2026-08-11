import type { SiteCaptureKind } from '@cerp/shared-types';
import {
  createOfflineQueue,
  MAX_SYNC_ATTEMPTS,
  type QueuedItemEnvelope,
  type QueuedItemStatus,
} from '../offline-sync/queue';

export type QueuedCaptureStatus = QueuedItemStatus;

export interface QueuedCapture extends QueuedItemEnvelope {
  projectId: string;
  subProjectId?: string;
  kind: SiteCaptureKind;
  notes?: string;
  /** ISO timestamp taken at capture time, not upload time. */
  capturedAt: string;
}

export type NewQueuedCapture = Omit<
  QueuedCapture,
  'clientId' | 'status' | 'attempts' | 'lastError' | 'createdAt' | 'updatedAt'
>;

export { MAX_SYNC_ATTEMPTS };

// Same db name/version/store/keyPath as before this module was generalized onto
// packages/core-web/src/offline-sync -- anything already queued in a user's browser
// keeps draining correctly with no migration.
const queue = createOfflineQueue<QueuedCapture>({
  dbName: 'cerp-site-capture',
  storeName: 'captures',
  dbVersion: 1,
});

/** Persists a captured photo + form metadata locally and returns the queued record. */
export const enqueueCapture = queue.enqueue;

/** All captures still waiting to reach the backend, oldest first. */
export const listQueuedCaptures = queue.list;

export const removeQueuedCapture = queue.remove;

export const markCaptureFailed = queue.markFailed;
