'use client';

import { useState, useCallback } from 'react';
import { Button } from '../components/ui/button';
import { PageHeader } from '../shell/PageHeader';
import { Modal } from '../components/ui/modal';
import { ProcurementSagaForm } from './ProcurementSagaForm';
import { ProcurementSagaList } from './ProcurementSagaList';
import { BiddingSagaList } from './BiddingSagaList';
import { BiddingSagaForm } from './BiddingSagaForm';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import type { Saga } from '../workflow/types';

export function ProcurementPageBody() {
  const [isProcurementModalOpen, setIsProcurementModalOpen] = useState(false);
  const [isBiddingModalOpen, setIsBiddingModalOpen] = useState(false);
  const [selectedProcurementSaga, setSelectedProcurementSaga] = useState<Saga | null>(null);
  const [selectedBiddingSaga, setSelectedBiddingSaga] = useState<Saga | null>(null);
  const [activeTab, setActiveTab] = useState('procurement');
  const [isEditing, setIsEditing] = useState(false);

  const handleProcurementSuccess = useCallback((saga: Saga) => {
    setSelectedProcurementSaga(saga);
  }, []);

  const handleProcurementCancel = useCallback(() => {
    setIsProcurementModalOpen(false);
    setSelectedProcurementSaga(null);
    setIsEditing(false);
  }, []);

  const handleBiddingSuccess = useCallback((saga: Saga) => {
    setSelectedBiddingSaga(saga);
  }, []);

  const handleBiddingCancel = useCallback(() => {
    setIsBiddingModalOpen(false);
    setSelectedBiddingSaga(null);
    setIsEditing(false);
  }, []);

  const handleProcurementSelect = useCallback((saga: Saga) => {
    setSelectedProcurementSaga(saga);
    setIsEditing(false);
    setIsProcurementModalOpen(true);
  }, []);

  const handleProcurementEdit = useCallback((saga: Saga) => {
    setSelectedProcurementSaga(saga);
    setIsEditing(true);
    setIsProcurementModalOpen(true);
  }, []);

  const handleBiddingSelect = useCallback((saga: Saga) => {
    setSelectedBiddingSaga(saga);
    setIsEditing(false);
    setIsBiddingModalOpen(true);
  }, []);

  const handleBiddingEdit = useCallback((saga: Saga) => {
    setSelectedBiddingSaga(saga);
    setIsEditing(true);
    setIsBiddingModalOpen(true);
  }, []);

  return (
    <div>
      <PageHeader title="Procurement" description="Manage procurement requests, tenders, and bidding workflows." />
      
      <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="procurement">Procurement Workflows</TabsTrigger>
          <TabsTrigger value="bidding">Bidding Workflows</TabsTrigger>
        </TabsList>

        {/* Procurement Tab */}
        <TabsContent value="procurement" className="space-y-6 mt-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold">Procurement Workflows</h2>
              <p className="text-sm text-muted-foreground">
                Track procurement requests, purchase orders, and deliveries. Create bidding/tenders from completed procurements.
              </p>
            </div>
            <Button onClick={() => setIsProcurementModalOpen(true)}>
              Create Procurement
            </Button>
          </div>

          <ProcurementSagaList
            onSagaSelect={handleProcurementSelect}
            onSagaEdit={handleProcurementEdit}
          />
        </TabsContent>

        {/* Bidding Tab */}
        <TabsContent value="bidding" className="space-y-6 mt-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold">Bidding Workflows</h2>
              <p className="text-sm text-muted-foreground">
                Manage tenders, vendor invitations, and bid evaluations. Supports Invited Bidding and Open Tender modes.
              </p>
            </div>
            <Button onClick={() => setIsBiddingModalOpen(true)}>
              Create Bidding
            </Button>
          </div>

          <BiddingSagaList
            onSagaSelect={handleBiddingSelect}
            onSagaEdit={handleBiddingEdit}
          />
        </TabsContent>
      </Tabs>

      {/* Procurement Create/Edit Modal */}
      <Modal
        open={isProcurementModalOpen}
        onClose={handleProcurementCancel}
        title={isEditing && selectedProcurementSaga ? 'Edit Procurement Saga' : 'Create Procurement'}
      >
        <ProcurementSagaForm
          tenantId=""
          saga={selectedProcurementSaga}
          onSuccess={handleProcurementSuccess}
          onCancel={handleProcurementCancel}
        />
      </Modal>

      {/* Bidding Create/Edit Modal */}
      <Modal
        open={isBiddingModalOpen}
        onClose={handleBiddingCancel}
        title={isEditing && selectedBiddingSaga ? 'Edit Bidding Saga' : 'Create Bidding'}
      >
        <BiddingSagaForm
          tenantId=""
          saga={selectedBiddingSaga}
          onSuccess={handleBiddingSuccess}
          onCancel={handleBiddingCancel}
        />
      </Modal>
    </div>
  );
}