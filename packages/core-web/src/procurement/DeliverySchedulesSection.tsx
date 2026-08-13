'use client';

import { useState, useEffect } from 'react';
import { Truck, CheckCircle, Clock, AlertCircle } from 'lucide-react';
import { Badge } from '../components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';

interface DeliverySchedulesSectionProps {
  procurementSagaId: string;
  tenantId: string;
}

function statusBadge(status: string): 'default' | 'success' | 'destructive' | 'secondary' | 'outline' {
  const s = status?.toUpperCase() || '';
  if (s === 'COMPLETED' || s === 'CONFIRMED') return 'success';
  if (s === 'FAILED' || s === 'ABORTED') return 'destructive';
  if (s === 'SCHEDULED') return 'secondary';
  if (s === 'IN_TRANSIT' || s === 'ARRIVED') return 'default';
  return 'outline';
}

function statusIcon(status: string) {
  const s = status?.toUpperCase() || '';
  if (s === 'COMPLETED' || s === 'CONFIRMED') return <CheckCircle className="h-4 w-4 text-green-600" />;
  if (s === 'FAILED' || s === 'ABORTED') return <AlertCircle className="h-4 w-4 text-destructive" />;
  if (s === 'IN_TRANSIT' || s === 'ARRIVED') return <Truck className="h-4 w-4 text-blue-600" />;
  return <Clock className="h-4 w-4 text-muted-foreground" />;
}

export function DeliverySchedulesSection({ procurementSagaId }: DeliverySchedulesSectionProps) {
  const [deliveries, setDeliveries] = useState<Array<{
    deliverySagaId: string; scheduledDate: string; quantity: number; status: string; createdAt: string;
  }>>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await fetch('/api/sagas?sagaType=DELIVERY_WORKFLOW&pageSize=100');
        if (!res.ok) return;
        const data = await res.json();
        if (cancelled) return;
        const items = (data.items ?? []) as Record<string, unknown>[];
        const matching = items.filter((d) => {
          const dData = (d.data ?? {}) as Record<string, unknown>;
          return dData.procurementSagaId === procurementSagaId;
        });
        setDeliveries(
          matching.map((d) => {
            const dData = (d.data ?? {}) as Record<string, unknown>;
            return {
              deliverySagaId: String(d.sagaId ?? ''),
              scheduledDate: String(dData.scheduledDate ?? ''),
              quantity: Number(dData.quantity) || 0,
              status: String(dData.deliveryStatus || dData.status || 'SCHEDULED'),
              createdAt: String(d.createdAt ?? ''),
            };
          }),
        );
      } catch {
        // ignore
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [procurementSagaId]);

  if (deliveries.length === 0) return null;

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base flex items-center gap-2">
          <Truck className="h-4 w-4" />
          Linked Delivery Schedules ({deliveries.length})
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-2">
          {deliveries.map((d) => (
            <div
              key={d.deliverySagaId}
              className="flex items-center justify-between p-3 rounded-lg border bg-card"
            >
              <div className="flex items-center gap-3">
                {statusIcon(d.status)}
                <div>
                  <div className="text-sm font-medium">{d.scheduledDate || "Date TBD"}</div>
                  <div className="text-xs text-muted-foreground">
                    Qty: {d.quantity} &middot; {d.deliverySagaId.slice(0, 12)}...
                  </div>
                </div>
              </div>
              <Badge variant={statusBadge(d.status)} className="capitalize">
                {d.status.replace(/_/g, ' ')}
              </Badge>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
