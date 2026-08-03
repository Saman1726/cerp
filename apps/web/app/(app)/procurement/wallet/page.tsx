'use client';

import { PageHeader } from '@cerp/core-web/shell';
import { BidWalletBalance } from '@cerp/core-web/procurement';

export default function BidWalletPage() {
  return (
    <div>
      <PageHeader
        title="Bid Wallet"
        description="View your bid balance and transaction history. Bids are used to create tenders and participate in open bidding."
      />

      <div className="mt-6">
        <BidWalletBalance />
      </div>
    </div>
  );
}
