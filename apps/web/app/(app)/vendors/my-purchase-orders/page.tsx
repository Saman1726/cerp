'use client';

import { useEffect, useState } from 'react';
import { FileText, Package, Clock, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@cerp/core-web/components/ui/card';
import { Badge } from '@cerp/core-web/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@cerp/core-web/components/ui/table';
import { Button } from '@cerp/core-web/components/ui/button';
import { PageHeader } from '@cerp/core-web/shell';
import type { VendorPurchaseOrder } from '@cerp/shared-types';

function statusBadge(status: string): 'default' | 'success' | 'destructive' | 'secondary' | 'outline' {
  switch (status) {
    case 'COMPLETED':
    case 'CLOSED':
      return 'success';
    case 'FAILED':
    case 'ABORTED':
      return 'destructive';
    case 'RUNNING':
    case 'IN_PROGRESS':
      return 'default';
    case 'PAUSED':
    case 'REQUIRES_MANUAL_INTERVENTION':
      return 'secondary';
    default:
      return 'outline';
  }
}

function stepIcon(step: string) {
  if (step.includes('Confirm') || step.includes('Close')) return <CheckCircle className="h-4 w-4" />;
  if (step.includes('Cancel') || step.includes('Reject')) return <XCircle className="h-4 w-4" />;
  if (step.includes('Error') || step.includes('Fail')) return <AlertCircle className="h-4 w-4" />;
  if (step.includes('Pause') || step.includes('Interv')) return <Clock className="h-4 w-4" />;
  return <FileText className="h-4 w-4" />;
}

export default function MyPurchaseOrdersPage() {
  const [orders, setOrders] = useState<VendorPurchaseOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await fetch('/api/vendors/my-purchase-orders');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        if (!cancelled) setOrders(data.items ?? []);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load purchase orders');
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Purchase Orders"
        description="View all purchase orders assigned to your vendor profile."
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardContent className="pt-6 flex items-center gap-4">
            <div className="p-2 bg-blue-100 rounded-full">
              <Package className="h-5 w-5 text-blue-700" />
            </div>
            <div>
              <div className="text-2xl font-bold">{orders.length}</div>
              <div className="text-sm text-muted-foreground">Total Orders</div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 flex items-center gap-4">
            <div className="p-2 bg-green-100 rounded-full">
              <CheckCircle className="h-5 w-5 text-green-700" />
            </div>
            <div>
              <div className="text-2xl font-bold">
                {orders.filter((o) => o.status === 'COMPLETED' || o.status === 'CLOSED').length}
              </div>
              <div className="text-sm text-muted-foreground">Completed</div>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 flex items-center gap-4">
            <div className="p-2 bg-yellow-100 rounded-full">
              <Clock className="h-5 w-5 text-yellow-700" />
            </div>
            <div>
              <div className="text-2xl font-bold">
                {orders.filter((o) => o.status === 'RUNNING' || o.status === 'PAUSED').length}
              </div>
              <div className="text-sm text-muted-foreground">In Progress</div>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Purchase Orders</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <p className="text-muted-foreground">Loading purchase orders...</p>
            </div>
          ) : error ? (
            <div className="flex items-center justify-center py-8">
              <p className="text-destructive">{error}</p>
            </div>
          ) : orders.length === 0 ? (
            <div className="flex items-center justify-center py-8">
              <p className="text-muted-foreground">
                No purchase orders assigned to you yet. Orders will appear here once a vendor is assigned to a procurement request.
              </p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Order ID</TableHead>
                  <TableHead>Material</TableHead>
                  <TableHead>Qty</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Step</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {orders.map((order) => (
                  <TableRow key={order.sagaId}>
                    <TableCell className="font-mono text-xs">
                      {order.sagaId.slice(0, 12)}...
                    </TableCell>
                    <TableCell className="font-medium">{order.materialId}</TableCell>
                    <TableCell>{order.quantity}</TableCell>
                    <TableCell className="max-w-xs truncate text-muted-foreground">
                      {order.description || '—'}
                    </TableCell>
                    <TableCell>
                      <Badge variant={statusBadge(order.status)} className="capitalize">
                        {order.status.replace(/_/g, ' ')}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1">
                        {stepIcon(order.currentStep)}
                        <span className="text-sm">{order.currentStep}</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-sm text-muted-foreground">
                      {new Date(order.createdAt).toLocaleDateString()}
                    </TableCell>
                    <TableCell>
                      <Button size="sm" variant="ghost" className="text-xs">
                        View Details
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}