'use client';

import type { Saga } from '@cerp/shared-types';
import { cn } from '../lib/utils';
import { SAGA_STATUS_LABELS } from './types';

const SUMMARY_STATUSES = [
  'RUNNING',
  'PAUSED',
  'COMPLETED',
  'FAILED',
  'COMPENSATING',
  'REQUIRES_MANUAL_INTERVENTION',
] as const;

const STATUS_DOT_COLORS: Record<(typeof SUMMARY_STATUSES)[number], string> = {
  RUNNING: 'bg-blue-500',
  PAUSED: 'bg-yellow-500',
  COMPLETED: 'bg-green-500',
  FAILED: 'bg-red-500',
  COMPENSATING: 'bg-yellow-500',
  REQUIRES_MANUAL_INTERVENTION: 'bg-orange-500',
};

export interface SagaStatusSummaryProps {
  sagas: Saga[];
  loading?: boolean;
  activeStatus?: string;
  onSelect: (status: string) => void;
  className?: string;
}

export function SagaStatusSummary({
  sagas,
  loading,
  activeStatus,
  onSelect,
  className,
}: SagaStatusSummaryProps) {
  return (
    <div
      className={cn(
        'flex flex-wrap items-center gap-2 rounded-md border bg-card px-3 py-2',
        className,
      )}
    >
      {SUMMARY_STATUSES.map((status) => {
        const count = sagas.filter((s) => s.status === status).length;
        const isActive = activeStatus === status;

        return (
          <button
            key={status}
            type="button"
            onClick={() => onSelect(status)}
            className={cn(
              'flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium transition-colors hover:bg-muted',
              isActive ? 'border-primary bg-primary/10 text-primary' : 'border-border text-foreground',
            )}
          >
            <span className={cn('size-1.5 shrink-0 rounded-full', STATUS_DOT_COLORS[status])} />
            {SAGA_STATUS_LABELS[status] ?? status}
            <span className="text-muted-foreground">{loading ? '…' : count}</span>
          </button>
        );
      })}
    </div>
  );
}
