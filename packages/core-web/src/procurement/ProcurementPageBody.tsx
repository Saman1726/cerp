import type { ReactNode } from 'react';
import type { PurchaseOrderWithReceipts } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { PageHeader } from '../shell/PageHeader';
import { PurchaseOrderForm } from './PurchaseOrderForm';
import { PurchaseOrderList } from './PurchaseOrderList';
import type { RaisePurchaseOrderActionState, RecordGoodsReceiptActionState } from './types';

export interface ProcurementPageBodyProps {
  raisePurchaseOrderAction: (
    state: RaisePurchaseOrderActionState,
    formData: FormData,
  ) => Promise<RaisePurchaseOrderActionState>;
  recordGoodsReceiptAction: (
    state: RecordGoodsReceiptActionState,
    formData: FormData,
  ) => Promise<RecordGoodsReceiptActionState>;
  extraFields?: ReactNode;
}

export async function ProcurementPageBody({
  raisePurchaseOrderAction,
  recordGoodsReceiptAction,
  extraFields,
}: ProcurementPageBodyProps) {
  const purchaseOrders = await apiClient.get<PurchaseOrderWithReceipts[]>('/purchase-orders');

  return (
    <div>
      <PageHeader title="Procurement" description="Manage purchase orders and goods receipts." />
      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-base">Raise purchase order</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-3">
            <PurchaseOrderForm action={raisePurchaseOrderAction} />
            {extraFields}
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent className="p-0">
          <PurchaseOrderList
            purchaseOrders={purchaseOrders}
            recordGoodsReceiptAction={recordGoodsReceiptAction}
          />
        </CardContent>
      </Card>
    </div>
  );
}