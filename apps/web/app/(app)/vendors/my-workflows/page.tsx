'use client';

import { useEffect, useState } from 'react';
import { PageHeader } from '@cerp/core-web/shell';
import { SagaPage } from '@cerp/core-web/workflow';
import { Card, CardContent } from '@cerp/core-web/components/ui/card';
import type { VendorProfile } from '@cerp/shared-types';

export default function MyWorkflowsPage() {
  const [vendor, setVendor] = useState<VendorProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await fetch('/api/vendors/me');
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        if (!cancelled) setVendor(data);
      } catch (err) {
        console.error('Failed to load vendor:', err);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const vendorId = vendor?.vendorId ?? vendor?.id;

  return (
    <div className="space-y-6">
      <PageHeader
        title="My Workflows"
        description="View and manage delivery and procurement workflows for your vendor profile."
      />

      {loading ? (
        <Card>
          <CardContent className="pt-6">
            <p className="text-muted-foreground">Loading...</p>
          </CardContent>
        </Card>
      ) : (
        <>
          {vendorId ? (
            <SagaPage vendorId={vendorId} />
          ) : (
            <Card>
              <CardContent className="pt-6">
                <p className="text-muted-foreground">No vendor profile found.</p>
              </CardContent>
            </Card>
          )}
        </>
      )}
    </div>
  );
}