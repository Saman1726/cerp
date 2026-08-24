// ── Site Capture Types ──
//
// Wire contracts for the field-capture PWA flow: a field worker photographs
// site progress, a delivery invoice, or a weight certificate, and the
// (compressed) image is uploaded directly to object storage via a presigned
// URL while the lightweight JSON metadata travels through the API gateway to
// Kafka. These types are shared between the Next.js API routes (apps/web) and
// the offline queue / sync engine (@cerp/core-web/site-capture).

export type SiteCaptureKind = 'PROGRESS_PHOTO' | 'DELIVERY_INVOICE' | 'WEIGHT_CERTIFICATE';

export interface SiteCapturePresignRequest {
  projectId: string;
  subProjectId?: string;
  kind: SiteCaptureKind;
  fileName: string;
  contentType: string;
}

export interface SiteCapturePresignResponse {
  /** Presigned S3 PUT URL with the public endpoint (for browser uploads). */
  uploadUrl: string;
  /** Presigned S3 PUT URL with the internal endpoint (for server-side uploads). */
  serverUploadUrl: string;
  /** Storage object key the backend will associate with the metadata below. */
  objectKey: string;
  /** ISO timestamp the presigned URL stops being valid. */
  expiresAt: string;
}

export interface SiteCaptureSubmission {
  /** Client-generated UUID; the idempotency key for this capture end-to-end. */
  clientId: string;
  projectId: string;
  subProjectId?: string;
  kind: SiteCaptureKind;
  notes?: string;
  /** ISO timestamp captured at photo time, which may be well before upload. */
  capturedAt: string;
  /** Must match the objectKey returned by the presign call once the PUT succeeds. */
  objectKey: string;
  originalFileName: string;
  /** Content type of the compressed upload (e.g. image/webp), not the original. */
  contentType: string;
  byteSize: number;
}

export interface SiteCaptureSubmissionResponse {
  id: string;
  status: string;
}

// Generic media presign-download contract (document-service's POST /api/v1/media/presign-download),
// used by Site Capture and by other features (e.g. workflow step attachments) that need to view
// or download an object after it's already been uploaded via the presign/PUT flow above.
export interface MediaPresignDownloadRequest {
  objectKey: string;
}

export interface MediaPresignDownloadResponse {
  url: string;
  expiresAt: string;
}
