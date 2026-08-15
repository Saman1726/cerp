import { NextRequest, NextResponse } from 'next/server';
import { DOCUMENT_SERVICE_URL, WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import type {
  MediaPresignDownloadResponse,
  SagaAttachment,
  SagaNoteCreateRequest,
} from '@cerp/shared-types';
import { getBackendHeaders, extractSessionUser } from '../../workflows/utils';
import { getDocumentServiceHeaders } from '../../uploads/utils';

/**
 * Proxy /api/sagas/* requests to the workflow service.
 *
 * The frontend apiClient routes /api/sagas to WORKFLOW_SERVICE_URL
 * (http://workflow-service-java:8089 in Docker). Since NEXT_PUBLIC_
 * variables are baked into the browser bundle, the browser can't
 * resolve Docker service names. This route acts as a server-side
 * proxy so the browser only needs to fetch from the relative path.
 *
 * Notes/attachments sub-resources are handled as special-case branches inside this file rather
 * than dedicated route files: Next.js forbids a differently-named dynamic segment (e.g.
 * `[sagaId]/notes/route.ts`) coexisting with this catch-all `[...path]` at the same tree
 * position (build-time error, not a style choice).
 */

function proxyPath(request: NextRequest): string {
  const url = new URL(request.url);
  const path = url.pathname;
  // Remove /api/sagas prefix to get the saga-specific path
  // e.g., /api/sagas?page=1 → /api/v1/sagas (base endpoint with query params)
  // e.g., /api/sagas/abc-123 → /api/v1/sagas/abc-123
  // e.g., /api/sagas/abc-123/events → /api/v1/sagas/abc-123/events
  const suffix = path.replace(/^\/api\/sagas/, '');
  return `/api/v1/sagas${suffix}`;
}

function matchAttachmentsPath(sagaPath: string): { sagaId: string; attachmentId?: string } | null {
  const match = sagaPath.match(/^\/api\/v1\/sagas\/([^/]+)\/attachments(?:\/([^/]+))?$/);
  return match ? { sagaId: match[1]!, attachmentId: match[2] } : null;
}

function matchNotesPath(sagaPath: string): { sagaId: string } | null {
  const match = sagaPath.match(/^\/api\/v1\/sagas\/([^/]+)\/notes$/);
  return match ? { sagaId: match[1]! } : null;
}

/** Raw shape workflow-service returns for a saga attachment (no @JsonIgnore needed - SagaAttachmentEntity has no relationships). */
interface BackendSagaAttachment {
  id: string;
  sagaId: string;
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
      console.error(`[sagas] presign-download failed (${res.status}) for ${objectKey}: ${body}`);
      return undefined;
    }
    const data = (await res.json()) as MediaPresignDownloadResponse;
    return data.url;
  } catch (error) {
    console.error(`[sagas] presign-download request failed for ${objectKey}:`, error);
    return undefined;
  }
}

async function toFrontendAttachment(
  request: NextRequest,
  raw: BackendSagaAttachment,
): Promise<SagaAttachment> {
  return {
    id: raw.id,
    sagaId: raw.sagaId,
    kind: raw.kind,
    fileName: raw.originalFileName,
    contentType: raw.contentType,
    byteSize: raw.byteSize,
    url: await resolveDownloadUrl(request, raw.objectKey),
    uploadedBy: raw.uploadedBy,
    createdAt: raw.createdAt,
  };
}

export async function GET(request: NextRequest) {
  try {
    const sagaPath = proxyPath(request);
    const { searchParams } = new URL(request.url);

    const targetUrl = new URL(sagaPath, WORKFLOW_SERVICE_URL);
    // Forward query parameters
    searchParams.forEach((value, key) => {
      targetUrl.searchParams.set(key, value);
    });

    const headers = getBackendHeaders(request);

    const res = await fetch(targetUrl.toString(), {
      headers,
      cache: 'no-store',
    });

    if (!res.ok) {
      const body = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: body?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const data = await res.json();

    // Attachment lists need objectKey resolved to a viewable url server-side, so the frontend
    // type never carries a raw storage key - same decision already made for workflow-step
    // attachments. Notes need no translation: SagaNote already matches the Java shape 1:1.
    const attachmentsMatch = matchAttachmentsPath(sagaPath);
    if (attachmentsMatch && !attachmentsMatch.attachmentId) {
      const attachments = data as BackendSagaAttachment[];
      const resolved = await Promise.all(attachments.map((a) => toFrontendAttachment(request, a)));
      return NextResponse.json(resolved);
    }

    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const sagaPath = proxyPath(request);
    const body = await request.json();

    // Never trust a client-supplied actor id - resolve it server-side, matching every existing
    // note/attachment write in this codebase.
    const notesMatch = matchNotesPath(sagaPath);
    if (notesMatch) {
      const { userId } = await extractSessionUser();
      if (!userId) {
        return NextResponse.json(
          { message: 'Could not resolve the current user' },
          { status: 401 },
        );
      }
      const noteBody = body as SagaNoteCreateRequest;
      const res = await fetch(`${WORKFLOW_SERVICE_URL}${sagaPath}`, {
        method: 'POST',
        headers: getBackendHeaders(request),
        body: JSON.stringify({
          authorId: userId,
          note: noteBody.note,
          type: noteBody.type ?? 'GENERAL',
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
      return NextResponse.json(await res.json());
    }

    const attachmentsMatch = matchAttachmentsPath(sagaPath);
    if (attachmentsMatch && !attachmentsMatch.attachmentId) {
      const { userId } = await extractSessionUser();
      if (!userId) {
        return NextResponse.json(
          { message: 'Could not resolve the current user' },
          { status: 401 },
        );
      }
      const submission = body as {
        clientId: string;
        kind: string;
        objectKey: string;
        originalFileName: string;
        contentType: string;
        byteSize: number;
      };
      // Backend expects { id, uploadedBy, kind, objectKey, originalFileName, contentType, byteSize },
      // not the frontend's { clientId, sagaId, ... } - matches the workflow-step-attachment precedent.
      const res = await fetch(`${WORKFLOW_SERVICE_URL}${sagaPath}`, {
        method: 'POST',
        headers: getBackendHeaders(request),
        body: JSON.stringify({
          id: submission.clientId,
          uploadedBy: userId,
          kind: submission.kind,
          objectKey: submission.objectKey,
          originalFileName: submission.originalFileName,
          contentType: submission.contentType,
          byteSize: submission.byteSize,
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
      const attachment = (await res.json()) as BackendSagaAttachment;
      return NextResponse.json({ id: attachment.id, status: 'RECORDED' });
    }

    // ── Existing generic behavior (saga start, execute step, compensate, etc.) - unchanged ──

    // Inject userId and tenantId into the request body for saga creation.
    // The backend SagaContext expects userId and projectId inside `data`,
    // and tenantId at the top level of StartSagaRequest.
    const { userId, tenantId } = await extractSessionUser();

    // Always override tenantId (the form passes tenantId="" which is falsy)
    if (body && typeof body === 'object') {
      (body as Record<string, unknown>).tenantId = tenantId;
      // Inject userId into data if not already present
      if (userId) {
        const data = (body.data as Record<string, unknown> | undefined) ?? {};
        if (!data.userId) {
          data.userId = userId;
          body.data = data;
        }
      }
    }

    const headers = getBackendHeaders(request);
    // Inject X-Tenant-Id header so the workflow service's TenantRequestFilter sets TenantContext
    (headers as Record<string, string>)['X-Tenant-Id'] = tenantId;

    const res = await fetch(`${WORKFLOW_SERVICE_URL}${sagaPath}`, {
      method: 'POST',
      headers,
      body: JSON.stringify(body),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}

export async function PUT(request: NextRequest) {
  try {
    const sagaPath = proxyPath(request);
    const body = await request.json();
    const headers = getBackendHeaders(request);

    const res = await fetch(`${WORKFLOW_SERVICE_URL}${sagaPath}`, {
      method: 'PUT',
      headers,
      body: JSON.stringify(body),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}

export async function PATCH(request: NextRequest) {
  try {
    const sagaPath = proxyPath(request);
    const body = await request.json();
    const headers = getBackendHeaders(request);

    const res = await fetch(`${WORKFLOW_SERVICE_URL}${sagaPath}`, {
      method: 'PATCH',
      headers,
      body: JSON.stringify(body),
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}

export async function DELETE(request: NextRequest) {
  try {
    const sagaPath = proxyPath(request);
    const headers = getBackendHeaders(request);

    const res = await fetch(`${WORKFLOW_SERVICE_URL}${sagaPath}`, {
      method: 'DELETE',
      headers,
      cache: 'no-store',
    });

    if (!res.ok) {
      const data = await res.json().catch(() => undefined);
      return NextResponse.json(
        { message: data?.message ?? res.statusText },
        { status: res.status },
      );
    }

    const attachmentsMatch = matchAttachmentsPath(sagaPath);
    if (attachmentsMatch && attachmentsMatch.attachmentId) {
      // Unlike every other saga DELETE, this one's backend response body carries {objectKey} so
      // we can also clean up storage (best-effort - the metadata row is already gone, which is
      // what makes the attachment disappear from the UI, so a failure here just leaves an
      // orphaned object rather than a user-visible correctness problem).
      const { objectKey } = (await res.json()) as { objectKey: string };
      try {
        const deleteRes = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/media/delete`, {
          method: 'POST',
          headers: getDocumentServiceHeaders(request),
          body: JSON.stringify({ objectKey }),
          signal: AbortSignal.timeout(15000),
        });
        if (!deleteRes.ok) {
          const errBody = await deleteRes.text().catch(() => '');
          console.error(
            `[sagas] object delete failed (${deleteRes.status}) for ${objectKey}: ${errBody}`,
          );
        }
      } catch (error) {
        console.error(`[sagas] object delete request failed for ${objectKey}:`, error);
      }
      return NextResponse.json({ success: true });
    }

    return new NextResponse(null, { status: res.status });
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}
