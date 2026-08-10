import type {
  SiteCapturePresignRequest,
  SiteCapturePresignResponse,
  SiteCaptureSubmission,
} from '@cerp/shared-types';
import {
  listQueuedCaptures,
  removeQueuedCapture,
  markCaptureFailed,
  MAX_SYNC_ATTEMPTS,
  type QueuedCapture,
} from './offline-queue';

export interface SyncEndpoints {
  /** Defaults to '/api/uploads/presign'. */
  presignUrl?: string;
  /** Defaults to '/api/site-captures'. */
  submitUrl?: string;
}

export interface SyncResult {
  synced: string[];
  failed: string[];
  remaining: number;
}

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
 * concurrently. Framework-agnostic (only fetch + IndexedDB) so it can run
 * from a plain window context or, later, a service worker.
 */
export async function processQueue(endpoints: SyncEndpoints = {}): Promise<SyncResult> {
  const resolved: ResolvedEndpoints = {
    presignUrl: endpoints.presignUrl ?? '/api/uploads/presign',
    submitUrl: endpoints.submitUrl ?? '/api/site-captures',
  };

  const queued = await listQueuedCaptures();
  const synced: string[] = [];
  const failed: string[] = [];

  for (const capture of queued) {
    // Connection dropped again mid-drain - stop rather than burn through
    // retry attempts on items that never got a real chance to send.
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      break;
    }

    if (capture.attempts >= MAX_SYNC_ATTEMPTS) {
      failed.push(capture.clientId);
      continue;
    }

    try {
      await syncOne(capture, resolved);
      await removeQueuedCapture(capture.clientId);
      synced.push(capture.clientId);
    } catch (error) {
      await markCaptureFailed(capture.clientId, error instanceof Error ? error.message : 'Unknown sync error');
      failed.push(capture.clientId);
    }
  }

  return { synced, failed, remaining: failed.length };
}
