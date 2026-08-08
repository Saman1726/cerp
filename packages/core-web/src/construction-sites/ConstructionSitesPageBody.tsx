import type { ConstructionSite } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';
import { PageHeader } from '../shell/PageHeader';
import { ConstructionSitesTree } from './ConstructionSitesTree';
import type { ConstructionSitesActions } from './types';

export interface ConstructionSitesPageBodyProps {
  actions: ConstructionSitesActions;
}

export async function ConstructionSitesPageBody({ actions }: ConstructionSitesPageBodyProps) {
  const sites = await apiClient.get<ConstructionSite[]>('/construction-sites');

  return (
    <div>
      <PageHeader
        title="Construction Sites"
        description="Manage construction sites, buildings, floors and units."
      />
      <ConstructionSitesTree sites={sites} actions={actions} />
    </div>
  );
}