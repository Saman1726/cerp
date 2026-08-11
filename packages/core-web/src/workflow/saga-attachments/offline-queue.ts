import type { SagaAttachmentKind } from '@cerp/shared-types';
import {
  createOfflineQueue,
  MAX_SYNC_ATTEMPTS,
  type QueuedItemEnvelope,
  type QueuedItemStatus,
} from '../../offline-sync/queue';

export type QueuedSagaAttachmentStatus = QueuedItemStatus;

export interface QueuedSagaAttachment extends QueuedItemEnvelope {
  sagaId: string;
  projectId: string;
  kind: SagaAttachmentKind;
  /** ISO timestamp taken at capture/pick time, not upload time. */
  capturedAt: string;
}

export type NewQueuedSagaAttachment = Omit<
  QueuedSagaAttachment,
  'clientId' | 'status' | 'attempts' | 'lastError' | 'createdAt' | 'updatedAt'
>;

export { MAX_SYNC_ATTEMPTS };

// Its own IndexedDB database, independent of Site Capture's ('cerp-site-capture') and the
// workflow-step attachment queue's ('cerp-workflow-step-attachments') -- a bug in one queue
// can't jam or corrupt the others.
const queue = createOfflineQueue<QueuedSagaAttachment>({
  dbName: 'cerp-saga-attachments',
  storeName: 'attachments',
  dbVersion: 1,
});

/** Persists a picked/captured photo or document locally and returns the queued record. */
export const enqueueSagaAttachment = queue.enqueue;

/** All attachments still waiting to reach the backend, oldest first. */
export const listQueuedSagaAttachments = queue.list;

export const removeQueuedSagaAttachment = queue.remove;

export const markSagaAttachmentFailed = queue.markFailed;
