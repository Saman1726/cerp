'use client';

import { useState, useCallback, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import { Modal } from '../components/ui/modal';
import { SagaStatusBadge } from '../workflow/SagaStatusBadge';
import { workflowApi } from '../workflow/api';
import { PROCUREMENT_SAGA_STEPS, SAGA_TYPE_LABELS } from '../workflow/types';
import type { Saga } from '../workflow/types';
import type {
  CreateProcurementSagaRequest,
  ExecuteProcurementSagaStepRequest,
  VendorPartnerSummary,
} from '@cerp/shared-types';
import { ProjectSelect } from './ProjectSelect';
import { VendorSelect } from './VendorSelect';
import { generateDeterministicSagaId, validateSagaName, normalizeSagaName } from '../lib/sagaIdUtils';

interface ProcurementSagaFormProps {
  tenantId: string;
  saga?: Saga | null;
  onSuccess?: (saga: Saga) => void;
  onCancel?: () => void;
  onViewBidding?: () => void;
  viewOnly?: boolean;
}

export function ProcurementSagaForm({ tenantId, saga, onSuccess, onCancel, onViewBidding, viewOnly }: ProcurementSagaFormProps) {
  const isEditMode = !!saga;
  const getInitialProjectId = () => saga?.data?.projectId || '';
  const getInitialDescription = () => (saga?.data?.description as string) || '';
  const getInitialMaterialId = () => (saga?.data?.materialId as string) || '';
  const getInitialQuantity = () => (saga?.data?.quantity?.toString()) || '';
  const getInitialSagaName = () => (saga?.data?.sagaName as string) || '';
  const [projectId, setProjectId] = useState(getInitialProjectId);
  const [materialId, setMaterialId] = useState(getInitialMaterialId);
  const [quantity, setQuantity] = useState(getInitialQuantity);
  const [description, setDescription] = useState(getInitialDescription);
  const [sagaName, setSagaName] = useState(getInitialSagaName);
  const [sagaNameError, setSagaNameError] = useState<string | null>(null);
  const [sagaId, setSagaId] = useState<string | null>(saga?.sagaId || null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdSaga, setCreatedSaga] = useState<Saga | null>(null);
  const [approvalReason, setApprovalReason] = useState('');
  // Generate deterministic saga ID when name, projectId, or materialId changes (only on create)
  useEffect(() => {
    if (isEditMode || !projectId) return;
    
    const generateId = async () => {
      if (sagaName && projectId && materialId) {
        try {
          const generatedId = await generateDeterministicSagaId(
            normalizeSagaName(sagaName),
            String(projectId),
            String(materialId)
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
  }, [sagaName, projectId, materialId, isEditMode]);

  // Vendor assignment state
  const [showVendorAssignModal, setShowVendorAssignModal] = useState(false);
  const [selectedVendorId, setSelectedVendorId] = useState('');
  const [selectedVendor, setSelectedVendor] = useState<VendorPartnerSummary | null>(null);
  const [vendors, setVendors] = useState<VendorPartnerSummary[]>([]);
  const [loadingVendors, setLoadingVendors] = useState(false);
  const [relatedBiddingSagas, setRelatedBiddingSagas] = useState<Saga[]>([]);

  // Load vendors on mount
  useEffect(() => {
    const loadVendors = async () => {
      setLoadingVendors(true);
      try {
        const res = await fetch('/api/vendors/my-vendors', {
          headers: { 'Content-Type': 'application/json' },
          cache: 'no-store',
        });
        if (res.ok) {
          const rawData = await res.json();
          const vendorList: VendorPartnerSummary[] = (rawData as Array<Record<string, unknown>>).map((v) => ({
            id: String((v.vendorId as string) ?? (v.id as string) ?? ''),
            tenantId: (v.tenantId as string) ?? '',
            legalName: (v.legalName as string) ?? 'Unknown Vendor',
            status: ((v.status as 'pending' | 'approved' | 'rejected') ?? 'pending') as 'pending' | 'approved' | 'rejected',
            city: (v.city as string) ?? null,
            categories: ((v.categories ?? []) as VendorPartnerSummary['categories']) as VendorPartnerSummary['categories'],
          }));
          setVendors(vendorList);
        }
      } catch {
        // silently fail
      } finally {
        setLoadingVendors(false);
      }
    };
    loadVendors();
  }, []);

  // Load related bidding sagas when saga changes
  useEffect(() => {
    const loadRelatedBiddingSagas = async (s: Saga | null) => {
      if (!s) {
        setRelatedBiddingSagas([]);
        return;
      }
      try {
        const response = await workflowApi.listSagas({
          sagaType: 'BIDDING_WORKFLOW',
          tenantId: s.tenantId,
          pageSize: 50,
        });
        // Filter to find bidding sagas related to this procurement saga
        const related = response.items.filter(
          (biddingSaga) => {
            const data = biddingSaga.data as Record<string, unknown>;
            return String(data?.procurementSagaId ?? '') === s.sagaId || String(data?.relatedProcurementSagaId ?? '') === s.sagaId;
          }
        );
        setRelatedBiddingSagas(related);
      } catch {
        setRelatedBiddingSagas([]);
      }
    };
    loadRelatedBiddingSagas(saga ?? createdSaga ?? null);
  }, [saga?.sagaId, createdSaga?.sagaId]);

  // Helper: get effective current step
  const getEffectiveStep = (s: Saga | null | undefined): string => {
    if (!s) return 'CreateProcurementRequest';
    const step = s.currentStep || 'CreateProcurementRequest';
    if (step === 'Resume') {
      return (s.data?.currentStep as string) || 'CreateProcurementRequest';
    }
    return step;
  };

  // Check if the saga is paused
  const isSagaPaused = (s: Saga | null | undefined): boolean => {
    if (!s) return false;
    return s.status === 'PAUSED' || s.status === 'REQUIRES_MANUAL_INTERVENTION';
  };

  // Check if bidding can be created: on ApproveRequest (after approval) or IssuePurchaseOrder step
  const canCreateBidding = (s: Saga | null | undefined): boolean => {
    if (!s) return false;
    const effectiveStep = getEffectiveStep(s);
    return effectiveStep === 'ApproveRequest' || effectiveStep === 'IssuePurchaseOrder';
  };

  // Check if a bidding saga exists and is in a terminal state (COMPLETED or ABORTED/FAILED)
  const hasCompletedBidding = (s: Saga | null | undefined): boolean => {
    if (!s) return false;
    return relatedBiddingSagas.some(
      (biddingSaga) =>
        biddingSaga.status === 'COMPLETED' || biddingSaga.status === 'FAILED' || biddingSaga.status === 'COMPENSATED'
    );
  };

  // Check if bidding is in progress
  const hasInProgressBidding = (s: Saga | null | undefined): boolean => {
    if (!s) return false;
    return relatedBiddingSagas.some(
      (biddingSaga) =>
        biddingSaga.status === 'RUNNING' ||
        biddingSaga.status === 'PAUSED' ||
        biddingSaga.status === 'REQUIRES_MANUAL_INTERVENTION'
    );
  };

  // Check if vendor is assigned
  const hasVendor = (s: Saga | null | undefined): boolean => {
    if (!s) return false;
    const data = s.data as Record<string, unknown>;
    return !!data?.vendorId;
  };

  // Get assigned vendor details
  const getAssignedVendor = (s: Saga | null | undefined): VendorPartnerSummary | null => {
    if (!s) return null;
    const data = s.data as Record<string, unknown>;
    const vendorId = data?.vendorId as string | undefined;
    if (!vendorId) return null;
    return vendors.find((v) => v.id === vendorId) ?? null;
  };

  // Fetch fresh saga data
  const refreshSaga = async (sagaId: string): Promise<Saga> => {
    const refreshed = await workflowApi.getSaga(sagaId);
    return refreshed;
  };

  // Reload related bidding sagas
  const reloadRelatedBiddingSagas = async (procurementSagaId: string) => {
    try {
      const tenantIdToUse = (saga?.tenantId ?? createdSaga?.tenantId ?? tenantId) as string;
      const response = await workflowApi.listSagas({
        sagaType: 'BIDDING_WORKFLOW',
        tenantId: tenantIdToUse,
        pageSize: 50,
      });
      const related = response.items.filter(
        (biddingSaga) => {
          const data = biddingSaga.data as Record<string, unknown>;
          return String(data?.procurementSagaId ?? '') === procurementSagaId || String(data?.relatedProcurementSagaId ?? '') === procurementSagaId;
        }
      );
      setRelatedBiddingSagas(related);
    } catch {
      // silently fail
    }
  };

  const handleCreateSaga = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSagaNameError(null);

    try {
      // Validate saga name on create
      if (!isEditMode) {
        const nameValidation = validateSagaName(sagaName);
        if (!nameValidation.valid) {
          setSagaNameError(nameValidation.error || 'Invalid saga name');
          setLoading(false);
          return;
        }
      }

      if (saga) {
        // Edit mode: update existing saga (name and materialId are locked)
        const updateData: Record<string, unknown> = {
          projectId,
          ...(materialId ? { materialId } : {}),
          ...(quantity ? { quantity: Number(quantity) } : {}),
          ...(description ? { description } : {}),
        };
        const updatedSaga = await workflowApi.updateSaga(saga.sagaId, updateData);
        const body = updatedSaga as Record<string, unknown>;
        const contextData = (body.context as Record<string, unknown>) || {};
        const mergedData = { ...saga.data, ...contextData };
        const sagaResult: Saga = {
          sagaId: saga.sagaId,
          sagaType: (body.sagaType as string) || 'PROCUREMENT_WORKFLOW',
          sagaTypeName: (body.sagaType as string) || 'PROCUREMENT_WORKFLOW',
          status: (body.status as Saga['status']) || 'RUNNING',
          currentStep: (contextData?.currentStep as string) || saga.currentStep,
          tenantId: saga.tenantId,
          data: mergedData,
          steps: saga.steps || [],
          createdAt: saga.createdAt,
          updatedAt: new Date().toISOString(),
        };
        setCreatedSaga(sagaResult);
        onSuccess?.(sagaResult);
      } else {
        // Create mode: generate deterministic saga ID and include saga name
        if (!sagaName || !projectId || !materialId) {
          setSagaNameError('Saga name, project, and material are required to create a procurement request.');
          setLoading(false);
          return;
        }

        const generatedSagaId = await generateDeterministicSagaId(
          normalizeSagaName(sagaName),
          String(projectId),
          String(materialId)
        );

        // Check if a saga with this ID already exists
        try {
          const existingSagas = await workflowApi.listSagas({
            sagaType: 'PROCUREMENT_WORKFLOW',
            tenantId,
            pageSize: 50,
          });
          
          const existing = existingSagas.items.find(
            (s: Saga) => {
              const data = s.data as Record<string, unknown>;
              return String(data?.sagaName ?? '') === normalizeSagaName(sagaName) &&
                     String(data?.projectId ?? '') === String(projectId) &&
                     String(data?.materialId ?? '') === String(materialId);
            }
          );

          if (existing && (existing.status === 'RUNNING' || existing.status === 'PAUSED' || existing.status === 'REQUIRES_MANUAL_INTERVENTION')) {
            setError(`A procurement request with this name already exists for project "${projectId}". Please use a different name.`);
            setLoading(false);
            return;
          }
        } catch {
          // Silently fail - we'll let the creation handle duplicate key errors
        }

        const request: CreateProcurementSagaRequest = {
          sagaType: 'PROCUREMENT_WORKFLOW',
          tenantId,
          sagaId: generatedSagaId,
          data: {
            sagaName: normalizeSagaName(sagaName),
            projectId,
            ...(materialId ? { materialId } : {}),
            ...(quantity ? { quantity: Number(quantity) } : {}),
            ...(description ? { description } : {}),
          },
        };

        const createdSagaResult = await workflowApi.createSaga(request as unknown as Parameters<typeof workflowApi.createSaga>[0]);
        setCreatedSaga(createdSagaResult);
        onSuccess?.(createdSagaResult);
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to save procurement saga';
      if (message.includes('duplicate') || message.includes('unique') || message.includes('already exists')) {
        setSagaNameError('A procurement request with this name already exists for this project. Please use a different name.');
      } else {
        setError(message);
      }
    } finally {
      setLoading(false);
    }
  }, [saga, tenantId, projectId, materialId, quantity, description, sagaName, onSuccess, isEditMode]);

  const handleExecuteStep = useCallback(async (command: ExecuteProcurementSagaStepRequest['command'], data?: Record<string, unknown>) => {
    if (!createdSaga) return;
    setLoading(true);
    setError(null);

    try {
      const request: ExecuteProcurementSagaStepRequest = {
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

  // Get the active saga (createdSaga takes precedence, fallback to saga)
  const activeSaga = createdSaga || saga || null;

  // Handle vendor assignment
  const handleAssignVendor = useCallback(async () => {
    const sagaToUse = activeSaga;
    if (!sagaToUse || !selectedVendorId) return;
    setLoading(true);
    setError(null);

    try {
      // Update the saga with vendorId
      await workflowApi.updateSaga(sagaToUse.sagaId, {
        vendorId: selectedVendorId,
      });

      // Refresh saga data
      const refreshedSaga = await refreshSaga(sagaToUse.sagaId);

      // Update saga state - always set createdSaga so UI refreshes properly
      // This works for both edit mode (createdSaga exists) and view mode (only saga prop exists)
      setCreatedSaga(refreshedSaga);

      setSelectedVendor(null);
      setSelectedVendorId('');
      setShowVendorAssignModal(false);

      // Reload related bidding sagas
      await reloadRelatedBiddingSagas(sagaToUse.sagaId);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to assign vendor');
    } finally {
      setLoading(false);
    }
  }, [activeSaga, selectedVendorId]);

  // Handle IssuePurchaseOrder button click
  const handleIssuePurchaseOrder = useCallback(async () => {
    if (!createdSaga) return;
    setLoading(true);
    setError(null);

    try {
      // Reload fresh related bidding sagas for this procurement
      await reloadRelatedBiddingSagas(createdSaga.sagaId);

      // Check for in-progress bidding
      const hasInProgress = hasInProgressBidding(createdSaga);
      if (hasInProgress) {
        setError('A bidding is currently in progress for this procurement. Please wait for it to complete before proceeding.');
        return;
      }

      // Main condition: vendor must be non-null before executing the step
      // Vendor is assigned via Bidding (COMPLETED/ABORTED) OR manually
      if (hasVendor(createdSaga)) {
        // Vendor is assigned, proceed to next step
        await handleExecuteStep('ReceiveDelivery');
        await reloadRelatedBiddingSagas(createdSaga.sagaId);
      } else {
        // No vendor assigned - show vendor assignment modal
        setShowVendorAssignModal(true);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to process IssuePurchaseOrder');
    } finally {
      setLoading(false);
    }
  }, [createdSaga, handleExecuteStep]);

  // Handle IssuePurchaseOrder for view mode (when viewing existing saga, not created)
  const handleIssuePurchaseOrderViewMode = useCallback(async (s: Saga) => {
    setLoading(true);
    setError(null);

    try {
      // Reload fresh related bidding sagas for this procurement
      await reloadRelatedBiddingSagas(s.sagaId);

      // Check for in-progress bidding
      const hasInProgress = hasInProgressBidding(s);
      if (hasInProgress) {
        setError('A bidding is currently in progress for this procurement. Please wait for it to complete before proceeding.');
        return;
      }

      // Main condition: vendor must be non-null before executing the step
      // Vendor is assigned via Bidding (COMPLETED/ABORTED) OR manually
      if (hasVendor(s)) {
        // Vendor is assigned, proceed to next step with force refresh
        const request: ExecuteProcurementSagaStepRequest = { command: 'ReceiveDelivery' };
        const updatedSaga = await workflowApi.executeStep(s.sagaId, request as unknown as Parameters<typeof workflowApi.executeStep>[1]);
        // Force refresh to get the latest currentStep value from backend
        const refreshed = await refreshSaga(updatedSaga.sagaId);
        setCreatedSaga(refreshed);
      } else {
        // No vendor assigned - show vendor assignment modal
        setShowVendorAssignModal(true);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to process IssuePurchaseOrder');
    } finally {
      setLoading(false);
    }
  }, [reloadRelatedBiddingSagas]);

  const currentStepIndex = createdSaga
    ? PROCUREMENT_SAGA_STEPS.findIndex((s) => s.code === createdSaga.currentStep)
    : -1;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-semibold">{SAGA_TYPE_LABELS['PROCUREMENT_WORKFLOW'] || 'Procurement Workflow'}</h2>
        <p className="text-sm text-muted-foreground">
          {saga ? 'Edit Procurement Saga' : 'Create and manage procurement sagas: Request → Approval → PO → Delivery → Close'}
        </p>
      </div>

      {/* Saga Info Banner when viewing or editing */}
      {saga && !createdSaga && (
        <Card className="border-blue-200 bg-blue-50/50">
          <CardContent className="pt-4 space-y-3">
            <div className="text-sm font-medium">{viewOnly ? 'Viewing Procurement Saga' : 'Editing Procurement Saga'}</div>
            <div className="text-xs text-muted-foreground font-mono">
              Saga ID: {saga.sagaId.slice(0, 8)}...
            </div>
            <div className="text-xs text-muted-foreground">
              Current Status: <SagaStatusBadge status={saga.status} />
            </div>
            <div className="text-xs text-muted-foreground">
              Current Step: {SAGA_TYPE_LABELS[saga.currentStep ?? ''] ?? saga.currentStep ?? 'N/A'}
            </div>
            {saga.data?.projectId && (
              <div className="text-xs text-muted-foreground">
                Project: {saga.data.projectId}
              </div>
            )}

            {/* Assigned Vendor Info - show when vendor is assigned */}
            {(() => {
              const assignedVendor = getAssignedVendor(saga);
              if (assignedVendor) {
                return (
                  <div className="text-xs text-muted-foreground border-t pt-2 mt-2">
                    <div className="font-medium text-foreground">Assigned Vendor:</div>
                    <div>{assignedVendor.legalName}</div>
                    {assignedVendor.city && <div className="text-xs">{assignedVendor.city}</div>}
                    {assignedVendor.status === 'approved' && (
                      <Badge variant="outline" className="text-xs mt-1">Approved</Badge>
                    )}
                  </div>
                );
              }
              return null;
            })()}

            {/* Action buttons for viewing mode */}
            {viewOnly && (saga.status === 'RUNNING' || saga.status === 'PAUSED' || saga.status === 'REQUIRES_MANUAL_INTERVENTION') && (
              <div className="pt-2 space-y-2">
                <div className="text-xs font-medium text-muted-foreground">Actions</div>

                {/* Pause/Resume button */}
                {(() => {
                  if (saga.status === 'RUNNING') {
                    return (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={async () => {
                          setLoading(true);
                          setError(null);
                          try {
                            await workflowApi.pauseSaga(saga.sagaId);
                            window.location.reload();
                          } catch (err) {
                            setError(err instanceof Error ? err.message : 'Failed to pause saga');
                          } finally {
                            setLoading(false);
                          }
                        }}
                        disabled={loading}
                      >
                        Pause
                      </Button>
                    );
                  }
                  if (saga.status === 'PAUSED') {
                    return (
                      <Button
                        size="sm"
                        onClick={async () => {
                          setLoading(true);
                          setError(null);
                          try {
                            await workflowApi.startSaga(saga.sagaId);
                            window.location.reload();
                          } catch (err) {
                            setError(err instanceof Error ? err.message : 'Failed to resume saga');
                          } finally {
                            setLoading(false);
                          }
                        }}
                        disabled={loading}
                      >
                        Resume
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Submit for Approval - shows for first step or null step */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'CreateProcurementRequest') {
                    return (
                      <Button
                        size="sm"
                        onClick={async () => {
                          setLoading(true);
                          setError(null);
                          try {
                            const request: ExecuteProcurementSagaStepRequest = {
                              command: 'SubmitForApproval',
                            };
                            const updatedSaga = await workflowApi.executeStep(saga.sagaId, request as unknown as Parameters<typeof workflowApi.executeStep>[1]);
                            // Force refresh to get the latest currentStep value from backend
                            const refreshed = await refreshSaga(updatedSaga.sagaId);
                            setCreatedSaga(refreshed);
                          } catch (err) {
                            setError(err instanceof Error ? err.message : `Failed to execute step: SubmitForApproval`);
                          } finally {
                            setLoading(false);
                          }
                        }}
                        disabled={loading}
                      >
                        Submit for Approval
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Approve/Reject Request - shows for SubmitForApproval step */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'SubmitForApproval') {
                    return (
                      <>
                        <Button
                          size="sm"
                          onClick={async () => {
                            setLoading(true);
                            setError(null);
                            try {
                              const request: ExecuteProcurementSagaStepRequest = {
                                command: 'ApproveRequest',
                                data: { approvalData: { approved: true } },
                              };
                              const updatedSaga = await workflowApi.executeStep(saga.sagaId, request as unknown as Parameters<typeof workflowApi.executeStep>[1]);
                              // Force refresh to get the latest currentStep value from backend
                              const refreshed = await refreshSaga(updatedSaga.sagaId);
                              setCreatedSaga(refreshed);
                            } catch (err) {
                              setError(err instanceof Error ? err.message : `Failed to execute step: ApproveRequest`);
                            } finally {
                              setLoading(false);
                            }
                          }}
                          disabled={loading}
                        >
                          Approve Request
                        </Button>
                        <Button
                          size="sm"
                          variant="destructive"
                          onClick={async () => {
                            setLoading(true);
                            setError(null);
                            try {
                              const request: ExecuteProcurementSagaStepRequest = {
                                command: 'ApproveRequest',
                                data: { approvalData: { approved: false, rejectionReason: approvalReason || 'Rejected during viewing' } },
                              };
                              const updatedSaga = await workflowApi.executeStep(saga.sagaId, request as unknown as Parameters<typeof workflowApi.executeStep>[1]);
                              // Force refresh to get the latest currentStep value from backend
                              const refreshed = await refreshSaga(updatedSaga.sagaId);
                              setCreatedSaga(refreshed);
                            } catch (err) {
                              setError(err instanceof Error ? err.message : `Failed to execute step: ApproveRequest`);
                            } finally {
                              setLoading(false);
                            }
                          }}
                          disabled={loading}
                        >
                          Reject Request
                        </Button>
                        <Input
                          placeholder="Rejection reason (if rejecting)"
                          value={approvalReason}
                          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setApprovalReason(e.target.value)}
                          className="max-w-xs"
                        />
                      </>
                    );
                  }
                  return null;
                })()}

                {/* Issue Purchase Order - shows for ApproveRequest step */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'ApproveRequest') {
                    return (
                      <Button
                        size="sm"
                        onClick={async () => {
                          await handleIssuePurchaseOrderViewMode(saga);
                        }}
                        disabled={loading}
                      >
                        Issue Purchase Order
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Receive Delivery - shows for IssuePurchaseOrder step AND vendor is assigned */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'IssuePurchaseOrder' && hasVendor(saga)) {
                    return (
                      <Button
                        size="sm"
                        onClick={async () => {
                          await handleIssuePurchaseOrderViewMode(saga);
                        }}
                        disabled={loading}
                      >
                        Receive Delivery
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Issue Purchase Order button for IssuePurchaseOrder step (vendor not assigned yet) */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'IssuePurchaseOrder' && !hasVendor(saga)) {
                    return (
                      <Button
                        size="sm"
                        onClick={async () => {
                          await handleIssuePurchaseOrderViewMode(saga);
                        }}
                        disabled={loading || showVendorAssignModal}
                      >
                        Issue Purchase Order (Assign Vendor)
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Confirm Delivery - shows for ReceiveDelivery step */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'ReceiveDelivery') {
                    return (
                      <Button
                        size="sm"
                        onClick={async () => {
                          setLoading(true);
                          setError(null);
                          try {
                            const request: ExecuteProcurementSagaStepRequest = {
                              command: 'ConfirmDelivery',
                            };
                            const updatedSaga = await workflowApi.executeStep(saga.sagaId, request as unknown as Parameters<typeof workflowApi.executeStep>[1]);
                            // Force refresh to get the latest currentStep value from backend
                            const refreshed = await refreshSaga(updatedSaga.sagaId);
                            setCreatedSaga(refreshed);
                          } catch (err) {
                            setError(err instanceof Error ? err.message : `Failed to execute step: ConfirmDelivery`);
                          } finally {
                            setLoading(false);
                          }
                        }}
                        disabled={loading}
                      >
                        Confirm Delivery
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Close Procurement - shows for ConfirmDelivery step */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'ConfirmDelivery') {
                    return (
                      <Button
                        size="sm"
                        onClick={async () => {
                          setLoading(true);
                          setError(null);
                          try {
                            const request: ExecuteProcurementSagaStepRequest = {
                              command: 'CloseProcurement',
                            };
                            const updatedSaga = await workflowApi.executeStep(saga.sagaId, request as unknown as Parameters<typeof workflowApi.executeStep>[1]);
                            // Force refresh to get the latest currentStep value from backend
                            const refreshed = await refreshSaga(updatedSaga.sagaId);
                            setCreatedSaga(refreshed);
                          } catch (err) {
                            setError(err instanceof Error ? err.message : `Failed to execute step: CloseProcurement`);
                          } finally {
                            setLoading(false);
                          }
                        }}
                        disabled={loading}
                      >
                        Close Procurement
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Resume Saga - shows when saga is paused */}
                {(() => {
                  if (isSagaPaused(saga)) {
                    const isManualIntervention = saga.status === 'REQUIRES_MANUAL_INTERVENTION';
                    return (
                      <div className="space-y-2">
                        <div className="text-xs font-medium text-orange-600">
                          {isManualIntervention ? 'Saga requires manual intervention' : 'Saga is paused - needs resume'}
                        </div>
                        <div className="flex gap-2">
                          {isManualIntervention ? (
                            <Button
                              size="sm"
                              onClick={async () => {
                                setLoading(true);
                                setError(null);
                                try {
                                  const targetStep = saga.data?.currentStep as string | undefined;
                                  const updatedSaga = await workflowApi.resumeSaga(saga.sagaId, targetStep);
                                  setCreatedSaga(updatedSaga);
                                } catch (err) {
                                  setError(err instanceof Error ? err.message : 'Failed to resume saga');
                                } finally {
                                  setLoading(false);
                                }
                              }}
                              disabled={loading}
                            >
                              Resume Saga
                            </Button>
                          ) : (
                            <Button
                              size="sm"
                              onClick={async () => {
                                setLoading(true);
                                setError(null);
                                try {
                                  await workflowApi.startSaga(saga.sagaId);
                                  window.location.reload();
                                } catch (err) {
                                  setError(err instanceof Error ? err.message : 'Failed to resume saga');
                                } finally {
                                  setLoading(false);
                                }
                              }}
                              disabled={loading}
                            >
                              Resume
                            </Button>
                          )}
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={async () => {
                              setLoading(true);
                              setError(null);
                              try {
                                const updatedSaga = await workflowApi.compensateSaga(saga.sagaId, 'Manual compensation requested');
                                setCreatedSaga(updatedSaga);
                              } catch (err) {
                                setError(err instanceof Error ? err.message : 'Failed to compensate saga');
                              } finally {
                                setLoading(false);
                              }
                            }}
                            disabled={loading}
                          >
                            Cancel Saga
                          </Button>
                        </div>
                      </div>
                    );
                  }
                  return null;
                })()}

                {/* Cancel Saga - shows for first two steps */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'CreateProcurementRequest' || effectiveStep === 'SubmitForApproval') {
                    return (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={async () => {
                          setLoading(true);
                          setError(null);
                          try {
                            const updatedSaga = await workflowApi.compensateSaga(saga.sagaId, 'Manual compensation requested');
                            setCreatedSaga(updatedSaga);
                          } catch (err) {
                            setError(err instanceof Error ? err.message : 'Failed to compensate saga');
                          } finally {
                            setLoading(false);
                          }
                        }}
                        disabled={loading}
                      >
                        Cancel Saga
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Create Bidding - ONLY shows on IssuePurchaseOrder step */}
                {(() => {
                  if (canCreateBidding(saga)) {
                    return (
                      <Button
                        size="sm"
                        variant="secondary"
                        onClick={onViewBidding}
                      >
                        Create Bidding
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Bidding in progress indicator */}
                {(() => {
                  const effectiveStep = getEffectiveStep(saga);
                  if (effectiveStep === 'IssuePurchaseOrder' && hasInProgressBidding(saga)) {
                    return (
                      <Badge variant="outline" className="bg-yellow-50 text-yellow-700 border-yellow-200">
                        Bidding in progress...
                      </Badge>
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

      {/* Created Saga Status */}
      {createdSaga && (
        <Card>
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Procurement Saga Status</CardTitle>
              <SagaStatusBadge status={createdSaga.status} />
            </div>
          </CardHeader>
          <CardContent className="space-y-6">
            {/* Saga Info */}
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-muted-foreground">Saga ID: </span>
                <span className="font-mono">{createdSaga.sagaId.slice(0, 8)}...</span>
              </div>
              <div>
                <span className="text-muted-foreground">Current Step: </span>
                <span>{SAGA_TYPE_LABELS[createdSaga.currentStep ?? ''] ?? createdSaga.currentStep ?? 'N/A'}</span>
              </div>
              <div>
                <span className="text-muted-foreground">Created: </span>
                <span>{new Date(createdSaga.createdAt).toLocaleString()}</span>
              </div>
              <div>
                <span className="text-muted-foreground">Updated: </span>
                <span>{new Date(createdSaga.updatedAt).toLocaleString()}</span>
              </div>
            </div>

            {/* Assigned Vendor Info */}
            {(() => {
              const assignedVendor = getAssignedVendor(createdSaga);
              if (assignedVendor) {
                return (
                  <Card className="border-green-200 bg-green-50/50">
                    <CardContent className="pt-4 space-y-2">
                      <div className="text-sm font-medium text-green-800">Assigned Vendor</div>
                      <div className="text-sm">{assignedVendor.legalName}</div>
                      {assignedVendor.city && (
                        <div className="text-xs text-muted-foreground">{assignedVendor.city}</div>
                      )}
                      {assignedVendor.status === 'approved' && (
                        <Badge variant="outline" className="text-xs">Approved</Badge>
                      )}
                    </CardContent>
                  </Card>
                );
              }
              return null;
            })()}

            {/* Related Bidding Info */}
            {(() => {
              if (relatedBiddingSagas.length > 0) {
                return (
                  <Card className="border-purple-200 bg-purple-50/50">
                    <CardContent className="pt-4 space-y-2">
                      <div className="text-sm font-medium text-purple-800">Related Bidding</div>
                      {relatedBiddingSagas.map((biddingSaga) => (
                        <div key={biddingSaga.sagaId} className="flex items-center gap-2 text-sm">
                          <SagaStatusBadge status={biddingSaga.status} />
                          <span className="font-mono text-xs">{biddingSaga.sagaId.slice(0, 8)}...</span>
                        </div>
                      ))}
                    </CardContent>
                  </Card>
                );
              }
              return null;
            })()}

            {/* Progress Steps */}
            <div className="space-y-2">
              {PROCUREMENT_SAGA_STEPS.map((step, index) => {
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
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
                {error}
              </div>
            )}

            {/* Action Buttons based on current step and status */}
            {(createdSaga.status === 'RUNNING' || createdSaga.status === 'PAUSED' || createdSaga.status === 'REQUIRES_MANUAL_INTERVENTION') && (
              <div className="flex flex-wrap gap-2">
                {/* Pause button for RUNNING saga */}
                {createdSaga.status === 'RUNNING' && (
                  <Button
                    variant="outline"
                    onClick={async () => {
                      setLoading(true);
                      setError(null);
                      try {
                        await workflowApi.pauseSaga(createdSaga.sagaId);
                        const refreshed = await refreshSaga(createdSaga.sagaId);
                        setCreatedSaga(refreshed);
                      } catch (err) {
                        setError(err instanceof Error ? err.message : 'Failed to pause saga');
                      } finally {
                        setLoading(false);
                      }
                    }}
                    disabled={loading}
                  >
                    Pause
                  </Button>
                )}

                {(() => {
                  const effectiveStep = getEffectiveStep(createdSaga);
                  if (effectiveStep === 'CreateProcurementRequest') {
                    return (
                      <Button
                        onClick={() => handleExecuteStep('SubmitForApproval')}
                        disabled={loading}
                      >
                        Submit for Approval
                      </Button>
                    );
                  }
                  return null;
                })()}

                {(() => {
                  const effectiveStep = getEffectiveStep(createdSaga);
                  if (effectiveStep === 'SubmitForApproval') {
                    return (
                      <>
                        <Button
                          onClick={() =>
                            handleExecuteStep('ApproveRequest', {
                              approvalData: { approved: true },
                            })
                          }
                          disabled={loading}
                        >
                          Approve Request
                        </Button>
                        <Button
                          variant="destructive"
                          onClick={() =>
                            handleExecuteStep('ApproveRequest', {
                              approvalData: { approved: false, rejectionReason: approvalReason },
                            })
                          }
                          disabled={loading}
                        >
                          Reject Request
                        </Button>
                        <Input
                          placeholder="Rejection reason (if rejecting)"
                          value={approvalReason}
                          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setApprovalReason(e.target.value)}
                          className="max-w-xs"
                        />
                      </>
                    );
                  }
                  return null;
                })()}

                {(() => {
                  const effectiveStep = getEffectiveStep(createdSaga);
                  if (effectiveStep === 'ApproveRequest') {
                    return (
                      <>
                        {/* Create Bidding button - show when no completed bidding exists */}
                        {!hasCompletedBidding(createdSaga) && onViewBidding && (
                          <Button
                            variant="secondary"
                            onClick={onViewBidding}
                            disabled={loading}
                          >
                            Create Bidding
                          </Button>
                        )}
                        <Button
                          onClick={() => {
                            // Check vendor before proceeding - show modal if not assigned
                            if (hasVendor(createdSaga)) {
                              handleExecuteStep('IssuePurchaseOrder');
                            } else {
                              setShowVendorAssignModal(true);
                            }
                          }}
                          disabled={loading}
                        >
                          Issue Purchase Order
                        </Button>
                      </>
                    );
                  }
                  return null;
                })()}

                {/* IssuePurchaseOrder step: Vendor not assigned - show Issue Purchase Order to trigger assignment */}
                {(() => {
                  const effectiveStep = getEffectiveStep(createdSaga);
                  if (effectiveStep === 'IssuePurchaseOrder' && !hasVendor(createdSaga)) {
                    return (
                      <Button
                        onClick={() => handleIssuePurchaseOrder()}
                        disabled={loading}
                      >
                        Issue Purchase Order (Assign Vendor)
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* IssuePurchaseOrder step: Vendor assigned - show Receive Delivery */}
                {(() => {
                  const effectiveStep = getEffectiveStep(createdSaga);
                  if (effectiveStep === 'IssuePurchaseOrder' && hasVendor(createdSaga)) {
                    return (
                      <>
                        <Button
                          onClick={() => handleIssuePurchaseOrder()}
                          disabled={loading}
                        >
                          Receive Delivery
                        </Button>
                        {hasInProgressBidding(createdSaga) && (
                          <Badge variant="outline" className="bg-yellow-50 text-yellow-700 border-yellow-200">
                            Bidding in progress...
                          </Badge>
                        )}
                      </>
                    );
                  }
                  return null;
                })()}

                {(() => {
                  const effectiveStep = getEffectiveStep(createdSaga);
                  if (effectiveStep === 'ReceiveDelivery') {
                    return (
                      <Button
                        onClick={() => handleExecuteStep('ConfirmDelivery')}
                        disabled={loading}
                      >
                        Confirm Delivery
                      </Button>
                    );
                  }
                  return null;
                })()}

                {(() => {
                  const effectiveStep = getEffectiveStep(createdSaga);
                  if (effectiveStep === 'ConfirmDelivery') {
                    return (
                      <Button
                        onClick={() => handleExecuteStep('CloseProcurement')}
                        disabled={loading}
                      >
                        Close Procurement
                      </Button>
                    );
                  }
                  return null;
                })()}

                {(() => {
                  const effectiveStep = getEffectiveStep(createdSaga);
                  if (effectiveStep === 'CreateProcurementRequest' || effectiveStep === 'SubmitForApproval') {
                    return (
                      <Button
                        variant="outline"
                        onClick={handleCompensate}
                        disabled={loading}
                      >
                        Cancel Saga
                      </Button>
                    );
                  }
                  return null;
                })()}

                {/* Resume/Start button for PAUSED saga */}
                {createdSaga.status === 'PAUSED' && (
                  <Button
                    onClick={async () => {
                      setLoading(true);
                      setError(null);
                      try {
                        await workflowApi.startSaga(createdSaga.sagaId);
                        const refreshed = await refreshSaga(createdSaga.sagaId);
                        setCreatedSaga(refreshed);
                      } catch (err) {
                        setError(err instanceof Error ? err.message : 'Failed to resume saga');
                      } finally {
                        setLoading(false);
                      }
                    }}
                    disabled={loading}
                  >
                    Resume
                  </Button>
                )}

                {/* Resume Saga - shows when saga requires manual intervention */}
                {createdSaga.status === 'REQUIRES_MANUAL_INTERVENTION' && (
                  <div className="space-y-2">
                    <div className="text-xs font-medium text-orange-600">Saga requires manual intervention</div>
                    <div className="flex gap-2">
                      <Button
                        onClick={async () => {
                          setLoading(true);
                          setError(null);
                          try {
                            const targetStep = createdSaga.data?.currentStep as string | undefined;
                            const updatedSaga = await workflowApi.resumeSaga(createdSaga.sagaId, targetStep);
                            setCreatedSaga(updatedSaga);
                          } catch (err) {
                            setError(err instanceof Error ? err.message : 'Failed to resume saga');
                          } finally {
                            setLoading(false);
                          }
                        }}
                        disabled={loading}
                      >
                        Resume Saga
                      </Button>
                      <Button
                        variant="outline"
                        onClick={handleCompensate}
                        disabled={loading}
                      >
                        Cancel Saga
                      </Button>
                    </div>
                  </div>
                )}

                {/* Create Bidding - ONLY shows on IssuePurchaseOrder step */}
                {(() => {
                  if (canCreateBidding(createdSaga)) {
                    return (
                      <Button
                        variant="secondary"
                        onClick={onViewBidding}
                      >
                        Create Bidding
                      </Button>
                    );
                  }
                  return null;
                })()}
              </div>
            )}

            {/* Saga Status Actions */}
            {createdSaga.status === 'FAILED' && (
              <div className="flex gap-2">
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
                <Badge variant="outline" className="bg-green-50">Procurement Completed</Badge>
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

      {/* Creation/Edit Form */}
      {!createdSaga && (
        <Card>
          <CardHeader>
            <CardTitle>{saga ? 'Edit Procurement Request' : 'Create New Procurement Request'}</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreateSaga} className="space-y-4">
              {/* Saga Name - required field for deterministic ID generation */}
              <div>
                <Label htmlFor="sagaName">
                  {isEditMode ? 'Procurement Name' : 'Procurement Name'}
                  {!isEditMode && <span className="text-destructive ml-1">*</span>}
                </Label>
                <Input
                  id="sagaName"
                  value={sagaName}
                  onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSagaName(e.target.value)}
                  placeholder="e.g., Foundation Concrete Order #1"
                  disabled={isEditMode}
                  readOnly={isEditMode}
                  className={isEditMode ? 'bg-muted cursor-not-allowed' : ''}
                  autoComplete="off"
                />
                {!isEditMode && (
                  <p className="text-xs text-muted-foreground mt-1">
                    This name will be used to create a unique identifier. Special characters are supported.
                  </p>
                )}
                {isEditMode && sagaName && (
                  <p className="text-xs text-muted-foreground mt-1">
                    🔒 This field is part of the saga identifier and cannot be changed
                  </p>
                )}
                {sagaNameError && (
                  <p className="text-xs text-destructive mt-1">{sagaNameError}</p>
                )}
              </div>

              {/* Project Selection - only show when creating new procurement, not when editing */}
              {!isEditMode && (
                <ProjectSelect
                  onChange={(pId: string, _project: unknown) => {
                    setProjectId(pId);
                  }}
                  label="Project"
                  placeholder="Select a project for this procurement..."
                  required
                  helpText="Select the project this procurement request is for"
                />
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="materialId">
                    Material ID
                    {!isEditMode && <span className="text-destructive ml-1">*</span>}
                  </Label>
                  <Input
                    id="materialId"
                    value={materialId}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setMaterialId(e.target.value)}
                    placeholder="uuid or material identifier"
                    disabled={isEditMode}
                    readOnly={isEditMode}
                    className={isEditMode ? 'bg-muted cursor-not-allowed' : ''}
                  />
                  {isEditMode && materialId && (
                    <p className="text-xs text-muted-foreground mt-1">
                      🔒 This field is part of the saga identifier and cannot be changed
                    </p>
                  )}
                </div>
                <div>
                  <Label htmlFor="quantity">Quantity</Label>
                  <Input
                    id="quantity"
                    type="number"
                    min="1"
                    value={quantity}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => setQuantity(e.target.value)}
                    placeholder="1"
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  value={description}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setDescription(e.target.value)}
                  placeholder="Brief description of the procurement..."
                  rows={3}
                  maxLength={500}
                />
              </div>

              {!isEditMode && sagaId && (
                <div className="bg-blue-50 border border-blue-200 text-blue-700 px-4 py-2 rounded text-xs">
                  <span className="font-medium">Generated Saga ID:</span> <span className="font-mono">{sagaId}</span>
                </div>
              )}

              {error && !sagaNameError && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
                  {error}
                </div>
              )}

              <div className="flex gap-2">
                <Button type="submit" disabled={loading || (!saga && !projectId) || (!isEditMode && (!sagaName || !projectId || !materialId))}>
                  {loading ? 'Creating...' : 'Update Procurement Saga'}
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

      {/* Vendor Assignment Modal */}
      <Modal
        open={showVendorAssignModal}
        onClose={() => {
          setShowVendorAssignModal(false);
          setSelectedVendorId('');
          setSelectedVendor(null);
        }}
        title="Assign Vendor to Procurement"
      >
        <div className="text-sm text-muted-foreground mb-4">
          A vendor must be assigned before you can proceed to the next step. This vendor will be responsible for delivering the materials.
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm mb-4">
            {error}
          </div>
        )}

        {/* Vendor Selection */}
        <div className="space-y-2 mb-4">
          <Label htmlFor="vendor-select">
            Vendor
            <span className="text-destructive ml-1">*</span>
          </Label>
          <VendorSelect
            mode="single"
            value={selectedVendorId ? [selectedVendorId] : []}
            onChange={(vendorIds: string[], vendorList: VendorPartnerSummary[]) => {
              if (vendorIds.length > 0) {
                setSelectedVendorId(vendorIds[0] ?? '');
                setSelectedVendor(vendorList[0] ?? null);
              } else {
                setSelectedVendorId('');
                setSelectedVendor(null);
              }
            }}
            placeholder="Select a vendor for this procurement..."
            required
            helpText="Select the vendor who will deliver the materials"
            disabled={loading}
          />
          {loadingVendors && (
            <div className="text-xs text-muted-foreground">Loading vendors...</div>
          )}
          {!loadingVendors && vendors.length === 0 && (
            <div className="text-xs text-muted-foreground">No vendors available. Please add vendors first.</div>
          )}
        </div>

        {/* Selected vendor preview */}
        {selectedVendor && (
          <Card className="border-muted mb-4">
            <CardContent className="pt-4 space-y-2">
              <div className="text-sm font-medium">Selected Vendor</div>
              <div className="text-sm">{selectedVendor.legalName}</div>
              {selectedVendor.city && (
                <div className="text-xs text-muted-foreground">{selectedVendor.city}</div>
              )}
              {selectedVendor.status === 'approved' && (
                <Badge variant="outline" className="text-xs">Approved</Badge>
              )}
            </CardContent>
          </Card>
        )}

        {/* Action buttons */}
        <div className="flex gap-2 justify-end mt-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => {
              setShowVendorAssignModal(false);
              setSelectedVendorId('');
              setSelectedVendor(null);
            }}
            disabled={loading}
          >
            Cancel
          </Button>
          <Button
            onClick={handleAssignVendor}
            disabled={loading || !selectedVendorId}
          >
            {loading ? 'Assigning...' : 'Assign Vendor'}
          </Button>
        </div>
      </Modal>
    </div>
  );
}