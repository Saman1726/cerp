'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { SagaStatusBadge } from './SagaStatusBadge';
import { SagaRowActions } from './SagaRowActions';
import { useSagaAttachmentSync } from './saga-attachments';
import { workflowApi } from './api';
import type { Saga, SagaEvent, SagaDetailTabId } from './types';
import { SAGA_DETAIL_TABS, getSagaProjectId } from './types';

interface SagaDetailPageProps {
  sagaId: string;
}

export function SagaDetailPage({ sagaId }: SagaDetailPageProps) {
  const [saga, setSaga] = useState<Saga | null>(null);
  const [events, setEvents] = useState<SagaEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<SagaDetailTabId>('overview');
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  // Owned once here (not per row - there's only one saga on this page, but this matches the
  // same single-listener convention used everywhere else this queue is drained from): the
  // upload queue is one shared IndexedDB store, so only one background-sync listener should own
  // draining it per page.
  const { syncNow: syncAttachmentsNow, isSyncing: attachmentsSyncing } = useSagaAttachmentSync();

  const fetchSaga = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [sagaData, eventsData] = await Promise.all([
        workflowApi.getSaga(sagaId),
        workflowApi.getSagaEvents(sagaId),
      ]);
      setSaga(sagaData);
      setEvents(eventsData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load saga details');
    } finally {
      setLoading(false);
    }
  }, [sagaId]);

  // Refetches whenever a saga-attachment sync round finishes (isSyncing true -> false), not just
  // right after enqueueing -- this also covers uploads that only complete later, e.g. once the
  // online-event listener fires after being offline, so the attachment icon/count update on
  // their own without the user needing to reload the page.
  const wasSyncingAttachmentsRef = useRef(false);
  useEffect(() => {
    if (wasSyncingAttachmentsRef.current && !attachmentsSyncing) {
      fetchSaga();
    }
    wasSyncingAttachmentsRef.current = attachmentsSyncing;
  }, [attachmentsSyncing, fetchSaga]);

  useEffect(() => {
    let mounted = true;
    void (async () => {
      setLoading(true);
      setError(null);
      try {
        const [sagaData, eventsData] = await Promise.all([
          workflowApi.getSaga(sagaId),
          workflowApi.getSagaEvents(sagaId),
        ]);
        if (!mounted) return;
        setSaga(sagaData);
        setEvents(eventsData);
      } catch (err) {
        if (mounted) setError(err instanceof Error ? err.message : 'Failed to load saga details');
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [sagaId]);

  const handleAction = async (
    action: 'compensate' | 'resume' | 'escalate',
    stepCode?: string,
    reason?: string,
  ) => {
    setActionLoading(action);
    try {
      await workflowApi.performAction(sagaId, {
        sagaId,
        action,
        stepCode,
        reason,
      });
      await fetchSaga();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to perform ${action}`);
    } finally {
      setActionLoading(null);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-muted-foreground">Loading saga details...</p>
      </div>
    );
  }

  if (error && !saga) {
    return (
      <div className="space-y-4">
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
        <Button variant="outline" onClick={() => window.history.back()}>
          Go Back
        </Button>
      </div>
    );
  }

  if (!saga) {
    return (
      <div className="space-y-4">
        <p className="text-muted-foreground">Saga not found</p>
        <Button variant="outline" onClick={() => window.history.back()}>
          Go Back
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="space-y-1">
          <Button variant="ghost" size="sm" onClick={() => window.history.back()} className="mb-2">
            Back to Sagas
          </Button>
          <h1 className="text-2xl font-bold">Saga Details</h1>
          <p className="text-muted-foreground font-mono text-sm">{saga.sagaId}</p>
        </div>
        <div className="flex items-center gap-2">
          <SagaStatusBadge status={saga.status} />
          <SagaRowActions
            saga={saga}
            projectId={getSagaProjectId(saga)}
            onSagaUpdated={fetchSaga}
            onAttachmentQueued={() => void syncAttachmentsNow()}
          />
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="flex gap-2 border-b">
        {SAGA_DETAIL_TABS.map((tab) => (
          <button
            key={tab.id}
            className={`px-4 py-2 text-sm font-medium ${
              activeTab === tab.id
                ? 'border-b-2 border-primary text-primary'
                : 'text-muted-foreground hover:text-foreground'
            }`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Overview Tab */}
      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Saga Information</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <span className="text-sm font-medium text-muted-foreground">Type</span>
                <p className="text-sm">{saga.sagaTypeName || saga.sagaType}</p>
              </div>
              <div>
                <span className="text-sm font-medium text-muted-foreground">Status</span>
                <div className="mt-1">
                  <SagaStatusBadge status={saga.status} />
                </div>
              </div>
              {saga.aggregateId && (
                <div>
                  <span className="text-sm font-medium text-muted-foreground">Aggregate ID</span>
                  <p className="text-sm font-mono">{saga.aggregateId}</p>
                </div>
              )}
              <div>
                <span className="text-sm font-medium text-muted-foreground">Tenant</span>
                <p className="text-sm font-mono">{saga.tenantId}</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Timing</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div>
                <span className="text-sm font-medium text-muted-foreground">Created</span>
                <p className="text-sm">{new Date(saga.createdAt).toLocaleString()}</p>
              </div>
              <div>
                <span className="text-sm font-medium text-muted-foreground">Updated</span>
                <p className="text-sm">{new Date(saga.updatedAt).toLocaleString()}</p>
              </div>
              <div>
                <span className="text-sm font-medium text-muted-foreground">Current Step</span>
                <p className="text-sm">{saga.currentStep || 'N/A'}</p>
              </div>
              <div>
                <span className="text-sm font-medium text-muted-foreground">Total Steps</span>
                <p className="text-sm">{saga.steps.length}</p>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Steps Tab */}
      {activeTab === 'steps' && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Execution Steps</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {saga.steps.map((step, index) => (
                <div key={index} className="border rounded-lg p-4 space-y-2">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-muted-foreground">
                        #{step.order}
                      </span>
                      <span className="font-medium">{step.stepName}</span>
                    </div>
                    <SagaStatusBadge status={step.status} variant="step" />
                  </div>
                  <div className="text-sm text-muted-foreground font-mono">{step.stepCode}</div>
                  {step.errorMessage && (
                    <div className="text-sm text-red-600 bg-red-50 p-2 rounded">
                      Error: {step.errorMessage}
                    </div>
                  )}
                  {step.compensationErrorMessage && (
                    <div className="text-sm text-yellow-600 bg-yellow-50 p-2 rounded">
                      Compensation Error: {step.compensationErrorMessage}
                    </div>
                  )}
                  <div className="flex gap-4 text-xs text-muted-foreground">
                    {step.startedAt && (
                      <span>Started: {new Date(step.startedAt).toLocaleString()}</span>
                    )}
                    {step.completedAt && (
                      <span>Completed: {new Date(step.completedAt).toLocaleString()}</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Events Tab */}
      {activeTab === 'events' && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Event Log</CardTitle>
          </CardHeader>
          <CardContent>
            {events.length === 0 ? (
              <p className="text-muted-foreground text-center py-8">No events recorded</p>
            ) : (
              <div className="space-y-2">
                {events.map((event) => (
                  <div key={event.eventId} className="border rounded p-3 text-sm">
                    <div className="flex items-center justify-between">
                      <span className="font-medium">{event.eventType}</span>
                      <span className="text-muted-foreground text-xs">
                        {new Date(event.timestamp).toLocaleString()}
                      </span>
                    </div>
                    <div className="text-xs text-muted-foreground font-mono mt-1">
                      {event.eventId}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Compensation Tab */}
      {activeTab === 'compensation' && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Saga Actions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {saga.status === 'FAILED' && (
              <div className="space-y-2">
                <p className="text-sm">Compensate this saga to rollback completed steps:</p>
                <Button
                  variant="destructive"
                  onClick={() => handleAction('compensate')}
                  disabled={actionLoading !== null}
                >
                  {actionLoading === 'compensate' ? 'Compensating...' : 'Compensate Saga'}
                </Button>
              </div>
            )}

            {saga.status === 'REQUIRES_MANUAL_INTERVENTION' && (
              <div className="space-y-2">
                <p className="text-sm">Resolve the issue and resume the saga:</p>
                <Button onClick={() => handleAction('resume')} disabled={actionLoading !== null}>
                  {actionLoading === 'resume' ? 'Resuming...' : 'Resume Saga'}
                </Button>
              </div>
            )}

            {saga.status === 'FAILED' && (
              <div className="space-y-2">
                <p className="text-sm">Escalate this saga for manual review:</p>
                <Button
                  variant="outline"
                  onClick={() => handleAction('escalate')}
                  disabled={actionLoading !== null}
                >
                  {actionLoading === 'escalate' ? 'Escalating...' : 'Escalate'}
                </Button>
              </div>
            )}

            {saga.errorDetails && Object.keys(saga.errorDetails).length > 0 && (
              <div>
                <p className="text-sm font-medium mb-2">Error Details:</p>
                <pre className="bg-muted p-4 rounded text-xs overflow-auto">
                  {JSON.stringify(saga.errorDetails, null, 2)}
                </pre>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
