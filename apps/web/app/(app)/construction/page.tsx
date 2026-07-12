import { HardHat } from 'lucide-react';
import { ComingSoon } from '@cerp/core-web/components';
import { PageHeader } from '@cerp/core-web/shell';

export default function ConstructionPage() {
  return (
    <div>
      <PageHeader title="Construction" description="Track construction projects and work orders." />
      <ComingSoon
        icon={HardHat}
        title="Construction module coming soon"
        description="Project tracking, work orders, and contractor assignments will live here."
      />
    </div>
  );
}
