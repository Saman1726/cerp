import { LoginForm } from '@cerp/core-web/auth';
import { branding } from '@/branding.config';
import { loginAction } from './actions';

export default function LoginPage() {
  return (
    <main style={{ maxWidth: 360, margin: '80px auto', fontFamily: 'system-ui' }}>
      <h1>{branding.productName}</h1>
      <p>Sign in to your organization</p>
      <LoginForm action={loginAction} />
    </main>
  );
}
