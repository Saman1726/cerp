'use client';

import { useRef, useState } from 'react';
import { Camera, MoreHorizontal, Paperclip, StickyNote } from 'lucide-react';
import type { Saga, SagaAttachmentKind } from '@cerp/shared-types';
import { Button } from '../components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { cn } from '../lib/utils';
import { compressImage } from '../site-capture/image-compression';
import { enqueueSagaAttachment, processQueue } from './saga-attachments';
import { SagaAttachmentsDialog } from './SagaAttachmentsDialog';
import { SagaNotesDialog } from './SagaNotesDialog';

export interface SagaRowActionsProps {
  saga: Saga;
  /**
   * Needed to presign an upload; without it the add-photo/document actions are disabled.
   * projectId isn't guaranteed for every saga type (present for Bidding sagas from their first
   * step onward, absent for Delivery sagas) - see getSagaProjectId in ./types.
   */
  projectId?: string;
  onSagaUpdated?: () => void;
  /**
   * Called right after a photo/document is queued, so the caller can trigger a sync attempt.
   * The upload queue is one shared IndexedDB store across every saga row on the page, so a
   * caller rendering many rows (both real call sites do) should own a single
   * `useSagaAttachmentSync()` and pass its `syncNow` here, rather than each row polling/listening
   * for connectivity independently -- N independent listeners would race draining the same
   * queue. Defaults to a one-shot drain attempt for standalone use.
   */
  onAttachmentQueued?: () => void;
  className?: string;
}

export function SagaRowActions({
  saga,
  projectId,
  onSagaUpdated,
  onAttachmentQueued = () => void processQueue(),
  className,
}: SagaRowActionsProps) {
  const [notesOpen, setNotesOpen] = useState(false);
  const [attachmentsOpen, setAttachmentsOpen] = useState(false);
  const pickerInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const hasAttachments = (saga.attachmentCount ?? 0) > 0;
  const dialogTitleSuffix = `— ${saga.sagaType}`;

  async function handleFiles(fileList: FileList | null) {
    if (!fileList || fileList.length === 0 || !projectId) return;

    try {
      for (const file of Array.from(fileList)) {
        const isImage = file.type.startsWith('image/');
        const kind: SagaAttachmentKind = isImage ? 'PHOTO' : 'DOCUMENT';

        let blob: Blob = file;
        let contentType = file.type || 'application/octet-stream';
        if (isImage) {
          const compressed = await compressImage(file);
          blob = compressed.blob;
          contentType = compressed.contentType;
        }

        await enqueueSagaAttachment({
          sagaId: saga.sagaId,
          projectId,
          kind,
          capturedAt: new Date().toISOString(),
          originalFileName: file.name,
          contentType,
          byteSize: blob.size,
          blob,
        });
      }
      onAttachmentQueued();
    } catch {
      alert('Failed to add photo/document');
    }
  }

  return (
    <div className={cn('flex items-center gap-1', className)}>
      <Button
        type="button"
        variant="ghost"
        size="icon"
        title="Notes"
        onClick={() => setNotesOpen(true)}
      >
        <StickyNote className="size-4" />
      </Button>

      {hasAttachments ? (
        <Button
          type="button"
          variant="ghost"
          size="icon"
          title="View attachments"
          onClick={() => setAttachmentsOpen(true)}
        >
          <Paperclip className="size-4" />
        </Button>
      ) : null}

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button type="button" variant="ghost" size="icon" title="More actions">
            <MoreHorizontal className="size-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem disabled={!projectId} onClick={() => pickerInputRef.current?.click()}>
            <Paperclip className="size-4" />
            Add Photos or Documents
          </DropdownMenuItem>
          <DropdownMenuItem disabled={!projectId} onClick={() => cameraInputRef.current?.click()}>
            <Camera className="size-4" />
            Take Picture
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => setNotesOpen(true)}>
            <StickyNote className="size-4" />
            Add Note
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <input
        ref={pickerInputRef}
        type="file"
        accept="image/*,application/pdf"
        multiple
        className="sr-only"
        onChange={(e) => {
          void handleFiles(e.target.files);
          e.target.value = '';
        }}
      />
      <input
        ref={cameraInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="sr-only"
        onChange={(e) => {
          void handleFiles(e.target.files);
          e.target.value = '';
        }}
      />

      {notesOpen ? (
        <SagaNotesDialog
          onClose={() => setNotesOpen(false)}
          sagaId={saga.sagaId}
          title={`Notes ${dialogTitleSuffix}`}
          onNoteAdded={onSagaUpdated}
        />
      ) : null}
      {attachmentsOpen ? (
        <SagaAttachmentsDialog
          onClose={() => setAttachmentsOpen(false)}
          sagaId={saga.sagaId}
          title={`Attachments ${dialogTitleSuffix}`}
          onAttachmentDeleted={onSagaUpdated}
        />
      ) : null}
    </div>
  );
}
