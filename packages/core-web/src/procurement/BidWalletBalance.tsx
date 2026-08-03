'use client';

import { useState, useEffect, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Loader2, Coins, ArrowUpRight, ArrowDownRight, RefreshCw } from 'lucide-react';
import { cn } from '../lib/utils';

export interface BidWalletData {
  userId: string;
  entityType: 'VENDOR' | 'PROJECT_OWNER';
  totalBids: number;
  spentBids: number;
  availableBids: number;
  bidUnitPriceEur: string;
  bidPriceDescription: string;
}

export interface BidTransactionData {
  id: string;
  transactionType: 'ALLOCATION' | 'SPEND' | 'REFUND';
  amount: number;
  unitPriceEur: string | null;
  totalPriceEur: string | null;
  balanceAfter: number;
  referenceType: string | null;
  referenceId: string | null;
  description: string | null;
  createdAt: string;
}

export interface BidTransactionsResponse {
  userId: string;
  entityType: string;
  transactions: BidTransactionData[];
  page: number;
  size: number;
  totalTransactions: number;
}

interface BidWalletBalanceProps {
  entityType?: 'VENDOR' | 'PROJECT_OWNER';
  className?: string;
}

const ENTITY_TYPE_LABELS = {
  VENDOR: 'Vendor Bids',
  PROJECT_OWNER: 'Project Owner Bids',
};

const TRANSACTION_TYPE_LABELS = {
  ALLOCATION: { label: 'Allocation', icon: ArrowUpRight, color: 'text-green-600' },
  SPEND: { label: 'Spend', icon: ArrowDownRight, color: 'text-red-600' },
  REFUND: { label: 'Refund', icon: ArrowUpRight, color: 'text-blue-600' },
};

export function BidWalletBalance({ entityType, className }: BidWalletBalanceProps) {
  const [walletData, setWalletData] = useState<BidWalletData | null>(null);
  const [transactions, setTransactions] = useState<BidTransactionData[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingTransactions, setLoadingTransactions] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState('balance');
  const [transactionPage, setTransactionPage] = useState(0);
  const TRANSACTION_PAGE_SIZE = 10;

  const loadWalletData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ _path: 'wallet/balance' });
      if (entityType) params.set('entityType', entityType);
      const response = await fetch(`/api/bidding?${params}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch wallet balance: ${response.statusText}`);
      }
      const data: BidWalletData = await response.json();
      setWalletData(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load bid wallet');
    } finally {
      setLoading(false);
    }
  }, [entityType]);

  const loadTransactions = useCallback(async (page: number) => {
    setLoadingTransactions(true);
    try {
      const params = new URLSearchParams({ _path: 'wallet/transactions', page: String(page), size: String(TRANSACTION_PAGE_SIZE) });
      if (entityType) params.set('entityType', entityType);
      const response = await fetch(`/api/bidding?${params}`);
      if (!response.ok) {
        throw new Error(`Failed to fetch transactions: ${response.statusText}`);
      }
      const data: BidTransactionsResponse = await response.json();
      setTransactions(data.transactions);
    } catch (err) {
      console.error('Failed to fetch transactions:', err);
      setTransactions([]);
    } finally {
      setLoadingTransactions(false);
    }
  }, [entityType, TRANSACTION_PAGE_SIZE]);

  useEffect(() => {
    loadWalletData();
  }, [entityType, loadWalletData]);

  useEffect(() => {
    if (activeTab === 'history') {
      loadTransactions(transactionPage);
    }
  }, [activeTab, loadTransactions, transactionPage]);

  const handleRefresh = () => {
    loadWalletData();
    if (activeTab === 'history') {
      loadTransactions(transactionPage);
    }
  };

  const handlePageChange = (newPage: number) => {
    setTransactionPage(newPage);
    loadTransactions(newPage);
  };

  const formatCurrency = (amount: number) => {
    if (walletData) {
      return `${amount.toFixed(2)} €`;
    }
    return `${amount.toFixed(2)} EUR`;
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString();
  };

  const getTransactionTypeInfo = (type: string) => {
    return TRANSACTION_TYPE_LABELS[type as keyof typeof TRANSACTION_TYPE_LABELS] || {
      label: type,
      icon: Coins,
      color: 'text-gray-600',
    };
  };

  if (loading) {
    return (
      <Card className={className}>
        <CardContent className="pt-6 flex items-center justify-center">
          <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
          <span className="ml-2 text-sm text-muted-foreground">Loading bid wallet...</span>
        </CardContent>
      </Card>
    );
  }

  if (error && !walletData) {
    return (
      <Card className={className}>
        <CardContent className="pt-6">
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded text-sm">
            {error}
          </div>
          <Button variant="outline" size="sm" className="mt-3" onClick={handleRefresh}>
            <RefreshCw className="w-3 h-3 mr-2" />
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={className}>
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <CardTitle className="text-base flex items-center gap-2">
            <Coins className="w-5 h-5" />
            {ENTITY_TYPE_LABELS[walletData?.entityType ?? entityType ?? 'VENDOR']}
          </CardTitle>
          <Button
            variant="ghost"
            size="sm"
            onClick={handleRefresh}
            disabled={loading}
          >
            <RefreshCw className={cn("w-3 h-3", loading && "animate-spin")} />
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {/* Balance Summary Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {/* Available Bids */}
            <div className="bg-gradient-to-br from-blue-50 to-blue-100/50 border border-blue-200 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-1">
                <Coins className="w-4 h-4 text-blue-600" />
                <span className="text-xs font-medium text-blue-700 uppercase">Available</span>
              </div>
              <div className="text-2xl font-bold text-blue-900">
                {walletData?.availableBids ?? 0}
              </div>
              <div className="text-xs text-blue-600">
                {formatCurrency(walletData?.availableBids ?? 0)}
              </div>
            </div>

            {/* Total Bids */}
            <div className="bg-gradient-to-br from-green-50 to-green-100/50 border border-green-200 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-1">
                <ArrowUpRight className="w-4 h-4 text-green-600" />
                <span className="text-xs font-medium text-green-700 uppercase">Total Received</span>
              </div>
              <div className="text-2xl font-bold text-green-900">
                {walletData?.totalBids ?? 0}
              </div>
              <div className="text-xs text-green-600">
                {formatCurrency(walletData?.totalBids ?? 0)}
              </div>
            </div>

            {/* Spent Bids */}
            <div className="bg-gradient-to-br from-amber-50 to-amber-100/50 border border-amber-200 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-1">
                <ArrowDownRight className="w-4 h-4 text-amber-600" />
                <span className="text-xs font-medium text-amber-700 uppercase">Spent</span>
              </div>
              <div className="text-2xl font-bold text-amber-900">
                {walletData?.spentBids ?? 0}
              </div>
              <div className="text-xs text-amber-600">
                {formatCurrency(walletData?.spentBids ?? 0)}
              </div>
            </div>
          </div>

          {/* Price Info */}
          {walletData && (
            <div className="text-xs text-muted-foreground bg-muted/50 rounded-md px-3 py-2">
              {walletData.bidPriceDescription || `Each bid costs ${walletData.bidUnitPriceEur} EUR`}
            </div>
          )}

          {/* Cost Reference */}
          <div className="bg-muted/30 rounded-md p-3 space-y-1">
            <div className="text-xs font-medium">Bid Costs:</div>
            <div className="text-xs text-muted-foreground">
              • Creating a bidding: <span className="font-medium">1 bid</span>
            </div>
            <div className="text-xs text-muted-foreground">
              • Open tender participation: <span className="font-medium">5 bids</span>
            </div>
          </div>

          {/* Transaction History */}
          <Tabs value={activeTab} onValueChange={setActiveTab} className="w-full">
            <TabsList className="grid w-full grid-cols-2">
              <TabsTrigger value="balance">Balance</TabsTrigger>
              <TabsTrigger value="history">Transaction History</TabsTrigger>
            </TabsList>

            <TabsContent value="balance" className="space-y-2 mt-3">
              <div className="text-sm text-muted-foreground">
                {walletData?.availableBids !== undefined && walletData.availableBids > 0
                  ? `You have ${walletData.availableBids} bid(s) available for use.`
                  : 'You have no bids available. Contact your administrator to allocate bids.'}
              </div>
            </TabsContent>

            <TabsContent value="history" className="mt-3">
              {loadingTransactions && transactions.length === 0 ? (
                <div className="flex items-center justify-center py-6">
                  <Loader2 className="w-4 h-4 animate-spin text-muted-foreground" />
                  <span className="ml-2 text-xs text-muted-foreground">Loading transactions...</span>
                </div>
              ) : transactions.length === 0 ? (
                <div className="text-sm text-muted-foreground text-center py-6">
                  No transactions found.
                </div>
              ) : (
                <div className="space-y-2">
                  {transactions.map((tx) => {
                    const typeInfo = getTransactionTypeInfo(tx.transactionType);
                    const Icon = typeInfo.icon;
                    return (
                      <div
                        key={tx.id}
                        className="flex items-center justify-between p-2 border rounded text-xs"
                      >
                        <div className="flex items-center gap-2">
                          <Icon className={cn("w-3 h-3", typeInfo.color)} />
                          <div>
                            <div className="font-medium">{typeInfo.label}</div>
                            {tx.description && (
                              <div className="text-muted-foreground">{tx.description}</div>
                            )}
                          </div>
                        </div>
                        <div className="text-right">
                          <div className={cn(
                            "font-medium",
                            tx.transactionType === 'SPEND' ? 'text-red-600' : 'text-green-600'
                          )}>
                            {tx.transactionType === 'SPEND' ? '-' : '+'}{tx.amount} bid(s)
                          </div>
                          <div className="text-muted-foreground">{formatDate(tx.createdAt)}</div>
                        </div>
                      </div>
                    );
                  })}

                  {/* Pagination */}
                  {transactions.length >= TRANSACTION_PAGE_SIZE && (
                    <div className="flex items-center justify-center gap-2 pt-2">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handlePageChange(transactionPage - 1)}
                        disabled={transactionPage === 0}
                      >
                        Previous
                      </Button>
                      <span className="text-xs text-muted-foreground">
                        Page {transactionPage + 1}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handlePageChange(transactionPage + 1)}
                      >
                        Next
                      </Button>
                    </div>
                  )}
                </div>
              )}
            </TabsContent>
          </Tabs>
        </div>
      </CardContent>
    </Card>
  );
}