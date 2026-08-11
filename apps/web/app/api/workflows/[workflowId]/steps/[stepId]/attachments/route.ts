import { NextRequest, NextResponse } from 'next/server';
import { DOCUMENT_SERVICE_URL, WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import type {
  MediaPresignDownloadResponse,
  WorkflowStepAttachment,
  WorkflowStepAttachmentSubmission,
} from '@cerp/shared-types';
import { getAuthHeaders, extractSessionUser } from '../../../../utils';
import { getDocumentServiceHeaders } from '../../../../../uploads/utils';

/** Raw shape workflow-service returns for a WorkflowStepAttachment (`workflow`/`step` are @JsonIgnore'd). */
interface BackendWorkflowStepAttachment {
  id: string;
  uploadedBy: string;
  kind: 'PHOTO' | 'DOCUMENT';
  objectKey: string;
  originalFileName: string;
  contentType: string;
  byteSize: number;
  createdAt: string;
}

/** Resolves a viewable URL server-side so the client never needs to know the storage object key. */
async function resolveDownloadUrl(
  request: NextRequest,
  objectKey: string,
): Promise<string | undefined> {
  try {
    const res = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/media/presign-download`, {
      method: 'POST',
      headers: getDocumentServiceHeaders(request),
      body: JSON.stringify({ objectKey }),
      signal: AbortSignal.timeout(15000),
    });
    if (!res.ok) {
      const body = await res.text().catch(() => '');
      console.error(`[attachments] presign-download failed (${res.status}) for ${objectKey}: ${body}`);
      return undefined;
    }
    const data = (await res.json()) as MediaPresignDownloadResponse;
    return data.url;
  } catch (error) {
    console.error(`[attachments] presign-download request failed for ${objectKey}:`, error);
    return undefined;
  }
}

async function toFrontendAttachment(
  request: NextRequest,
  raw: BackendWorkflowStepAttachment,
  workflowId: string,
  stepId: string,
): Promise<WorkflowStepAttachment> {
  return {
    id: raw.id,
    workflowId,
    stepId,
    kind: raw.kind,
    fileName: raw.originalFileName,
    contentType: raw.contentType,
    byteSize: raw.byteSize,
    url: await resolveDownloadUrl(request, raw.objectKey),
    uploadedBy: raw.uploadedBy,
    createdAt: raw.createdAt,
  };
}

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string; stepId: string }> },
) {
  try {
    const { workflowId, stepId } = await params;
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}/steps/${stepId}/attachments`;

    const res = await fetch(url, {
      headers: getAuthHeaders(request),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const attachments = (await res.json()) as BackendWorkflowStepAttachment[];
    const resolved = await Promise.all(
      attachments.map((attachment) =>
        toFrontendAttachment(request, attachment, workflowId, stepId),
      ),
    );
    return NextResponse.json(resolved);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string; stepId: string }> },
) {
  try {
    const { workflowId, stepId } = await params;
    const body = (await request.json()) as WorkflowStepAttachmentSubmission;
    const { userId } = await extractSessionUser();
    if (!userId) {
      return NextResponse.json({ message: 'Could not resolve the current user' }, { status: 401 });
    }

    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}/steps/${stepId}/attachments`;
    const res = await fetch(url, {
      method: 'POST',
      headers: getAuthHeaders(request),
      // Backend expects { id, uploadedBy, kind, objectKey, originalFileName, contentType, byteSize },
      // not the frontend's { clientId, workflowId, stepId, ... }.
      body: JSON.stringify({
        id: body.clientId,
        uploadedBy: userId,
        kind: body.kind,
        objectKey: body.objectKey,
        originalFileName: body.originalFileName,
        contentType: body.contentType,
        byteSize: body.byteSize,
      }),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const attachment = (await res.json()) as BackendWorkflowStepAttachment;
    return NextResponse.json({ id: attachment.id, status: 'RECORDED' });
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}
