'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@cerp/core-web/components/ui';
import { Button } from '@cerp/core-web/components/ui/button';
import { Input } from '@cerp/core-web/components/ui/input';
import { Label } from '@cerp/core-web/components/ui/label';

interface RegisterState {
  error?: string;
  success?: string;
}

export default function VendorRegisterPage() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [state, setState] = useState<RegisterState>({});
  const [formData, setFormData] = useState({
    companyName: '',
    contactName: '',
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
    phone: '',
    website: '',
    category: '',
    address: '',
    city: '',
    country: '',
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setState({});
    setPending(true);

    if (formData.password !== formData.confirmPassword) {
      setState({ error: 'Passwords do not match' });
      setPending(false);
      return;
    }

    if (formData.password.length < 6) {
      setState({ error: 'Password must be at least 6 characters' });
      setPending(false);
      return;
    }

    try {
      // Step 1: Register user account via identity service
      const response = await fetch('/api/auth/register-vendor', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: formData.username,
          email: formData.email,
          password: formData.password,
          vendorData: {
            legalName: formData.companyName,
            contactName: formData.contactName,
            phone: formData.phone,
            website: formData.website,
            categories: formData.category ? [formData.category] : [],
            address: formData.address,
            city: formData.city,
            country: formData.country,
          },
        }),
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.message || 'Registration failed');
      }

      // Registration successful - redirect to login
      router.push('/register/vendor/success');
    } catch (err) {
      setState({ error: err instanceof Error ? err.message : 'Registration failed. Please try again.' });
    } finally {
      setPending(false);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-lg">
        <CardHeader>
          <CardTitle className="text-xl">Register as Vendor</CardTitle>
          <CardDescription>Join the procurement network as a vendor</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            {/* Account Information */}
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

            {/* Company Information */}
            <div className="grid gap-2">
              <Label htmlFor="companyName">Company Legal Name</Label>
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
              <Label htmlFor="contactName">Contact Person Name</Label>
              <Input
                id="contactName"
                name="contactName"
                type="text"
                value={formData.contactName}
                onChange={(e) => setFormData({ ...formData, contactName: e.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="category">Vendor Category</Label>
              <Input
                id="category"
                name="category"
                type="text"
                placeholder="e.g., Construction Materials, Electronics"
                value={formData.category}
                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
              />
            </div>

            {/* Contact Information */}
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label htmlFor="phone">Phone</Label>
                <Input
                  id="phone"
                  name="phone"
                  type="tel"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="website">Website</Label>
                <Input
                  id="website"
                  name="website"
                  type="url"
                  placeholder="https://example.com"
                  value={formData.website}
                  onChange={(e) => setFormData({ ...formData, website: e.target.value })}
                />
              </div>
            </div>

            {/* Address */}
            <div className="grid gap-2">
              <Label htmlFor="address">Address</Label>
              <Input
                id="address"
                name="address"
                type="text"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <div className="grid gap-2">
                <Label htmlFor="city">City</Label>
                <Input
                  id="city"
                  name="city"
                  type="text"
                  value={formData.city}
                  onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="country">Country</Label>
                <Input
                  id="country"
                  name="country"
                  type="text"
                  placeholder="e.g., Germany"
                  value={formData.country}
                  onChange={(e) => setFormData({ ...formData, country: e.target.value })}
                />
              </div>
            </div>

            {state.error ? <p className="text-sm text-destructive">{state.error}</p> : null}

            <Button type="submit" disabled={pending} className="mt-2">
              {pending ? 'Creating vendor account...' : 'Create vendor account'}
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