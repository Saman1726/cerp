'use client';

import { useState, useEffect, useCallback } from 'react';
import {
  Truck,
  CheckCircle,
  Clock,
  AlertCircle,
  X,
  Users,
  Hash,
  Calendar,
  Package,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '../components/ui/modal/dialog';
import {
  DeliverySagaActionButtons,
} from './DeliverySagaActionButtons';
import type { DeliverySchedule } from '@cerp/shared-types';

function stepLabel(step: string): string {
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

function statusIcon(status: string | undefined) {
  const s = status?.toUpperCase() || '';
  if (s === 'COMPLETED' || s === 'CONFIRMED' || s === 'CLOSED') return <CheckCircle className="h-5 w-5 text-green-600" />;
  if (s === 'FAILED' || s === 'ABORTED') return <AlertCircle className="h-5 w-5 text-destructive" />;
  if (s === 'IN_TRANSIT' || s === 'ARRIVED' || s === 'SCHEDULED') return <Truck className="h-5 w-5 text-blue-600" />;
  return <Clock className="h-5 w-5 text-muted-foreground" />;
}

function statusBadgeVariant(status: string | undefined): 'default' | 'success' | 'destructive' | 'secondary' | 'outline' {
  const s = (status ?? '').toUpperCase();
  if (s === 'COMPLETED' || s === 'CONFIRMED' || s === 'CLOSED') return 'success';
  if (s === 'FAILED' || s === 'ABORTED') return 'destructive';
  if (s === 'IN_TRANSIT' || s === 'ARRIVED' || s === 'SCHEDULED' || s === 'RUNNING') return 'secondary';
  return 'outline';
}

interface SagaData {
  sagaId: string;
  sagaType: string;
  sagaTypeName: string;
  status: string;
  currentStep: string;
  data: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

export interface DeliverySagaDetailDialogProps {
  sagaId: string;
  deliverySaga: DeliverySchedule;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onRefresh: () => void;
}

export function DeliverySagaDetailDialog({
  sagaId,
  deliverySaga,
  open,
  onOpenChange,
  onRefresh,
}: DeliverySagaDetailDialogProps) {
  const [saga, setSaga] = useState<SagaData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchSaga = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/sagas/${sagaId}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setSaga(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load saga details');
    } finally {
      setLoading(false);
    }
  }, [sagaId]);

  useEffect(() => {
    if (open) {
      fetchSaga();
    }
  }, [open, fetchSaga]);

  const sagaStatus = saga?.status ?? deliverySaga.status;
  const currentStep = saga?.currentStep ?? deliverySaga.currentStep;
  const sagaData = saga?.data ?? deliverySaga;
  const isTerminal = ['COMPLETED', 'CLOSED', 'CANCELED', 'CANCELLED'].includes(sagaStatus?.toUpperCase() || '');

  const scheduledDate = String(sagaData.scheduledDate ?? deliverySaga.scheduledDate ?? '');
  const quantity = Number(sagaData.quantity ?? deliverySaga.quantity) || 0;
  const totalQuantity = Number(sagaData.totalQuantity ?? deliverySaga.totalQuantity ?? quantity) || quantity;
  const description = String(sagaData.description ?? deliverySaga.description ?? '');
  const deliveryPercentage = Number(sagaData.deliveryPercentage ?? deliverySaga.deliveryPercentage ?? 0);
  const assignedTo = String(sagaData.assignedTo ?? deliverySaga.assignedTo ?? '');
  const partyIds = String(sagaData.partyIds ?? deliverySaga.partyIds ?? '');
return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              {statusIcon(sagaStatus)}
              <DialogTitle className="text-lg">Delivery Details</DialogTitle>
            </div>
            <Button size="sm" variant="ghost" className="h-6 w-6 p-0" onClick={() => onOpenChange(false)}>
              <X className="h-4 w-4" />
            </Button>
          </div>
          <DialogDescription className="flex items-center gap-2">
            <Badge variant={statusBadgeVariant(sagaStatus)} className="capitalize">{sagaStatus}</Badge>
            <span className="text-xs text-muted-foreground">{stepLabel(currentStep ?? '')}</span>
          </DialogDescription>
        </DialogHeader>

        {loading && <div className="py-8 text-center text-sm text-muted-foreground">Loading delivery details...</div>}
        {error && <div className="py-4 text-sm text-destructive">{error}</div>}

        {!loading && !error && (
          <div className="space-y-4">
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm flex items-center gap-2">
                  <Package className="h-4 w-4" />Delivery Details
                </CardTitle>
              </CardHeader>
              <CardContent>
                <dl className="grid grid-cols-2 gap-3 text-sm">
                  <div><dt className="text-xs text-muted-foreground">Scheduled Date</dt><dd className="flex items-center gap-1"><Calendar className="h-3.5 w-3.5" />{scheduledDate || '\u2014'}</dd></div>
                  <div><dt className="text-xs text-muted-foreground">Quantity</dt><dd className="flex items-center gap-1"><Hash className="h-3.5 w-3.5" />{quantity.toLocaleString()}</dd></div>
                  <div><dt className="text-xs text-muted-foreground">Total Quantity</dt><dd className="flex items-center gap-1"><Hash className="h-3.5 w-3.5" />{totalQuantity.toLocaleString()}</dd></div>
                  <div><dt className="text-xs text-muted-foreground">Progress</dt>
                    <dd><div className="flex items-center gap-2"><div className="h-2 flex-1 bg-muted rounded-full overflow-hidden"><div className="h-full bg-blue-500 rounded-full transition-all" style={{ width: `${Math.min(100, Math.max(0, deliveryPercentage))}%` }} /></div><span className="text-xs font-medium">{Math.min(100, Math.max(0, deliveryPercentage))}%</span></div></dd>
                  </div>
                  {assignedTo && <div><dt className="text-xs text-muted-foreground">Assigned To</dt><dd className="text-xs truncate" title={assignedTo}>{assignedTo}</dd></div>}
                  {partyIds && <div><dt className="text-xs text-muted-foreground">Party IDs</dt><dd className="flex items-center gap-1 text-xs truncate" title={partyIds}><Users className="h-3 w-3 shrink-0" />{partyIds}</dd></div>}
                </dl>
              </CardContent>
            </Card>

            {description && (
              <Card><CardHeader className="pb-2"><CardTitle className="text-sm">Description</CardTitle></CardHeader><CardContent><p className="text-sm whitespace-pre-wrap">{description}</p></CardContent></Card>
            )}

            <Card><CardHeader className="pb-2"><CardTitle className="text-sm">Saga Metadata</CardTitle></CardHeader>
              <CardContent><dl className="grid grid-cols-2 gap-3 text-sm">
                <div><dt className="text-xs text-muted-foreground">Saga Type</dt><dd className="text-xs">{saga?.sagaType ?? '\u2014'}</dd></div>
                <div><dt className="text-xs text-muted-foreground">Current Step</dt><dd className="text-xs">{stepLabel(currentStep ?? '')}</dd></div>
                <div><dt className="text-xs text-muted-foreground">Created</dt><dd className="text-xs">{saga?.createdAt ? new Date(saga.createdAt).toLocaleString() : '\u2014'}</dd></div>
                <div><dt className="text-xs text-muted-foreground">Updated</dt><dd className="text-xs">{saga?.updatedAt ? new Date(saga.updatedAt).toLocaleString() : '\u2014'}</dd></div>
              </dl></CardContent>
            </Card>
{!isTerminal && deliverySaga.deliverySagaId && (
              <Card><CardHeader className="pb-2"><CardTitle className="text-sm">Actions</CardTitle></CardHeader>
                <CardContent>
                  <DeliverySagaActionButtons sagaId={deliverySaga.deliverySagaId} currentStep={currentStep} status={sagaStatus}
                    onStepCompleted={() => { void fetchSaga(); onRefresh(); }} />
                </CardContent></Card>
            )}

            {isTerminal && <p className="text-sm text-muted-foreground text-center py-2">This delivery is complete. No further actions are available.</p>}
          </div>
        )}

        <DialogFooter><Button variant="outline" onClick={() => onOpenChange(false)}>Close</Button></DialogFooter>
      </DialogContent>
    </Dialog>
  );
}