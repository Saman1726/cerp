'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@cerp/core-web/components/ui';
import { Button } from '@cerp/core-web/components/ui/button';
import { Input } from '@cerp/core-web/components/ui/input';
import { Label } from '@cerp/core-web/components/ui/label';

const VENDOR_CATEGORIES = [
  'Construction Materials',
  'Equipment Rental',
  'Labor Services',
  'Engineering Consulting',
  'Logistics & Transport',
  'Electrical & HVAC',
  'Plumbing',
  'Finishing & Interior',
  'IT & Software',
  'Other',
];

export default function VendorRegisterPage() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [formData, setFormData] = useState({
    vendorName: '',
    businessRegistrationNumber: '',
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
    categories: [] as string[],
    city: '',
    description: '',
  });

  const handleCategoryToggle = (category: string) => {
    setFormData((prev) => ({
      ...prev,
      categories: prev.categories.includes(category)
        ? prev.categories.filter((c) => c !== category)
        : [...prev.categories, category],
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPending(true);

    if (formData.password !== formData.confirmPassword) {
      alert('Passwords do not match');
      setPending(false);
      return;
    }

    if (formData.categories.length === 0) {
      alert('Please select at least one vendor category');
      setPending(false);
      return;
    }

    // TODO: Implement actual registration API call
    console.log('Vendor registration:', formData);

    // Simulate API call
    setTimeout(() => {
      setPending(false);
      router.push('/login');
    }, 1000);
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="text-xl">Register as Vendor</CardTitle>
          <CardDescription>Create your vendor account</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <div className="grid gap-2">
              <Label htmlFor="vendorName">Vendor Name</Label>
              <Input
                id="vendorName"
                name="vendorName"
                type="text"
                required
                value={formData.vendorName}
                onChange={(e) => setFormData({ ...formData, vendorName: e.target.value })}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="businessRegistrationNumber">Business Registration Number</Label>
              <Input
                id="businessRegistrationNumber"
                name="businessRegistrationNumber"
                type="text"
                required
                value={formData.businessRegistrationNumber}
                onChange={(e) => setFormData({ ...formData, businessRegistrationNumber: e.target.value })}
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
              <Label htmlFor="categories">Categories</Label>
              <div className="flex flex-wrap gap-2">
                {VENDOR_CATEGORIES.map((category) => (
                  <button
                    key={category}
                    type="button"
                    onClick={() => handleCategoryToggle(category)}
                    className={`rounded-full px-3 py-1 text-sm transition-colors ${
                      formData.categories.includes(category)
                        ? 'bg-purple-600 text-white'
                        : 'bg-muted text-muted-foreground hover:bg-muted/80'
                    }`}
                  >
                    {category}
                  </button>
                ))}
              </div>
            </div>
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
              <Label htmlFor="description">Description</Label>
              <textarea
                id="description"
                name="description"
                rows={3}
                className="w-full rounded-md border bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
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