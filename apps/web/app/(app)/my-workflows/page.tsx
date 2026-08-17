'use client';

import { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useRouter, usePathname } from 'next/navigation';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@cerp/core-web/components/ui/tabs';
import { Card, CardContent, CardHeader, CardTitle } from '@cerp/core-web/components/ui/card';
import { Badge } from '@cerp/core-web/components/ui/badge';
import { Button } from '@cerp/core-web/components/ui/button';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@cerp/core-web/components/ui/table';
import { workflowApi, SAGA_TYPE_LABELS, SagaDetailDialog, WorkflowDetailDialog } from '@cerp/core-web/workflow';
import { listWorkflowsByAccessMode } from '@cerp/core-web/construction-engineering';
import type { Saga, SagaQuery } from '@cerp/core-web/workflow';
import type { Workflow } from '@cerp/shared-types';
import { UserPlus, UserCheck, AlertCircle } from 'lucide-react';

const PAGE_SIZE = 100;

// ── Unified Type ──

type WorkflowSource = 'saga' | 'ce';

interface UnifiedWorkflowItem {
  __typename: 'Saga' | 'CEWorkflow';
  source: WorkflowSource;
  id: string;
  type: string;
  status: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  detailsUrl: string;
}

const getStatusLabel = (source: WorkflowSource, status: string): string => {
  if (source === 'saga') {
    switch (status) {
      case 'COMPLETED': case 'COMPENSATED': return 'Completed';
      case 'FAILED': case 'ABORTED': return 'Failed';
      case 'RUNNING': case 'IN_PROGRESS': return 'Running';
      case 'PAUSED': case 'REQUIRES_MANUAL_INTERVENTION': return 'Paused';
      default: return status;
    }
  }
  switch (status) {
    case 'COMPLETED': case 'APPROVED': return 'Approved';
    case 'CANCELLED': case 'REJECTED': return 'Rejected';
    case 'ACTIVE': case 'IN_PROGRESS': return 'Active';
    case 'PAUSED': return 'Paused';
    case 'PLANNED': return 'Planned';
    default: return status;
  }
};

const getTypeLabel = (source: WorkflowSource, typeOrCode: string): string => {
  if (source === 'saga') {
    return SAGA_TYPE_LABELS[typeOrCode] || typeOrCode;
  }
  return typeOrCode || 'CE Workflow';
};

const getSourceBadge = (source: WorkflowSource) => {
  if (source === 'saga') {
    return <Badge variant="outline" className="text-xs font-mono">Saga</Badge>;
  }
  return <Badge variant="secondary" className="text-xs">CE Workflow</Badge>;
};

export default function MyWorkflowsPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const activeTab = searchParams.get('tab') || 'created';

  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selectedSource, setSelectedSource] = useState<WorkflowSource | null>(null);

  const [createdItems, setCreatedItems] = useState<UnifiedWorkflowItem[]>([]);
  const [assignedItems, setAssignedItems] = useState<UnifiedWorkflowItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAllWorkflows = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [createdSagas, assignedSagas, createdCE, assignedCE] = await Promise.all([
        workflowApi.listSagas({
          accessMode: 'created',
          page: 1,
          pageSize: PAGE_SIZE,
          sortBy: 'updatedAt',
          sortOrder: 'desc',
        } as SagaQuery),
        workflowApi.listSagas({
          accessMode: 'assigned',
          page: 1,
          pageSize: PAGE_SIZE,
          sortBy: 'updatedAt',
          sortOrder: 'desc',
        } as SagaQuery),
        listWorkflowsByAccessMode('created'),
        listWorkflowsByAccessMode('assigned'),
      ]);

      // Convert Sagas to unified format
      const createdSagaItems: UnifiedWorkflowItem[] = (createdSagas.items || []).map((saga: Saga) => ({
        __typename: 'Saga',
        source: 'saga',
        id: saga.sagaId,
        type: saga.sagaType,
        status: saga.status,
        createdBy: saga.createdBy ?? '',
        createdAt: saga.createdAt,
        updatedAt: saga.updatedAt,
        detailsUrl: `/workflows/saga/${saga.sagaId}`,
      }));

      const assignedSagaItems: UnifiedWorkflowItem[] = (assignedSagas.items || []).map((saga: Saga) => ({
        __typename: 'Saga',
        source: 'saga',
        id: saga.sagaId,
        type: saga.sagaType,
        status: saga.status,
        createdBy: saga.createdBy ?? '',
        createdAt: saga.createdAt,
        updatedAt: saga.updatedAt,
        detailsUrl: `/workflows/saga/${saga.sagaId}`,
      }));

      // Convert CE Workflows to unified format
      const createdCEItems: UnifiedWorkflowItem[] = (createdCE || []).map((wf: Workflow) => ({
        __typename: 'CEWorkflow',
        source: 'ce',
        id: wf.id,
        type: wf.templateCode || wf.code,
        status: wf.status,
        createdBy: wf.updatedAt,
        createdAt: wf.createdAt,
        updatedAt: wf.updatedAt,
        detailsUrl: `/workflows/workflow/${wf.id}`,
      }));

      const assignedCEItems: UnifiedWorkflowItem[] = (assignedCE || []).map((wf: Workflow) => ({
        __typename: 'CEWorkflow',
        source: 'ce',
        id: wf.id,
        type: wf.templateCode || wf.code,
        status: wf.status,
        createdBy: wf.updatedAt,
        createdAt: wf.createdAt,
        updatedAt: wf.updatedAt,
        detailsUrl: `/workflows/workflow/${wf.id}`,
      }));

      // Sort all items by updatedAt descending
      const sortedCreated = [...createdSagaItems, ...createdCEItems]
        .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());
      const sortedAssigned = [...assignedSagaItems, ...assignedCEItems]
        .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime());

      setCreatedItems(sortedCreated);
      setAssignedItems(sortedAssigned);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load workflows');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchAllWorkflows();
  }, [fetchAllWorkflows]);

  useEffect(() => {
    const currentTab = searchParams.get('tab') || 'created';
    if (currentTab !== activeTab) {
      const params = new URLSearchParams(searchParams);
      params.set('tab', activeTab);
      router.replace(`${pathname}?${params.toString()}`);
    }
  }, [activeTab, searchParams, router, pathname]);

  const getStatusColor = (status: string): 'default' | 'success' | 'destructive' | 'secondary' | 'outline' => {
    switch (status) {
      case 'COMPLETED': case 'COMPENSATED': return 'success';
      case 'FAILED': case 'ABORTED': return 'destructive';
      case 'RUNNING': case 'IN_PROGRESS': return 'default';
      case 'PAUSED': case 'REQUIRES_MANUAL_INTERVENTION': return 'secondary';
      default: return 'outline';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">My Workflows</h1>
          <p className="text-muted-foreground mt-1">View workflows you have created or been assigned to</p>
        </div>
        <Button variant="outline" onClick={fetchAllWorkflows}>Refresh</Button>
      </div>

      <Tabs value={activeTab} onValueChange={(val) => {
        const params = new URLSearchParams(searchParams);
        params.set('tab', val);
        router.replace(`${pathname}?${params.toString()}`);
      }} className="space-y-6">
        <TabsList>
          <TabsTrigger value="created" className="gap-2">
            <UserPlus className="h-4 w-4" /> Created by Me
          </TabsTrigger>
          <TabsTrigger value="assigned" className="gap-2">
            <UserCheck className="h-4 w-4" /> Assigned to Me
          </TabsTrigger>
        </TabsList>

        <TabsContent value="created">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <UserPlus className="h-4 w-4" /> Created by Me{' '}
                <Badge variant="secondary">{createdItems.length}</Badge>
              </CardTitle>
            </CardHeader>
            <CardContent>
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 flex items-center gap-2">
                  <AlertCircle className="h-4 w-4" /> {error}
                </div>
              )}
              {loading ? (
                <div className="text-center py-8 text-muted-foreground">Loading workflows...</div>
              ) : createdItems.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  <p className="text-lg font-medium">No workflows created</p>
                  <p className="text-sm mt-1">Workflows you create will appear here</p>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Source</TableHead>
                      <TableHead>Workflow ID</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Created</TableHead>
                      <TableHead>Updated</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {createdItems.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell>{getSourceBadge(item.source)}</TableCell>
                        <TableCell className="font-mono text-xs">{item.id.slice(0, 8)}...</TableCell>
                        <TableCell>{getTypeLabel(item.source, item.type)}</TableCell>
                        <TableCell>
                          <Badge variant={getStatusColor(item.status)} className="text-xs">
                            {getStatusLabel(item.source, item.status)}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">{new Date(item.createdAt).toLocaleString()}</TableCell>
                        <TableCell className="text-sm text-muted-foreground">{new Date(item.updatedAt).toLocaleString()}</TableCell>
                        <TableCell>
                          <Button variant="ghost" size="sm" onClick={() => { setSelectedId(item.id); setSelectedSource(item.source); setDialogOpen(true); }}>
                            View Details
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="assigned">
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <UserCheck className="h-4 w-4" /> Assigned to Me{' '}
                <Badge variant="secondary">{assignedItems.length}</Badge>
              </CardTitle>
            </CardHeader>
            <CardContent>
              {error && (
                <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 flex items-center gap-2">
                  <AlertCircle className="h-4 w-4" /> {error}
                </div>
              )}
              {loading ? (
                <div className="text-center py-8 text-muted-foreground">Loading workflows...</div>
              ) : assignedItems.length === 0 ? (
                <div className="text-center py-8 text-muted-foreground">
                  <p className="text-lg font-medium">No workflows assigned</p>
                  <p className="text-sm mt-1">Workflows assigned to you will appear here</p>
                </div>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Source</TableHead>
                      <TableHead>Workflow ID</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Created By</TableHead>
                      <TableHead>Created</TableHead>
                      <TableHead>Updated</TableHead>
                      <TableHead>Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {assignedItems.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell>{getSourceBadge(item.source)}</TableCell>
                        <TableCell className="font-mono text-xs">{item.id.slice(0, 8)}...</TableCell>
                        <TableCell>{getTypeLabel(item.source, item.type)}</TableCell>
                        <TableCell>
                          <Badge variant={getStatusColor(item.status)} className="text-xs">
                            {getStatusLabel(item.source, item.status)}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {item.createdBy ? item.createdBy.slice(0, 8) + '...' : '\u2014'}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">{new Date(item.createdAt).toLocaleString()}</TableCell>
                        <TableCell className="text-sm text-muted-foreground">{new Date(item.updatedAt).toLocaleString()}</TableCell>
                        <TableCell>
                          <Button variant="ghost" size="sm" onClick={() => { setSelectedId(item.id); setSelectedSource(item.source); setDialogOpen(true); }}>
                            View Details
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {selectedId && selectedSource === 'saga' && (
        <SagaDetailDialog
          sagaId={selectedId}
          open={dialogOpen}
          onOpenChange={(open) => { if (!open) setDialogOpen(false); }}
          onRefresh={() => void fetchAllWorkflows()}
        />
      )}
      {selectedId && selectedSource === 'ce' && (
        <WorkflowDetailDialog
          workflowId={selectedId}
          open={dialogOpen}
          onOpenChange={(open) => { if (!open) setDialogOpen(false); }}
          onRefresh={() => void fetchAllWorkflows()}
        />
      )}
    </div>
  );
}