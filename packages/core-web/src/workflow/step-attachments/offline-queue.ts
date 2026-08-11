import type { WorkflowStepAttachmentKind } from '@cerp/shared-types';
import {
  createOfflineQueue,
  MAX_SYNC_ATTEMPTS,
  type QueuedItemEnvelope,
  type QueuedItemStatus,
} from '../../offline-sync/queue';

export type QueuedStepAttachmentStatus = QueuedItemStatus;

export interface QueuedStepAttachment extends QueuedItemEnvelope {
  workflowId: string;
  stepId: string;
  projectId: string;
  subProjectId?: string;
  kind: WorkflowStepAttachmentKind;
  /** ISO timestamp taken at capture/pick time, not upload time. */
  capturedAt: string;
}

export type NewQueuedStepAttachment = Omit<
  QueuedStepAttachment,
  'clientId' | 'status' | 'attempts' | 'lastError' | 'createdAt' | 'updatedAt'
>;

export { MAX_SYNC_ATTEMPTS };

// Its own IndexedDB database, independent of Site Capture's ('cerp-site-capture') -- a bug in
// one queue can't jam or corrupt the other.
const queue = createOfflineQueue<QueuedStepAttachment>({
  dbName: 'cerp-workflow-step-attachments',
  storeName: 'attachments',
  dbVersion: 1,
});

/** Persists a picked/captured photo or document locally and returns the queued record. */
export const enqueueStepAttachment = queue.enqueue;

/** All attachments still waiting to reach the backend, oldest first. */
export const listQueuedStepAttachments = queue.list;

export const removeQueuedStepAttachment = queue.remove;

export const markStepAttachmentFailed = queue.markFailed;
