'use client';

import { useState } from 'react';
import type { Project, WorkOrder, ProjectUserRole } from '@cerp/shared-types';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { PageHeader } from '../shell/PageHeader';
import type { Partner } from '../partners/types';
import { WorkOrderForm } from './WorkOrderForm';
import { WorkOrderList } from './WorkOrderList';
import { ProjectList } from './ProjectList';
import { Modal } from '../components/ui/modal';
import type { IssueWorkOrderActionState } from './types';

export interface ConstructionEngineeringPageBodyProps {
  formAction: (state: IssueWorkOrderActionState, formData: FormData) => Promise<IssueWorkOrderActionState>;
  createProjectAction: (formData: FormData) => Promise<void>;
  createSubProjectAction: (formData: FormData) => Promise<void>;
  deleteProjectAction: (projectId: string) => Promise<void>;
  deleteSubProjectAction: (formData: FormData) => Promise<void>;
}

export interface ConstructionEngineeringPageBodyClientProps {
  projects: Project[];
  workOrders: WorkOrder[];
  contractors: Partner[];
  constructionSites: import('@cerp/shared-types').ConstructionSite[];
  createProjectAction: (formData: FormData) => Promise<void>;
  createSubProjectAction: (formData: FormData) => Promise<void>;
  deleteProjectAction: (projectId: string) => Promise<void>;
  deleteSubProjectAction: (formData: FormData) => Promise<void>;
  formAction: (state: IssueWorkOrderActionState, formData: FormData) => Promise<IssueWorkOrderActionState>;
  currentUserEmail?: string;
  assignUsersAction?: (projectId: string, assignments: Array<{ userId: string; role: ProjectUserRole }>) => Promise<{ error?: string }>;
  removeUserAction?: (projectId: string, userId: string) => Promise<{ error?: string }>;
}

export function ConstructionEngineeringPageBodyClient({
  projects,
  workOrders,
  contractors,
  constructionSites,
  createProjectAction,
  createSubProjectAction,
  deleteProjectAction,
  deleteSubProjectAction,
  formAction,
  currentUserEmail,
  assignUsersAction,
  removeUserAction,
}: ConstructionEngineeringPageBodyClientProps) {
  const [workOrderModalOpen, setWorkOrderModalOpen] = useState(false);
  const [selectedProjectId, setSelectedProjectId] = useState<string>('');
  const [selectedSubProjectId, setSelectedSubProjectId] = useState<string>('');
  const [selectedProjectName, setSelectedProjectName] = useState<string>('');
  const [selectedSubProjectName, setSelectedSubProjectName] = useState<string>('');

  const handleCreateWorkOrder = (project: Project, subProject?: { id: string; name: string }) => {
    setSelectedProjectId(project.id);
    setSelectedProjectName(project.name);
    if (subProject) {
      setSelectedSubProjectId(subProject.id);
      setSelectedSubProjectName(subProject.name);
    } else {
      setSelectedSubProjectId('');
      setSelectedSubProjectName('');
    }
    setWorkOrderModalOpen(true);
  };

  const handleFormSubmit = async (state: IssueWorkOrderActionState, formData: FormData): Promise<IssueWorkOrderActionState> => {
    formData.set('projectId', selectedProjectId);
    if (selectedSubProjectId) {
      formData.set('subProjectId', selectedSubProjectId);
    }
    const result = await formAction(state, formData);
    if (!result.error) {
      setWorkOrderModalOpen(false);
      setSelectedProjectId('');
      setSelectedSubProjectId('');
      window.location.reload();
    }
    return result;
  };

  return (
    <div>
      <PageHeader title="Project Management" description="Manage projects, subprojects, and work orders. Each construction site can only be linked to one project." />

      <Card className="mb-6">
        <CardContent className="p-0">
          <ProjectList
            projects={projects}
            constructionSites={constructionSites}
            createProjectAction={createProjectAction}
            createSubProjectAction={createSubProjectAction}
            deleteProjectAction={deleteProjectAction}
            deleteSubProjectAction={deleteSubProjectAction}
            onCreateWorkOrder={handleCreateWorkOrder}
            assignUsersAction={assignUsersAction}
            removeUserAction={removeUserAction}
            currentUserEmail={currentUserEmail}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Work Orders</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <WorkOrderList workOrders={workOrders} />
        </CardContent>
      </Card>

      {/* Create Work Order Modal */}
      <Modal open={workOrderModalOpen} onClose={() => setWorkOrderModalOpen(false)} title={`Create work order${selectedSubProjectName ? ` - ${selectedSubProjectName}` : ` - ${selectedProjectName}`}`}>
        <WorkOrderForm action={handleFormSubmit} projects={projects} contractors={contractors} selectedProjectId={selectedProjectId} selectedSubProjectId={selectedSubProjectId} />
      </Modal>
    </div>
  );
}