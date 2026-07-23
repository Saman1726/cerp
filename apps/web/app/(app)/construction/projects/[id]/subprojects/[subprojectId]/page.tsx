import { notFound } from 'next/navigation';
import { apiClient } from '@cerp/core-web/lib/api-client';
import type { SubProjectResponse } from '@cerp/shared-types';
import SubProjectDetailClient from './SubProjectDetailClient';

// Allow dynamic rendering for subprojects not pre-rendered at build time
export const dynamicParams = true;
export async function generateStaticParams() {
  const params: Array<{ id: string; subprojectId: string }> = [];

  try {
    // Fetch all projects
    const projects = await apiClient.get<import('@cerp/shared-types').Project[]>(
      '/api/v1/projects'
    );

    for (const project of projects) {
      try {
        // Fetch all subprojects for this project
        const subProjects = await apiClient.get<import('@cerp/shared-types').SubProjectResponse[]>(
          `/api/v1/projects/${project.id}/subprojects`
        );

        for (const subProject of subProjects) {
          params.push({
            id: project.id,
            subprojectId: subProject.id,
          });
        }
      } catch {
        // Skip projects that fail to fetch subprojects
        console.warn(`[SubProjectDetailPage] Could not fetch subprojects for project ${project.id}`);
      }
    }
  } catch {
    console.warn('[SubProjectDetailPage] Could not fetch projects for static params generation');
  }

  return params;
}

export default async function SubProjectDetailPage({
  params,
}: {
  params: Promise<{ id: string; subprojectId: string }>;
}) {
  console.log('[SubProjectDetailPage] ENTERED');
  const { id: projectId, subprojectId } = await params;
  console.log('[SubProjectDetailPage] resolved params:', { projectId, subprojectId });

  let subProject: SubProjectResponse | null = null;
  try {
    console.log('[SubProjectDetailPage] calling apiClient.get');
    subProject = await apiClient.get<SubProjectResponse>(
      `/api/v1/projects/${projectId}/subprojects/${subprojectId}`
    );
    console.log('[SubProjectDetailPage] API call succeeded:', subProject?.code);
  } catch (error) {
    console.error('[SubProjectDetailPage] Failed to fetch subproject:', {
      projectId,
      subprojectId,
      error: error instanceof Error ? error.message : String(error),
      status: (error as { status?: number })?.status,
    });
  }

  if (!subProject) {
    console.error('[SubProjectDetailPage] SubProject not found or API error');
    notFound();
  }

  console.log('[SubProjectDetailPage] rendering SubProjectDetailClient');
  return (
    <SubProjectDetailClient subProject={subProject} projectId={projectId} />
  );
}