import { NextRequest } from 'next/server';
import { extractSessionUser } from '../../workflows/utils';

const NOTIFICATION_SERVICE_URL = process.env.NOTIFICATION_SERVICE_URL || 'http://localhost:8091';

/**
 * SSE endpoint that streams new open_tender_* notifications to a subscribed vendor.
 * Clients connect via EventSource and receive events whenever a new open tender appears.
 */
export async function GET(req: NextRequest) {
  const { userId, tenantId } = await extractSessionUser();
  const encoder = new TextEncoder();
  const seenIds = new Set<string>();

  const stream = new ReadableStream({
    async start(controller) {
      const enqueue = (event: string, data: unknown) => {
        try {
          controller.enqueue(encoder.encode(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`));
        } catch {
          // Connection closed
        }
      };

      const heartbeat = () => {
        try {
          controller.enqueue(encoder.encode(': ping\n\n'));
        } catch {
          // Connection closed
        }
      };

      const poll = async () => {
        try {
          const res = await fetch(
            `${NOTIFICATION_SERVICE_URL}/api/notifications/me?size=50&status=PENDING`,
            {
              headers: {
                'X-Tenant-Id': tenantId,
                'X-User-Id': userId ?? '',
              },
              cache: 'no-store',
            }
          );
          if (!res.ok) return;

          const json = (await res.json()) as { content?: unknown[] };
          const notifications = (json.content ?? []) as Array<Record<string, unknown>>;

          for (const n of notifications) {
            const code = n['code'] as string | undefined;
            const id = n['id'] as string | undefined;
            if (
              id &&
              code?.startsWith('open_tender_') &&
              !code.startsWith('open_tender_confirm_') &&
              !seenIds.has(id)
            ) {
              seenIds.add(id);
              enqueue('open_tender', { notification: n });
            }
          }
        } catch {
          // Notification service temporarily unavailable — retry on next poll
        }
      };

      // Send initial snapshot
      await poll();
      enqueue('connected', { message: 'Subscribed to open tender channel' });

      // Poll every 10 s for new tenders
      const pollInterval = setInterval(poll, 10_000);
      // Heartbeat every 25 s to prevent proxy timeouts
      const heartbeatInterval = setInterval(heartbeat, 25_000);

      req.signal.addEventListener('abort', () => {
        clearInterval(pollInterval);
        clearInterval(heartbeatInterval);
        try {
          controller.close();
        } catch {
          // Already closed
        }
      });
    },
  });

  return new Response(stream, {
    headers: {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      Connection: 'keep-alive',
      'X-Accel-Buffering': 'no',
    },
  });
}
