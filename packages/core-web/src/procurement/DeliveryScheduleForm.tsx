'use client';

import { useState, useEffect } from 'react';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose,
} from '../components/ui/modal/dialog';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';

interface DeliveryScheduleFormProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  sagaId: string;
  vendorId?: string;
  availableQuantity: number;
  onSuccess: () => void;
}

export function DeliveryScheduleForm({
  open, onOpenChange, sagaId, vendorId, availableQuantity, onSuccess,
}: DeliveryScheduleFormProps) {
  const [formData, setFormData] = useState({
    scheduledDate: '', expectedArrivalTime: '', quantity: '', description: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => { if (open) setError(null); }, [open]);
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault(); setLoading(true); setError(null);
    const qty = Number(formData.quantity);
    if (isNaN(qty) || qty <= 0) { setError('Quantity must be positive'); setLoading(false); return; }
    if (qty > availableQuantity) { setError(`Quantity exceeds remaining PO quantity (${availableQuantity})`); setLoading(false); return; }
    try {
      const res = await fetch(`/api/vendors/${vendorId}/purchase-orders/${sagaId}/delivery-schedules`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ scheduledDate: formData.scheduledDate, quantity: qty, expectedArrivalTime: formData.expectedArrivalTime || undefined, description: formData.description || undefined }),
      });
      if (!res.ok) { const d = await res.json().catch(() => undefined); throw new Error(d?.details || 'Failed to create delivery schedule'); }
      setFormData({ scheduledDate: '', expectedArrivalTime: '', quantity: '', description: '' }); onSuccess();
    } catch (err) { setError(err instanceof Error ? err.message : 'Failed to create delivery schedule'); }
    finally { setLoading(false); }
  };
  const handleOpenChange = (n: boolean) => {
    if (!n) { setFormData({ scheduledDate: '', expectedArrivalTime: '', quantity: '', description: '' }); setError(null); }
    onOpenChange(n);
  };
  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader><DialogTitle>Create Delivery Schedule</DialogTitle></DialogHeader>
        <form onSubmit={handleSubmit}>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="ds-qty">Quantity <span className="text-destructive">*</span></Label>
              <Input id="ds-qty" type="number" min="1" max={availableQuantity} step="any" placeholder={`Max: ${availableQuantity}`} value={formData.quantity} onChange={(e) => setFormData(p => ({ ...p, quantity: e.target.value }))} required />
              <p className="text-xs text-muted-foreground">Max available: {availableQuantity}</p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="ds-date">Scheduled Date <span className="text-destructive">*</span></Label>
              <Input id="ds-date" type="date" value={formData.scheduledDate} onChange={(e) => setFormData(p => ({ ...p, scheduledDate: e.target.value }))} min={new Date().toISOString().split('T')[0]} required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="ds-time">Expected Arrival Time</Label>
              <Input id="ds-time" type="time" value={formData.expectedArrivalTime} onChange={(e) => setFormData(p => ({ ...p, expectedArrivalTime: e.target.value }))} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="ds-desc">Description</Label>
              <Textarea id="ds-desc" placeholder="Optional notes about this delivery" rows={3} maxLength={500} value={formData.description} onChange={(e) => setFormData(p => ({ ...p, description: e.target.value }))} />
            </div>
            {error && <p className="text-sm text-destructive bg-destructive/10 p-2 rounded">{error}</p>}
          </div>
          <DialogFooter>
            <DialogClose asChild><Button type="button" variant="outline">Cancel</Button></DialogClose>
            <Button type="submit" disabled={loading}>{loading ? 'Creating...' : 'Create Schedule'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
