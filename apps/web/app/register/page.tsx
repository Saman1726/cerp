import { User, Building2, Store } from 'lucide-react';
import Link from 'next/link';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@cerp/core-web/components/ui';

export default function RegisterPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-3xl">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">Create your account</CardTitle>
          <CardDescription>Choose how you want to register</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            {/* Register as User */}
            <Link href="/register/user" className="group">
              <Card className="h-full cursor-pointer transition-shadow hover:shadow-lg">
                <CardHeader className="text-center">
                  <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-blue-100">
                    <User className="h-6 w-6 text-blue-600" />
                  </div>
                  <CardTitle className="text-lg">User</CardTitle>
                  <CardDescription>
                    Register as an individual user to access the platform
                  </CardDescription>
                </CardHeader>
              </Card>
            </Link>

            {/* Register as Company */}
            <Link href="/register/company" className="group">
              <Card className="h-full cursor-pointer transition-shadow hover:shadow-lg">
                <CardHeader className="text-center">
                  <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-green-100">
                    <Building2 className="h-6 w-6 text-green-600" />
                  </div>
                  <CardTitle className="text-lg">Company</CardTitle>
                  <CardDescription>
                    Register your organization to manage projects and teams
                  </CardDescription>
                </CardHeader>
              </Card>
            </Link>

            {/* Register as Vendor */}
            <Link href="/register/vendor" className="group">
              <Card className="h-full cursor-pointer transition-shadow hover:shadow-lg">
                <CardHeader className="text-center">
                  <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-purple-100">
                    <Store className="h-6 w-6 text-purple-600" />
                  </div>
                  <CardTitle className="text-lg">Vendor</CardTitle>
                  <CardDescription>
                    Register as a vendor to join the procurement network
                  </CardDescription>
                </CardHeader>
              </Card>
            </Link>
          </div>

          <div className="mt-6 text-center text-sm">
            Already have an account?{' '}
            <Link href="/login" className="text-blue-600 hover:underline">
              Sign in
            </Link>
          </div>
        </CardContent>
      </Card>
    </main>
  );
}