'use client';

import { Button } from '../components/ui/button';
import type { Saga } from './types';

interface ProcurementActionsProps {
  saga: Saga;
  onAction: (command: string, reason?: string) => Promise<void>;
  rejectionReason: string;
  onSetRejectionReason: (reason: string) => void;
  loading: boolean;
}

export function ProcurementActions({ saga, onAction, loading }: ProcurementActionsProps) {
  const effectiveStep = saga.currentStep || '';
  const isComplete = effectiveStep === 'CloseProcurement';

  if (isComplete) {
    return <p className="text-muted-foreground text-sm">This procurement has been closed.</p>;
  }

  return (
    <>
      {/* CreateProcurementRequest → Submit for Approval */}
      {effectiveStep === 'CreateProcurementRequest' && (
        <div>
          <p className="text-sm font-medium mb-2">Create Procurement Request</p>
          <Button
            onClick={() => onAction('SubmitForApproval')}
            disabled={loading}
          >
            {loading ? 'Submitting...' : 'Submit for Approval'}
          </Button>
        </div>
      )}

      {/* SubmitForApproval → Approve/Reject */}
      {effectiveStep === 'SubmitForApproval' && (
        <div className="space-y-2">
          <p className="text-sm font-medium">Submit for Approval</p>
          <div className="flex gap-2">
            <Button
              onClick={() => onAction('ApproveRequest')}
              disabled={loading}
            >
              {loading ? 'Approving...' : 'Approve Request'}
            </Button>
          </div>
        </div>
      )}

      {/* ApproveRequest → Issue Purchase Order */}
      {effectiveStep === 'ApproveRequest' && (
        <div>
          <p className="text-sm font-medium mb-2">Request Approved</p>
          <p className="text-sm text-muted-foreground mb-2">Issue a purchase order to vendor.</p>
          <Button
            onClick={() => onAction('IssuePurchaseOrder')}
            disabled={loading}
          >
            {loading ? 'Issuing PO...' : 'Issue Purchase Order'}
          </Button>
        </div>
      )}

      {/* IssuePurchaseOrder → Receive Delivery */}
      {effectiveStep === 'IssuePurchaseOrder' && (
        <div>
          <p className="text-sm font-medium mb-2">Purchase Order Issued</p>
          <p className="text-sm text-muted-foreground mb-2">Await delivery from vendor.</p>
          <Button
            onClick={() => onAction('ReceiveDelivery')}
            disabled={loading}
          >
            {loading ? 'Receiving...' : 'Receive Delivery'}
          </Button>
        </div>
      )}

      {/* ReceiveDelivery → Confirm Delivery */}
      {effectiveStep === 'ReceiveDelivery' && (
        <div>
          <p className="text-sm font-medium mb-2">Receive Delivery</p>
          <Button
            onClick={() => onAction('ConfirmDelivery')}
            disabled={loading}
          >
            {loading ? 'Confirming...' : 'Confirm Delivery'}
          </Button>
        </div>
      )}

      {/* ConfirmDelivery → Close Procurement */}
      {effectiveStep === 'ConfirmDelivery' && (
        <div>
          <p className="text-sm font-medium mb-2">Confirm Delivery</p>
          <Button
            onClick={() => onAction('CloseProcurement')}
            disabled={loading}
          >
            {loading ? 'Closing...' : 'Close Procurement'}
          </Button>
        </div>
      )}
    </>
  );
}