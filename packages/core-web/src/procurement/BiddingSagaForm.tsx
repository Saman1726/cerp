'use client';

import { useState, useCallback, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import { cn } from '../lib/utils';
import { SagaStatusBadge } from '../workflow/SagaStatusBadge';
import { workflowApi } from '../workflow/api';
import { BIDDING_SAGA_STEPS, OPEN_TENDER_SAGA_STEPS, SAGA_TYPE_LABELS } from '../workflow/types';
import type { Saga } from '../workflow/types';
import type {
  CreateBiddingSagaRequest,
  ExecuteBiddingSagaStepRequest,
} from '@cerp/shared-types';
import { VendorSelect } from './VendorSelect';
import { generateDeterministicSagaId, normalizeSagaName } from '../lib/sagaIdUtils';

interface BiddingSagaFormProps {
  tenantId: string;
  saga?: Saga | null;
  onSuccess?: (saga: Saga) => void;
  onCancel?: () => void;
  initialProcurementSagaId?: string;
  viewOnly?: boolean;
}

const getInitialProjectIdForCreate = (saga: Saga | undefined, inheritedProjectId: string): string => {
  if (saga?.data?.projectId) return saga.data.projectId as string;
  if (inheritedProjectId) return inheritedProjectId;
  return '';
};

export function BiddingSagaForm({ tenantId, saga, onSuccess, onCancel, initialProcurementSagaId, viewOnly }: BiddingSagaFormProps) {
  const isEditMode = !!saga;
  // Project ID is inherited from the related procurement saga when creating from ProcurementPageBody
  // For creation mode, projectId comes from the procurement saga context
  // For edit mode, projectId comes from the existing saga data
  const [inheritedProjectId, setInheritedProjectId] = useState<string>('');
  const [sagaName, setSagaName] = useState(() => (saga?.data?.sagaName as string) || '');
  const [sagaNameError, setSagaNameError] = useState<string | null>(null);
  const [sagaId, setSagaId] = useState<string | null>(saga?.sagaId || null);

  const getInitialBiddingMode = () => ((saga?.data?.biddingMode as string) || 'INVITED') as 'INVITED' | 'OPEN_TENDER';
  const getInitialVendorIds = () => {
    // Check both vendorIds and invitedVendorIds as the field name may vary
    const vendorIds = saga?.data?.vendorIds as string[] | undefined;
    const invitedVendorIds = saga?.data?.invitedVendorIds as string[] | undefined;
    if (vendorIds && vendorIds.length > 0) return vendorIds;
    if (invitedVendorIds && invitedVendorIds.length > 0) return invitedVendorIds;
    return [];
  };
  const getInitialBidDeadline = () => {
    const inviteConfig = saga?.data?.inviteConfig as { bidDeadline?: string } | undefined;
    return inviteConfig?.bidDeadline || '';
  };
  const getInitialDescription = () => {
    // Check both description fields for backwards compatibility
    const desc = saga?.data?.description as string | undefined;
    if (desc) return desc;
    return '';
  };

  const [biddingMode, setBiddingMode] = useState<'INVITED' | 'OPEN_TENDER'>(getInitialBiddingMode);
  const [selectedVendorIds, setSelectedVendorIds] = useState<string[]>(getInitialVendorIds);
  const [bidDeadline, setBidDeadline] = useState(getInitialBidDeadline);
  const [description, setDescription] = useState(getInitialDescription);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdSaga, setCreatedSaga] = useState<Saga | null>(null);
  const [ownerWalletBids, setOwnerWalletBids] = useState<number | null>(null);
  const [allocatingBids, setAllocatingBids] = useState(false);

  // Check if this is for creating a bidding from procurement (has initialProcurementSagaId)
  const isBiddingFromProcurement = !!initialProcurementSagaId && !isEditMode;

  // Load projectId from the procurement saga when initialProcurementSagaId is provided
  useEffect(() => {
    if (initialProcurementSagaId && !saga?.data?.projectId) {
      (async () => {
        try {
          const procurementSaga = await workflowApi.getSaga(initialProcurementSagaId);
          if (procurementSaga.data?.projectId) {
            setInheritedProjectId(procurementSaga.data.projectId as string);
          }
        } catch (err) {
          console.error('Failed to load procurement saga for projectId:', err);
        }
      })();
    }
  }, [initialProcurementSagaId, saga?.data?.projectId]);

  const projectId = getInitialProjectIdForCreate(saga ?? undefined, inheritedProjectId);

  // Generate deterministic saga ID when name, projectId, or biddingMode changes (only on create)
  useEffect(() => {
    if (isEditMode || !projectId) return;
    
    const generateId = async () => {
      if (sagaName && projectId) {
        try {
          const generatedId = await generateDeterministicSagaId(
            normalizeSagaName(sagaName),
            String(projectId),
            biddingMode
          );
          setSagaId(generatedId);
        } catch {
          // silently fail
        }
      } else {
        setSagaId(null);
      }
    };
    
    generateId();
  }, [sagaName, projectId, biddingMode, isEditMode]);

  const [evaluationNotes, setEvaluationNotes] = useState('');

  // Fetch fresh saga data
  const refreshSaga = async (sagaId: string): Promise<Saga> => {
    const refreshed = await workflowApi.getSaga(sagaId);
    return refreshed;
  };

  // Load related procurement saga reference if provided
  const [relatedProcurementSaga, setRelatedProcurementSaga] = useState<Saga | null>(null);

  // Load the related procurement saga to get project info when creating from procurement
  useEffect(() => {
    if (initialProcurementSagaId && !saga) {
      const loadProcurementSaga = async () => {
        try {
          // Use the workflow API to fetch the procurement saga
          const response = await fetch(`/api/sagas/${initialProcurementSagaId}`);
          if (response.ok) {
            const sagaData = await response.json();
            setRelatedProcurementSaga(sagaData);
          }
        } catch (err) {
          console.error('Failed to load procurement saga:', err);
        }
      };
      loadProcurementSaga();
    }
  }, [initialProcurementSagaId, saga]);

  // Fetch project owner wallet balance when needed for the CollectBids step
  useEffect(() => {
    const activeSaga = createdSaga ?? saga;
    if (!activeSaga) return;
    const step = activeSaga.currentStep || 'Tender.CreateAndOpen';
    if (step !== 'Tender.CreateAndOpen') return;
    (async () => {
      try {
        const res = await fetch('/api/bidding?_path=wallet/balance&entityType=PROJECT_OWNER');
        if (res.ok) {
          const data = await res.json() as { availableBids?: number };
          setOwnerWalletBids(data.availableBids ?? 0);
        }
      } catch {
        // Non-critical — button will still show but won't be disabled
      }
    })();
  }, [createdSaga, saga]);

  // Helper: extract vendor IDs from any saga object (check both field names)
  const getVendorIdsFromSaga = (s: Saga | null | undefined): string[] => {
    if (!s) return [];
    const vendorIds = s.data?.vendorIds as string[] | undefined;
    const invitedVendorIds = s.data?.invitedVendorIds as string[] | undefined;
    if (vendorIds && vendorIds.length > 0) return vendorIds;
    if (invitedVendorIds && invitedVendorIds.length > 0) return invitedVendorIds;
    return [];
  };

  // Helper: get effective current step (treat null/undefined as first step for legacy sagas)
  const getEffectiveStep = (s: Saga | null | undefined): string => {
    if (!s) return 'Tender.CreateAndOpen';
    return s.currentStep || 'Tender.CreateAndOpen';
  };

  const handleAllocateOwnerBids = useCallback(async () => {
    setAllocatingBids(true);
    try {
      const res = await fetch('/api/bidding?_path=wallet/allocate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ entityType: 'PROJECT_OWNER', amount: 10 }),
      });
      if (res.ok) {
        const data = await res.json() as { wallet?: { availableBids?: number } };
        setOwnerWalletBids(data.wallet?.availableBids ?? null);
      } else {
        const data = await res.json().catch(() => ({})) as { message?: string };
        setError(data.message ?? 'Failed to allocate bids');
      }
    } catch {
      setError('Failed to allocate bids');
    } finally {
      setAllocatingBids(false);
    }
  }, []);

  const handleCreateSaga = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    if (biddingMode === 'INVITED' && selectedVendorIds.length === 0) {
      setError('At least one vendor is required for Invited Bidding mode');
      setLoading(false);
      return;
    }

    try {
       if (saga) {
         const biddingData: Record<string, unknown> = {
           biddingMode,
           projectId,
         };

         // Always include vendorIds when in INVITED mode (even if empty to clear)
         if (biddingMode === 'INVITED') {
           biddingData.vendorIds = selectedVendorIds;
         }

         if (bidDeadline) {
           biddingData.inviteConfig = { bidDeadline };
         }

         // Always include description (even if empty string) to ensure it's saved
         biddingData.description = description;

        // Save the updated data first, then force refresh to get the latest state from backend
        await workflowApi.updateSaga(saga.sagaId, biddingData);
        const refreshed = await refreshSaga(saga.sagaId);
        setCreatedSaga(refreshed);
        onSuccess?.(refreshed);
      } else {
        const biddingData: Record<string, unknown> = {
          biddingMode,
          projectId,
        };

        if (biddingMode === 'INVITED' && selectedVendorIds.length > 0) {
          biddingData.vendorIds = selectedVendorIds;
        }

        if (bidDeadline) {
          biddingData.inviteConfig = { bidDeadline };
        }

        if (description) {
          biddingData.description = description;
        }

        if (initialProcurementSagaId) {
          biddingData.procurementSagaId = initialProcurementSagaId;
        }

        const request: CreateBiddingSagaRequest & { sagaId?: string } = {
          sagaType: 'BIDDING_WORKFLOW',
          tenantId,
          data: biddingData as CreateBiddingSagaRequest['data'],
        };

        // Include sagaId if generated (deterministic ID for idempotency)
        if (sagaId) {
          request.sagaId = sagaId;
        }

        // Include sagaName if provided
        if (sagaName) {
          request.data = {
            ...biddingData,
            sagaName: sagaName,
          } as unknown as CreateBiddingSagaRequest['data'];
        }

        const sagaResult = await workflowApi.createSaga(request as unknown as Parameters<typeof workflowApi.createSaga>[0]);
        setCreatedSaga(sagaResult);
        onSuccess?.(sagaResult);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save bidding saga');
    } finally {
      setLoading(false);
    }
  }, [saga, tenantId, projectId, biddingMode, selectedVendorIds, bidDeadline, description, initialProcurementSagaId, onSuccess, sagaId, sagaName]);

  const handleExecuteStep = useCallback(async (command: ExecuteBiddingSagaStepRequest['command'], data?: Record<string, unknown>) => {
    if (!createdSaga) return;
    setLoading(true);
    setError(null);

    try {
      const request: ExecuteBiddingSagaStepRequest = {
        command,
        ...(data ? { data } : {}),
      };

      const updatedSaga = await workflowApi.executeStep(createdSaga.sagaId, request as unknown as Parameters<typeof workflowApi.executeStep>[1]);
      // Force refresh to get the latest currentStep value from backend
      const refreshed = await refreshSaga(updatedSaga.sagaId);
      setCreatedSaga(refreshed);
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to execute step: ${command}`);
    } finally {
      setLoading(false);
    }
  }, [createdSaga]);


  const handleEvaluateBids = useCallback(async () => {
    if (!createdSaga) return;

    await handleExecuteStep('Bidding.Evaluate', {
      evaluation: { notes: evaluationNotes || undefined },
    });
  }, [createdSaga, evaluationNotes, handleExecuteStep]);

  const handleAwardTender = useCallback(async () => {
    if (!createdSaga) return;
    await handleExecuteStep('Tender.Award');
  }, [createdSaga, handleExecuteStep]);

  const handleCompensate = useCallback(async () => {
    if (!createdSaga) return;
    setLoading(true);
    setError(null);

    try {
      const updatedSaga = await workflowApi.compensateSaga(createdSaga.sagaId, 'Manual compensation requested');
      setCreatedSaga(updatedSaga);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to compensate saga');
    } finally {
      setLoading(false);
    }
  }, [createdSaga]);

  // Pause saga handler
  const handlePauseSaga = useCallback(async (sagaId: string) => {
    setLoading(true);
    setError(null);
    try {
      await workflowApi.pauseSaga(sagaId);
      // Refresh to get updated state
      const refreshed = await workflowApi.getSaga(sagaId);
      setCreatedSaga(refreshed);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to pause saga');
    } finally {
      setLoading(false);
    }
  }, []);

  // Resume saga handler
  const handleResumeSaga = useCallback(async (sagaId: string, targetStep?: string) => {
    setLoading(true);
    setError(null);
    try {
      const updatedSaga = await workflowApi.resumeSaga(sagaId, targetStep);
      setCreatedSaga(updatedSaga);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to resume saga');
    } finally {
      setLoading(false);
    }
  }, []);

  // Execute step handler for view mode (uses saga from closure)
  const handleViewModeExecuteStep = useCallback(async (command: string, data?: Record<string, unknown>) => {
    if (!saga) {
      setError('No saga context available');
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const request: ExecuteBiddingSagaStepRequest = {
        command: command as ExecuteBiddingSagaStepRequest['command'],
        ...(data ? { data } : {}),
      };
      const updatedSaga = await workflowApi.executeStep(saga.sagaId, request);
      // Force refresh to get the latest currentStep value from backend
      const refreshed = await refreshSaga(updatedSaga.sagaId);
      setCreatedSaga(refreshed);
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to execute step: ${command}`);
    } finally {
      setLoading(false);
    }
  }, [saga]);

  const activeBiddingSteps =
    (createdSaga?.data?.biddingMode ?? biddingMode) === 'OPEN_TENDER'
      ? OPEN_TENDER_SAGA_STEPS
      : BIDDING_SAGA_STEPS;

  const currentStepIndex = createdSaga
    ? activeBiddingSteps.findIndex((s) => s.code === createdSaga.currentStep)
    : -1;

  // Determine which action buttons to show for RUNNING sagas
  const effectiveStep = getEffectiveStep(createdSaga);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-semibold">{SAGA_TYPE_LABELS['BIDDING_WORKFLOW'] || 'Marketplace Bidding'}</h2>
        <p className="text-sm text-muted-foreground">
          {saga ? 'Edit Bidding Saga' : 'Create and manage bidding sagas: Tender → Vendor Invitation → Bidding → Evaluation → Award'}
        </p>
      </div>

      {/* Saga Info Banner when viewing or editing */}
      {saga && !createdSaga && (
        <Card className="border-blue-200 bg-blue-50/50">
          <CardContent className="pt-4 space-y-3">
            <div className="text-sm font-medium">{viewOnly ? 'Viewing Bidding Saga' : 'Editing Bidding Saga'}</div>
            <div className="text-xs text-muted-foreground font-mono">
              Saga ID: {saga.sagaId.slice(0, 8)}...
            </div>
            <div className="text-xs text-muted-foreground">
              Current Status: <SagaStatusBadge status={saga.status} />
            </div>
            <div className="text-xs text-muted-foreground">
              Current Step: {SAGA_TYPE_LABELS[saga.currentStep ?? ''] ?? saga.currentStep ?? 'N/A'}
            </div>
            {(saga.data?.projectId || saga.data?.projectName) && (
              <div className="text-xs text-muted-foreground">
                Project: {saga.data.projectName || saga.data.projectId}
              </div>
            )}
            {saga.data?.biddingMode && (
              <div className="text-xs text-muted-foreground">
                Mode: <Badge variant="outline">{saga.data.biddingMode as string}</Badge>
              </div>
            )}

            {/* Action buttons for RUNNING, PAUSED, or REQUIRES_MANUAL_INTERVENTION sagas */}
            {(saga.status === 'RUNNING' || saga.status === 'PAUSED' || saga.status === 'REQUIRES_MANUAL_INTERVENTION') && (
              <div className="pt-2 space-y-2">
                <div className="text-xs font-medium text-muted-foreground">Actions</div>

                {/* Pause button for RUNNING saga */}
                {saga.status === 'RUNNING' && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={async () => {
                      await handlePauseSaga(saga.sagaId);
                    }}
                    disabled={loading}
                  >
                    Pause
                  </Button>
                )}

                {/* Resume button for PAUSED saga */}
                {saga.status === 'PAUSED' && (
                  <Button
                    size="sm"
                    onClick={async () => {
                      await handleResumeSaga(saga.sagaId);
                    }}
                    disabled={loading}
                  >
                    Resume
                  </Button>
                )}

                {/* Resume/Cancel for REQUIRES_MANUAL_INTERVENTION */}
                {saga.status === 'REQUIRES_MANUAL_INTERVENTION' && (
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={async () => {
                        const targetStep = saga.data?.currentStep as string | undefined;
                        await handleResumeSaga(saga.sagaId, targetStep);
                      }}
                      disabled={loading}
                    >
                      Resume Saga
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={async () => {
                        await workflowApi.compensateSaga(saga.sagaId, 'Manual compensation requested');
                        setCreatedSaga(null);
                      }}
                      disabled={loading}
                    >
                      Cancel Saga
                    </Button>
                  </div>
                )}

                {/* Step-specific action buttons for RUNNING sagas */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);

                  // Tender.CreateAndOpen -> Collect Bids
                  if (effectiveStep === 'Tender.CreateAndOpen') {
                    const insufficientBids = ownerWalletBids !== null && ownerWalletBids < 1;
                    return (
                      <div className="space-y-2">
                        {ownerWalletBids !== null && (
                          <p className="text-xs text-muted-foreground">
                            Your bid wallet:{' '}
                            <span className={insufficientBids ? 'font-semibold text-red-600' : 'font-semibold'}>
                              {ownerWalletBids} bid{ownerWalletBids !== 1 ? 's' : ''}
                            </span>
                            {' '}— opening bid collection costs <strong>1 bid</strong>.
                          </p>
                        )}
                        {insufficientBids && (
                          <div className="flex items-center gap-2">
                            <p className="text-xs text-red-600">Insufficient bids.</p>
                            <button
                              type="button"
                              onClick={handleAllocateOwnerBids}
                              disabled={allocatingBids}
                              className="text-xs underline text-blue-600 hover:text-blue-800 disabled:opacity-50"
                            >
                              {allocatingBids ? 'Adding…' : 'Add 10 bids'}
                            </button>
                          </div>
                        )}
                        <Button
                          size="sm"
                          onClick={async () => {
                            await handleViewModeExecuteStep('Bidding.CollectBids');
                          }}
                          disabled={loading || allocatingBids || insufficientBids}
                        >
                          Open Bid Collection
                        </Button>
                      </div>
                    );
                  }

                  // Bidding.CollectBids -> mode-dependent next step
                  if (effectiveStep === 'Bidding.CollectBids') {
                    const isOpenTender = saga.data?.biddingMode === 'OPEN_TENDER';
                    if (isOpenTender) {
                      return (
                        <Button
                          size="sm"
                          onClick={async () => {
                            await handleViewModeExecuteStep('OpenTender.Publish');
                          }}
                          disabled={loading}
                        >
                          Publish Open Tender
                        </Button>
                      );
                    }
                    const vendorIds = (saga.data?.invitedVendorIds as string[]) || (saga.data?.vendorIds as string[]) || [];
                    const inviteConfig = saga.data?.inviteConfig as Record<string, unknown> | undefined;
                    const hasVendors = vendorIds.length > 0;
                    return (
                      <>
                        {!hasVendors && (
                          <div className="text-xs text-amber-600 mb-2">
                            No vendors selected. Please assign vendors to the bidding saga first before inviting them.
                          </div>
                        )}
                        <Button
                          size="sm"
                          onClick={async () => {
                            await handleViewModeExecuteStep('Tender.InviteVendors', {
                              vendorIds,
                              inviteConfig,
                            });
                          }}
                          disabled={loading || !hasVendors}
                        >
                          Invite Vendors
                        </Button>
                      </>
                    );
                  }

                  // OpenTender.Publish -> waiting for open tender bids
                  if (effectiveStep === 'OpenTender.Publish') {
                    return (
                      <span className="text-xs text-muted-foreground">
                        Open tender published — waiting for vendor bids...
                      </span>
                    );
                  }

                  // Tender.InviteVendors -> Cancel Invitation (compensate)
                  if (effectiveStep === 'Tender.InviteVendors') {
                    return (
                      <>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={async () => {
                            await workflowApi.compensateSaga(saga.sagaId, 'Cancel invitation');
                          }}
                          disabled={loading}
                        >
                          Cancel Invitation
                        </Button>
                        <span className="text-xs text-muted-foreground">
                          Waiting for vendors to submit bids...
                        </span>
                      </>
                    );
                  }

                  // Bidding.Evaluate -> Evaluate & Select Winner
                  if (effectiveStep === 'Bidding.Evaluate') {
                    return (
                      <Button
                        size="sm"
                        onClick={async () => {
                          await handleViewModeExecuteStep('Bidding.Evaluate', {
                            evaluation: { notes: undefined },
                          });
                        }}
                        disabled={loading}
                      >
                        Evaluate & Select Winner
                      </Button>
                    );
                  }

                  // Tender.Award -> Award Tender & Issue PO
                  if (effectiveStep === 'Tender.Award') {
                    return (
                      <Button
                        size="sm"
                        onClick={async () => {
                          await handleViewModeExecuteStep('Tender.Award');
                        }}
                        disabled={loading}
                      >
                        Award Tender & Issue PO
                      </Button>
                    );
                  }

                  return null;
                })()}

                {/* Cancel Saga - shows for first steps */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'Tender.CreateAndOpen' || effectiveStep === 'Bidding.CollectBids' || effectiveStep === 'Tender.InviteVendors' || effectiveStep === 'OpenTender.Publish') {
                    return (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={async () => {
                          await workflowApi.compensateSaga(saga.sagaId, 'Manual compensation requested');
                        }}
                        disabled={loading}
                      >
                        Cancel Saga
                      </Button>
                    );
                  }
                  return null;
                })()}

                {error && (
                  <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-2 rounded text-xs">
                    {error}
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Related Procurement Saga */}
      {relatedProcurementSaga && (
        <Card className="border-blue-200 bg-blue-50/50">
          <CardContent className="pt-4">
            <div className="text-sm">
              <span className="text-muted-foreground">Related to Procurement: </span>
              <span className="font-mono">{relatedProcurementSaga.sagaId.slice(0, 8)}...</span>
              <span className="ml-2">
                <SagaStatusBadge status={relatedProcurementSaga.status} />
              </span>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Created Saga Status */}
      {createdSaga && (
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Bidding Saga Status</CardTitle>
              <SagaStatusBadge status={createdSaga.status} />
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4 text-sm mb-4">
              <div>
                <span className="text-muted-foreground">Saga ID: </span>
                <span className="font-mono">{createdSaga.sagaId.slice(0, 8)}...</span>
              </div>
              <div>
                <span className="text-muted-foreground">Mode: </span>
                <Badge variant="outline">{createdSaga.data?.biddingMode as string || 'INVITED'}</Badge>
              </div>
              <div>
                <span className="text-muted-foreground">Current Step: </span>
                <span>{SAGA_TYPE_LABELS[createdSaga.currentStep ?? ''] ?? createdSaga.currentStep ?? 'N/A'}</span>
              </div>
              <div>
                <span className="text-muted-foreground">Created: </span>
                <span>{new Date(createdSaga.createdAt).toLocaleString()}</span>
              </div>
            </div>

            {/* Progress Steps */}
            <div className="space-y-2">
              {activeBiddingSteps.map((step, index) => {
                const isCompleted = index < currentStepIndex;
                const isCurrent = index === currentStepIndex;

                return (
                  <div key={step.code} className="flex items-center gap-3">
                    <div
                      className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-medium ${
                        isCompleted
                          ? 'bg-green-100 text-green-800'
                          : isCurrent
                          ? 'bg-blue-100 text-blue-800'
                          : 'bg-gray-100 text-gray-500'
                      }`}
                    >
                      {isCompleted ? '✓' : index + 1}
                    </div>
                    <div className="flex-1">
                      <div className="text-sm font-medium">{step.label}</div>
                      <div className="text-xs text-muted-foreground">{step.description}</div>
                    </div>
                    {isCurrent && (
                      <Badge variant="outline" className="bg-blue-50">In Progress</Badge>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Error Display */}
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mt-4 text-sm">
                {error}
              </div>
            )}

            {/* Action Buttons based on current step (handle null currentStep for legacy sagas) */}
            {createdSaga.status === 'RUNNING' && (
              <div className="space-y-4 mt-4">
                {effectiveStep === 'Tender.CreateAndOpen' && (
                  <div className="space-y-2">
                    {ownerWalletBids !== null && (
                      <p className="text-xs text-muted-foreground">
                        Your bid wallet:{' '}
                        <span className={ownerWalletBids < 1 ? 'font-semibold text-red-600' : 'font-semibold'}>
                          {ownerWalletBids} bid{ownerWalletBids !== 1 ? 's' : ''}
                        </span>
                        {' '}— opening bid collection costs <strong>1 bid</strong>.
                      </p>
                    )}
                    {ownerWalletBids !== null && ownerWalletBids < 1 && (
                      <div className="flex items-center gap-2">
                        <p className="text-xs text-red-600">Insufficient bids.</p>
                        <button
                          type="button"
                          onClick={handleAllocateOwnerBids}
                          disabled={allocatingBids}
                          className="text-xs underline text-blue-600 hover:text-blue-800 disabled:opacity-50"
                        >
                          {allocatingBids ? 'Adding…' : 'Add 10 bids'}
                        </button>
                      </div>
                    )}
                    <Button
                      onClick={() => handleExecuteStep('Bidding.CollectBids')}
                      disabled={loading || allocatingBids || (ownerWalletBids !== null && ownerWalletBids < 1)}
                    >
                      Open Bid Collection
                    </Button>
                  </div>
                )}

                {effectiveStep === 'Bidding.CollectBids' && (() => {
                  const isOpenTender = (createdSaga.data?.biddingMode as string) === 'OPEN_TENDER';
                  if (isOpenTender) {
                    return (
                      <Button onClick={() => handleExecuteStep('OpenTender.Publish')} disabled={loading}>
                        Publish Open Tender
                      </Button>
                    );
                  }
                  const vendorIds = getVendorIdsFromSaga(createdSaga);
                  const hasVendors = vendorIds.length > 0;
                  return (
                    <>
                      {!hasVendors && (
                        <div className="text-xs text-amber-600 mb-2">
                          No vendors selected. Please assign vendors before inviting them.
                        </div>
                      )}
                      <Button
                        onClick={() => handleExecuteStep('Tender.InviteVendors', {
                          vendorIds,
                          inviteConfig: createdSaga.data?.inviteConfig,
                        })}
                        disabled={loading || !hasVendors}
                      >
                        Invite Vendors
                      </Button>
                    </>
                  );
                })()}

                {effectiveStep === 'OpenTender.Publish' && (
                  <span className="text-sm text-muted-foreground block">
                    Open tender published — waiting for vendor bids...
                  </span>
                )}

                {effectiveStep === 'Tender.InviteVendors' && (
                  <>
                    <Button
                      variant="outline"
                      onClick={handleCompensate}
                      disabled={loading}
                    >
                      Cancel Invitation
                    </Button>
                    <span className="text-sm text-muted-foreground block">
                      Waiting for vendors to submit bids...
                    </span>
                  </>
                )}

                {effectiveStep === 'Bidding.Evaluate' && (
                  <div className="space-y-3">
                    <h4 className="text-sm font-medium">Evaluation</h4>
                    <Textarea
                      placeholder="Evaluation notes..."
                      value={evaluationNotes}
                      onChange={(e) => setEvaluationNotes(e.target.value)}
                      rows={3}
                    />
                    <div className="flex gap-2">
                      <Button onClick={handleEvaluateBids} disabled={loading}>
                        Evaluate & Select Winner
                      </Button>
                      <Button variant="outline" onClick={handleCompensate} disabled={loading}>
                        Reopen Bidding
                      </Button>
                    </div>
                  </div>
                )}

                {effectiveStep === 'Tender.Award' && (
                  <div className="flex gap-2">
                    <Button onClick={handleAwardTender} disabled={loading}>
                      Award Tender & Issue PO
                    </Button>
                  </div>
                )}
              </div>
            )}

            {createdSaga.status === 'FAILED' && (
              <div className="flex gap-2 mt-4">
                <Button variant="outline" onClick={handleCompensate} disabled={loading}>
                  Compensate (Rollback)
                </Button>
                <Button variant="ghost" onClick={onCancel}>
                  Dismiss
                </Button>
              </div>
            )}

            {createdSaga.status === 'COMPLETED' && (
              <div className="mt-4">
                <Badge variant="outline" className="bg-green-50">Bidding Completed - Tender Awarded</Badge>
                {onCancel && (
                  <Button variant="ghost" className="ml-2" onClick={onCancel}>
                    Create Another
                  </Button>
                )}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {/* Creation/Form */}
      {!createdSaga && (
        <>
          {/* View Mode - Read-only display, no editable inputs */}
          {viewOnly && saga ? (
            <Card>
              <CardHeader>
                <CardTitle>View Bidding Saga</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {/* Bidding Mode - Read only */}
                  <div className="space-y-2">
                    <Label>Bidding Mode</Label>
                    <div className="text-sm">
                      <Badge variant="outline">{saga.data?.biddingMode as string || 'INVITED'}</Badge>
                    </div>
                  </div>

                  {/* Invited Vendors - Read only */}
                  {(() => {
                    const vendorIds = (saga.data?.invitedVendorIds as string[]) || (saga.data?.vendorIds as string[]) || [];
                    return vendorIds.length > 0 ? (
                      <div className="space-y-2">
                        <Label>Invited Vendors</Label>
                        <div className="flex flex-wrap gap-2">
                          {vendorIds.map((vendorId) => (
                            <Badge key={vendorId} variant="secondary" className="text-xs">
                              {vendorId.slice(0, 8)}
                            </Badge>
                          ))}
                        </div>
                      </div>
                    ) : null;
                  })()}

                  {/* Bid Deadline - Read only */}
                  {(() => {
                    const inviteConfig = saga.data?.inviteConfig as { bidDeadline?: string } | undefined;
                    const deadline = inviteConfig?.bidDeadline;
                    return deadline ? (
                      <div className="space-y-2">
                        <Label>Bid Deadline</Label>
                        <div className="text-sm">{new Date(deadline).toLocaleString()}</div>
                      </div>
                    ) : null;
                  })()}

                  {/* Description - Read only */}
                  {(() => {
                    const desc = saga.data?.description as string | undefined;
                    return desc ? (
                      <div className="space-y-2">
                        <Label>Description</Label>
                        <div className="text-sm rounded-md border p-3 bg-muted/30">{desc}</div>
                      </div>
                    ) : null;
                  })()}

                  {/* Saga metadata */}
                  <div className="pt-4 border-t text-xs text-muted-foreground space-y-1">
                    <div>Saga ID: <span className="font-mono">{saga.sagaId}</span></div>
                    <div>Created: <span className="font-mono">{new Date(saga.createdAt).toLocaleString()}</span></div>
                    <div>Updated: <span className="font-mono">{new Date(saga.updatedAt).toLocaleString()}</span></div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ) : (
            /* Edit/Create Mode - Full editable form */
            <Card>
              <CardHeader>
                <CardTitle>{saga ? 'Edit Bidding Saga' : 'Create New Bidding Saga'}</CardTitle>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleCreateSaga} className="space-y-4">
                  {/* Project info display when inherited from procurement saga */}
                  {initialProcurementSagaId && relatedProcurementSaga && (
                    <Card className="border-blue-200 bg-blue-50/50">
                      <CardContent className="pt-4">
                        <div className="text-sm font-medium">Related Procurement Saga</div>
                        <div className="text-xs text-muted-foreground mt-1">
                          Project: {relatedProcurementSaga.data?.projectId || initialProcurementSagaId.slice(0, 8)}
                        </div>
                      </CardContent>
                    </Card>
                  )}

                  {/* Saga Name (only for create mode) */}
                  {!saga && (
                    <div className="space-y-2">
                      <Label htmlFor="sagaName">Name (Required - used to prevent duplicates)</Label>
                      <Input
                        id="sagaName"
                        value={sagaName}
                        onChange={(e) => {
                          setSagaName(e.target.value);
                          setSagaNameError(null);
                        }}
                        placeholder="e.g., Q4 2025 Cement Procurement"
                        maxLength={100}
                        required
                      />
                      {sagaNameError && (
                        <div className="text-xs text-red-600">{sagaNameError}</div>
                      )}
                      <p className="text-xs text-muted-foreground">
                        This name will be used to create a unique identifier for this saga. You cannot change it after creation.
                      </p>
                    </div>
                  )}

                  {/* Bidding Mode Selection */}
                  <div className="space-y-2">
                    <Label>Bidding Mode</Label>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div
                        className={cn(
                          "flex items-center gap-2 rounded-md border p-3 cursor-pointer transition-colors",
                          biddingMode === 'INVITED' ? "border-primary bg-primary/5" : "border-input hover:bg-accent"
                        )}
                        onClick={() => setBiddingMode('INVITED')}
                      >
                        <div className={cn(
                          "w-4 h-4 rounded-full border flex items-center justify-center",
                          biddingMode === 'INVITED' ? "bg-primary border-primary" : "border-input"
                        )}>
                          {biddingMode === 'INVITED' && (
                            <div className="w-2 h-2 rounded-full bg-primary-foreground" />
                          )}
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-medium">Invited Bidding</span>
                            <Badge variant="outline" className="text-xs">INVITED</Badge>
                          </div>
                          <p className="text-xs text-muted-foreground mt-1">
                            Specific vendors are invited to submit bids
                          </p>
                        </div>
                      </div>

                      <div
                        className={cn(
                          "flex items-center gap-2 rounded-md border p-3 cursor-pointer transition-colors",
                          biddingMode === 'OPEN_TENDER' ? "border-primary bg-primary/5" : "border-input hover:bg-accent"
                        )}
                        onClick={() => setBiddingMode('OPEN_TENDER')}
                      >
                        <div className={cn(
                          "w-4 h-4 rounded-full border flex items-center justify-center",
                          biddingMode === 'OPEN_TENDER' ? "bg-primary border-primary" : "border-input"
                        )}>
                          {biddingMode === 'OPEN_TENDER' && (
                            <div className="w-2 h-2 rounded-full bg-primary-foreground" />
                          )}
                        </div>
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-medium">Open Tender</span>
                            <Badge variant="outline" className="text-xs">OPEN_TENDER</Badge>
                          </div>
                          <p className="text-xs text-muted-foreground mt-1">
                            All approved vendors can submit bids
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Vendor Selection (only for INVITED mode) */}
                  {biddingMode === 'INVITED' && (
                    <VendorSelect
                      value={selectedVendorIds}
                      onChange={setSelectedVendorIds}
                      label="Invited Vendors"
                      placeholder="Select vendors to invite..."
                      mode="multi"
                      required
                      helpText="Select vendors to invite for this bidding"
                    />
                  )}

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <Label htmlFor="bidDeadline">Bid Deadline (Optional)</Label>
                      <Input
                        id="bidDeadline"
                        type="datetime-local"
                        value={bidDeadline}
                        onChange={(e) => setBidDeadline(e.target.value)}
                      />
                    </div>
                  </div>

                  <div>
                    <Label htmlFor="description">Description (Optional)</Label>
                    <Textarea
                      id="description"
                      value={description}
                      onChange={(e) => setDescription(e.target.value)}
                      placeholder="Brief description of the bidding requirements..."
                      rows={3}
                      maxLength={500}
                    />
                  </div>

                  {error && (
                    <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
                      {error}
                    </div>
                  )}

                  <div className="flex gap-2">
                    <Button type="submit" disabled={loading || (saga ? false : !projectId) || (biddingMode === 'INVITED' && selectedVendorIds.length === 0)}>
                      {loading ? 'Updating...' : saga ? 'Update Bidding Saga' : 'Create Bidding Saga'}
                      {isBiddingFromProcurement && !sagaName && !isEditMode && <span className="ml-2 text-xs text-muted-foreground">(Enter a name first)</span>}
                    </Button>
                    {onCancel && (
                      <Button type="button" variant="outline" onClick={onCancel}>
                        Cancel
                      </Button>
                    )}
                  </div>
                </form>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
}