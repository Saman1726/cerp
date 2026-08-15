'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { CheckCircle, AlertCircle, Clock, FileText, Truck, Users } from 'lucide-react';
import { SagaStatusBadge } from './SagaStatusBadge';
import { SagaRowActions } from './SagaRowActions';
import { useSagaAttachmentSync } from './saga-attachments';
import { workflowApi } from './api';
import type { Saga, SagaEvent, SagaDetailTabId, ExecuteSagaStepRequest } from './types';
import { SAGA_DETAIL_TABS, getSagaProjectId, PROCUREMENT_SAGA_STEPS } from './types';
import { ProcurementActions } from './ProcurementActions';

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

      {/* Procurement & Delivery Details */}
      {activeTab === 'overview' && saga.data && (
        <div className="space-y-4">
          {(saga.data.purchaseOrderNumber || saga.data.poNumber || saga.data.vendorId || saga.data.vendorName || saga.data.materialName || saga.data.quantity != null ||
           (saga.sagaType?.toLowerCase().includes('procurement') && saga.data.linkedDeliveries)) && (
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-base flex items-center gap-2"><FileText className="h-5 w-5" />Procurement Details</CardTitle></CardHeader>
              <CardContent>
                <dl className="grid grid-cols-2 md:grid-cols-3 gap-4 text-sm">
                  {(saga.data.purchaseOrderNumber || saga.data.poNumber) && (<div><dt className="text-xs text-muted-foreground">Purchase Order</dt><dd className="font-mono font-medium">{String(saga.data.purchaseOrderNumber ?? saga.data.poNumber)}</dd></div>)}
                  {saga.data.vendorName && (<div><dt className="text-xs text-muted-foreground">Vendor</dt><dd>{saga.data.vendorName}</dd></div>)}
                  {saga.data.vendorId && !saga.data.vendorName && (<div><dt className="text-xs text-muted-foreground">Vendor ID</dt><dd className="font-mono">{saga.data.vendorId}</dd></div>)}
                  {saga.data.projectId && (<div><dt className="text-xs text-muted-foreground">Project</dt><dd className="font-mono">{saga.data.projectTitle || saga.data.projectId}</dd></div>)}
                  {(saga.data.materialName || saga.data.materialId) && (<div><dt className="text-xs text-muted-foreground">Material</dt><dd>{saga.data.materialName || saga.data.materialId}</dd></div>)}
                  {saga.data.description && (<div className="col-span-2"><dt className="text-xs text-muted-foreground">Description</dt><dd>{saga.data.description}</dd></div>)}
                  {saga.data.quantity != null && (<div><dt className="text-xs text-muted-foreground">Quantity</dt><dd className="font-medium">{String(saga.data.quantity)}</dd></div>)}
                  {saga.data.deliveredQuantity != null && (<div><dt className="text-xs text-muted-foreground">Delivered</dt><dd>{String(saga.data.deliveredQuantity)}</dd></div>)}
                  {saga.data.deliveryPercentage != null && (<div><dt className="text-xs text-muted-foreground">Delivery %</dt><dd>{String(saga.data.deliveryPercentage)}%</dd></div>)}
                </dl>
              </CardContent>
            </Card>
          )}
          {saga.sagaType?.toLowerCase().includes('delivery') && !saga.sagaType?.toLowerCase().includes('procurement') && (saga.data.scheduledDate || saga.data.expectedArrivalTime) && (
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-base flex items-center gap-2"><Truck className="h-5 w-5" />Delivery Schedule</CardTitle></CardHeader>
              <CardContent className="space-y-3">
                {saga.data.scheduledDate && (<div className="flex justify-between text-sm"><span className="text-muted-foreground">Scheduled</span><span>{new Date(saga.data.scheduledDate).toLocaleString()}</span></div>)}
                {saga.data.expectedArrivalTime && (<div className="flex justify-between text-sm"><span className="text-muted-foreground">Expected Arrival</span><span>{new Date(saga.data.expectedArrivalTime).toLocaleString()}</span></div>)}
                {saga.data.deliveryPercentage != null && (<div><div className="flex justify-between text-xs mb-1"><span className="text-muted-foreground">Progress</span><span className="font-medium">{saga.data.deliveryPercentage}%</span></div><div className="h-2 bg-muted rounded-full overflow-hidden"><div className="h-full bg-blue-600 rounded-full" style={{ width: `${Math.min(Number(saga.data.deliveryPercentage), 100)}%` }} /></div></div>)}
              </CardContent>
            </Card>
          )}
          {saga.sagaType?.toLowerCase().includes('procurement') && Array.isArray(saga.data.linkedDeliveries) && saga.data.linkedDeliveries.length > 0 && (
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-base flex items-center gap-2"><Truck className="h-5 w-5" />Linked Deliveries ({(saga.data.linkedDeliveries as []).length})</CardTitle></CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {(saga.data.linkedDeliveries as []).map((dl: Record<string, unknown>, i: number) => (
                    <div key={i} className="flex items-center justify-between p-2.5 border rounded-lg text-xs hover:bg-muted/40">
                      <div className="flex items-center gap-3 flex-1 min-w-0">
                        <button className="font-medium text-blue-700 hover:underline truncate" onClick={() => { const dId = dl.deliverySagaId ?? dl.sagaId ?? dl.id; if (dId) { window.location.href = `/workflows/saga/${dId}`; } }}>
                          Delivery #{String(dl.deliverySagaId ?? dl.sagaId ?? dl.id ?? '???').slice(0, 8)}
                        </button>
                        <span className="text-muted-foreground">{String(dl.scheduledDate ?? dl.date ?? '—')}</span>
                        <span>Qty: <strong>{String(dl.quantity ?? '—')}</strong></span>
                      </div>
                      <div className="flex items-center gap-2 shrink-0 ml-2">
                        <span className="text-muted-foreground">{String(dl.deliveryPercentage ?? 0)}%</span>
                        <span className="capitalize">{String(dl.deliveryStatus ?? dl.status ?? 'SCHEDULED')}</span>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
          {saga.sagaType?.toLowerCase().includes('delivery') && saga.data.procurementSagaId && (
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-base flex items-center gap-2"><FileText className="h-5 w-5" />Parent Procurement</CardTitle></CardHeader>
              <CardContent>
                <button className="text-sm font-medium text-blue-700 hover:underline" onClick={() => { window.location.href = `/workflows/saga/${saga.data.procurementSagaId}`; }}>
                  PO #{String(saga.data.procurementSagaId).slice(0, 8)}
                </button>
              </CardContent>
            </Card>
          )}
          {saga.steps.length > 0 && (
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-base">Workflow Steps</CardTitle></CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {saga.steps.map((step, i) => {
                    const stepStatus = (step.status ?? 'PENDING').toString().toUpperCase();
                    const isCurrent = step.stepCode === saga.currentStep || step.stepName === saga.currentStep;
                    return (
                      <div key={i} className="flex items-center gap-2 text-sm">
                        {stepStatus === 'COMPLETED' ? (<CheckCircle className="h-4 w-4 text-green-600 shrink-0" />) : stepStatus === 'FAILED' ? (<AlertCircle className="h-4 w-4 text-destructive shrink-0" />) : isCurrent ? (<Clock className="h-4 w-4 text-blue-600 shrink-0 animate-pulse" />) : (<div className="h-4 w-4 rounded-full border-2 border-muted-foreground/30 shrink-0" />)}
                        <span className={isCurrent ? 'font-semibold text-blue-700' : ''}>{step.stepName || step.stepCode}</span>
                        <span className="ml-auto text-xs text-muted-foreground">{stepStatus}</span>
                      </div>
                    );
                  })}
                </div>
              </CardContent>
            </Card>
          )}
          {saga.partyIds?.length ? (
            <Card>
              <CardHeader className="pb-2"><CardTitle className="text-base flex items-center gap-2"><Users className="h-5 w-5" />Access Parties ({saga.partyIds.length})</CardTitle></CardHeader>
              <CardContent><div className="flex flex-wrap gap-1.5">{saga.partyIds.map((pid, i) => <Badge key={i} variant="outline" className="text-xs font-mono">{pid}</Badge>)}</div></CardContent>
            </Card>
          ) : null}
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

      {/* Actions Tab */}
      {activeTab === 'actions' && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Workflow Actions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {(saga.sagaType === 'PROCUREMENT_WORKFLOW' || saga.sagaTypeName === 'PROCUREMENT_WORKFLOW') ? (
              <ProcurementActions
                saga={saga}
                onAction={async (command: string, reason?: string) => {
                  setActionLoading(command);
                  try {
                    const request: ExecuteSagaStepRequest = command === 'ApproveRequest'
                      ? {
                          command,
                          data: reason !== undefined
                            ? { approvalData: { approved: false, rejectionReason: reason } }
                            : { approvalData: { approved: true } },
                        }
                      : { command };
                    await workflowApi.executeStep(saga.sagaId, request);
                    await fetchSaga();
                  } catch (err) {
                    setError(err instanceof Error ? err.message : `Failed to execute step: ${command}`);
                  } finally {
                    setActionLoading(null);
                  }
                }}
                rejectionReason=""
                onSetRejectionReason={() => {}}
                loading={actionLoading !== null}
              />
            ) : (
              <p className="text-muted-foreground text-sm">
                No workflow actions available for saga type: {saga.sagaTypeName || saga.sagaType}
              </p>
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
