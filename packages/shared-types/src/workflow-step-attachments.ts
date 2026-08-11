// ── Workflow Step Attachment Types ──
//
// Photo/document evidence attached to a single construction workflow step. The upload path
// mirrors Site Capture's: the browser presigns and PUTs the (compressed, for photos) blob
// directly to object storage via document-service, then submits lightweight JSON metadata that
// workflow-service persists as the system of record for "what's attached to this step".

export type WorkflowStepAttachmentKind = 'PHOTO' | 'DOCUMENT';

export interface WorkflowStepAttachmentSubmission {
  /** Client-generated UUID; the idempotency key for this attachment end-to-end. */
  clientId: string;
  workflowId: string;
  stepId: string;
  kind: WorkflowStepAttachmentKind;
  /** Must match the objectKey returned by the presign call once the PUT succeeds. */
  objectKey: string;
  originalFileName: string;
  /** Content type of the upload (compressed, for photos), not necessarily the original file's. */
  contentType: string;
  byteSize: number;
}

export interface WorkflowStepAttachment {
  id: string;
  workflowId: string;
  stepId: string;
  kind: WorkflowStepAttachmentKind;
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

export interface WorkflowStepAttachmentSubmissionResponse {
  id: string;
  status: string;
}
