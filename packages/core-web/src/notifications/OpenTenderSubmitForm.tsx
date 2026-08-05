'use client';

import { useState, useEffect } from 'react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { useNotifications } from './NotificationProvider';
import { workflowApi } from '../workflow/api';
import type { NotificationResponse } from '@cerp/shared-types';

const PAYMENT_METHODS = [
  { value: 'BANK_TRANSFER', label: 'Bank Transfer' },
  { value: 'LETTER_OF_CREDIT', label: 'Letter of Credit' },
  { value: 'CASH', label: 'Cash' },
  { value: 'INSTALLMENTS', label: 'Installments' },
] as const;

const OPEN_TENDER_COST = 5;

export interface TenderEntry {
  tenderId: string;
  sagaId: string;
  projectId?: string;
  bidDeadline?: unknown;
}

export interface OpenTenderSubmitFormProps {
  /** Notification from the notification feed (if the vendor received one via subscription). */
  notification?: NotificationResponse;
  /** Direct tender data when rendered from the channel page without a notification. */
  tenderData?: TenderEntry;
  onSuccess: () => void;
  onCancel: () => void;
}

interface WalletInfo {
  availableBids: number;
  entityId?: string;
}

interface SagaDetails {
  description?: string;
  materialId?: string;
  quantity?: string | number;
  sagaName?: string;
  projectName?: string;
}

function formatDeadline(value: unknown): string {
  if (!value) return '—';
  const d = new Date(String(value));
  if (isNaN(d.getTime())) return String(value);
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
}

export function OpenTenderSubmitForm({ notification, tenderData, onSuccess, onCancel }: OpenTenderSubmitFormProps) {
  const { takeAction } = useNotifications();

  // Resolve tender context from either source
  const payload = notification?.payload ?? {};
  const tenderId = tenderData?.tenderId ?? (payload['tenderId'] as string | undefined);
  const biddingSagaId = tenderData?.sagaId ?? (payload['sagaId'] as string | undefined);
  const projectId = tenderData?.projectId ?? (payload['projectId'] as string | undefined);
  const bidDeadline = tenderData?.bidDeadline ?? payload['bidDeadline'];

  const [sagaDetails, setSagaDetails] = useState<SagaDetails | null>(null);
  const [wallet, setWallet] = useState<WalletInfo | null>(null);
  const [totalPrice, setTotalPrice] = useState('');
  const [deliveryDate, setDeliveryDate] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('');
  const [description, setDescription] = useState('');
  const [costConfirmed, setCostConfirmed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!biddingSagaId) return;
    (async () => {
      try {
        const biddingSaga = await workflowApi.getSaga(biddingSagaId);
        const bd = biddingSaga.data ?? {};
        const details: SagaDetails = {
          sagaName: bd['sagaName'] as string | undefined,
          projectName: bd['projectName'] as string | undefined,
          description: bd['description'] as string | undefined,
        };
        const procurementSagaId = bd['procurementSagaId'] as string | undefined;
        if (procurementSagaId) {
          try {
            const procSaga = await workflowApi.getSaga(procurementSagaId);
            const pd = procSaga.data ?? {};
            details.materialId = pd['materialId'] as string | undefined;
            details.quantity = pd['quantity'] as string | number | undefined;
            if (pd['description']) details.description = pd['description'] as string;
          } catch {
            // Procurement saga unavailable — fall back to bidding saga fields
          }
        }
        setSagaDetails(details);
      } catch {
        // Non-critical — form still works without saga details
      }
    })();
  }, [biddingSagaId]);

  useEffect(() => {
    (async () => {
      try {
        const res = await fetch('/api/bidding?_path=wallet/balance&entityType=VENDOR');
        if (res.ok) {
          const data = await res.json() as { availableBids?: number; entityId?: string };
          setWallet({ availableBids: data.availableBids ?? 0, entityId: data.entityId });
        }
      } catch {
        // Wallet info unavailable — form still works but bid balance won't show
      }
    })();
  }, []);

  const hasEnoughBids = wallet === null || wallet.availableBids >= OPEN_TENDER_COST;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!costConfirmed) {
      setError(`Please confirm that you accept the ${OPEN_TENDER_COST}-bid participation cost.`);
      return;
    }
    if (!totalPrice || !deliveryDate || !paymentMethod) {
      setError('Total price, delivery date, and payment method are required.');
      return;
    }
    if (!tenderId || !biddingSagaId) {
      setError('Tender data is incomplete. Please contact support.');
      return;
    }

    const vendorId = wallet?.entityId;
    if (!vendorId) {
      setError('Could not resolve your vendor profile. Please refresh and try again.');
      return;
    }

    setSubmitting(true);
    try {
      const price = parseFloat(totalPrice);
      const res = await fetch('/api/bidding?_path=submissions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tenderId,
          biddingSagaId,
          vendorId,
          price,
          quantity: 1,
          totalPrice: price,
          timeline: deliveryDate,
          notes: description || null,
          paymentMethod,
          biddingType: 'OPEN_TENDER',
        }),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error((data as { message?: string }).message ?? `Submission failed (${res.status})`);
      }

      const data = await res.json() as { bid?: { id?: string } };
      // Mark the notification ACTION_TAKEN only when one exists (subscription-driven flow)
      if (notification) {
        await takeAction(notification.id, {
          bidSubmitted: true,
          bidId: data.bid?.id ?? null,
          biddingType: 'OPEN_TENDER',
        });
      }
      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit price offer.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-5">
      {/* Participation cost banner */}
      <div className="rounded-md border border-amber-200 bg-amber-50 p-3 flex items-start gap-2">
        <span className="mt-0.5 text-base">⚡</span>
        <div className="text-sm text-amber-800">
          <span className="font-semibold">Open Tender — costs {OPEN_TENDER_COST} bids to participate</span>
          {wallet !== null && (
            <span className="ml-1 text-amber-700">
              {'(your balance: '}
              <span
                className={
                  wallet.availableBids < OPEN_TENDER_COST
                    ? 'font-semibold text-red-600'
                    : 'font-semibold'
                }
              >
                {wallet.availableBids}
              </span>
              {' bids)'}
            </span>
          )}
        </div>
      </div>

      {/* Tender context */}
      <div className="rounded-md border border-blue-100 bg-blue-50/60 p-4 space-y-3">
        <h4 className="text-xs font-semibold uppercase tracking-wide text-blue-700">Open Tender Details</h4>
        <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-2 text-sm">
          <div>
            <dt className="text-xs font-medium text-gray-500">Tender ID</dt>
            <dd className="mt-0.5 font-mono text-gray-900">
              {tenderId ? tenderId.slice(0, 8) + '…' : '—'}
            </dd>
          </div>

          <div>
            <dt className="text-xs font-medium text-gray-500">Project</dt>
            <dd className="mt-0.5 text-gray-900">
              {sagaDetails?.projectName
                ? sagaDetails.projectName
                : projectId
                  ? <span className="font-mono">{String(projectId).slice(0, 8)}…</span>
                  : '—'}
            </dd>
          </div>

          <div>
            <dt className="text-xs font-medium text-gray-500">Bid Deadline</dt>
            <dd className="mt-0.5 text-gray-900">{formatDeadline(bidDeadline)}</dd>
          </div>

          {sagaDetails?.sagaName && (
            <div>
              <dt className="text-xs font-medium text-gray-500">Tender Name</dt>
              <dd className="mt-0.5 text-gray-900">{sagaDetails.sagaName}</dd>
            </div>
          )}

          {sagaDetails?.materialId && (
            <div>
              <dt className="text-xs font-medium text-gray-500">Material</dt>
              <dd className="mt-0.5 font-mono text-gray-900">
                {sagaDetails.materialId.length > 8
                  ? sagaDetails.materialId.slice(0, 8) + '…'
                  : sagaDetails.materialId}
              </dd>
            </div>
          )}

          {sagaDetails?.quantity != null && (
            <div>
              <dt className="text-xs font-medium text-gray-500">Quantity</dt>
              <dd className="mt-0.5 text-gray-900">
                {typeof sagaDetails.quantity === 'number'
                  ? sagaDetails.quantity.toLocaleString()
                  : sagaDetails.quantity}
              </dd>
            </div>
          )}

          {sagaDetails?.description && (
            <div className="sm:col-span-2">
              <dt className="text-xs font-medium text-gray-500">Description</dt>
              <dd className="mt-0.5 text-gray-700 leading-relaxed">{sagaDetails.description}</dd>
            </div>
          )}
        </dl>
      </div>

      {!hasEnoughBids && (
        <div className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          Insufficient bids. You need {OPEN_TENDER_COST} to participate but only have{' '}
          {wallet?.availableBids ?? 0}. Please top up your bid wallet.
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <Label htmlFor="ot-totalPrice">Total Price *</Label>
            <Input
              id="ot-totalPrice"
              type="number"
              min="0"
              step="0.01"
              placeholder="0.00"
              value={totalPrice}
              onChange={(e) => setTotalPrice(e.target.value)}
              required
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="ot-deliveryDate">Delivery Date *</Label>
            <Input
              id="ot-deliveryDate"
              type="date"
              value={deliveryDate}
              onChange={(e) => setDeliveryDate(e.target.value)}
              required
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="ot-paymentMethod">Payment Method *</Label>
          <select
            id="ot-paymentMethod"
            value={paymentMethod}
            onChange={(e) => setPaymentMethod(e.target.value)}
            required
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            <option value="">Select a payment method…</option>
            {PAYMENT_METHODS.map(({ value, label }) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="ot-description">Description</Label>
          <Textarea
            id="ot-description"
            placeholder="Additional details about your offer…"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
          />
        </div>

        <label className="flex items-start gap-2 cursor-pointer text-sm">
          <input
            type="checkbox"
            checked={costConfirmed}
            onChange={(e) => setCostConfirmed(e.target.checked)}
            className="mt-0.5 h-4 w-4 rounded border-gray-300 text-blue-600"
          />
          <span>
            I understand submitting this offer will deduct{' '}
            <strong>{OPEN_TENDER_COST} bids</strong> from my wallet.
          </span>
        </label>

        {error && <p className="text-sm text-red-600">{error}</p>}

        <div className="flex items-center gap-2">
          <Button type="submit" size="sm" disabled={submitting || !hasEnoughBids}>
            {submitting ? 'Submitting…' : `Submit Offer (−${OPEN_TENDER_COST} bids)`}
          </Button>
          <Button
            type="button"
            size="sm"
            variant="outline"
            onClick={onCancel}
            disabled={submitting}
          >
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
}
