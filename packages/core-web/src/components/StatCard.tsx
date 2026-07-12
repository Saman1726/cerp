import type { ComponentType } from 'react';
import { Card, CardContent } from './ui/card';

export interface StatCardProps {
  label: string;
  value: string | number;
  icon: ComponentType<{ className?: string }>;
}

export function StatCard({ label, value, icon: Icon }: StatCardProps) {
  return (
    <Card>
      <CardContent className="flex items-center gap-4 py-6">
        <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
          <Icon className="size-5" />
        </div>
        <div>
          <p className="text-2xl leading-none font-semibold">{value}</p>
          <p className="text-sm text-muted-foreground">{label}</p>
        </div>
      </CardContent>
    </Card>
  );
}
