'use client';

import { useEffect, useState } from 'react';
import React from 'react';
import { useRouter } from 'next/navigation';
import {
  FileText,
  Package,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  Plus,
  Truck,
  ChevronDown,
  ChevronRight,
  Eye,
  Layers,
  Calendar,
  Hash,
  User,
  Users,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@cerp/core-web/components/ui/card';
import { Badge } from '@cerp/core-web/components/ui/badge';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@cerp/core-web/components/ui/table';
import { Button } from '@cerp/core-web/components/ui/button';
import { PageHeader } from '@cerp/core-web/shell';
import { DeliveryScheduleForm, DeliverySagaActionButtons, DeliverySagaDetailDialog } from '@cerp/core-web/procurement';
import type { VendorPurchaseOrder, DeliverySchedule } from '@cerp/shared-types';

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
  const router = useRouter();
  const [orders, setOrders] = useState<VendorPurchaseOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedRow, setExpandedRow] = useState<string | null>(null);
  const [deliveryFormOpen, setDeliveryFormOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<VendorPurchaseOrder | null>(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [selectedDeliverySagaId, setSelectedDeliverySagaId] = useState<string | null>(null);
  const [selectedDeliverySaga, setSelectedDeliverySaga] = useState<DeliverySchedule | null>(null);

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

  const refreshOrders = async () => {
    try {
      const res = await fetch('/api/vendors/my-purchase-orders');
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setOrders(data.items ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to refresh orders');
    }
  };

  const handleAddDelivery = (order: VendorPurchaseOrder) => {
    setSelectedOrder(order);
    setDeliveryFormOpen(true);
  };

  const handleViewDeliveryDetails = (schedule: DeliverySchedule) => {
    setSelectedDeliverySagaId(schedule.deliverySagaId ?? null);
    setSelectedDeliverySaga(schedule);
    setDetailDialogOpen(true);
  };

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

      {error && (
        <Card>
          <CardContent className="pt-6">
            <p className="text-destructive text-sm">{error}</p>
          </CardContent>
        </Card>
      )}

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
                  <TableHead className="w-10"></TableHead>
                  <TableHead>Order ID</TableHead>
                  <TableHead>Material</TableHead>
                  <TableHead>Qty</TableHead>
                  <TableHead>Delivered</TableHead>
                  <TableHead>Deliveries</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Step</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {orders.map((order) => {
                  const isExpanded = expandedRow === order.sagaId;
                  const deliveries = (order.deliverySchedules as DeliverySchedule[]) ?? [];
                  return (
                    <React.Fragment key={order.sagaId}>
                      <TableRow
                        key={order.sagaId}
                        className="hover:bg-muted/50 cursor-pointer"
                        onClick={() => setExpandedRow(isExpanded ? null : order.sagaId)}
                      >
                        <TableCell className="w-10">
                          {isExpanded ? (
                            <ChevronDown className="h-4 w-4 text-muted-foreground" />
                          ) : (
                            <ChevronRight className="h-4 w-4 text-muted-foreground" />
                          )}
                        </TableCell>
                        <TableCell className="font-mono text-xs">{order.sagaId.slice(0, 12)}...</TableCell>
                        <TableCell className="font-medium">{order.materialId}</TableCell>
                        <TableCell>{order.quantity}</TableCell>
                        <TableCell>{order.deliveredQuantity ?? 0}</TableCell>
                        <TableCell>
                          {deliveries.length > 0 ? (
                            <div className="flex items-center gap-1">
                              <Truck className="h-3.5 w-3.5 text-muted-foreground" />
                              <span className="text-sm">{deliveries.length}</span>
                            </div>
                          ) : (
                            <span className="text-xs text-muted-foreground">—</span>
                          )}
                        </TableCell>
                        <TableCell className="max-w-xs truncate text-muted-foreground">{order.description || '—'}</TableCell>
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
                        <TableCell className="text-sm text-muted-foreground">{new Date(order.createdAt).toLocaleDateString()}</TableCell>
                        <TableCell>
                          <Button size="sm" variant="ghost" className="text-xs" onClick={(e) => { e.stopPropagation(); router.push(`/workflows/saga/${order.sagaId}`); }}>
                            <Eye className="h-3 w-3" /> Details
                          </Button>
                        </TableCell>
                      </TableRow>
                      {isExpanded && (
                        <TableRow key={`${order.sagaId}:expanded`} className="bg-muted/30">
                          <TableCell colSpan={10} className="p-0">
                            <div className="px-6 py-4 space-y-4">
                              <div className="flex items-start justify-between">
                                <div className="space-y-1">
                                  <p className="text-sm font-medium">Procurement Saga Details</p>
                                  <p className="text-xs font-mono text-muted-foreground">{order.sagaId}</p>
                                </div>
                                <div className="flex items-center gap-2">
                                  <Button size="sm" variant="default" onClick={(e) => { e.stopPropagation(); handleAddDelivery(order); }}>
                                    <Plus className="h-3.5 w-3.5 mr-1" /> Add Delivery
                                  </Button>
                                  <Button size="sm" variant="outline" onClick={() => router.push(`/workflows/saga/${order.sagaId}`)}>
                                    <Layers className="h-3.5 w-3.5 mr-1" /> Full Saga View
                                  </Button>
                                </div>
                              </div>
                              <div className="grid grid-cols-5 gap-4 text-sm">
                                <div><p className="text-muted-foreground flex items-center gap-1.5"><Hash className="h-3.5 w-3.5" /> Material</p><p className="font-medium mt-0.5">{order.materialId || '—'}</p></div>
                                <div><p className="text-muted-foreground flex items-center gap-1.5"><Layers className="h-3.5 w-3.5" /> Step</p><p className="font-medium mt-0.5 flex items-center gap-1.5">{stepIcon(order.currentStep)} {order.currentStep}</p></div>
                                <div><p className="text-muted-foreground flex items-center gap-1.5"><Calendar className="h-3.5 w-3.5" /> Created</p><p className="font-medium mt-0.5">{new Date(order.createdAt).toLocaleDateString()}</p></div>
                                <div><p className="text-muted-foreground flex items-center gap-1.5"><User className="h-3.5 w-3.5" /> Created By</p><p className="font-medium mt-0.5">{order.createdBy ? order.createdBy.slice(0, 12) : '—'}</p></div>
                                <div><p className="text-muted-foreground flex items-center gap-1.5"><Truck className="h-3.5 w-3.5" /> Deliveries</p><p className="font-medium mt-0.5">{deliveries.length} scheduled · {order.deliveredQuantity ?? 0}/{order.quantity} qty</p></div>
                              </div>
                              {deliveries.length > 0 && (
                                <div>
                                  <p className="text-xs font-medium text-muted-foreground mb-2">Linked Delivery Schedules</p>
                                  <div className="border rounded overflow-hidden">
                                    <Table>
                                      <TableHeader>
                                        <TableRow className="bg-muted/50">
                                          <TableHead className="text-xs">Date</TableHead>
                                          <TableHead className="text-xs">Qty</TableHead>
                                          <TableHead className="text-xs">Progress</TableHead>
                                          <TableHead className="text-xs">Assigned To</TableHead>
                                          <TableHead className="text-xs">Party IDs</TableHead>
                                          <TableHead className="text-xs">Status</TableHead>
                                          <TableHead className="text-xs">Actions</TableHead>
                                        </TableRow>
                                      </TableHeader>
                                      <TableBody>
                                        {deliveries.map((d) => {
                                          const totalQty = d.totalQuantity || d.quantity || 0;
                                          const pct = totalQty > 0 ? Math.round((d.deliveryPercentage || (d.status === 'COMPLETED' ? 100 : 0))) : 0;
                                          const assigned = d.assignedTo ? d.assignedTo.slice(0, 12) : '—';
                                          const parties = d.partyIds ? d.partyIds.slice(0, 20) : '—';
                                          return (
                                            <TableRow key={d.deliverySagaId ?? d.id} className="hover:bg-transparent">
                                              <TableCell className="text-xs py-1.5">{d.scheduledDate ? new Date(d.scheduledDate).toLocaleDateString() : '—'}</TableCell>
                                              <TableCell className="text-xs py-1.5">{d.quantity}{totalQty > 0 && totalQty !== d.quantity ? ` / ${totalQty}` : ''}</TableCell>
                                              <TableCell className="text-xs py-1.5">
                                                <div className="flex items-center gap-1">
                                                  <div className="w-10 h-1.5 bg-muted rounded-full overflow-hidden">
                                                    <div className="h-full bg-blue-500 rounded-full" style={{ width: `${pct}%` }} />
                                                  </div>
                                                  <span className="text-[10px]">{pct}%</span>
                                                </div>
                                              </TableCell>
                                              <TableCell className="text-xs py-1.5">{assigned}</TableCell>
                                              <TableCell className="text-xs py-1.5">
                                                <div className="flex items-center gap-1 text-[10px] text-muted-foreground">
                                                  <Users className="h-2.5 w-2.5" />
                                                  {parties}
                                                </div>
                                              </TableCell>
                                              <TableCell className="text-xs py-1.5">
                                                <Badge variant={d.status === 'COMPLETED' ? 'success' : d.status === 'FAILED' ? 'destructive' : 'secondary'} className="text-[10px]">{d.status}</Badge>
                                              </TableCell>
                                              <TableCell className="text-xs py-1.5">
                                                <div className="flex items-center gap-1">
                                                  {d.deliverySagaId && (
                                                    <DeliverySagaActionButtons
                                                      sagaId={d.deliverySagaId}
                                                      currentStep={d.currentStep}
                                                      status={d.status}
                                                      onStepCompleted={refreshOrders}
                                                    />
                                                  )}
                                                  <Button size="sm" variant="ghost" className="text-xs h-5 px-1.5" onClick={() => handleViewDeliveryDetails(d)} title="View Details">
                                                    <Eye className="h-3 w-3" />
                                                  </Button>
                                                </div>
                                              </TableCell>
                                            </TableRow>
                                          );
                                        })}
                                      </TableBody>
                                    </Table>
                                  </div>
                                </div>
                              )}
                            </div>
                          </TableCell>
                        </TableRow>
                      )}
                    </React.Fragment>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Delivery Schedule Creation Dialog */}
      {selectedOrder && (
        <DeliveryScheduleForm
          open={deliveryFormOpen}
          onOpenChange={(open) => { setDeliveryFormOpen(open); if (!open) setSelectedOrder(null); }}
          sagaId={selectedOrder.sagaId}
          vendorId={selectedOrder.vendorId}
          availableQuantity={(selectedOrder.quantity ?? 0) - (selectedOrder.deliveredQuantity ?? 0)}
          onSuccess={refreshOrders}
        />
      )}

      {/* Delivery Saga Detail Dialog */}
      {selectedDeliverySagaId && selectedDeliverySaga && (
        <DeliverySagaDetailDialog
          sagaId={selectedDeliverySagaId}
          deliverySaga={selectedDeliverySaga}
          open={detailDialogOpen}
          onOpenChange={(open) => {
            setDetailDialogOpen(open);
            if (!open) { setSelectedDeliverySagaId(null); setSelectedDeliverySaga(null); }
          }}
          onRefresh={refreshOrders}
        />
      )}
    </div>
  );
}