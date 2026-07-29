'use client';

import { Tabs, TabsContent, TabsList, TabsTrigger } from '@cerp/core-web/components/ui/tabs';
import { SagaPage } from '@cerp/core-web/workflow';
import { TemplatesPage } from '@cerp/core-web/workflow';

export default function WorkflowsPage() {
  return (
    <div className="container mx-auto py-8">
      <Tabs defaultValue="sagas" className="space-y-6">
        <TabsList>
          <TabsTrigger value="sagas">Saga Monitoring</TabsTrigger>
          <TabsTrigger value="templates">Workflow Templates</TabsTrigger>
        </TabsList>
        <TabsContent value="sagas">
          <SagaPage />
        </TabsContent>
        <TabsContent value="templates">
          <TemplatesPage />
        </TabsContent>
      </Tabs>
    </div>
  );
}