'use client';

import type { Project, ProjectUserRole, ProjectAssignedUser } from '@cerp/shared-types';
import { PageHeader } from '../shell/PageHeader';
import { ProjectList } from './ProjectList';

export interface ConstructionEngineeringPageBodyClientProps {
  projects: Project[];
  constructionSites: import('@cerp/shared-types').ConstructionSite[];
  createProjectAction: (formData: FormData) => Promise<void>;
  createSubProjectAction: (formData: FormData) => Promise<void>;
  deleteProjectAction: (projectId: string) => Promise<void>;
  deleteSubProjectAction: (formData: FormData) => Promise<void>;
  currentUserEmail?: string;
  assignUsersAction?: (projectId: string, assignments: Array<{ userId: string; role: ProjectUserRole }>) => Promise<{ error?: string }>;
  removeUserAction?: (projectId: string, userId: string) => Promise<{ error?: string }>;
  getAssignedUsersAction?: (projectId: string) => Promise<ProjectAssignedUser[]>;
}

export function ConstructionEngineeringPageBodyClient({
  projects,
  constructionSites,
  createProjectAction,
  createSubProjectAction,
  deleteProjectAction,
  deleteSubProjectAction,
  currentUserEmail,
  assignUsersAction,
  removeUserAction,
  getAssignedUsersAction,
}: ConstructionEngineeringPageBodyClientProps) {
  return (
    <div>
      <PageHeader title="Project Management" description="Manage projects and subprojects. Each construction site can only be linked to one project." />

      <ProjectList
        projects={projects}
        constructionSites={constructionSites}
        createProjectAction={createProjectAction}
        createSubProjectAction={createSubProjectAction}
        deleteProjectAction={deleteProjectAction}
        deleteSubProjectAction={deleteSubProjectAction}
        assignUsersAction={assignUsersAction}
        removeUserAction={removeUserAction}
        getAssignedUsersAction={getAssignedUsersAction}
        currentUserEmail={currentUserEmail}
      />
    </div>
  );
}