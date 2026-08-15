'use client';

import { useState, useEffect, useCallback } from 'react';
import {
  CheckCircle, Clock, AlertCircle, X, Hash, FileText, Truck, Calendar, Users, ChevronRight,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '../components/ui/modal/dialog';
import { DeliverySagaActionButtons } from '../procurement/DeliverySagaActionButtons';
import { workflowApi } from './api';
import type { SagaEvent, SagaDetailTabId, ExecuteSagaStepRequest } from './types';
import type { Saga as SagaType } from '@cerp/shared-types';
import { SAGA_DETAIL_TABS, SAGA_TYPE_LABELS } from './types';

function stepLabel(step: string): string {
  if (SAGA_TYPE_LABELS[step]) return SAGA_TYPE_LABELS[step];
  const labels: Record<string, string> = {
    ScheduleDelivery: 'Schedule Delivery',
    StartTransit: 'Start Transit',
    DeliveryArrived: 'Delivery Arrived',
    StartUnloading: 'Start Unloading',
    ConfirmDelivery: 'Confirm Delivery',
    RaiseDiscrepancy: 'Raise Discrepancy',
    ResolveDiscrepancy: 'Resolve Discrepancy',
    CloseDelivery: 'Close Delivery',
  };
  return labels[step] || step;
}

function isTerminal(status: string): boolean {
  return ['COMPLETED', 'CLOSED', 'CANCELED', 'CANCELLED', 'COMPENSATED', 'ABORTED'].includes(status.toUpperCase());
}

function statusIcon(status: string | undefined) {
  const s = status?.toUpperCase() || '';
  if (s === 'COMPLETED' || s === 'CONFIRMED' || s === 'CLOSED') return <CheckCircle className="h-5 w-5 text-green-600" />;
  if (s === 'FAILED' || s === 'ABORTED') return <AlertCircle className="h-5 w-5 text-destructive" />;
  return <Clock className="h-5 w-5 text-muted-foreground" />;
}

function statusBadgeVariant(status: string | undefined): 'default' | 'success' | 'destructive' | 'secondary' | 'outline' {
  const s = (status ?? '').toUpperCase();
  if (s === 'COMPLETED' || s === 'CONFIRMED' || s === 'CLOSED') return 'success';
  if (s === 'FAILED' || s === 'ABORTED') return 'destructive';
  if (s === 'RUNNING' || s === 'ACTIVE' || s === 'IN_PROGRESS' || s === 'IN_TRANSIT' || s === 'SCHEDULED' || s === 'ARRIVED') return 'secondary';
  return 'outline';
}

function fmtDate(d: string | undefined): string {
  if (!d) return '\u2014';
  return new Date(d).toLocaleString();
}

function safeNum(v: unknown): number | null {
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

function extractProcurementData(saga: SagaType) {
  const d = saga.data ?? {};
  return {
    purchaseOrderNumber: d.purchaseOrderNumber ?? d.poNumber ?? d.purchaseOrderNo ?? null,
    vendorId: d.vendorId ?? null,
    vendorName: d.vendorName ?? null,
    projectId: d.projectId ?? null,
    projectTitle: d.projectTitle ?? null,
    materialId: d.materialId ?? null,
    materialName: d.materialName ?? null,
    materialDescription: d.materialDescription ?? null,
    quantity: safeNum(d.quantity),
    totalQuantity: safeNum(d.totalQuantity) ?? safeNum(d.quantity),
    deliveredQuantity: safeNum(d.deliveredQuantity) ?? safeNum(d.quantityDelivered),
    deliveryStatus: d.deliveryStatus ?? null,
    description: d.description ?? null,
    deliveryPercentage: safeNum(d.deliveryPercentage),
    scheduledDate: d.scheduledDate ?? null,
    expectedArrivalTime: d.expectedArrivalTime ?? null,
    procurementSagaId: d.procurementSagaId ?? null,
    linkedDeliveries: Array.isArray(d.linkedDeliveries) ? d.linkedDeliveries : null,
  };
}

function extractDeliverySchedules(d: Record<string, unknown> | Record<string, unknown>[] | null) {
  if (!d) return [];
  const arr = Array.isArray(d) ? d : (d.deliveries ?? d.deliverySchedules ?? []);
  if (!Array.isArray(arr)) return [];
  return arr.map((item: Record<string, unknown>) => {
    const id = item.deliverySagaId ?? item.sagaId ?? item.id ?? '';
    const sd = item as Record<string, unknown>;
    return {
      id: String(id),
      scheduledDate: String(sd.scheduledDate ?? sd.date ?? ''),
      quantity: safeNum(sd.quantity) ?? 0,
      status: String(sd.deliveryStatus ?? sd.status ?? 'SCHEDULED'),
      deliveryPercentage: safeNum(sd.deliveryPercentage) ?? 0,
      totalQuantity: safeNum(sd.totalQuantity) ?? safeNum(sd.quantity) ?? 0,
      currentStep: String(sd.currentStep ?? sd.sagaStep ?? ''),
      description: String(sd.description ?? ''),
    };
  });
}

function extractWorkflowProgress(saga: SagaType) {
  const steps = saga.steps ?? [];
  if (steps.length === 0) return null;
  return steps.map((s, i) => {
    const code = s.stepCode ?? s.stepName ?? '';
    const status = (s.status ?? 'PENDING').toString().toUpperCase();
    return { index: i, code, label: stepLabel(code), status, completed: status === 'COMPLETED', failed: status === 'FAILED' };
  });
}

export interface SagaDetailDialogProps {
  sagaId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onRefresh?: () => void;
}

export function SagaDetailDialog({ sagaId, open, onOpenChange, onRefresh }: SagaDetailDialogProps) {
  const [saga, setSaga] = useState<SagaType | null>(null);
  const [events, setEvents] = useState<SagaEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<SagaDetailTabId>('overview');
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');

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

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    if (open) fetchSaga();
  }, [open, fetchSaga]);

  const handleAction = async (command: string, reason?: string) => {
    setActionLoading(command);
    setError(null);
    try {
      const request: ExecuteSagaStepRequest = command === 'ApproveRequest'
        ? {
            command,
            data: reason !== undefined
              ? { approvalData: { approved: false, rejectionReason: reason } }
              : { approvalData: { approved: true } },
          }
        : { command };
      await workflowApi.executeStep(sagaId, request);
      await fetchSaga();
      onRefresh?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to execute step: ${command}`);
    } finally {
      setActionLoading(null);
      setRejectionReason('');
    }
  };

  const sagaStatus = saga?.status ?? '';
  const currentStep = saga?.currentStep ?? '';
  const terminal = isTerminal(sagaStatus);
  const isDelivery = saga?.sagaType?.toLowerCase().includes('delivery');
  const isProcurement = saga?.sagaType?.toLowerCase().includes('procurement');
  const pd = saga ? extractProcurementData(saga) : null;
  const progress = saga ? extractWorkflowProgress(saga) : null;
  const deliverySchedules = pd?.linkedDeliveries ? extractDeliverySchedules(pd.linkedDeliveries as Record<string, unknown>[]) : [];

  const renderProgressSteps = () => {
    if (!progress) return null;
    return (
      <div className="space-y-2">
        {progress.map((s) => (
          <div key={s.index} className="flex items-center gap-2 text-xs">
            {s.completed ? (
              <CheckCircle className="h-4 w-4 text-green-600 shrink-0" />
            ) : s.failed ? (
              <AlertCircle className="h-4 w-4 text-destructive shrink-0" />
            ) : s.code.toLowerCase() === currentStep.toLowerCase() ? (
              <Clock className="h-4 w-4 text-blue-600 shrink-0 animate-pulse" />
            ) : (
              <div className="h-4 w-4 rounded-full border-2 border-muted-foreground/30 shrink-0" />
            )}
            <span className={s.code.toLowerCase() === currentStep.toLowerCase() ? 'font-semibold text-blue-700' : ''}>
              {s.label}
            </span>
            <Badge variant={s.failed ? 'destructive' : s.completed ? 'success' : 'outline'} className="ml-auto text-[10px] h-4">
              {s.status}
            </Badge>
          </div>
        ))}
      </div>
    );
  };

  const renderDeliveryRow = (dl: ReturnType<typeof extractDeliverySchedules>[number]) => (
    <div key={dl.id} className="flex items-center justify-between p-2.5 border rounded-lg text-xs hover:bg-muted/40 transition-colors">
      <div className="flex items-center gap-3 flex-1 min-w-0">
        <button
          className="font-medium text-blue-700 hover:underline truncate flex-shrink-0"
          onClick={() => { onOpenChange(false); setTimeout(() => onOpenChange(true), 100); }}
        >
          Delivery #{dl.id.slice(0, 8)}
        </button>
        <span className="flex items-center gap-1 text-muted-foreground shrink-0">
          <Calendar className="h-3 w-3" />
          {dl.scheduledDate || '\u2014'}
        </span>
        <span className="shrink-0">Qty: <strong>{String(dl.quantity)}</strong></span>
      </div>
      <div className="flex items-center gap-2 shrink-0 ml-2">
        <span className="text-muted-foreground w-14 text-right">{String(dl.deliveryPercentage)}%</span>
        <Badge variant={dl.status?.toUpperCase() === 'COMPLETED' ? 'success' : 'outline'} className="capitalize">
          {dl.status}
        </Badge>
      </div>
    </div>
  );
const renderOverviewContent = () => (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Badge variant={statusBadgeVariant(sagaStatus)} className="capitalize text-xs">{sagaStatus}</Badge>
        {saga?.sagaTypeName && <Badge variant="outline" className="text-xs">{saga.sagaTypeName}</Badge>}
        {saga?.sagaType && saga.sagaTypeName !== SAGA_TYPE_LABELS[saga.sagaType] && (
          <Badge variant="outline" className="text-[10px] font-mono">{SAGA_TYPE_LABELS[saga.sagaType] || saga.sagaType}</Badge>
        )}
      </div>
      {(pd?.purchaseOrderNumber || pd?.vendorId || pd?.vendorName || pd?.projectId || pd?.materialId || pd?.materialName || pd?.quantity != null || pd?.description) && (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><FileText className="h-4 w-4" />Procurement Details</CardTitle></CardHeader>
          <CardContent>
            <dl className="grid grid-cols-2 gap-3 text-sm">
              {pd?.purchaseOrderNumber && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Purchase Order</dt><dd className="font-mono text-xs font-medium">{String(pd.purchaseOrderNumber)}</dd></div>}
              {(pd?.vendorName || pd?.vendorId) && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Vendor</dt><dd className="text-xs">{pd.vendorName || <span className="font-mono">{pd.vendorId}</span>}</dd></div>}
              {pd?.projectId && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Project</dt><dd className="font-mono text-xs">{pd.projectTitle || pd.projectId}</dd></div>}
              {(pd?.materialName || pd?.materialId) && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Material</dt><dd className="text-xs">{pd.materialName || <span className="font-mono">{pd.materialId}</span>}</dd></div>}
              {pd?.description && <div className="col-span-2"><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Description</dt><dd className="text-xs">{pd.description}</dd></div>}
              {pd?.quantity != null && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Quantity</dt><dd className="text-xs font-medium">{String(pd.quantity)}</dd></div>}
              {pd?.totalQuantity != null && String(pd.totalQuantity) !== String(pd.quantity) && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Total Qty</dt><dd className="text-xs">{String(pd.totalQuantity)}</dd></div>}
              {pd?.deliveredQuantity != null && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Delivered</dt><dd className="text-xs">{String(pd.deliveredQuantity)}</dd></div>}
              {pd?.deliveryPercentage != null && <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Delivery %</dt><dd className="text-xs">{String(pd.deliveryPercentage)}%</dd></div>}
            </dl>
          </CardContent>
        </Card>
      )}
      {isDelivery && !isProcurement && (pd?.scheduledDate || pd?.expectedArrivalTime) && (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><Truck className="h-4 w-4" />Delivery Schedule</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {pd?.scheduledDate && <div className="flex justify-between text-xs"><span className="text-muted-foreground">Scheduled Date</span><span>{fmtDate(pd.scheduledDate)}</span></div>}
            {pd?.expectedArrivalTime && <div className="flex justify-between text-xs"><span className="text-muted-foreground">Expected Arrival</span><span>{fmtDate(pd.expectedArrivalTime)}</span></div>}
            {pd?.deliveryPercentage != null && (<div><div className="flex justify-between text-xs mb-1"><span className="text-muted-foreground">Delivery Progress</span><span className="font-medium">{String(pd.deliveryPercentage)}%</span></div><div className="h-2 bg-muted rounded-full overflow-hidden"><div className="h-full bg-blue-600 rounded-full transition-all" style={{ width: `${Math.min(pd.deliveryPercentage, 100)}%` }} /></div></div>)}
          </CardContent>
        </Card>
      )}
      {isProcurement && deliverySchedules.length > 0 && (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><Truck className="h-4 w-4" />Delivery Schedules ({deliverySchedules.length})</CardTitle></CardHeader>
          <CardContent><div className="space-y-2">{deliverySchedules.map(renderDeliveryRow)}</div></CardContent>
        </Card>
      )}
      {isDelivery && pd?.procurementSagaId && (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><FileText className="h-4 w-4" />Parent Procurement</CardTitle></CardHeader>
          <CardContent><button className="text-xs font-medium text-blue-700 hover:underline" onClick={() => { onOpenChange(false); setTimeout(() => onOpenChange(true), 100); }}>PO #{pd.procurementSagaId.slice(0, 8)}</button></CardContent>
        </Card>
      )}
      {progress && (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><ChevronRight className="h-4 w-4" />Workflow Steps</CardTitle></CardHeader>
          <CardContent>{renderProgressSteps()}</CardContent>
        </Card>
      )}
<Card>
        <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><Hash className="h-4 w-4" />Metadata</CardTitle></CardHeader>
        <CardContent>
          <dl className="grid grid-cols-2 gap-3 text-sm">
            <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Saga ID</dt><dd className="font-mono text-xs truncate">{saga?.sagaId ?? '\u2014'}</dd></div>
            <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Status</dt><dd className="text-xs capitalize">{sagaStatus}</dd></div>
            <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Created</dt><dd className="text-xs">{fmtDate(saga?.createdAt)}</dd></div>
            <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Updated</dt><dd className="text-xs">{fmtDate(saga?.updatedAt)}</dd></div>
            <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Created By</dt><dd className="font-mono text-xs truncate">{saga?.createdBy ?? '\u2014'}</dd></div>
            <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Assigned To</dt><dd className="font-mono text-xs truncate">{saga?.assignedTo ?? '\u2014'}</dd></div>
            <div><dt className="text-[11px] text-muted-foreground uppercase tracking-wide">Tenant</dt><dd className="font-mono text-xs truncate">{saga?.tenantId ?? '\u2014'}</dd></div>
          </dl>
        </CardContent>
      </Card>
      {saga?.partyIds?.length ? (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm flex items-center gap-2"><Users className="h-4 w-4" />Access Parties ({saga.partyIds.length})</CardTitle></CardHeader>
          <CardContent><div className="flex flex-wrap gap-1.5">{saga.partyIds.map((pid, i) => <Badge key={i} variant="outline" className="text-[10px] font-mono">{pid}</Badge>)}</div></CardContent>
        </Card>
      ) : null}
    </div>
  );

  const renderEventsContent = () => (
    <div className="space-y-1 max-h-80 overflow-y-auto">
      {events.length === 0 && <p className="text-center py-8 text-sm text-muted-foreground">No events recorded</p>}
      {events.map((ev, i) => (
        <div key={i} className="flex gap-3 py-1.5 border-b border-border/50 text-xs">
          <span className="font-mono text-muted-foreground shrink-0 w-40">{fmtDate(ev.timestamp)}</span>
          <span className="font-medium">{ev.eventType}</span>
        </div>
      ))}
    </div>
  );

  const renderActionsContent = () => {
    if (terminal) return <p className="text-muted-foreground text-sm">This saga is complete. No further actions available.</p>;
    const actions: Array<{ label: string; command: string }> = [];
    if (isProcurement) {
      const step = currentStep ?? '';
      if (step === 'CreateProcurementRequest') actions.push({ label: 'Submit for Approval', command: 'SubmitForApproval' });
      if (step === 'SubmitForApproval') actions.push({ label: 'Approve Request', command: 'ApproveRequest' });
      if (step === 'ApproveRequest') actions.push({ label: 'Issue Purchase Order', command: 'IssuePurchaseOrder' });
      if (step === 'IssuePurchaseOrder') actions.push({ label: 'Receive Delivery', command: 'ReceiveDelivery' });
      if (step === 'ReceiveDelivery') actions.push({ label: 'Confirm Delivery', command: 'ConfirmDelivery' });
      if (step === 'ConfirmDelivery') actions.push({ label: 'Close Procurement', command: 'CloseProcurement' });
    }
    if (actions.length === 0) return <p className="text-muted-foreground text-sm">No actions available for saga type: {saga?.sagaTypeName || saga?.sagaType}</p>;
    return (<div className="space-y-2">{actions.map((a) => (<Button key={a.command} variant="default" onClick={() => handleAction(a.command, rejectionReason)} disabled={!!actionLoading}>{actionLoading === a.command ? 'Processing...' : a.label}</Button>))}</div>);
  };

  const renderCompensationContent = () => (
    <div className="space-y-4">
      {sagaStatus === 'FAILED' && (<Button variant="destructive" onClick={() => handleAction('Compensate')} disabled={!!actionLoading}>{actionLoading === 'Compensate' ? 'Compensating...' : 'Compensate Saga'}</Button>)}
      {sagaStatus === 'REQUIRES_MANUAL_INTERVENTION' && (<Button onClick={() => handleAction('Resume')} disabled={!!actionLoading}>{actionLoading === 'Resume' ? 'Resuming...' : 'Resume Saga'}</Button>)}
      {sagaStatus === 'FAILED' && (<Button variant="outline" onClick={() => handleAction('Escalate')} disabled={!!actionLoading}>{actionLoading === 'Escalate' ? 'Escalating...' : 'Escalate'}</Button>)}
      {saga?.errorDetails && Object.keys(saga.errorDetails).length > 0 && (<div><p className="text-xs font-medium mb-1">Error Details:</p><pre className="bg-muted p-3 rounded text-xs overflow-auto max-h-32">{JSON.stringify(saga.errorDetails, null, 2)}</pre></div>)}
    </div>
  );

  const renderDeliveryActions = () => {
    if (terminal || !isDelivery) return null;
    return (
      <Card>
        <CardHeader className="pb-2"><CardTitle className="text-sm">Delivery Actions</CardTitle></CardHeader>
        <CardContent>
          <DeliverySagaActionButtons sagaId={sagaId} currentStep={currentStep} status={sagaStatus} quantity={pd?.quantity ?? undefined} onStepCompleted={() => { void fetchSaga(); onRefresh?.(); }} />
        </CardContent>
      </Card>
    );
  };
return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">{statusIcon(sagaStatus)}<DialogTitle className="text-lg">Workflow Details</DialogTitle></div>
            <Button size="sm" variant="ghost" className="h-6 w-6 p-0" onClick={() => onOpenChange(false)}><X className="h-4 w-4" /></Button>
          </div>
          <DialogDescription className="flex flex-wrap items-center gap-2">
            <Badge variant={statusBadgeVariant(sagaStatus)} className="capitalize">{sagaStatus || '\u2014'}</Badge>
            {saga?.sagaTypeName && <Badge variant="outline" className="text-xs">{saga.sagaTypeName}</Badge>}
            {saga?.currentStep && <span className="text-xs text-muted-foreground">{'\u2192'} {stepLabel(saga.currentStep)}</span>}
          </DialogDescription>
        </DialogHeader>
        {error && <div className="px-1 py-2 text-sm text-destructive bg-destructive/10 rounded">{error}</div>}
        {!loading && !error && saga && (
          <div className="space-y-0">
            <div className="flex gap-0.5 mb-4 border-b pb-0">
              {SAGA_DETAIL_TABS.map((tab) => (
                <button key={tab.id} className={`text-xs font-medium px-3 py-2 transition-colors border-b-2 ${activeTab === tab.id ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground hover:border-muted'}`} onClick={() => setActiveTab(tab.id as SagaDetailTabId)}>{tab.label}</button>
              ))}
            </div>
            {activeTab === 'overview' && renderOverviewContent()}
            {activeTab === 'steps' && renderProgressSteps()}
            {activeTab === 'events' && renderEventsContent()}
            {activeTab === 'actions' && renderActionsContent()}
            {activeTab === 'compensation' && renderCompensationContent()}
            {activeTab === 'overview' && renderDeliveryActions()}
          </div>
        )}
        {loading && <div className="py-8 text-center text-sm text-muted-foreground">Loading workflow details...</div>}
        <DialogFooter className="mt-4">
          <Button variant="outline" onClick={() => onOpenChange(false)}>Close</Button>
          <Button variant="default" onClick={() => { void fetchSaga(); onRefresh?.(); }}>Refresh</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}