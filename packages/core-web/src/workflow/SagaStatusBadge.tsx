import { Badge } from '../components/ui/badge';
import { SAGA_STATUS_COLORS, SAGA_STATUS_LABELS, STEP_STATUS_COLORS, STEP_STATUS_LABELS } from './types';
import type { SagaStatus } from '@cerp/shared-types';

interface StatusBadgeProps {
  status: SagaStatus | string;
  variant?: 'saga' | 'step';
}

export function SagaStatusBadge({ status, variant = 'saga' }: StatusBadgeProps) {
  const colors = variant === 'saga' ? SAGA_STATUS_COLORS : STEP_STATUS_COLORS;
  const labels = variant === 'saga' ? SAGA_STATUS_LABELS : STEP_STATUS_LABELS;

  return (
    <Badge
      variant="secondary"
      className={colors[status] || 'bg-gray-100 text-gray-800'}
    >
      {labels[status] || status}
    </Badge>
  );
}