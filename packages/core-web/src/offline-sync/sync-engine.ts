import { MAX_SYNC_ATTEMPTS, type QueuedItemEnvelope } from './queue';

export interface SyncableQueue<TItem extends QueuedItemEnvelope> {
  list(): Promise<TItem[]>;
  remove(clientId: string): Promise<void>;
  markFailed(clientId: string, error: string): Promise<void>;
}

export interface SyncResult {
  synced: string[];
  failed: string[];
  remaining: number;
}

/**
 * Drains a queue one item at a time, oldest first, so a weak site connection is never asked to
 * push multiple multi-megabyte uploads concurrently. Framework-agnostic (only the queue + a
 * caller-supplied `syncOne`) so it can run from a plain window context or, later, a service
 * worker.
 */
export async function processQueue<TItem extends QueuedItemEnvelope>(
  queue: SyncableQueue<TItem>,
  syncOne: (item: TItem) => Promise<void>,
): Promise<SyncResult> {
  const queued = await queue.list();
  const synced: string[] = [];
  const failed: string[] = [];

  for (const item of queued) {
    // Connection dropped again mid-drain - stop rather than burn through
    // retry attempts on items that never got a real chance to send.
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      break;
    }

    if (item.attempts >= MAX_SYNC_ATTEMPTS) {
      failed.push(item.clientId);
      continue;
    }

    try {
      await syncOne(item);
      await queue.remove(item.clientId);
      synced.push(item.clientId);
    } catch (error) {
      await queue.markFailed(
        item.clientId,
        error instanceof Error ? error.message : 'Unknown sync error',
      );
      failed.push(item.clientId);
    }
  }

  return { synced, failed, remaining: failed.length };
}
