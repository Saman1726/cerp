import { PropertiesPageBody } from '@cerp/core-web/properties';
import { createPropertyAction } from './actions';

export default function PropertiesPage() {
  return <PropertiesPageBody formAction={createPropertyAction} />;
}
