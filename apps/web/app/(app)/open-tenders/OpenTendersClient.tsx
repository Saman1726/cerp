'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { Bell, RefreshCw, Wifi, WifiOff, BellOff } from 'lucide-react';
import { NotificationProvider, OpenTenderSubmitForm } from '@cerp/core-web/notifications';
import type { TenderEntry } from '@cerp/core-web/notifications';

interface OpenTender {
  sagaId: string;
  tenderId: string;
  projectId?: string;
  bidDeadline?: string;
  status: string;
  description?: string;
  sagaName?: string;
  createdAt?: string;
}

function OpenTendersContent() {
  const [tenders, setTenders] = useState<OpenTender[]>([]);
  const [loading, setLoading] = useState(true);
  const [subscribed, setSubscribed] = useState(false);
  const [subscribeLoading, setSubscribeLoading] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [liveConnected, setLiveConnected] = useState(false);
  const [newCount, setNewCount] = useState(0);
  const eventSourceRef = useRef<EventSource | null>(null);

  const fetchTenders = useCallback(async () => {
    try {
      const res = await fetch('/api/open-tenders');
      if (res.ok) {
        const data = await res.json() as { tenders?: OpenTender[] };
        setTenders(data.tenders ?? []);
      }
    } catch {
      // ignore fetch errors
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchSubscriptionStatus = useCallback(async () => {
    try {
      const res = await fetch('/api/open-tenders/subscription');
      if (res.ok) {
        const data = await res.json() as { subscribed?: boolean };
        setSubscribed(data.subscribed ?? false);
      }
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    fetchTenders();
    fetchSubscriptionStatus();
  }, [fetchTenders, fetchSubscriptionStatus]);

  // SSE: only subscribed vendors get notifications, but we use the stream
  // to refresh the channel in real-time when new tenders arrive.
  const connectSSE = useCallback(() => {
    if (!subscribed) return;
    if (eventSourceRef.current) eventSourceRef.current.close();

    const es = new EventSource('/api/notifications/stream');
    eventSourceRef.current = es;

    es.addEventListener('connected', () => setLiveConnected(true));

    es.addEventListener('open_tender', async () => {
      setNewCount((c) => c + 1);
      await fetchTenders();
    });

    es.onerror = () => {
      setLiveConnected(false);
      es.close();
      setTimeout(connectSSE, 5_000);
    };
  }, [subscribed, fetchTenders]);

  useEffect(() => {
    connectSSE();
    return () => {
      eventSourceRef.current?.close();
      setLiveConnected(false);
    };
  }, [connectSSE]);

  const handleRefresh = async () => {
    setNewCount(0);
    setLoading(true);
    await fetchTenders();
  };

  const handleSubscribeToggle = async () => {
    setSubscribeLoading(true);
    try {
      const method = subscribed ? 'DELETE' : 'POST';
      const res = await fetch('/api/open-tenders/subscription', { method });
      if (res.ok) {
        const data = await res.json() as { subscribed?: boolean };
        setSubscribed(data.subscribed ?? !subscribed);
      }
    } catch {
      // ignore
    } finally {
      setSubscribeLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">Open Tender Channel</h1>
          <p className="text-sm text-gray-500">
            {tenders.length} open tender{tenders.length !== 1 ? 's' : ''} available
          </p>
        </div>
        <div className="flex items-center gap-3">
          {subscribed && (
            <span
              className={`flex items-center gap-1.5 text-xs font-medium ${
                liveConnected ? 'text-green-600' : 'text-gray-400'
              }`}
            >
              {liveConnected ? <Wifi className="h-3.5 w-3.5" /> : <WifiOff className="h-3.5 w-3.5" />}
              {liveConnected ? 'Live' : 'Connecting…'}
            </span>
          )}

          <button
            onClick={handleSubscribeToggle}
            disabled={subscribeLoading}
            className={`flex items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              subscribed
                ? 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
                : 'bg-blue-600 text-white hover:bg-blue-700'
            }`}
          >
            {subscribed ? (
              <>
                <BellOff className="h-3.5 w-3.5" />
                {subscribeLoading ? 'Unsubscribing…' : 'Unsubscribe'}
              </>
            ) : (
              <>
                <Bell className="h-3.5 w-3.5" />
                {subscribeLoading ? 'Subscribing…' : 'Subscribe for notifications'}
              </>
            )}
          </button>

          <button
            onClick={handleRefresh}
            className="flex items-center gap-2 rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh
            {newCount > 0 && (
              <span className="ml-1 inline-flex h-5 w-5 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">
                {newCount > 9 ? '9+' : newCount}
              </span>
            )}
          </button>
        </div>
      </div>

      {/* Subscription info */}
      <div
        className={`rounded-md border p-3 text-sm ${
          subscribed
            ? 'border-green-200 bg-green-50 text-green-800'
            : 'border-gray-200 bg-gray-50 text-gray-700'
        }`}
      >
        {subscribed ? (
          <span>
            <span className="font-semibold">Subscribed.</span> You will receive a notification
            whenever a new open tender is published in this channel.
          </span>
        ) : (
          <span>
            You can browse and bid on all open tenders below.{' '}
            <span className="font-medium">Subscribe</span> to also receive push notifications when
            new tenders are published.
          </span>
        )}
        <span className="ml-2 text-gray-500">
          Participating in an open tender costs <strong>5 bids</strong>.
        </span>
      </div>

      {/* Tender list */}
      <div className="space-y-4">
        {loading && <div className="p-8 text-center text-gray-400">Loading open tenders…</div>}

        {!loading && tenders.length === 0 && (
          <div className="flex flex-col items-center justify-center rounded-lg border border-dashed p-16 text-center">
            <Bell className="mb-4 h-12 w-12 text-gray-300" />
            <p className="text-gray-500">No open tenders available right now</p>
            <p className="mt-1 text-xs text-gray-400">
              {subscribed
                ? 'You will be notified when new tenders are published'
                : 'Subscribe to get notified when new tenders are published'}
            </p>
          </div>
        )}

        {tenders.map((tender) => {
          const isExpanded = expandedId === tender.sagaId;

          const tenderEntry: TenderEntry = {
            tenderId: tender.tenderId,
            sagaId: tender.sagaId,
            projectId: tender.projectId,
            bidDeadline: tender.bidDeadline,
          };

          return (
            <div
              key={tender.sagaId}
              className="overflow-hidden rounded-lg border border-amber-200 bg-amber-50/20 transition-colors"
            >
              <div className="p-4">
                <div className="flex items-start gap-4">
                  <div className="mt-1 h-2.5 w-2.5 flex-shrink-0 rounded-full bg-amber-500" />

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h3 className="font-medium text-gray-900">
                        {tender.sagaName ?? `Open Tender ${tender.tenderId?.slice(0, 8) ?? ''}…`}
                      </h3>
                      <span className="inline-flex rounded bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-700">
                        Open Tender
                      </span>
                    </div>

                    {tender.description && (
                      <p className="mt-1 text-sm text-gray-600">{tender.description}</p>
                    )}

                    <dl className="mt-2 flex flex-wrap gap-x-6 gap-y-1 text-xs text-gray-500">
                      {tender.bidDeadline && (
                        <div className="flex gap-1">
                          <dt className="font-medium">Deadline:</dt>
                          <dd>{new Date(tender.bidDeadline).toLocaleDateString()}</dd>
                        </div>
                      )}
                      {tender.createdAt && (
                        <div className="flex gap-1">
                          <dt className="font-medium">Published:</dt>
                          <dd>{new Date(tender.createdAt).toLocaleString()}</dd>
                        </div>
                      )}
                    </dl>

                    <div className="mt-3">
                      <button
                        onClick={() => setExpandedId(isExpanded ? null : tender.sagaId)}
                        className="rounded-md bg-amber-500 px-4 py-1.5 text-xs font-medium text-white hover:bg-amber-600"
                      >
                        {isExpanded ? 'Close' : 'Submit Offer (−5 bids)'}
                      </button>
                    </div>

                    {isExpanded && (
                      <div className="mt-4 rounded-md border border-gray-200 bg-white p-4">
                        <OpenTenderSubmitForm
                          tenderData={tenderEntry}
                          onSuccess={() => {
                            setExpandedId(null);
                            fetchTenders();
                          }}
                          onCancel={() => setExpandedId(null)}
                        />
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function OpenTendersClient({ tenantId, userId }: { tenantId: string; userId: string }) {
  // NotificationProvider is required because OpenTenderSubmitForm uses useNotifications()
  // internally to mark subscription-driven notifications ACTION_TAKEN when present.
  return (
    <NotificationProvider tenantId={tenantId} userId={userId}>
      <OpenTendersContent />
    </NotificationProvider>
  );
}
