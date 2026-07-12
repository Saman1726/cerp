'use client';

import { useActionState } from 'react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { cn } from '../lib/utils';
import type { IssueWorkOrderActionState } from './types';

const initialState: IssueWorkOrderActionState = {};

export interface WorkOrderFormProjectOption {
  id: string;
  name: string;
}

export interface WorkOrderFormContractorOption {
  id: string;
  name: string;
  type: string;
}

export interface WorkOrderFormProps {
  action: (state: IssueWorkOrderActionState, formData: FormData) => Promise<IssueWorkOrderActionState>;
  projects: WorkOrderFormProjectOption[];
  contractors: WorkOrderFormContractorOption[];
}

const fieldClassName = cn(
  'flex h-9 w-full min-w-0 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors outline-none',
  'focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-1',
  'disabled:cursor-not-allowed disabled:opacity-50',
);

export function WorkOrderForm({ action, projects, contractors }: WorkOrderFormProps) {
  const [state, formAction, pending] = useActionState(action, initialState);

  return (
    <form action={formAction} className="flex flex-col gap-4">
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="grid gap-2">
          <Label htmlFor="projectId">Project</Label>
          <select id="projectId" name="projectId" required defaultValue="" className={fieldClassName}>
            <option value="" disabled>
              Select a project
            </option>
            {projects.map((project) => (
              <option key={project.id} value={project.id}>
                {project.name}
              </option>
            ))}
          </select>
        </div>
        <div className="grid gap-2">
          <Label htmlFor="contractorPartnerId">Contractor</Label>
          <select
            id="contractorPartnerId"
            name="contractorPartnerId"
            required
            defaultValue=""
            className={fieldClassName}
          >
            <option value="" disabled>
              Select a contractor
            </option>
            {contractors.map((contractor) => (
              <option key={contractor.id} value={contractor.id}>
                {contractor.name} ({contractor.type})
              </option>
            ))}
          </select>
        </div>
      </div>
      <div className="grid gap-2">
        <Label htmlFor="title">Title</Label>
        <Input id="title" name="title" placeholder="Foundation repair" required />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="description">Description (optional)</Label>
        <textarea
          id="description"
          name="description"
          rows={3}
          placeholder="Fix cracks in the north foundation wall"
          className={cn(fieldClassName, 'h-auto py-2')}
        />
      </div>
      {state.error ? <p className="text-sm text-destructive">{state.error}</p> : null}
      <Button type="submit" disabled={pending} className="self-start">
        {pending ? 'Issuing...' : 'Issue work order'}
      </Button>
    </form>
  );
}
