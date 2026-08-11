// ── Saga Attachment Types ──
//
// Photo/document evidence attached to a saga as a whole (sagas have no individually-addressable
// step records - see the backend's SagaNoteEntity Javadoc). Upload path mirrors the workflow-step
// attachment one: the browser presigns and PUTs the (compressed, for photos) blob directly to
// object storage via document-service, then submits lightweight JSON metadata that
// workflow-service persists as the system of record for "what's attached to this saga".

export type SagaAttachmentKind = 'PHOTO' | 'DOCUMENT';

export interface SagaAttachmentSubmission {
  /** Client-generated UUID; the idempotency key for this attachment end-to-end. */
  clientId: string;
  sagaId: string;
  kind: SagaAttachmentKind;
  /** Must match the objectKey returned by the presign call once the PUT succeeds. */
  objectKey: string;
  originalFileName: string;
  /** Content type of the upload (compressed, for photos), not necessarily the original file's. */
  contentType: string;
  byteSize: number;
}

export interface SagaAttachment {
  id: string;
  sagaId: string;
  kind: SagaAttachmentKind;
  fileName: string;
  contentType: string;
  byteSize: number;
  /**
   * A short-lived, fetchable URL for viewing/downloading the object -- resolved on demand via
   * the presign-download endpoint, not stored. Absent until resolved.
   */
  url?: string;
  uploadedBy: string;
  createdAt: string;
}

export interface SagaAttachmentSubmissionResponse {
  id: string;
  status: string;
}
