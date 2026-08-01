'use client';

import { useState, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import { Separator } from '../components/ui/separator';
import { cn } from '../lib/utils';
import { SagaStatusBadge } from '../workflow/SagaStatusBadge';
import { workflowApi } from '../workflow/api';
import { PROCUREMENT_SAGA_STEPS, SAGA_TYPE_LABELS } from '../workflow/types';
import type { Saga } from '../workflow/types';
import type {
  CreateProcurementSagaRequest,
  ExecuteProcurementSagaStepRequest,
  ProjectSummary,
} from '@cerp/shared-types';
import { ProjectSelect } from './ProjectSelect';
import { VendorSelect } from './VendorSelect';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/modal/dialog';
import { Input } from '../components/ui/input';
import { ArrowRight, Plus, Loader2, ExternalLink } from 'lucide-react';

interface ProcurementSagaFormProps {
  tenantId: string;
  saga?: Saga | null;
  onSuccess?: (saga: Saga) => void;
  onCancel?: () => void;
}

type BiddingMode = 'INVITED' | 'OPEN_TENDER';

interface BiddingConfig {
  biddingMode: BiddingMode;
  vendorIds: string[];
  bidDeadline?: string;
  description?: string;
}

export function ProcurementSagaForm({ tenantId, saga, onSuccess, onCancel }: ProcurementSagaFormProps) {
  // Initialize form state from saga data if in edit mode
  const getInitialProjectId = () => saga?.data?.projectId || '';
  const getInitialDescription = () => (saga?.data?.description as string) || '';
  const getInitialMaterialId = () => (saga?.data?.materialId as string) || '';
  const getInitialQuantity = () => (saga?.data?.quantity?.toString()) || '';
  const [projectId, setProjectId] = useState(getInitialProjectId);
  const [selectedProject, setSelectedProject] = useState<ProjectSummary | null>(null);
  const [materialId, setMaterialId] = useState(getInitialMaterialId);
  const [quantity, setQuantity] = useState(getInitialQuantity);
  const [description, setDescription] = useState(getInitialDescription);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createdSaga, setCreatedSaga] = useState<Saga | null>(null);
  const [approvalReason, setApprovalReason] = useState('');

  // Bidding/Tender dialog state
  const [showBiddingDialog, setShowBiddingDialog] = useState(false);
  const [biddingConfig, setBiddingConfig] = useState<BiddingConfig>({
    biddingMode: 'INVITED',
    vendorIds: [],
  });
  const [creatingBidding, setCreatingBidding] = useState(false);
  const [createdBiddingSaga, setCreatedBiddingSaga] = useState<Saga | null>(null);

  const handleCreateSaga = useCallback(async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const request: CreateProcurementSagaRequest = {
        sagaType: 'PROCUREMENT_WORKFLOW',
        tenantId,
        data: {
          projectId,
          ...(materialId ? { materialId } : {}),
          ...(quantity ? { quantity: Number(quantity) } : {}),
          ...(description ? { description } : {}),
        },
      };

      const createdSaga = await workflowApi.createSaga(request as unknown as Parameters<typeof workflowApi.createSaga>[0]);
      setCreatedSaga(createdSaga);
      onSuccess?.(createdSaga);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create procurement saga');
    } finally {
      setLoading(false);
    }
  }, [tenantId, projectId, materialId, quantity, description, onSuccess]);

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
      setCreatedSaga(updatedSaga);
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

  // Handle creating bidding from procurement
  const handleCreateBidding = useCallback(async () => {
    if (!createdSaga) return;
    setCreatingBidding(true);
    setError(null);

    try {
      const biddingData: Record<string, unknown> = {
        procurementSagaId: createdSaga.sagaId,
        biddingMode: biddingConfig.biddingMode,
        projectId: createdSaga.data?.projectId || projectId,
      };

      if (biddingConfig.biddingMode === 'INVITED' && biddingConfig.vendorIds.length > 0) {
        biddingData.vendorIds = biddingConfig.vendorIds;
      }

      if (biddingConfig.description) {
        biddingData.description = biddingConfig.description;
      }

      if (biddingConfig.bidDeadline) {
        biddingData.inviteConfig = { bidDeadline: biddingConfig.bidDeadline };
      }

      const request = {
        sagaType: 'BIDDING_WORKFLOW' as const,
        tenantId,
        data: biddingData,
      };

      const biddingSaga = await workflowApi.createSaga(request as unknown as Parameters<typeof workflowApi.createSaga>[0]);
      setCreatedBiddingSaga(biddingSaga);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create bidding saga');
    } finally {
      setCreatingBidding(false);
    }
  }, [createdSaga, biddingConfig, tenantId, projectId]);

  const currentStepIndex = createdSaga
    ? PROCUREMENT_SAGA_STEPS.findIndex((s) => s.code === createdSaga.currentStep)
    : -1;

  // Get related bid sagas from saga data
  const relatedBiddingSagas = createdSaga?.data?.relatedBiddingSagas as Saga[] | undefined;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-xl font-semibold">{SAGA_TYPE_LABELS['PROCUREMENT_WORKFLOW'] || 'Procurement Workflow'}</h2>
        <p className="text-sm text-muted-foreground">
          {saga ? 'Edit Procurement Saga' : 'Create and manage procurement sagas: Request → Approval → PO → Delivery → Close'}
        </p>
      </div>

      {/* Saga Info Banner when editing */}
      {saga && !createdSaga && (
        <Card className="border-blue-200 bg-blue-50/50">
          <CardContent className="pt-4 space-y-2">
            <div className="text-sm font-medium">Editing Procurement Saga</div>
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
          </CardContent>
        </Card>
      )}

      {/* Created Saga Status with Bidding Section */}
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

            {/* Action Buttons based on current step */}
            {createdSaga.status === 'RUNNING' && (
              <div className="flex flex-wrap gap-2">
                {createdSaga.currentStep === 'CreateProcurementRequest' && (
                  <Button
                    onClick={() => handleExecuteStep('SubmitForApproval')}
                    disabled={loading}
                  >
                    Submit for Approval
                  </Button>
                )}

                {createdSaga.currentStep === 'SubmitForApproval' && (
                  <>
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
                      onChange={(e) => setApprovalReason(e.target.value)}
                      className="max-w-xs"
                    />
                  </>
                )}

                {createdSaga.currentStep === 'ApproveRequest' && (
                  <Button
                    onClick={() => handleExecuteStep('IssuePurchaseOrder')}
                    disabled={loading}
                  >
                    Issue Purchase Order
                  </Button>
                )}

                {createdSaga.currentStep === 'IssuePurchaseOrder' && (
                  <Button
                    onClick={() => handleExecuteStep('ReceiveDelivery')}
                    disabled={loading}
                  >
                    Receive Delivery
                  </Button>
                )}

                {createdSaga.currentStep === 'ReceiveDelivery' && (
                  <Button
                    onClick={() => handleExecuteStep('ConfirmDelivery')}
                    disabled={loading}
                  >
                    Confirm Delivery
                  </Button>
                )}

                {createdSaga.currentStep === 'ConfirmDelivery' && (
                  <Button
                    onClick={() => handleExecuteStep('CloseProcurement')}
                    disabled={loading}
                  >
                    Close Procurement
                  </Button>
                )}

                {(createdSaga.currentStep === 'CreateProcurementRequest' || createdSaga.currentStep === 'SubmitForApproval') && (
                  <Button
                    variant="outline"
                    onClick={handleCompensate}
                    disabled={loading}
                  >
                    Cancel Saga
                  </Button>
                )}
              </div>
            )}

            {/* Create Bidding/Tender Button */}
            {(createdSaga.status === 'RUNNING' || createdSaga.status === 'COMPLETED') && (
              <>
                <Separator />
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-sm font-medium">Bidding & Tender</h3>
                    <p className="text-xs text-muted-foreground">
                      Create a bidding/saga related to this procurement
                    </p>
                  </div>
                  <Button
                    onClick={() => {
                      setShowBiddingDialog(true);
                    }}
                    disabled={loading}
                  >
                    <Plus className="w-4 h-4 mr-2" />
                    Create Bidding/Tender
                  </Button>
                </div>

                {/* Related Bidding Sagas */}
                {relatedBiddingSagas && relatedBiddingSagas.length > 0 && (
                  <div className="space-y-2">
                    <Label className="text-xs">Related Bidding Sagas</Label>
                    {relatedBiddingSagas.map((bidSaga) => (
                      <Card key={bidSaga.sagaId} className="p-3">
                        <div className="flex items-center justify-between">
                          <div className="space-y-1">
                            <div className="flex items-center gap-2">
                              <Badge variant="outline">BIDDING_WORKFLOW</Badge>
                              <SagaStatusBadge status={bidSaga.status} />
                            </div>
                            <div className="text-xs text-muted-foreground font-mono">
                              {bidSaga.sagaId.slice(0, 8)}...
                            </div>
                            <div className="text-xs">
                              Current: {SAGA_TYPE_LABELS[bidSaga.currentStep ?? ''] ?? bidSaga.currentStep ?? 'N/A'}
                            </div>
                          </div>
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => window.open(`/sagas/${bidSaga.sagaId}`, '_blank')}
                          >
                            <ExternalLink className="w-3 h-3 mr-1" />
                            View
                          </Button>
                        </div>
                      </Card>
                    ))}
                  </div>
                )}
              </>
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

      {/* Creation Form */}
      {!createdSaga && (
        <Card>
          <CardHeader>
            <CardTitle>{saga ? 'Edit Procurement Request' : 'Create New Procurement Request'}</CardTitle>
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
                placeholder="Select a project for this procurement..."
                required
                helpText="Select the project this procurement request is for"
              />

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="materialId">Material ID</Label>
                  <Input
                    id="materialId"
                    value={materialId}
                    onChange={(e) => setMaterialId(e.target.value)}
                    placeholder="uuid or material identifier"
                  />
                </div>
                <div>
                  <Label htmlFor="quantity">Quantity</Label>
                  <Input
                    id="quantity"
                    type="number"
                    min="1"
                    value={quantity}
                    onChange={(e) => setQuantity(e.target.value)}
                    placeholder="1"
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Brief description of the procurement..."
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
                <Button type="submit" disabled={loading || !projectId}>
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

      {/* Create Bidding Dialog */}
      <Dialog open={showBiddingDialog} onOpenChange={setShowBiddingDialog}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Create Bidding/Tender</DialogTitle>
            <DialogDescription>
              Create a bidding saga related to this procurement request.
              The bidding saga will track the tender process from vendor invitations through award.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            {/* Procurement Reference */}
            <Card>
              <CardContent className="pt-4">
                <div className="text-sm">
                  <span className="text-muted-foreground">Related to Procurement: </span>
                  <span className="font-mono">{createdSaga?.sagaId.slice(0, 8) || saga?.sagaId.slice(0, 8)}...</span>
                </div>
                {selectedProject && (
                  <div className="text-sm mt-1">
                    <span className="text-muted-foreground">Project: </span>
                    <span>{selectedProject.name}</span>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Bidding Mode - Simple Radio Implementation */}
            <div className="space-y-2">
              <Label>Bidding Mode</Label>
              <div className="space-y-2">
                <div
                  className={cn(
                    "flex items-center gap-2 rounded-md border p-3 cursor-pointer transition-colors",
                    biddingConfig.biddingMode === 'INVITED' ? "border-primary bg-primary/5" : "border-input hover:bg-accent"
                  )}
                  onClick={() => setBiddingConfig(prev => ({
                    ...prev,
                    biddingMode: 'INVITED',
                    vendorIds: prev.vendorIds,
                  }))}
                >
                  <div className={cn(
                    "w-4 h-4 rounded-full border flex items-center justify-center",
                    biddingConfig.biddingMode === 'INVITED' ? "bg-primary border-primary" : "border-input"
                  )}>
                    {biddingConfig.biddingMode === 'INVITED' && (
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
                    biddingConfig.biddingMode === 'OPEN_TENDER' ? "border-primary bg-primary/5" : "border-input hover:bg-accent"
                  )}
                  onClick={() => setBiddingConfig(prev => ({
                    ...prev,
                    biddingMode: 'OPEN_TENDER',
                    vendorIds: [],
                  }))}
                >
                  <div className={cn(
                    "w-4 h-4 rounded-full border flex items-center justify-center",
                    biddingConfig.biddingMode === 'OPEN_TENDER' ? "bg-primary border-primary" : "border-input"
                  )}>
                    {biddingConfig.biddingMode === 'OPEN_TENDER' && (
                      <div className="w-2 h-2 rounded-full bg-primary-foreground" />
                    )}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium">Open Tender</span>
                      <Badge variant="outline" className="text-xs">OPEN_TENDER</Badge>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      All approved vendors in the system can submit bids
                    </p>
                  </div>
                </div>
              </div>
              <p className="text-xs text-muted-foreground">
                {biddingConfig.biddingMode === 'INVITED'
                  ? 'Select specific vendors to invite for bidding.'
                  : 'All approved vendors in the system can submit bids.'}
              </p>
            </div>

            {/* Vendor Selection (only for INVITED mode) */}
            {biddingConfig.biddingMode === 'INVITED' && (
              <VendorSelect
                value={biddingConfig.vendorIds}
                onChange={(vendorIds, _vendors) => setBiddingConfig(prev => ({
                  ...prev,
                  vendorIds,
                }))}
                label="Invited Vendors"
                placeholder="Select vendors to invite..."
                mode="multi"
                required
                helpText="Select vendors to invite for this bidding"
              />
            )}

            {/* Bid Deadline */}
            <div className="space-y-2">
              <Label htmlFor="bidDeadline">Bid Deadline (Optional)</Label>
              <Input
                id="bidDeadline"
                type="datetime-local"
                value={biddingConfig.bidDeadline || ''}
                onChange={(e) => setBiddingConfig(prev => ({
                  ...prev,
                  bidDeadline: e.target.value || undefined,
                }))}
              />
              <p className="text-xs text-muted-foreground">Deadline for vendors to submit their bids</p>
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label htmlFor="biddingDescription">Bidding Description (Optional)</Label>
              <Textarea
                id="biddingDescription"
                value={biddingConfig.description || ''}
                onChange={(e) => setBiddingConfig(prev => ({
                  ...prev,
                  description: e.target.value,
                }))}
                placeholder="Additional details about the bidding requirements..."
                rows={3}
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setShowBiddingDialog(false)}
              disabled={creatingBidding}
            >
              Cancel
            </Button>
            <Button
              onClick={handleCreateBidding}
              disabled={creatingBidding || (biddingConfig.biddingMode === 'INVITED' && biddingConfig.vendorIds.length === 0)}
            >
              {creatingBidding ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  Creating...
                </>
              ) : (
                <>
                  <ArrowRight className="w-4 h-4 mr-2" />
                  Create Bidding
                </>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Created Bidding Saga Status */}
      {createdBiddingSaga && (
        <Card className="border-green-200 bg-green-50/50">
          <CardHeader className="pb-3">
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">Bidding Saga Created</CardTitle>
              <SagaStatusBadge status={createdBiddingSaga.status} />
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-2 text-sm">
              <div>
                <span className="text-muted-foreground">Bidding Saga ID: </span>
                <span className="font-mono">{createdBiddingSaga.sagaId.slice(0, 8)}...</span>
              </div>
              <div>
                <span className="text-muted-foreground">Mode: </span>
                <Badge variant="outline">{biddingConfig.biddingMode}</Badge>
              </div>
              <div className="flex gap-2 mt-4">
                <Button
                  variant="outline"
                  onClick={() => window.open(`/sagas/${createdBiddingSaga.sagaId}`, '_blank')}
                >
                  <ExternalLink className="w-3 h-3 mr-1" />
                  View Bidding Details
                </Button>
                <Button
                  variant="ghost"
                  onClick={() => {
                    setCreatedBiddingSaga(null);
                    setShowBiddingDialog(false);
                  }}
                >
                  Close
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}