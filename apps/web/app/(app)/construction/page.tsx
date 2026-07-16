import { ConstructionEngineeringPageBody } from '@cerp/core-web/construction-engineering';
import { issueWorkOrderAction } from './actions';

export default function ConstructionPage() {

  return <ConstructionEngineeringPageBody formAction={issueWorkOrderAction} />;
}