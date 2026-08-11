'use client';

import { useCallback, useEffect, useState } from 'react';
import type { WorkflowInstanceStep, WorkflowNote } from '@cerp/shared-types';
import { Modal } from '../components/ui/modal';
import { Button } from '../components/ui/button';
import { Textarea } from '../components/ui/textarea';

export interface StepNotesDialogProps {
  onClose: () => void;
  workflowId: string;
  step: WorkflowInstanceStep;
  onNoteAdded?: () => void;
}

/**
 * Mounted only while open (the caller renders it conditionally), so the initial-load effect
 * below runs exactly once per open, and `loading` starting `true` needs no synchronous
 * setState anywhere to show a spinner on that first render.
 */
export function StepNotesDialog({ onClose, workflowId, step, onNoteAdded }: StepNotesDialogProps) {
  const [notes, setNotes] = useState<WorkflowNote[]>([]);
  const [loading, setLoading] = useState(true);
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/workflows/${workflowId}/steps/${step.id}/notes`)
      .then((res) => {
        if (!res.ok) throw new Error('Failed to load notes');
        return res.json();
      })
      .then((data: WorkflowNote[]) => {
        if (cancelled) return;
        setNotes(data);
        setLoading(false);
      })
      .catch(() => {
        if (cancelled) return;
        setError('Failed to load notes');
        setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [workflowId, step.id]);

  const fetchNotes = useCallback(async () => {
    const res = await fetch(`/api/workflows/${workflowId}/steps/${step.id}/notes`);
    if (!res.ok) throw new Error('Failed to load notes');
    setNotes(await res.json());
  }, [workflowId, step.id]);

  const handleAdd = async () => {
    const trimmed = content.trim();
    if (!trimmed) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch(`/api/workflows/${workflowId}/steps/${step.id}/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: trimmed }),
      });
      if (!res.ok) throw new Error('Failed to add note');
      setContent('');
      await fetchNotes();
      onNoteAdded?.();
    } catch {
      setError('Failed to add note');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal open onClose={onClose} title={`Notes — ${step.name}`}>
      <div className="flex flex-col gap-3">
        <div className="flex gap-2">
          <Textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Add a note..."
            className="flex-1"
            rows={2}
          />
          <Button onClick={handleAdd} size="sm" disabled={!content.trim() || submitting}>
            Add
          </Button>
        </div>
        {error ? <p className="text-sm text-destructive">{error}</p> : null}
        <div className="flex flex-col gap-2">
          {loading ? (
            <p className="text-sm text-muted-foreground">Loading notes...</p>
          ) : notes.length === 0 ? (
            <p className="text-sm text-muted-foreground">No notes yet.</p>
          ) : (
            notes.map((note) => (
              <div key={note.id} className="rounded-md border p-2">
                <p className="text-sm whitespace-pre-wrap">{note.content}</p>
                <p className="mt-1 text-xs text-muted-foreground">
                  {new Date(note.createdAt).toLocaleString()}
                </p>
              </div>
            ))
          )}
        </div>
      </div>
    </Modal>
  );
}
