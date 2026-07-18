import { notFound } from 'next/navigation';
import { apiClient } from '@cerp/core-web/lib/api-client';
import type { Project, ConstructionSite } from '@cerp/shared-types';
import ProjectDetailClient from './ProjectDetailClient';

export default async function ProjectDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id: projectId } = await params;

  let project: Project | null = null;
  let constructionSites: ConstructionSite[] = [];

  try {
    project = await apiClient.get<Project>(`/api/projects/v1/projects/${projectId}`);
  } catch {
    // Project not found
  }

  if (!project) {
    notFound();
  }

  try {
    constructionSites = await apiClient.get<ConstructionSite[]>('/construction-sites');
  } catch {
    console.warn('Failed to fetch construction sites');
  }

  return (
    <ProjectDetailClient
      project={project}
      projectId={projectId}
      constructionSites={constructionSites}
    />
  );
}