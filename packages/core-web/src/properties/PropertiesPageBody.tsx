import type { ReactNode } from 'react';
import type { Property } from '@cerp/shared-types';
import { apiClient } from '../lib/api-client';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { PageHeader } from '../shell/PageHeader';
import { PropertyList } from './PropertyList';
import { PropertyForm } from './PropertyForm';
import type { CreatePropertyActionState } from './types';

export interface PropertiesPageBodyProps {
  formAction: (state: CreatePropertyActionState, formData: FormData) => Promise<CreatePropertyActionState>;
  extraFields?: ReactNode;
}

export async function PropertiesPageBody({ formAction, extraFields }: PropertiesPageBodyProps) {
  const properties = await apiClient.get<Property[]>('/properties');

  return (
    <div>
      <PageHeader title="Properties" description="Manage the properties in your portfolio." />
      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-base">Add property</CardTitle>
        </CardHeader>
        <CardContent>
          <PropertyForm action={formAction} extraFields={extraFields} />
        </CardContent>
      </Card>
      <Card>
        <CardContent className="p-0">
          <PropertyList properties={properties} />
        </CardContent>
      </Card>
    </div>
  );
}
