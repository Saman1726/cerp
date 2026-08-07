'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@cerp/core-web/components/ui';
import { Button } from '@cerp/core-web/components/ui/button';
import { Input } from '@cerp/core-web/components/ui/input';
import { Label } from '@cerp/core-web/components/ui/label';

export default function CompanyRegisterPage() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    companyName: '',
    companyRegistrationNumber: '',
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
    industry: '',
    companySize: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPending(true);
    setError(null);

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      setPending(false);
      return;
    }

    try {
      const response = await fetch('/api/auth/register-company', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          companyName: formData.companyName,
          companyRegistrationNumber: formData.companyRegistrationNumber,
          email: formData.email,
          username: formData.username,
          password: formData.password,
          industry: formData.industry,
          companySize: formData.companySize,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setError(data.message || 'Registration failed. Please try again.');
        setPending(false);
        return;
      }

      // Registration successful, redirect to login
      router.push('/login?registered=true');
    } catch (err) {
      console.error('Registration error:', err);
      setError('An unexpected error occurred. Please try again.');
      setPending(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">Register as Company</CardTitle>
          <CardDescription>Create your organization account</CardDescription>
        </CardHeader>
        <CardContent>
          {error && (
            <div className="mb-4 rounded bg-red-50 p-3 text-sm text-red-600">
              {error}
            </div>
          )}
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="grid gap-2">
              <Label htmlFor="companyName">Company Name</Label>
              <Input
                id="companyName"
                name="companyName"
                type="text"
                required
                value={formData.companyName}
                onChange={(e) => setFormData({ ...formData, companyName: e.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="companyRegistrationNumber">Company Registration Number</Label>
              <Input
                id="companyRegistrationNumber"
                name="companyRegistrationNumber"
                type="text"
                required
                value={formData.companyRegistrationNumber}
                onChange={(e) => setFormData({ ...formData, companyRegistrationNumber: e.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                name="email"
                type="email"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="username">Username</Label>
              <Input
                id="username"
                name="username"
                type="text"
                required
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                name="password"
                type="password"
                required
                minLength={6}
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="confirmPassword">Confirm Password</Label>
              <Input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                required
                minLength={6}
                value={formData.confirmPassword}
                onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="industry">Industry</Label>
              <Input
                id="industry"
                name="industry"
                type="text"
                value={formData.industry}
                onChange={(e) => setFormData({ ...formData, industry: e.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="companySize">Company Size</Label>
              <Input
                id="companySize"
                name="companySize"
                type="text"
                placeholder="e.g., 1-50, 51-200, 201-500"
                value={formData.companySize}
                onChange={(e) => setFormData({ ...formData, companySize: e.target.value })}
              />
            </div>
            <Button type="submit" disabled={pending} className="mt-2">
              {pending ? 'Creating account...' : 'Create account'}
            </Button>
          </form>
          <div className="mt-4 flex items-center gap-2 text-sm">
            <Link href="/register" className="text-blue-600 hover:underline">
              <ArrowLeft className="mr-1 inline h-4 w-4" />
              Back to registration options
            </Link>
          </div>
        </CardContent>
      </Card>
    </main>
  );
}