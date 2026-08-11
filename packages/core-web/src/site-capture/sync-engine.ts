import type {
  SiteCapturePresignRequest,
  SiteCapturePresignResponse,
  SiteCaptureSubmission,
} from '@cerp/shared-types';
import { processQueue as processGenericQueue, type SyncResult } from '../offline-sync/sync-engine';
import {
  listQueuedCaptures,
  removeQueuedCapture,
  markCaptureFailed,
  type QueuedCapture,
} from './offline-queue';

export interface SyncEndpoints {
  /** Defaults to '/api/uploads/presign'. */
  presignUrl?: string;
  /** Defaults to '/api/site-captures'. */
  submitUrl?: string;
}

export type { SyncResult };

type ResolvedEndpoints = Required<SyncEndpoints>;

async function syncOne(capture: QueuedCapture, endpoints: ResolvedEndpoints): Promise<void> {
  const presignRequest: SiteCapturePresignRequest = {
    projectId: capture.projectId,
    subProjectId: capture.subProjectId,
    kind: capture.kind,
    fileName: capture.originalFileName,
    contentType: capture.contentType,
  };

  const presignRes = await fetch(endpoints.presignUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(presignRequest),
  });
  if (!presignRes.ok) {
    throw new Error(`Presign request failed (${presignRes.status})`);
  }
  const { uploadUrl, objectKey } = (await presignRes.json()) as SiteCapturePresignResponse;

  // Goes straight to S3 - the whole point of a presigned URL is that this
  // multi-megabyte PUT never touches our own servers.
  const uploadRes = await fetch(uploadUrl, {
    method: 'PUT',
    headers: { 'Content-Type': capture.contentType },
    body: capture.blob,
  });
  if (!uploadRes.ok) {
    throw new Error(`Upload to storage failed (${uploadRes.status})`);
  }

  const submission: SiteCaptureSubmission = {
    clientId: capture.clientId,
    projectId: capture.projectId,
    subProjectId: capture.subProjectId,
    kind: capture.kind,
    notes: capture.notes,
    capturedAt: capture.capturedAt,
    objectKey,
    originalFileName: capture.originalFileName,
    contentType: capture.contentType,
    byteSize: capture.byteSize,
  };

  const submitRes = await fetch(endpoints.submitUrl, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(submission),
  });
  if (!submitRes.ok) {
    throw new Error(`Metadata submission failed (${submitRes.status})`);
  }
}

/**
 * Drains the offline queue one capture at a time, oldest first, so a weak
 * site connection is never asked to push multiple multi-megabyte uploads
 * concurrently.
 */
export async function processQueue(endpoints: SyncEndpoints = {}): Promise<SyncResult> {
  const resolved: ResolvedEndpoints = {
    presignUrl: endpoints.presignUrl ?? '/api/uploads/presign',
    submitUrl: endpoints.submitUrl ?? '/api/site-captures',
  };

  return processGenericQueue(
    { list: listQueuedCaptures, remove: removeQueuedCapture, markFailed: markCaptureFailed },
    (capture) => syncOne(capture, resolved),
  );
}
