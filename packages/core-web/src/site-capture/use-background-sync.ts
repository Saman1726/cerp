'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { listQueuedCaptures } from './offline-queue';
import { processQueue, type SyncEndpoints } from './sync-engine';

// Some mobile browsers/PWAs don't reliably fire `online` (e.g. after the OS
// silently swaps wifi for cellular mid-session), so this polls as a backstop
// whenever the browser currently claims to be online.
const POLL_INTERVAL_MS = 20_000;

export interface UseBackgroundSyncResult {
  pendingCount: number;
  isSyncing: boolean;
  lastError: string | null;
  /** Manual trigger, e.g. for a "retry now" button on failed captures. */
  syncNow: () => Promise<void>;
}

/**
 * Drains the offline capture queue whenever the network comes back, and
 * exposes queue state for UI feedback (pending badge, sync spinner, errors).
 */
export function useBackgroundSync(endpoints?: SyncEndpoints): UseBackgroundSyncResult {
  const presignUrl = endpoints?.presignUrl;
  const submitUrl = endpoints?.submitUrl;

  const [pendingCount, setPendingCount] = useState(0);
  const [isSyncing, setIsSyncing] = useState(false);
  const [lastError, setLastError] = useState<string | null>(null);
  // Guards against the `online` event and the poll interval both firing a
  // drain at once; plain state wouldn't be readable synchronously enough.
  const isSyncingRef = useRef(false);

  const refreshPendingCount = useCallback(async () => {
    const queued = await listQueuedCaptures();
    setPendingCount(queued.length);
  }, []);

  const syncNow = useCallback(async () => {
    if (isSyncingRef.current) return;
    if (typeof navigator !== 'undefined' && !navigator.onLine) return;

    isSyncingRef.current = true;
    setIsSyncing(true);
    try {
      const result = await processQueue({ presignUrl, submitUrl });
      setLastError(result.failed.length > 0 ? `${result.failed.length} capture(s) still pending` : null);
    } catch (error) {
      setLastError(error instanceof Error ? error.message : 'Sync failed');
    } finally {
      isSyncingRef.current = false;
      setIsSyncing(false);
      await refreshPendingCount();
    }
  }, [presignUrl, submitUrl, refreshPendingCount]);

  useEffect(() => {
    let cancelled = false;

    // Inlined rather than calling refreshPendingCount() directly: the queue
    // can hold captures from a previous, now-closed session, and this must
    // show that count even while offline - not just after a sync attempt.
    listQueuedCaptures().then((queued) => {
      if (!cancelled) setPendingCount(queued.length);
    });

    const attemptSync = () => {
      if (navigator.onLine) void syncNow();
    };

    window.addEventListener('online', attemptSync);
    const interval = window.setInterval(attemptSync, POLL_INTERVAL_MS);
    // Deferred a tick rather than called inline, so the mount-time attempt
    // is scheduled work (like the listener/interval above) instead of a
    // synchronous state cascade during the effect's own commit.
    const initialAttempt = window.setTimeout(attemptSync, 0);

    return () => {
      cancelled = true;
      window.removeEventListener('online', attemptSync);
      window.clearInterval(interval);
      window.clearTimeout(initialAttempt);
    };
  }, [syncNow]);

  return { pendingCount, isSyncing, lastError, syncNow };
}
