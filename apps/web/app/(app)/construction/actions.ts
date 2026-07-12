'use server';

import { revalidatePath } from 'next/cache';
import { issueWorkOrder } from '@cerp/core-web/construction-engineering';
import type { IssueWorkOrderActionState } from '@cerp/core-web/construction-engineering';

export async function issueWorkOrderAction(
  _prevState: IssueWorkOrderActionState,
  formData: FormData,
): Promise<IssueWorkOrderActionState> {
  const result = await issueWorkOrder(formData);
  if (!result.error) {
    revalidatePath('/construction');
  }
  return result;
}
