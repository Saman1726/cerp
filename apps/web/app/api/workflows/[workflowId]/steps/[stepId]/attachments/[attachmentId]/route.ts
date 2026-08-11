import { NextRequest, NextResponse } from 'next/server';
import { DOCUMENT_SERVICE_URL, WORKFLOW_SERVICE_URL } from '@cerp/core-web/lib/env';
import { getAuthHeaders } from '../../../../../utils';
import { getDocumentServiceHeaders } from '../../../../../../uploads/utils';

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ workflowId: string; stepId: string; attachmentId: string }> },
) {
  try {
    const { workflowId, stepId, attachmentId } = await params;
    const url = `${WORKFLOW_SERVICE_URL}/api/v1/workflows/${workflowId}/steps/${stepId}/attachments/${attachmentId}`;

    const res = await fetch(url, {
      method: 'DELETE',
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

    const { objectKey } = (await res.json()) as { objectKey: string };

    // Best-effort: the metadata row is already gone (that's what makes the attachment
    // disappear from the UI), so a failure here just leaves an orphaned object in storage --
    // not a user-visible correctness problem, so it doesn't fail the request. Still logged
    // (both branches) so an orphan is at least visible instead of silently accumulating.
    try {
      const deleteRes = await fetch(`${DOCUMENT_SERVICE_URL}/api/v1/media/delete`, {
        method: 'POST',
        headers: getDocumentServiceHeaders(request),
        body: JSON.stringify({ objectKey }),
        signal: AbortSignal.timeout(15000),
      });
      if (!deleteRes.ok) {
        const body = await deleteRes.text().catch(() => '');
        console.error(
          `[attachments] object delete failed (${deleteRes.status}) for ${objectKey}: ${body}`,
        );
      }
    } catch (error) {
      console.error(`[attachments] object delete request failed for ${objectKey}:`, error);
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json(
      { message: error instanceof Error ? error.message : 'Unknown error' },
      { status: 500 },
    );
  }
}
