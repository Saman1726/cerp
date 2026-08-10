'use client';

import { useEffect, useRef, useState, type ChangeEvent, type FormEvent } from 'react';
import type { SiteCaptureKind } from '@cerp/shared-types';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { cn } from '../lib/utils';
import { compressImage, type CompressedImage } from './image-compression';
import { enqueueCapture } from './offline-queue';
import { useBackgroundSync, type UseBackgroundSyncResult } from './use-background-sync';

const KIND_LABELS: Record<SiteCaptureKind, string> = {
  PROGRESS_PHOTO: 'Site progress photo',
  DELIVERY_INVOICE: 'Delivery invoice',
  WEIGHT_CERTIFICATE: 'Weight certificate',
};

export interface SiteCaptureFormProps {
  projectId: string;
  subProjectId?: string;
  className?: string;
}

function QueueStatus({ sync }: { sync: UseBackgroundSyncResult }) {
  return (
    <div className="flex items-center justify-between rounded-md border border-input bg-muted/40 px-3 py-2 text-sm">
      <span className="text-muted-foreground">
        {sync.pendingCount === 0
          ? 'All captures synced'
          : `${sync.pendingCount} capture${sync.pendingCount === 1 ? '' : 's'} waiting to upload`}
        {sync.isSyncing && ' · syncing…'}
      </span>
      {sync.lastError && (
        <Button type="button" variant="outline" size="sm" onClick={() => void sync.syncNow()}>
          Retry
        </Button>
      )}
    </div>
  );
}

/**
 * Field-capture form: take a photo (or invoice/weight-certificate snapshot),
 * compress it client-side, and queue it locally. Works fully offline - the
 * capture is safely in IndexedDB the moment "Save capture" is tapped, and
 * useBackgroundSync drains it to the backend whenever a connection exists.
 */
export function SiteCaptureForm({ projectId, subProjectId, className }: SiteCaptureFormProps) {
  const sync = useBackgroundSync();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [kind, setKind] = useState<SiteCaptureKind>('PROGRESS_PHOTO');
  const [notes, setNotes] = useState('');
  const [compressed, setCompressed] = useState<CompressedImage | null>(null);
  const [originalFileName, setOriginalFileName] = useState<string | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isCompressing, setIsCompressing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [justQueued, setJustQueued] = useState(false);

  // Revokes the *previous* preview URL right before a new one replaces it,
  // and the current one on unmount - createObjectURL blobs otherwise leak
  // for the life of the page.
  useEffect(() => {
    if (!previewUrl) return;
    return () => URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    setError(null);
    setIsCompressing(true);
    setJustQueued(false);
    try {
      const result = await compressImage(file);
      setCompressed(result);
      setOriginalFileName(file.name);
      setPreviewUrl(URL.createObjectURL(result.blob));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to process photo');
    } finally {
      setIsCompressing(false);
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (!compressed || !originalFileName) return;

    await enqueueCapture({
      projectId,
      subProjectId,
      kind,
      notes: notes.trim() || undefined,
      capturedAt: new Date().toISOString(),
      originalFileName,
      contentType: compressed.contentType,
      byteSize: compressed.byteSize,
      blob: compressed.blob,
    });

    setCompressed(null);
    setOriginalFileName(null);
    setPreviewUrl(null);
    setNotes('');
    setJustQueued(true);
    void sync.syncNow();
  }

  const canSubmit = compressed !== null && !isCompressing;

  return (
    <Card className={cn('w-full max-w-md', className)}>
      <CardHeader>
        <CardTitle>New site capture</CardTitle>
        <CardDescription>Saved on this device instantly, even with no signal.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <QueueStatus sync={sync} />

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="capture-kind">Type</Label>
            <select
              id="capture-kind"
              value={kind}
              onChange={(e) => setKind(e.target.value as SiteCaptureKind)}
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            >
              {Object.entries(KIND_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="capture-photo">Photo</Label>
            <input
              ref={fileInputRef}
              id="capture-photo"
              type="file"
              accept="image/*"
              capture="environment"
              onChange={handleFileChange}
              className="sr-only"
            />

            {previewUrl ? (
              // eslint-disable-next-line @next/next/no-img-element -- transient local blob preview, not an optimizable remote asset
              <img
                src={previewUrl}
                alt="Capture preview"
                className="h-48 w-full rounded-md border border-input object-cover"
              />
            ) : (
              <Button
                type="button"
                variant="outline"
                className="h-48 w-full flex-col gap-1"
                onClick={() => fileInputRef.current?.click()}
                disabled={isCompressing}
              >
                {isCompressing ? 'Processing photo…' : 'Take photo'}
              </Button>
            )}

            {compressed && previewUrl && (
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>
                  {Math.round(compressed.byteSize / 1024)} KB ({compressed.width}×{compressed.height}, down from{' '}
                  {Math.round(compressed.originalByteSize / 1024)} KB)
                </span>
                <button
                  type="button"
                  className="underline"
                  onClick={() => fileInputRef.current?.click()}
                >
                  Retake
                </button>
              </div>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="capture-notes">Notes</Label>
            <Textarea
              id="capture-notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Optional context for this capture"
            />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}
          {justQueued && <p className="text-sm text-muted-foreground">Saved - will upload automatically.</p>}

          <Button type="submit" disabled={!canSubmit} className="w-full">
            Save capture
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
