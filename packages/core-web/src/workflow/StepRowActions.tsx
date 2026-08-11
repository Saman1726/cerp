'use client';

import { useRef, useState } from 'react';
import { Camera, MoreHorizontal, Paperclip, StickyNote } from 'lucide-react';
import type { WorkflowInstanceStep, WorkflowStepAttachmentKind } from '@cerp/shared-types';
import { Button } from '../components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { cn } from '../lib/utils';
import { compressImage } from '../site-capture/image-compression';
import { enqueueStepAttachment, processQueue } from './step-attachments';
import { StepAttachmentsDialog } from './StepAttachmentsDialog';
import { StepNotesDialog } from './StepNotesDialog';

export interface StepRowActionsProps {
  workflowId: string;
  /** Needed to presign an upload; without it the add-photo/document actions are disabled. */
  projectId?: string;
  subProjectId?: string;
  step: WorkflowInstanceStep;
  onStepUpdated?: () => void;
  /**
   * Called right after a photo/document is queued, so the caller can trigger a sync attempt.
   * The upload queue is one shared IndexedDB store across every step row on the page, so a
   * caller rendering many rows (both real call sites do) should own a single
   * `useAttachmentSync()` and pass its `syncNow` here, rather than each row polling/listening
   * for connectivity independently -- N independent listeners would race draining the same
   * queue. Defaults to a one-shot drain attempt for standalone use.
   */
  onAttachmentQueued?: () => void;
  className?: string;
}

export function StepRowActions({
  workflowId,
  projectId,
  subProjectId,
  step,
  onStepUpdated,
  onAttachmentQueued = () => void processQueue(),
  className,
}: StepRowActionsProps) {
  const [notesOpen, setNotesOpen] = useState(false);
  const [attachmentsOpen, setAttachmentsOpen] = useState(false);
  const pickerInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  const hasAttachments = (step.attachmentCount ?? 0) > 0;

  async function handleFiles(fileList: FileList | null) {
    if (!fileList || fileList.length === 0 || !projectId) return;

    try {
      for (const file of Array.from(fileList)) {
        const isImage = file.type.startsWith('image/');
        const kind: WorkflowStepAttachmentKind = isImage ? 'PHOTO' : 'DOCUMENT';

        let blob: Blob = file;
        let contentType = file.type || 'application/octet-stream';
        if (isImage) {
          const compressed = await compressImage(file);
          blob = compressed.blob;
          contentType = compressed.contentType;
        }

        await enqueueStepAttachment({
          workflowId,
          stepId: step.id,
          projectId,
          subProjectId,
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
        <StepNotesDialog
          onClose={() => setNotesOpen(false)}
          workflowId={workflowId}
          step={step}
          onNoteAdded={onStepUpdated}
        />
      ) : null}
      {attachmentsOpen ? (
        <StepAttachmentsDialog
          onClose={() => setAttachmentsOpen(false)}
          workflowId={workflowId}
          step={step}
          onAttachmentDeleted={onStepUpdated}
        />
      ) : null}
    </div>
  );
}
