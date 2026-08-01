'use client';

import { useState, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import { cn } from '../lib/utils';
import { SagaStatusBadge } from '../workflow/SagaStatusBadge';
import { workflowApi } from '../workflow/api';
import { BIDDING_SAGA_STEPS, SAGA_TYPE_LABELS } from '../workflow/types';
import type { Saga } from '../workflow/types';
import type {
  CreateBiddingSagaRequest,
  ExecuteBiddingSagaStepRequest,
  ProjectSummary,
} from '@cerp/shared-types';
import { ProjectSelect } from './ProjectSelect';
import { VendorSelect } from './VendorSelect';

interface BiddingSagaFormProps {
  tenantId: string;
  saga?: Saga | null;
  onSuccess?: (saga: Saga) => void;
  onCancel?: () => void;
  initialProcurementSagaId?: string;
}

export function BiddingSagaForm({ tenantId, saga, onSuccess, onCancel, initialProcurementSagaId }: BiddingSagaFormProps) {
  // Initialize form state from saga data if in edit mode
  const getInitialProjectId = () => saga?.data?.projectId || '';
  const getInitialBiddingMode = () => ((saga?.data?.biddingMode as string) || 'INVITED') as 'INVITED' | 'OPEN_TENDER';
  const getInitialVendorIds = () => {
    const vendorIds = saga?.data?.vendorIds as string[] | undefined;
    return vendorIds || [];
  };
  const getInitialBidDeadline = () => {
    const inviteConfig = saga?.data?.inviteConfig as { bidDeadline?: string } | undefined;
    return inviteConfig?.bidDeadline || '';
  };
  const getInitialDescription = () => (saga?.data?.description as string) || '';

  const [projectId, setProjectId] = useState(getInitialProjectId);
  const [selectedProject, setSelectedProject] = useState<ProjectSummary | null>(null);
  const [biddingMode, setBiddingMode] = useState<'INVITED' | 'OPEN_TENDER'>(getInitialBiddingMode);
  const [selectedVendorIds, setSelectedVendorIds] = useState<string[]>(getInitialVendorIds);
  const [bidDeadline, setBidDeadline] = useState(getInitialBidDeadline);
  const [description, setDescription] = useState(getInitialDescription);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdSaga, setCreatedSaga] = useState<Saga | null>(null);

  // Bid collection state
  const [bids, setBids] = useState<Array<{ vendorId: string; price: string; timeline: string; notes: string }>>([
    { vendorId: '', price: '', timeline: '', notes: '' },
  ]);
  const [evaluationNotes, setEvaluationNotes] = useState('');

  // Load related procurement saga reference if provided (for future use when loading from initialProcurementSagaId)
  const [relatedProcurementSaga, setRelatedProcurementSaga] = useState<Saga | null>(null);
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

      const request: CreateBiddingSagaRequest = {
        sagaType: 'BIDDING_WORKFLOW',
        tenantId,
        data: biddingData as CreateBiddingSagaRequest['data'],
      };

      const saga = await workflowApi.createSaga(request as unknown as Parameters<typeof workflowApi.createSaga>[0]);
      setCreatedSaga(saga);
      onSuccess?.(saga);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create bidding saga');
    } finally {
      setLoading(false);
    }
  }, [tenantId, projectId, biddingMode, selectedVendorIds, bidDeadline, description, initialProcurementSagaId, onSuccess]);

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
      setCreatedSaga(updatedSaga);
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to execute step: ${command}`);
    } finally {
      setLoading(false);
    }
  }, [createdSaga]);

  const handleAddBid = useCallback(() => {
    setBids((prev) => [...prev, { vendorId: '', price: '', timeline: '', notes: '' }]);
  }, []);

  const handleUpdateBid = useCallback((index: number, field: keyof typeof bids[0], value: string) => {
    setBids((prev) =>
      prev.map((bid, i) => (i === index ? { ...bid, [field]: value } : bid))
    );
  }, []);

  const handleRemoveBid = useCallback((index: number) => {
    setBids((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const handleCollectBids = useCallback(async () => {
    if (!createdSaga) return;

    const validBids = bids.filter((b) => b.vendorId.trim() && b.price.trim());
    if (validBids.length === 0) {
      setError('At least one valid bid is required (vendorId and price)');
      return;
    }

    const bidsPayload = validBids.map((b) => ({
      vendorId: b.vendorId.trim(),
      price: Number(b.price) || 0,
      timeline: b.timeline || undefined,
      notes: b.notes || undefined,
    }));

    await handleExecuteStep('Bidding.CollectBids', { bids: bidsPayload });
  }, [createdSaga, bids, handleExecuteStep]);

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

  const currentStepIndex = createdSaga
    ? BIDDING_SAGA_STEPS.findIndex((s) => s.code === createdSaga.currentStep)
    : -1;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-semibold">{SAGA_TYPE_LABELS['BIDDING_WORKFLOW'] || 'Marketplace Bidding'}</h2>
        <p className="text-sm text-muted-foreground">
          {saga ? 'Edit Bidding Saga' : 'Create and manage bidding sagas: Tender → Vendor Invitation → Bidding → Evaluation → Award'}
        </p>
      </div>

      {/* Saga Info Banner when editing */}
      {saga && !createdSaga && (
        <Card className="border-blue-200 bg-blue-50/50">
          <CardContent className="pt-4 space-y-2">
            <div className="text-sm font-medium">Editing Bidding Saga</div>
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
              {BIDDING_SAGA_STEPS.map((step, index) => {
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

            {/* Action Buttons based on current step */}
            {createdSaga.status === 'RUNNING' && (
              <div className="space-y-4 mt-4">
                {createdSaga.currentStep === 'Tender.CreateAndOpen' && (
                  <Button
                    onClick={() => handleExecuteStep('Tender.InviteVendors', {
                      vendorIds: (createdSaga.data?.invitedVendorIds as string[]) || [],
                      inviteConfig: createdSaga.data?.inviteConfig,
                    })}
                    disabled={loading}
                  >
                    Invite Vendors
                  </Button>
                )}

                {createdSaga.currentStep === 'Tender.InviteVendors' && (
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

                {createdSaga.currentStep === 'Bidding.CollectBids' && (
                  <div className="space-y-3">
                    <h4 className="text-sm font-medium">Submit Bids</h4>
                    {bids.map((bid, index) => (
                      <div key={index} className="grid grid-cols-1 sm:grid-cols-4 gap-2 p-3 border rounded">
                        <Input
                          placeholder="Vendor ID"
                          value={bid.vendorId}
                          onChange={(e) => handleUpdateBid(index, 'vendorId', e.target.value)}
                        />
                        <Input
                          type="number"
                          placeholder="Price"
                          value={bid.price}
                          onChange={(e) => handleUpdateBid(index, 'price', e.target.value)}
                        />
                        <Input
                          placeholder="Timeline"
                          value={bid.timeline}
                          onChange={(e) => handleUpdateBid(index, 'timeline', e.target.value)}
                        />
                        <div className="flex gap-2">
                          <Input
                            placeholder="Notes"
                            value={bid.notes}
                            onChange={(e) => handleUpdateBid(index, 'notes', e.target.value)}
                          />
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleRemoveBid(index)}
                            disabled={bids.length === 1}
                          >
                            ×
                          </Button>
                        </div>
                      </div>
                    ))}
                    <div className="flex gap-2">
                      <Button variant="outline" size="sm" onClick={handleAddBid}>
                        Add Bid
                      </Button>
                      <Button size="sm" onClick={handleCollectBids} disabled={loading}>
                        Submit Bids
                      </Button>
                    </div>
                  </div>
                )}

                {createdSaga.currentStep === 'Bidding.Evaluate' && (
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

                {createdSaga.currentStep === 'Tender.Award' && (
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

      {/* Creation Form */}
      {!createdSaga && (
        <Card>
              <CardHeader>
                <CardTitle>{saga ? 'Edit Bidding Saga' : 'Create New Bidding Saga'}</CardTitle>
              </CardHeader>
          <CardContent>
            <form onSubmit={handleCreateSaga} className="space-y-4">
              {/* Project Selection */}
              <ProjectSelect
                onChange={(pId, project) => {
                  setProjectId(pId);
                  setSelectedProject(project);
                }}
                label="Project"
                placeholder="Select a project for this bidding..."
                required
                helpText="Select the project this bidding is for"
              />

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
                <Button type="submit" disabled={loading || !projectId || (biddingMode === 'INVITED' && selectedVendorIds.length === 0)}>
                  {loading ? 'Updating...' : 'Update Bidding Saga'}
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
    </div>
  );
}