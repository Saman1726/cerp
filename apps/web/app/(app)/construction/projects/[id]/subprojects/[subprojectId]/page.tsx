import { notFound } from 'next/navigation';
import { apiClient } from '@cerp/core-web/lib/api-client';
import type { SubProjectResponse } from '@cerp/shared-types';
import SubProjectDetailClient from './SubProjectDetailClient';

export default async function SubProjectDetailPage({
  params,
}: {
  params: Promise<{ id: string; subprojectId: string }>;
}) {
  const { id: projectId, subprojectId } = await params;

  let subProject: SubProjectResponse | null = null;

  try {
    subProject = await apiClient.get<SubProjectResponse>(
      `/api/v1/projects/${projectId}/subprojects/${subprojectId}`
    );
  } catch {
    // SubProject not found
  }

  if (!subProject) {
    notFound();
  }

  return (
    <SubProjectDetailClient subProject={subProject} projectId={projectId} />
  );
}
