'use client';

import { useEffect } from 'react';
import { cn } from '../../lib/utils';

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  className?: string;
  children: React.ReactNode;
}

export function Modal({ open, onClose, title, className, children }: ModalProps) {
  useEffect(() => {
    function onKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    if (open) {
      document.addEventListener('keydown', onKeyDown);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.style.overflow = '';
    };
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
    >
      <div
        className={cn(
          'w-full max-w-lg rounded-md border border-border bg-background shadow-lg',
          'animate-in fade-in-0 zoom-in-95',
          className,
        )}
        onClick={(e) => e.stopPropagation()}
      >
        {title ? (
          <div className="border-b px-4 py-3">
            <h2 className="text-base font-semibold">{title}</h2>
          </div>
        ) : null}
        <div className="p-4">{children}</div>
      </div>
    </div>
  );
}