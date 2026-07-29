'use client';

import { useState, useCallback } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { workflowApi } from './api';
import type { MermaidDiagram, StateMachine } from './types';

export function WorkflowDiagramViewer() {
  const [diagrams, setDiagrams] = useState<MermaidDiagram[]>([]);
  const [stateMachines, setStateMachines] = useState<StateMachine[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedDiagram, setSelectedDiagram] = useState<string | null>(null);
  const [selectedStateMachine, setSelectedStateMachine] = useState<string | null>(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [diagramsData, smData] = await Promise.all([
        workflowApi.listDiagrams(),
        workflowApi.listStateMachines(),
      ]);
      setDiagrams(diagramsData);
      setStateMachines(smData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load diagrams');
    } finally {
      setLoading(false);
    }
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <p className="text-muted-foreground">Loading diagrams...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Workflow Diagrams</h1>
          <p className="text-muted-foreground mt-1">
            Visualize workflow sequences and state machines
          </p>
        </div>
        <Button variant="outline" onClick={fetchData}>
          Refresh
        </Button>
      </div>

      {/* Error */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {/* Tabs */}
      <div className="space-y-4">
        <div className="flex gap-2 border-b">
          <button className="px-4 py-2 text-sm font-medium border-b-2 border-primary text-primary">
            Sequence Diagrams
          </button>
          <button className="px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground">
            State Machines
          </button>
        </div>

        {/* Sequence Diagrams */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          {/* Diagram List */}
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle className="text-base">Diagrams</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                {diagrams.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-4">
                    No diagrams available
                  </p>
                ) : (
                  diagrams.map((diagram) => (
                    <Button
                      key={diagram.id}
                      variant={selectedDiagram === diagram.id ? 'default' : 'ghost'}
                      className="w-full justify-start text-sm"
                      onClick={() => setSelectedDiagram(diagram.id)}
                    >
                      {diagram.title}
                    </Button>
                  ))
                )}
              </div>
            </CardContent>
          </Card>

          {/* Diagram Viewer */}
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle className="text-base">
                {selectedDiagram
                  ? diagrams.find((d) => d.id === selectedDiagram)?.title
                  : 'Select a diagram'}
              </CardTitle>
            </CardHeader>
            <CardContent>
              {selectedDiagram ? (() => {
                const diagram = diagrams.find((d) => d.id === selectedDiagram);
                if (!diagram) return null;
                return (
                  <div className="bg-muted p-6 rounded-lg overflow-auto max-h-[600px]">
                    <pre className="whitespace-pre-wrap text-sm font-mono">
                      {diagram.content}
                    </pre>
                  </div>
                );
              })() : (
                <div className="text-center py-12 text-muted-foreground">
                  Select a diagram from the list to view it
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* State Machines */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
          {/* State Machine List */}
          <Card className="lg:col-span-1">
            <CardHeader>
              <CardTitle className="text-base">State Machines</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-2">
                {stateMachines.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-4">
                    No state machines available
                  </p>
                ) : (
                  stateMachines.map((sm) => (
                    <Button
                      key={sm.aggregate}
                      variant={selectedStateMachine === sm.aggregate ? 'default' : 'ghost'}
                      className="w-full justify-start text-sm"
                      onClick={() => setSelectedStateMachine(sm.aggregate)}
                    >
                      {sm.aggregate}
                    </Button>
                  ))
                )}
              </div>
            </CardContent>
          </Card>

          {/* State Machine Viewer */}
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle className="text-base">
                {selectedStateMachine
                  ? stateMachines.find((s) => s.aggregate === selectedStateMachine)?.aggregate
                  : 'Select a state machine'}
              </CardTitle>
            </CardHeader>
            <CardContent>
              {selectedStateMachine ? (() => {
                const sm = stateMachines.find((s) => s.aggregate === selectedStateMachine);
                if (!sm) return null;
                return (
                  <div className="space-y-4">
                    <div>
                      <h3 className="text-sm font-medium mb-2">States</h3>
                      <div className="flex flex-wrap gap-2">
                        {sm.states.map((state: string) => (
                          <Badge key={state} variant="secondary">
                            {state}
                          </Badge>
                        ))}
                      </div>
                    </div>
                    <div>
                      <h3 className="text-sm font-medium mb-2">Transitions</h3>
                      <div className="bg-muted p-4 rounded-lg overflow-auto max-h-[400px]">
                        <pre className="whitespace-pre-wrap text-xs font-mono">
                          {JSON.stringify(sm.transitions, null, 2)}
                        </pre>
                      </div>
                    </div>
                  </div>
                );
              })() : (
                <div className="text-center py-12 text-muted-foreground">
                  Select a state machine from the list to view it
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}