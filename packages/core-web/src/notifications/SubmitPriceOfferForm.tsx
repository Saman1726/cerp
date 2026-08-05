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

interface SubmitPriceOfferFormProps {
  notification: NotificationResponse;
  onSuccess: () => void;
  onCancel: () => void;
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

export function SubmitPriceOfferForm({ notification, onSuccess, onCancel }: SubmitPriceOfferFormProps) {
  const { takeAction } = useNotifications();

  const payload = notification.payload ?? {};
  const tenderId = payload['tenderId'] as string | undefined;
  const biddingSagaId = payload['sagaId'] as string | undefined;
  const vendorId = payload['vendorId'] as string | undefined;
  const projectId = payload['projectId'] as string | undefined;
  const bidDeadline = payload['bidDeadline'];

  const [sagaDetails, setSagaDetails] = useState<SagaDetails | null>(null);
  const [totalPrice, setTotalPrice] = useState('');
  const [deliveryDate, setDeliveryDate] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('');
  const [description, setDescription] = useState('');
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
          // Bidding saga description is a fallback only
          description: bd['description'] as string | undefined,
        };

        // Prefer procurement saga for material, quantity, and description
        const procurementSagaId = bd['procurementSagaId'] as string | undefined;
        if (procurementSagaId) {
          try {
            const procSaga = await workflowApi.getSaga(procurementSagaId);
            const pd = procSaga.data ?? {};
            details.materialId = pd['materialId'] as string | undefined;
            details.quantity = pd['quantity'] as string | number | undefined;
            if (pd['description']) {
              details.description = pd['description'] as string;
            }
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!totalPrice || !deliveryDate || !paymentMethod) {
      setError('Total price, delivery date, and payment method are required.');
      return;
    }
    if (!tenderId || !biddingSagaId || !vendorId) {
      setError('Invitation data is incomplete. Please contact support.');
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
          biddingType: 'INVITED',
        }),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error((data as { message?: string }).message ?? `Submission failed (${res.status})`);
      }

      const data = await res.json() as { bid?: { id?: string } };
      await takeAction(notification.id, {
        bidSubmitted: true,
        bidId: data.bid?.id ?? null,
      });
      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit price offer.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-5">
      {/* Invitation context */}
      <div className="rounded-md border border-blue-100 bg-blue-50/60 p-4 space-y-3">
        <h4 className="text-xs font-semibold uppercase tracking-wide text-blue-700">Invitation Details</h4>
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

      {/* Price offer form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="space-y-1.5">
            <Label htmlFor="totalPrice">Total Price *</Label>
            <Input
              id="totalPrice"
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
            <Label htmlFor="deliveryDate">Delivery Date *</Label>
            <Input
              id="deliveryDate"
              type="date"
              value={deliveryDate}
              onChange={(e) => setDeliveryDate(e.target.value)}
              required
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="paymentMethod">Payment Method *</Label>
          <select
            id="paymentMethod"
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
          <Label htmlFor="offerDescription">Description</Label>
          <Textarea
            id="offerDescription"
            placeholder="Additional details about your offer…"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
          />
        </div>

        {error && (
          <p className="text-sm text-red-600">{error}</p>
        )}

        <div className="flex items-center gap-2">
          <Button type="submit" size="sm" disabled={submitting}>
            {submitting ? 'Submitting…' : 'Submit Offer'}
          </Button>
          <Button type="button" size="sm" variant="outline" onClick={onCancel} disabled={submitting}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
}
