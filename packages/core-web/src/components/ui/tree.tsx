'use client';

import { useState, type ReactNode } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import { cn } from '../../lib/utils';

export interface TreeProps {
  children: ReactNode;
  isEmpty?: boolean;
  emptyMessage?: string;
  className?: string;
}

export function Tree({ children, isEmpty, emptyMessage, className }: TreeProps) {
  if (isEmpty) {
    return (
      <p className="py-6 text-center text-sm text-muted-foreground">
        {emptyMessage ?? 'Nothing here yet.'}
      </p>
    );
  }
  return <div className={cn('flex flex-col gap-0.5', className)}>{children}</div>;
}

export interface TreeNodeProps {
  /** Primary row label. Clicking it opens the detail popup via onSelect, or toggles expansion if onSelect is omitted. */
  label: ReactNode;
  icon?: ReactNode;
  /** Trailing badges/text shown next to the label. */
  meta?: ReactNode;
  /** Row-level controls (add child, overflow menu). Rendered outside the label button, so they never trigger onSelect. */
  actions?: ReactNode;
  /** Child nodes. Omit entirely for leaf nodes (no chevron is shown). */
  children?: ReactNode;
  defaultOpen?: boolean;
  onSelect?: () => void;
  /** Fires the first time (and every time) this node transitions from closed to open — use it to lazy-load children. */
  onExpand?: () => void;
  className?: string;
}

export function TreeNode({
  label,
  icon,
  meta,
  actions,
  children,
  defaultOpen = false,
  onSelect,
  onExpand,
  className,
}: TreeNodeProps) {
  const [open, setOpen] = useState(defaultOpen);
  const hasChildren = children !== undefined && children !== null && children !== false;

  const toggle = () => {
    const next = !open;
    setOpen(next);
    if (next) onExpand?.();
  };

  const handleLabelClick = () => {
    if (onSelect) {
      onSelect();
    } else if (hasChildren) {
      toggle();
    }
  };

  return (
    <div className={cn('flex flex-col', className)}>
      <div className="group flex items-center gap-1.5 rounded-md px-1.5 py-1 hover:bg-muted/60">
        <button
          type="button"
          onClick={toggle}
          aria-expanded={open}
          tabIndex={hasChildren ? 0 : -1}
          className={cn(
            'flex size-5 shrink-0 items-center justify-center rounded text-muted-foreground',
            !hasChildren && 'invisible',
          )}
        >
          {hasChildren ? (
            open ? <ChevronDown className="size-3.5" /> : <ChevronRight className="size-3.5" />
          ) : null}
        </button>
        {icon ? <span className="flex shrink-0 items-center text-muted-foreground">{icon}</span> : null}
        <button
          type="button"
          onClick={handleLabelClick}
          className="flex min-w-0 flex-1 items-center gap-2 truncate text-left text-sm font-medium"
        >
          <span className="truncate">{label}</span>
          {meta ? (
            <span className="flex shrink-0 items-center gap-1 text-xs font-normal text-muted-foreground">
              {meta}
            </span>
          ) : null}
        </button>
        {actions ? (
          <div className="flex shrink-0 items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100 group-focus-within:opacity-100">
            {actions}
          </div>
        ) : null}
      </div>
      {hasChildren && open ? (
        <div className="ml-[10px] flex flex-col border-l pl-3">{children}</div>
      ) : null}
    </div>
  );
}
