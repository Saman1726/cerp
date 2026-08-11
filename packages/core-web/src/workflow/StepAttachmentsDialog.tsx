'use client';

import { useEffect, useState } from 'react';
import { FileText, Trash2 } from 'lucide-react';
import type { WorkflowInstanceStep, WorkflowStepAttachment } from '@cerp/shared-types';
import { Modal } from '../components/ui/modal';
import { Button } from '../components/ui/button';

export interface StepAttachmentsDialogProps {
  onClose: () => void;
  workflowId: string;
  step: WorkflowInstanceStep;
  /** Called after a successful delete, so the caller can refresh attachmentCount (e.g. to hide the paperclip icon once the last attachment is gone). */
  onAttachmentDeleted?: () => void;
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * Lists a step's photo/document attachments. The list endpoint resolves each attachment's
 * viewable `url` server-side (via document-service's presign-download), so this component never
 * needs to know the underlying object storage key.
 *
 * Mounted only while open (the caller renders it conditionally), so the load effect below runs
 * exactly once per open, and `loading` starting `true` needs no synchronous setState anywhere to
 * show a spinner on that first render.
 */
export function StepAttachmentsDialog({
  onClose,
  workflowId,
  step,
  onAttachmentDeleted,
}: StepAttachmentsDialogProps) {
  const [attachments, setAttachments] = useState<WorkflowStepAttachment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/workflows/${workflowId}/steps/${step.id}/attachments`)
      .then((res) => {
        if (!res.ok) throw new Error('Failed to load attachments');
        return res.json();
      })
      .then((data: WorkflowStepAttachment[]) => {
        if (cancelled) return;
        setAttachments(data);
        setLoading(false);
      })
      .catch(() => {
        if (cancelled) return;
        setError('Failed to load attachments');
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [workflowId, step.id]);

  const handleDelete = async (attachment: WorkflowStepAttachment) => {
    if (!confirm(`Delete "${attachment.fileName}"? This cannot be undone.`)) return;
    setDeletingId(attachment.id);
    try {
      const res = await fetch(
        `/api/workflows/${workflowId}/steps/${step.id}/attachments/${attachment.id}`,
        {
          method: 'DELETE',
        },
      );
      if (!res.ok) throw new Error('Failed to delete attachment');
      setAttachments((prev) => prev.filter((a) => a.id !== attachment.id));
      onAttachmentDeleted?.();
    } catch {
      alert('Failed to delete attachment');
    } finally {
      setDeletingId(null);
    }
  };

  const photos = attachments.filter((a) => a.kind === 'PHOTO');
  const documents = attachments.filter((a) => a.kind === 'DOCUMENT');

  return (
    <Modal open onClose={onClose} title={`Attachments — ${step.name}`} className="max-w-2xl">
      {loading ? (
        <p className="text-sm text-muted-foreground">Loading attachments...</p>
      ) : error ? (
        <p className="text-sm text-destructive">{error}</p>
      ) : attachments.length === 0 ? (
        <p className="text-sm text-muted-foreground">No attachments yet.</p>
      ) : (
        <div className="flex flex-col gap-4">
          {photos.length > 0 ? (
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
              {photos.map((photo) => (
                <div key={photo.id} className="group relative">
                  {photo.url ? (
                    <a href={photo.url} target="_blank" rel="noreferrer" className="block">
                      {/* Presigned, remote object-storage URL - not a next/image-optimizable static asset. */}
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={photo.url}
                        alt={photo.fileName}
                        className="h-24 w-full rounded-md border border-input object-cover"
                      />
                    </a>
                  ) : (
                    <div className="flex h-24 items-center justify-center rounded-md border border-input text-xs text-muted-foreground">
                      Preview unavailable
                    </div>
                  )}
                  <Button
                    type="button"
                    variant="destructive"
                    size="icon"
                    className="absolute top-1 right-1 size-6 opacity-0 transition-opacity group-hover:opacity-100"
                    disabled={deletingId === photo.id}
                    onClick={() => handleDelete(photo)}
                    title="Delete photo"
                  >
                    <Trash2 className="size-3" />
                  </Button>
                </div>
              ))}
            </div>
          ) : null}
          {documents.length > 0 ? (
            <div className="flex flex-col gap-2">
              {documents.map((doc) => (
                <div
                  key={doc.id}
                  className="flex items-center gap-2 rounded-md border border-input p-2 text-sm"
                >
                  {doc.url ? (
                    <a
                      href={doc.url}
                      target="_blank"
                      rel="noreferrer"
                      className="flex flex-1 items-center gap-2 hover:underline"
                    >
                      <FileText className="size-4 shrink-0 text-muted-foreground" />
                      <span className="flex-1 truncate">{doc.fileName}</span>
                      <span className="text-xs text-muted-foreground">
                        {formatBytes(doc.byteSize)}
                      </span>
                    </a>
                  ) : (
                    <>
                      <FileText className="size-4 shrink-0 text-muted-foreground" />
                      <span className="flex-1 truncate text-muted-foreground">{doc.fileName}</span>
                      <span className="text-xs text-muted-foreground">unavailable</span>
                    </>
                  )}
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="size-6 shrink-0 text-destructive hover:text-destructive"
                    disabled={deletingId === doc.id}
                    onClick={() => handleDelete(doc)}
                    title="Delete document"
                  >
                    <Trash2 className="size-3" />
                  </Button>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      )}
    </Modal>
  );
}
