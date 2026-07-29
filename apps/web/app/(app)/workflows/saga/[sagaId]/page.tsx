'use client';

import { use } from 'react';
import { SagaDetailPage } from '@cerp/core-web/workflow';

export default function SagaDetailRoute(props: { params: Promise<{ sagaId: string }> }) {
  const params = use(props.params);
  return <SagaDetailPage sagaId={params.sagaId} />;
}
