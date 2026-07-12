import type { PurchaseOrderWithReceipts } from '@cerp/shared-types';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../components/ui/table';
import { Badge } from '../components/ui/badge';
import { GoodsReceiptForm } from './GoodsReceiptForm';
import type { RecordGoodsReceiptActionState } from './types';

export interface PurchaseOrderListProps {
  purchaseOrders: PurchaseOrderWithReceipts[];
  recordGoodsReceiptAction: (
    state: RecordGoodsReceiptActionState,
    formData: FormData,
  ) => Promise<RecordGoodsReceiptActionState>;
}

export function PurchaseOrderList({ purchaseOrders, recordGoodsReceiptAction }: PurchaseOrderListProps) {
  if (purchaseOrders.length === 0) {
    return <p className="py-6 text-center text-sm text-muted-foreground">No purchase orders yet.</p>;
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Vendor</TableHead>
          <TableHead>Description</TableHead>
          <TableHead>Status</TableHead>
          <TableHead className="text-right">Receipts</TableHead>
          <TableHead className="w-[280px]">Actions</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {purchaseOrders.map((po) => (
          <TableRow key={po.id}>
            <TableCell className="font-medium">{po.vendorPartnerName}</TableCell>
            <TableCell className="text-muted-foreground">{po.description}</TableCell>
            <TableCell>
              <Badge variant={po.status === 'received' ? 'default' : 'secondary'} className="capitalize">
                {po.status}
              </Badge>
            </TableCell>
            <TableCell className="text-right">{po.receipts.length}</TableCell>
            <TableCell>
              <GoodsReceiptForm purchaseOrderId={po.id} action={recordGoodsReceiptAction} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}