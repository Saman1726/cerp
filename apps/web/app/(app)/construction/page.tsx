import { ConstructionEngineeringPageBodyClient } from '@cerp/core-web/construction-engineering';
import { issueWorkOrderAction, createProjectActionWrapper as createProjectAction, deleteProjectActionWrapper as deleteProjectAction, createSubProjectActionWrapper as createSubProjectAction, deleteSubProjectActionWrapper as deleteSubProjectAction } from './actions';
import { apiClient } from '@cerp/core-web/lib/api-client';
import type { Project, WorkOrder, ConstructionSite } from '@cerp/shared-types';
import type { Partner } from '@cerp/core-web/partners';

export default async function ConstructionPage() {
  const [workOrders, contractors, constructionSites]: [WorkOrder[], Partner[], ConstructionSite[]] = await Promise.all([
    apiClient.get<WorkOrder[]>('/work-orders'),
    apiClient.get<Partner[]>('/partners'),
    apiClient.get<ConstructionSite[]>('/construction-sites'),
  ]);

  let projects: Project[] = [];
  try {
    const rawProjects = await apiClient.get<Record<string, unknown>[]>('/api/v1/projects');
    projects = (rawProjects || []).map((p) => ({
      ...p,
      type: (p.projectType as string) || (p.type as string),
      subProjects: (p.subProjects as unknown[]) || [],
    })) as unknown as Project[];
  } catch {
    console.warn('Failed to fetch projects, returning empty array');
  }

  return (
    <ConstructionEngineeringPageBodyClient
      projects={projects}
      workOrders={workOrders}
      contractors={contractors}
      constructionSites={constructionSites}
      formAction={issueWorkOrderAction}
      createProjectAction={createProjectAction}
      createSubProjectAction={createSubProjectAction}
      deleteProjectAction={deleteProjectAction}
      deleteSubProjectAction={deleteSubProjectAction}
    />
  );
}
