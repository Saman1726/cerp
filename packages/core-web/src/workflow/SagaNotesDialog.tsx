'use client';

import { useCallback, useEffect, useState } from 'react';
import type { SagaNote } from '@cerp/shared-types';
import { Modal } from '../components/ui/modal';
import { Button } from '../components/ui/button';
import { Textarea } from '../components/ui/textarea';

export interface SagaNotesDialogProps {
  onClose: () => void;
  sagaId: string;
  title?: string;
  onNoteAdded?: () => void;
}

/**
 * Saga-level notes (sagas have no individually-addressable step records to scope to - see
 * SagaNoteEntity's backend Javadoc). Mounted only while open (the caller renders it
 * conditionally), so the initial-load effect below runs exactly once per open, and `loading`
 * starting `true` needs no synchronous setState anywhere to show a spinner on that first render.
 */
export function SagaNotesDialog({ onClose, sagaId, title, onNoteAdded }: SagaNotesDialogProps) {
  const [notes, setNotes] = useState<SagaNote[]>([]);
  const [loading, setLoading] = useState(true);
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch(`/api/sagas/${sagaId}/notes`)
      .then((res) => {
        if (!res.ok) throw new Error('Failed to load notes');
        return res.json();
      })
      .then((data: SagaNote[]) => {
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
  }, [sagaId]);

  const fetchNotes = useCallback(async () => {
    const res = await fetch(`/api/sagas/${sagaId}/notes`);
    if (!res.ok) throw new Error('Failed to load notes');
    setNotes(await res.json());
  }, [sagaId]);

  const handleAdd = async () => {
    const trimmed = content.trim();
    if (!trimmed) return;
    setSubmitting(true);
    setError(null);
    try {
      const res = await fetch(`/api/sagas/${sagaId}/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ note: trimmed }),
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
    <Modal open onClose={onClose} title={title ?? 'Notes'}>
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
                <p className="text-sm whitespace-pre-wrap">{note.note}</p>
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
