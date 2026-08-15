'use client';

import { useState, useEffect, useCallback, useRef } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { SagaStatusBadge } from './SagaStatusBadge';
import { SagaRowActions } from './SagaRowActions';
import { useSagaAttachmentSync } from './saga-attachments';
import { workflowApi } from './api';
import type { Saga, SagaQuery, SagaQueryResponse, SagaAccessMode } from './types';
import { SAGA_TYPE_LABELS, getSagaProjectId } from './types';

const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

export function SagaPage({ vendorId, accessMode }: { vendorId?: string; accessMode?: SagaAccessMode }) {
  const [sagas, setSagas] = useState<Saga[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [total, setTotal] = useState(0);

  const [filters, setFilters] = useState<SagaQuery>({
    sagaType: undefined,
    status: undefined,
    vendorId: vendorId ?? undefined,
    tenantId: undefined,
    accessMode: accessMode,
    page: 1,
    pageSize: 25,
    sortBy: 'updatedAt',
    sortOrder: 'desc',
  });

  const [search, setSearch] = useState('');

  // Owned once here (not per row): the upload queue is one shared IndexedDB store across every
  // saga row on the page, so a single background-sync listener must drain it, not one per row.
  const { syncNow: syncAttachmentsNow, isSyncing: attachmentsSyncing } = useSagaAttachmentSync();

  const fetchSagas = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result: SagaQueryResponse = await workflowApi.listSagas(filters);
      setSagas(result.items);
      setTotal(result.total);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load sagas');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  // Refetches whenever a saga-attachment sync round finishes (isSyncing true -> false), not just
  // right after enqueueing -- this also covers uploads that only complete later, e.g. once the
  // online-event listener fires after being offline, so the attachment icon/count update on
  // their own without the user needing to reload the page.
  const wasSyncingAttachmentsRef = useRef(false);
  useEffect(() => {
    if (wasSyncingAttachmentsRef.current && !attachmentsSyncing) {
      fetchSagas();
    }
    wasSyncingAttachmentsRef.current = attachmentsSyncing;
  }, [attachmentsSyncing, fetchSagas]);

  const initialized = useRef(false);

  useEffect(() => {
    if (!initialized.current) {
      initialized.current = true;
      fetchSagas();
    }
  }, [fetchSagas]);

  // Sync vendorId prop into filters
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setFilters((prev) => {
      if (prev.vendorId !== vendorId) {
        return { ...prev, vendorId: vendorId ?? undefined, page: 1 };
      }
      return prev;
    });
    fetchSagas();
  }, [vendorId, fetchSagas]);

  const handleStatusFilter = (status: string) => {
    setFilters((prev) => ({
      ...prev,
      status: prev.status === status ? undefined : (status as SagaQuery['status']),
      page: 1,
    }));
  };

  const handlePageChange = (page: number) => {
    setFilters((prev) => ({ ...prev, page }));
  };

  const handlePageSizeChange = (pageSize: number) => {
    setFilters((prev) => ({ ...prev, pageSize, page: 1 }));
  };

  const getStatusFilterOptions = () => [
    { value: 'RUNNING', label: 'Running', color: 'bg-blue-100 text-blue-800' },
    { value: 'COMPLETED', label: 'Completed', color: 'bg-green-100 text-green-800' },
    { value: 'FAILED', label: 'Failed', color: 'bg-red-100 text-red-800' },
    { value: 'COMPENSATING', label: 'Compensating', color: 'bg-yellow-100 text-yellow-800' },
    {
      value: 'REQUIRES_MANUAL_INTERVENTION',
      label: 'Needs Attention',
      color: 'bg-orange-100 text-orange-800',
    },
  ];

  const totalPages = Math.ceil(total / (filters.pageSize ?? 25));

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Workflow Saga Monitoring</h1>
          <p className="text-muted-foreground mt-1">
            Track and manage long-running business workflows
          </p>
        </div>
        <Button variant="outline" onClick={fetchSagas}>
          Refresh
        </Button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
        {getStatusFilterOptions().map((option) => (
          <Card
            key={option.value}
            className="cursor-pointer hover:shadow-md transition-shadow"
            onClick={() => handleStatusFilter(option.value)}
          >
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-medium">{option.label}</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {loading ? '...' : sagas.filter((s) => s.status === option.value).length}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Filters */}
      <Card>
        <CardHeader className="pb-3">
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">Filters</CardTitle>
            <div className="flex items-center gap-2">
              <Input
                placeholder="Search by saga ID..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-64"
              />
              <Button
                variant="outline"
                size="sm"
                onClick={() => {
                  setFilters((prev) => ({
                    ...prev,
                    sagaType: undefined,
                    status: undefined,
                    tenantId: undefined,
                    page: 1,
                  }));
                  setSearch('');
                }}
              >
                Clear All
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {/* Active Filters */}
          <div className="flex flex-wrap gap-2 mb-4">
            {filters.sagaType && (
              <Badge variant="secondary">
                Type: {filters.sagaType}
                <button
                  className="ml-1 hover:text-red-600"
                  onClick={() => setFilters((prev) => ({ ...prev, sagaType: undefined }))}
                >
                  ×
                </button>
              </Badge>
            )}
            {filters.status && (
              <Badge variant="secondary">
                Status: {filters.status}
                <button
                  className="ml-1 hover:text-red-600"
                  onClick={() => setFilters((prev) => ({ ...prev, status: undefined }))}
                >
                  ×
                </button>
              </Badge>
            )}
          </div>

          {/* Table */}
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {loading ? (
            <div className="text-center py-8 text-muted-foreground">Loading sagas...</div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Saga ID</TableHead>
                    <TableHead>Type</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Created By</TableHead>
                    <TableHead>Assigned To</TableHead>
                    <TableHead>Tenant</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead>Updated</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {sagas.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={9} className="text-center text-muted-foreground">
                        No sagas found
                      </TableCell>
                    </TableRow>
                  ) : (
                    sagas.map((saga) => (
                      <TableRow key={saga.sagaId}>
                        <TableCell className="font-mono text-xs">
                          {saga.sagaId.slice(0, 8)}...
                        </TableCell>
                        <TableCell>{SAGA_TYPE_LABELS[saga.sagaType] || saga.sagaType}</TableCell>
                        <TableCell>
                          <SagaStatusBadge status={saga.status} />
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {saga.createdBy ? saga.createdBy.slice(0, 8) + '...' : '—'}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {saga.assignedTo ? saga.assignedTo.slice(0, 8) + '...' : '—'}
                        </TableCell>
                        <TableCell className="font-mono text-xs">
                          {saga.tenantId.slice(0, 8)}...
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {new Date(saga.createdAt).toLocaleString()}
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {new Date(saga.updatedAt).toLocaleString()}
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() =>
                                (window.location.href = `/workflows/saga/${saga.sagaId}`)
                              }
                            >
                              View Details
                            </Button>
                            <SagaRowActions
                              saga={saga}
                              projectId={getSagaProjectId(saga)}
                              onSagaUpdated={fetchSagas}
                              onAttachmentQueued={() => void syncAttachmentsNow()}
                            />
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>

              {/* Pagination */}
              <div className="flex items-center justify-between mt-4">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">Page size:</span>
                  <select
                    className="border rounded px-2 py-1 text-sm"
                    value={filters.pageSize}
                    onChange={(e) => handlePageSizeChange(Number(e.target.value))}
                  >
                    {PAGE_SIZE_OPTIONS.map((size) => (
                      <option key={size} value={size}>
                        {size}
                      </option>
                    ))}
                  </select>
                  <span className="text-sm text-muted-foreground">{total} total</span>
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={filters.page === 1}
                    onClick={() => handlePageChange(filters.page! - 1)}
                  >
                    Previous
                  </Button>
                  <span className="text-sm">
                    Page {filters.page} of {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={filters.page! >= totalPages}
                    onClick={() => handlePageChange(filters.page! + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
