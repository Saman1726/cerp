import type { ConstructionSite } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
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
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Sites overview</CardTitle>
        </CardHeader>
        <CardContent>
          <ConstructionSitesTree sites={sites} actions={actions} />
        </CardContent>
      </Card>
    </div>
  );
}