import { ProcurementPageBody } from '@cerp/core-web/procurement';
import { raisePurchaseOrderAction, recordGoodsReceiptAction } from './actions';

export default function ProcurementPage() {
  return (
    <ProcurementPageBody
      raisePurchaseOrderAction={raisePurchaseOrderAction}
      recordGoodsReceiptAction={recordGoodsReceiptAction}
    />
  );
}