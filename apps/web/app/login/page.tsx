import { LoginForm } from '@cerp/core-web/auth';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@cerp/core-web/components/ui';
import { loginAction } from './actions';

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">cerp</CardTitle>
          <CardDescription>Sign in to your organization</CardDescription>
        </CardHeader>
        <CardContent>
          <LoginForm action={loginAction} />
        </CardContent>
      </Card>
    </main>
  );
}
