import { LoginForm } from './login-form';

export default function LoginPage() {
  return (
    <main style={{ maxWidth: 360, margin: '80px auto', fontFamily: 'system-ui' }}>
      <h1>cerp</h1>
      <p>Sign in to your organization</p>
      <LoginForm />
    </main>
  );
}
