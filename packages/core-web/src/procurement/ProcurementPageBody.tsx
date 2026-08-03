'use client';

import { useState, useCallback } from 'react';
import { Button } from '../components/ui/button';
import { PageHeader } from '../shell/PageHeader';
import { Modal } from '../components/ui/modal';
import { ProcurementSagaForm } from './ProcurementSagaForm';
import { ProcurementSagaList } from './ProcurementSagaList';
import { BiddingSagaList } from './BiddingSagaList';
import { BiddingSagaForm } from './BiddingSagaForm';
import { BidWalletBalance } from './BidWalletBalance';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import type { Saga } from '../workflow/types';

export function ProcurementPageBody() {
  const [isProcurementModalOpen, setIsProcurementModalOpen] = useState(false);
  const [isBiddingModalOpen, setIsBiddingModalOpen] = useState(false);
  const [selectedProcurementSaga, setSelectedProcurementSaga] = useState<Saga | null>(null);
  const [selectedBiddingSaga, setSelectedBiddingSaga] = useState<Saga | null>(null);
  const [activeTab, setActiveTab] = useState('procurement');
  const [isEditing, setIsEditing] = useState(false);
  // Track when Create Bidding button is clicked from procurement form
  const [viewingBiddingFromProcurement, setViewingBiddingFromProcurement] = useState(false);

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
    setViewingBiddingFromProcurement(false);
  }, []);

  // Handler for "Create Bidding" button from the procurement saga view form
  const handleViewBiddingFromProcurement = useCallback(() => {
    if (selectedProcurementSaga) {
      setViewingBiddingFromProcurement(true);
      setIsBiddingModalOpen(true);
    }
  }, [selectedProcurementSaga]);

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
        <TabsList className="grid w-full max-w-lg grid-cols-3">
          <TabsTrigger value="procurement">Procurement</TabsTrigger>
          <TabsTrigger value="bidding">Bidding</TabsTrigger>
          <TabsTrigger value="wallet">Bid Wallet</TabsTrigger>
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
                Create bidding from completed procurement sagas via the context menu.
              </p>
            </div>
          </div>

          <BiddingSagaList
            onSagaSelect={handleBiddingSelect}
            onSagaEdit={handleBiddingEdit}
          />
        </TabsContent>

        {/* Bid Wallet Tab */}
        <TabsContent value="wallet" className="space-y-6 mt-6">
          <div>
            <h2 className="text-lg font-semibold">Bid Wallet</h2>
            <p className="text-sm text-muted-foreground">
              View your bid balance and transaction history. Bids are used to create tenders and participate in open bidding.
            </p>
          </div>

          <BidWalletBalance />
        </TabsContent>
      </Tabs>

      {/* Procurement View Modal */}
      <Modal
        open={isProcurementModalOpen && !isEditing}
        onClose={handleProcurementCancel}
        title="View Procurement Saga"
      >
        <ProcurementSagaForm
          tenantId=""
          saga={selectedProcurementSaga}
          onSuccess={handleProcurementSuccess}
          onCancel={handleProcurementCancel}
          onViewBidding={handleViewBiddingFromProcurement}
          viewOnly
        />
      </Modal>

      {/* Procurement Edit Modal */}
      <Modal
        open={isProcurementModalOpen && isEditing}
        onClose={handleProcurementCancel}
        title="Edit Procurement Saga"
      >
        <ProcurementSagaForm
          tenantId=""
          saga={selectedProcurementSaga}
          onSuccess={handleProcurementSuccess}
          onCancel={handleProcurementCancel}
          onViewBidding={handleViewBiddingFromProcurement}
        />
      </Modal>

      {/* Bidding View Modal */}
      <Modal
        open={isBiddingModalOpen && !isEditing}
        onClose={handleBiddingCancel}
        title="View Bidding Saga"
      >
        <BiddingSagaForm
          tenantId=""
          saga={selectedBiddingSaga}
          onSuccess={handleBiddingSuccess}
          onCancel={handleBiddingCancel}
          viewOnly
        />
      </Modal>

      {/* Bidding Edit Modal */}
      <Modal
        open={isBiddingModalOpen && isEditing}
        onClose={handleBiddingCancel}
        title="Edit Bidding Saga"
      >
        <BiddingSagaForm
          tenantId=""
          saga={selectedBiddingSaga}
          onSuccess={handleBiddingSuccess}
          onCancel={handleBiddingCancel}
        />
      </Modal>

      {/* Create Bidding from Procurement Saga Modal */}
      <Modal
        open={isBiddingModalOpen && viewingBiddingFromProcurement && !isEditing}
        onClose={handleBiddingCancel}
        title="Create Bidding from Procurement"
      >
        <BiddingSagaForm
          tenantId=""
          initialProcurementSagaId={selectedProcurementSaga?.sagaId}
          onSuccess={handleBiddingSuccess}
          onCancel={handleBiddingCancel}
        />
      </Modal>
    </div>
  );
}