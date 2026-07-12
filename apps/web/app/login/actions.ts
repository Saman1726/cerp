'use server';

import { performLogin } from '@cerp/core-web/auth';
import type { LoginActionState } from '@cerp/core-web/auth';

export async function loginAction(
  _prevState: LoginActionState,
  formData: FormData,
): Promise<LoginActionState> {
  return performLogin(formData, { redirectTo: '/dashboard' });
}
