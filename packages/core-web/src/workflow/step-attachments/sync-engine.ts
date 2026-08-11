import type {
  SiteCapturePresignResponse,
  WorkflowStepAttachmentSubmission,
} from '@cerp/shared-types';
import {
  processQueue as processGenericQueue,
  type SyncResult,
} from '../../offline-sync/sync-engine';
import {
  listQueuedStepAttachments,
  removeQueuedStepAttachment,
  markStepAttachmentFailed,
  type QueuedStepAttachment,
} from './offline-queue';

export interface SyncEndpoints {
  /** Defaults to '/api/uploads/presign'. */
  presignUrl?: string;
}

export type { SyncResult };

async function syncOne(item: QueuedStepAttachment, presignUrl: string): Promise<void> {
  const presignRes = await fetch(presignUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      projectId: item.projectId,
      subProjectId: item.subProjectId,
      kind: item.kind,
      fileName: item.originalFileName,
      contentType: item.contentType,
    }),
  });
  if (!presignRes.ok) {
    throw new Error(`Presign request failed (${presignRes.status})`);
  }
  // Reuses the same document-service presign contract Site Capture uses -- the endpoint doesn't
  // care what feature is calling it, only projectId/contentType.
  const { uploadUrl, objectKey } = (await presignRes.json()) as SiteCapturePresignResponse;

  // Goes straight to S3/MinIO - the whole point of a presigned URL is that this multi-megabyte
  // PUT never touches our own servers.
  const uploadRes = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': item.contentType },
    body: item.blob,
  });
  if (!uploadRes.ok) {
    throw new Error(`Upload to storage failed (${uploadRes.status})`);
  }

  const submission: WorkflowStepAttachmentSubmission = {
    clientId: item.clientId,
    workflowId: item.workflowId,
    stepId: item.stepId,
    kind: item.kind,
    objectKey,
    originalFileName: item.originalFileName,
    contentType: item.contentType,
    byteSize: item.byteSize,
  };

  const submitRes = await fetch(
    `/api/workflows/${item.workflowId}/steps/${item.stepId}/attachments`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(submission),
    },
  );
  if (!submitRes.ok) {
    throw new Error(`Metadata submission failed (${submitRes.status})`);
  }
}

/**
 * Drains the offline queue one attachment at a time, oldest first, so a weak site connection is
 * never asked to push multiple multi-megabyte uploads concurrently.
 */
export async function processQueue(endpoints: SyncEndpoints = {}): Promise<SyncResult> {
  const presignUrl = endpoints.presignUrl ?? '/api/uploads/presign';

  return processGenericQueue(
    {
      list: listQueuedStepAttachments,
      remove: removeQueuedStepAttachment,
      markFailed: markStepAttachmentFailed,
    },
    (item) => syncOne(item, presignUrl),
  );
}
