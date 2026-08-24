'use client';

import { useState, useEffect } from 'react';
import type { ConstructionSite } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';
import { PageHeader } from '../shell/PageHeader';
import { ConstructionSitesTree } from './ConstructionSitesTree';
import { FilePreviewPanel } from './FilePreviewPanel';
import type { ConstructionSitesActions } from './types';

export interface ConstructionSitesPageBodyProps {
  actions: ConstructionSitesActions;
}

export function ConstructionSitesPageBody({ actions }: ConstructionSitesPageBodyProps) {
  const [sites, setSites] = useState<ConstructionSite[]>([]);

  useEffect(() => {
    apiClient.get<ConstructionSite[]>('/construction-sites').then(setSites);
  }, []);

  const [selectedSiteId, setSelectedSiteId] = useState<string | null>(null);
  const [selectedSiteName, setSelectedSiteName] = useState('');

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="Construction Sites"
        description="Manage construction sites, buildings, floors and units."
      />
      <div className="flex flex-1 gap-4">
        <ConstructionSitesTree
          sites={sites}
          actions={actions}
          onSiteSelect={(id, name) => {
            setSelectedSiteId(id);
            setSelectedSiteName(name);
          }}
        />
        <FilePreviewPanel
          siteId={selectedSiteId}
          siteName={selectedSiteName}
          actions={actions}
        />
      </div>
    </div>
  );
}