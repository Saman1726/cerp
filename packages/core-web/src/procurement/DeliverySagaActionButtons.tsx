'use client';

import { useState } from 'react';
import { Button } from '../components/ui/button';
import {
  Truck,
  PackageCheck,
  Wrench,
  CheckCircle,
  AlertTriangle,
  RotateCcw,
} from 'lucide-react';

export const DELIVERY_SAGA_STEPS = {
  SCHEDULE_DELIVERY: 'ScheduleDelivery',
  START_TRANSIT: 'StartTransit',
  DELIVERY_ARRIVED: 'DeliveryArrived',
  START_UNLOADING: 'StartUnloading',
  CONFIRM_DELIVERY: 'ConfirmDelivery',
  RAISE_DISCREPANCY: 'RaiseDiscrepancy',
  RESOLVE_DISCREPANCY: 'ResolveDiscrepancy',
  CLOSE_DELIVERY: 'CloseDelivery',
} as const;

export interface DeliverySagaActionButtonsProps {
  sagaId: string;
  currentStep?: string;
  status?: string;
  quantity?: number;
  onStepCompleted?: () => void;
}

function isTerminalStatus(status?: string): boolean {
  const s = (status ?? '').toUpperCase();
  return s === 'COMPLETED' || s === 'CLOSED' || s === 'CANCELED' || s === 'CANCELLED';
}

function isFailedStatus(status?: string): boolean {
  return (status ?? '').toUpperCase() === 'FAILED';
}

function isTerminalStep(step?: string): boolean {
  return step === DELIVERY_SAGA_STEPS.CLOSE_DELIVERY;
}

interface DeliveryActionConfig {
  key?: string;
  command: string;
  label: string;
  description: string;
  needsConfirmation: boolean;
}

function getAvailableActions(currentStep?: string, status?: string): string[] {
  const actions: string[] = [];
  const step = currentStep ?? '';

  if (isFailedStatus(status)) {
    actions.push(DELIVERY_SAGA_STEPS.START_TRANSIT);
    return actions;
  }

  switch (step) {
    case DELIVERY_SAGA_STEPS.START_TRANSIT:
      actions.push(DELIVERY_SAGA_STEPS.DELIVERY_ARRIVED);
      break;
    case DELIVERY_SAGA_STEPS.DELIVERY_ARRIVED:
      actions.push(DELIVERY_SAGA_STEPS.START_UNLOADING);
      break;
    case DELIVERY_SAGA_STEPS.START_UNLOADING:
      actions.push(DELIVERY_SAGA_STEPS.CONFIRM_DELIVERY, DELIVERY_SAGA_STEPS.RAISE_DISCREPANCY);
      break;
    case DELIVERY_SAGA_STEPS.CONFIRM_DELIVERY:
      actions.push(DELIVERY_SAGA_STEPS.CLOSE_DELIVERY);
      break;
    case DELIVERY_SAGA_STEPS.RAISE_DISCREPANCY:
      actions.push(DELIVERY_SAGA_STEPS.RESOLVE_DISCREPANCY);
      break;
    case DELIVERY_SAGA_STEPS.RESOLVE_DISCREPANCY:
      actions.push(DELIVERY_SAGA_STEPS.CLOSE_DELIVERY);
      break;
    default:
      actions.push(DELIVERY_SAGA_STEPS.START_TRANSIT);
      break;
  }

  return actions;
}

const ACTION_CONFIGS: Record<string, DeliveryActionConfig> = {
  [DELIVERY_SAGA_STEPS.START_TRANSIT]: {
    command: 'StartTransit',
    label: 'Start Transit',
    description: 'Mark delivery as in transit',
    needsConfirmation: true,
  },
  [DELIVERY_SAGA_STEPS.DELIVERY_ARRIVED]: {
    command: 'DeliveryArrived',
    label: 'Delivery Arrived',
    description: 'Confirm delivery has arrived at destination',
    needsConfirmation: true,
  },
  [DELIVERY_SAGA_STEPS.START_UNLOADING]: {
    command: 'StartUnloading',
    label: 'Start Unloading',
    description: 'Begin unloading materials',
    needsConfirmation: false,
  },
  [DELIVERY_SAGA_STEPS.CONFIRM_DELIVERY]: {
    command: 'ConfirmDelivery',
    label: 'Confirm Delivery',
    description: 'Confirm all materials received',
    needsConfirmation: true,
  },
  [DELIVERY_SAGA_STEPS.RAISE_DISCREPANCY]: {
    command: 'RaiseDiscrepancy',
    label: 'Raise Discrepancy',
    description: 'Report missing or damaged items',
    needsConfirmation: true,
  },
  [DELIVERY_SAGA_STEPS.RESOLVE_DISCREPANCY]: {
    command: 'ResolveDiscrepancy',
    label: 'Resolve Discrepancy',
    description: 'Mark discrepancy as resolved',
    needsConfirmation: true,
  },
  [DELIVERY_SAGA_STEPS.CLOSE_DELIVERY]: {
    command: 'CloseDelivery',
    label: 'Close Delivery',
    description: 'Complete the delivery workflow',
    needsConfirmation: true,
  },
};

const ACTION_ICONS: Record<string, typeof Truck> = {
  [DELIVERY_SAGA_STEPS.START_TRANSIT]: Truck,
  [DELIVERY_SAGA_STEPS.DELIVERY_ARRIVED]: PackageCheck,
  [DELIVERY_SAGA_STEPS.START_UNLOADING]: Wrench,
  [DELIVERY_SAGA_STEPS.CONFIRM_DELIVERY]: CheckCircle,
  [DELIVERY_SAGA_STEPS.RAISE_DISCREPANCY]: AlertTriangle,
  [DELIVERY_SAGA_STEPS.RESOLVE_DISCREPANCY]: RotateCcw,
  [DELIVERY_SAGA_STEPS.CLOSE_DELIVERY]: CheckCircle,
};

const ACTION_VARIANTS: Record<string, 'default' | 'destructive' | 'outline' | 'secondary'> = {
  [DELIVERY_SAGA_STEPS.START_TRANSIT]: 'default',
  [DELIVERY_SAGA_STEPS.DELIVERY_ARRIVED]: 'default',
  [DELIVERY_SAGA_STEPS.START_UNLOADING]: 'secondary',
  [DELIVERY_SAGA_STEPS.CONFIRM_DELIVERY]: 'default',
  [DELIVERY_SAGA_STEPS.RAISE_DISCREPANCY]: 'destructive',
  [DELIVERY_SAGA_STEPS.RESOLVE_DISCREPANCY]: 'outline',
  [DELIVERY_SAGA_STEPS.CLOSE_DELIVERY]: 'default',
};

export function DeliverySagaActionButtons({
  sagaId,
  currentStep,
  status,
  onStepCompleted,
}: DeliverySagaActionButtonsProps) {
  const [executing, setExecuting] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showConfirm, setShowConfirm] = useState<string | null>(null);

  const isTerminal = isTerminalStatus(status) || isTerminalStep(currentStep);
  const isFailed = isFailedStatus(status);

  if (isTerminal && !isFailed) return null;

  const actionKeys = getAvailableActions(currentStep, status);
  const actions = actionKeys.map((key) => ({ key, ...ACTION_CONFIGS[key] })).filter(Boolean) as Array<DeliveryActionConfig>;

  const handleExecute = async (action: DeliveryActionConfig) => {
    setExecuting(action.command);
    setError(null);
    setShowConfirm(null);
    try {
      const url = `/api/sagas/${sagaId}/execute`;
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command: action.command }),
      });
      if (!res.ok) {
        const d = await res.json().catch(() => undefined);
        throw new Error(d?.details || `HTTP ${res.status}`);
      }
      if (onStepCompleted) onStepCompleted();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to execute step');
    } finally {
      setExecuting(null);
    }
  };

  if (actions.length === 0) return null;

  return (
    <div className="flex items-center gap-1.5 flex-wrap">
      {actions.map((action) => {
        const isExecuting = executing === action.command;
        const needsConfirm = action.needsConfirmation && showConfirm === action.command;
        const Icon = ACTION_ICONS[action.key ?? ''] || Truck;
        const variant = ((ACTION_VARIANTS as Record<string, string>)[action.key ?? ''] ?? 'default') as 'default' | 'destructive' | 'outline' | 'secondary';

        return (
          <div key={action.command} className="flex flex-col items-start gap-1">
            {needsConfirm ? (
              <div className="flex items-center gap-1">
                <Button
                  size="sm"
                  variant="destructive"
                  className="text-[10px] h-5 px-1.5"
                  onClick={() => handleExecute(action)}
                  disabled={isExecuting}
                >
                  {isExecuting ? '...' : 'Yes'}
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  className="text-[10px] h-5 px-1.5"
                  onClick={() => setShowConfirm(null)}
                >
                  No
                </Button>
              </div>
            ) : (
              <Button
                size="sm"
                variant={variant}
                className="text-[10px] h-5 px-1.5"
                onClick={() => {
                  if (action.needsConfirmation) {
                    setShowConfirm(action.command);
                  } else {
                    handleExecute(action);
                  }
                }}
                disabled={isExecuting}
                title={action.description}
              >
                {isExecuting ? (
                  '...'
                ) : (
                  <>
                    <Icon className="h-3 w-3 mr-1" />
                    {action.label}
                  </>
                )}
              </Button>
            )}
          </div>
        );
      })}
      {error && (
        <p className="text-[10px] text-destructive">{error}</p>
      )}
    </div>
  );
}