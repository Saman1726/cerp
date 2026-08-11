'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

// Some mobile browsers/PWAs don't reliably fire `online` (e.g. after the OS
// silently swaps wifi for cellular mid-session), so this polls as a backstop
// whenever the browser currently claims to be online.
const POLL_INTERVAL_MS = 20_000;

export interface UseOfflineSyncConfig {
  listPendingCount: () => Promise<number>;
  sync: () => Promise<{ failed: string[] }>;
}

export interface UseOfflineSyncResult {
  pendingCount: number;
  isSyncing: boolean;
  lastError: string | null;
  /** Manual trigger, e.g. for a "retry now" button on failed items. */
  syncNow: () => Promise<void>;
}

/**
 * Drains an offline upload queue whenever the network comes back, and exposes queue state for UI
 * feedback (pending badge, sync spinner, errors). Domain-agnostic: callers supply how to count
 * pending items and how to run one sync pass.
 */
export function useOfflineSync(config: UseOfflineSyncConfig): UseOfflineSyncResult {
  const [pendingCount, setPendingCount] = useState(0);
  const [isSyncing, setIsSyncing] = useState(false);
  const [lastError, setLastError] = useState<string | null>(null);
  // Guards against the `online` event and the poll interval both firing a
  // drain at once; plain state wouldn't be readable synchronously enough.
  const isSyncingRef = useRef(false);
  // Callers commonly pass a fresh {listPendingCount, sync} object every render (it closes over
  // feature-specific endpoints). Stashing the latest in a ref -- read inside callbacks rather
  // than captured by them -- keeps syncNow's identity stable, so the mount effect below doesn't
  // re-subscribe its listeners/interval every render. Assigned in an effect (not during render)
  // per the rules of React; it still lands before any user-triggered callback can read it.
  const configRef = useRef(config);
  useEffect(() => {
    configRef.current = config;
  });

  const refreshPendingCount = useCallback(async () => {
    setPendingCount(await configRef.current.listPendingCount());
  }, []);

  const syncNow = useCallback(async () => {
    if (isSyncingRef.current) return;
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      // Still refresh: the caller (e.g. a form, right after enqueueing a new
      // item) needs the count to reflect reality even though there's no
      // connection to actually drain the queue over.
      await refreshPendingCount();
      return;
    }

    isSyncingRef.current = true;
    setIsSyncing(true);
    try {
      const result = await configRef.current.sync();
      setLastError(
        result.failed.length > 0 ? `${result.failed.length} item(s) still pending` : null,
      );
    } catch (error) {
      setLastError(error instanceof Error ? error.message : 'Sync failed');
    } finally {
      isSyncingRef.current = false;
      setIsSyncing(false);
      await refreshPendingCount();
    }
  }, [refreshPendingCount]);

  useEffect(() => {
    let cancelled = false;

    // Inlined rather than calling refreshPendingCount() directly: the queue
    // can hold items from a previous, now-closed session, and this must show
    // that count even while offline - not just after a sync attempt.
    configRef.current.listPendingCount().then((count) => {
      if (!cancelled) setPendingCount(count);
    });

    const attemptSync = () => {
      if (navigator.onLine) void syncNow();
    };

    window.addEventListener('online', attemptSync);
    const interval = window.setInterval(attemptSync, POLL_INTERVAL_MS);
    // Deferred a tick rather than called inline, so the mount-time attempt is
    // scheduled work (like the listener/interval above) instead of a
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
