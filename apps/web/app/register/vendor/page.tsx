// app/register/vendor/page.tsx
'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useState, useCallback } from 'react';
import { ArrowLeft, Check, ChevronRight, ChevronLeft } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@cerp/core-web/components/ui';
import { Button } from '@cerp/core-web/components/ui/button';
import { Input } from '@cerp/core-web/components/ui/input';
import { Label } from '@cerp/core-web/components/ui/label';
import { Textarea } from '@cerp/core-web/components/ui/textarea';
import { Checkbox } from '@cerp/core-web/components/ui/checkbox';
import { CollapsibleSection } from '@cerp/core-web/components/ui/collapsible-section';
import {
  VENDOR_TYPES,
  VENDOR_PROJECT_TYPES,
  type VendorProjectType,
} from '@cerp/shared-types/vendor';

// ── Form State ──

interface FormData {
  // Step 1: Account
  username: string;
  email: string;
  password: string;
  confirmPassword: string;

  // Step 2: Company Info
  legalName: string;
  contactName: string;
  phone: string;
  website: string;
  registrationNumber: string;
  description: string;
  logoUrl: string;
  address: string;
  city: string;
  country: string;
  regions: string[];

  // Step 3: Vendor Details
  vendorTypes: string[];
  projectTypes: VendorProjectType[];
  yearsInBusiness: string;
}

const initialFormData: FormData = {
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  legalName: '',
  contactName: '',
  phone: '',
  website: '',
  registrationNumber: '',
  description: '',
  logoUrl: '',
  address: '',
  city: '',
  country: '',
  regions: [],
  vendorTypes: [],
  projectTypes: [] as VendorProjectType[],
  yearsInBusiness: '',
};

const STEPS = [
  { id: 0, title: 'Account', description: 'Login credentials' },
  { id: 1, title: 'Company', description: 'Business details' },
  { id: 2, title: 'Vendor Profile', description: 'Categories & types' },
  { id: 3, title: 'Review', description: 'Confirm & submit' },
];

const REGIONS = ['Central', 'Eastern', 'Western', 'Northern', 'Southern', 'Coastal'];

// ── Page Component ──

export default function VendorRegisterPage() {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState<FormData>(initialFormData);

  const updateField = useCallback(
    (field: keyof FormData, value: string) => {
      setFormData((prev) => ({ ...prev, [field]: value }));
      setError(null);
    },
    [],
  );

  const toggleMulti = useCallback(
    <T extends string>(field: 'vendorTypes' | 'regions', value: T) => {
      setFormData((prev) => ({
        ...prev,
        [field]: prev[field].includes(value)
          ? prev[field].filter((v) => v !== value)
          : [...prev[field], value],
      }));
      setError(null);
    },
    [],
  );

  const toggleProjectType = useCallback((value: VendorProjectType) => {
    setFormData((prev) => ({
      ...prev,
      projectTypes: prev.projectTypes.includes(value)
        ? prev.projectTypes.filter((v) => v !== value)
        : [...prev.projectTypes, value],
    }));
    setError(null);
  }, []);

  const nextStep = () => setStep((s) => Math.min(s + 1, STEPS.length - 1));
  const prevStep = () => setStep((s) => Math.max(s - 1, 0));

  // ── Validation ──

  const validateStep = (s: number): boolean => {
    switch (s) {
      case 0: // Account
        if (!formData.username.trim()) { setError('Username is required'); return false; }
        if (!formData.email.trim() || !/\S+@\S+\.\S+/.test(formData.email)) {
          setError('Valid email is required'); return false;
        }
        if (formData.password.length < 6) { setError('Password must be at least 6 characters'); return false; }
        if (formData.password !== formData.confirmPassword) { setError('Passwords do not match'); return false; }
        return true;
      case 1: // Company
        if (!formData.legalName.trim()) { setError('Legal company name is required'); return false; }
        return true;
      case 2: // Vendor Details
        if (formData.vendorTypes.length === 0) { setError('Select at least one vendor type'); return false; }
        return true;
      case 3: // Review
        return true;
      default:
        return true;
    }
  };

  // ── Submit ──

  const handleSubmit = async () => {
    if (!validateStep(step)) return;
    setPending(true);
    setError(null);

    try {
      const res = await fetch('/api/auth/register-vendor', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: formData.username,
          email: formData.email,
          password: formData.password,
          vendorData: {
            legalName: formData.legalName,
            contactName: formData.contactName || undefined,
            phone: formData.phone || undefined,
            website: formData.website || undefined,
            vendorTypes: formData.vendorTypes,
            projectTypes: formData.projectTypes,
            registrationNumber: formData.registrationNumber || undefined,
            description: formData.description || undefined,
            logoUrl: formData.logoUrl || undefined,
            regions: formData.regions.length > 0 ? formData.regions : undefined,
            address: formData.address || undefined,
            city: formData.city || undefined,
            country: formData.country || undefined,
          },
        }),
      });

      if (!res.ok) {
        const data = await res.json();
        throw new Error(data.message || 'Registration failed');
      }

      router.push('/register/vendor/success');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setPending(false);
    }
  };

  // ── Step Renderers ──

  const renderAccountStep = () => (
    <div className="space-y-4">
      <div className="grid gap-2">
        <Label htmlFor="username">Username</Label>
        <Input id="username" value={formData.username} onChange={(e) => updateField('username', e.target.value)} required />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="email">Email</Label>
        <Input id="email" type="email" value={formData.email} onChange={(e) => updateField('email', e.target.value)} required />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="password">Password</Label>
        <Input id="password" type="password" value={formData.password} onChange={(e) => updateField('password', e.target.value)} required minLength={6} />
      </div>
      <div className="grid gap-2">
        <Label htmlFor="confirmPassword">Confirm Password</Label>
        <Input id="confirmPassword" type="password" value={formData.confirmPassword} onChange={(e) => updateField('confirmPassword', e.target.value)} required minLength={6} />
        {formData.password && formData.confirmPassword && formData.password !== formData.confirmPassword && (
          <span className="text-xs text-red-500">Passwords do not match</span>
        )}
      </div>
    </div>
  );

  const renderCompanyStep = () => (
    <div className="space-y-4">
      <CollapsibleSection title="Legal Information" defaultOpen>
        <div className="grid gap-2">
          <Label htmlFor="legalName">Legal Company Name <span className="text-red-500">*</span></Label>
          <Input id="legalName" value={formData.legalName} onChange={(e) => updateField('legalName', e.target.value)} required />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="contactName">Contact Person</Label>
          <Input id="contactName" value={formData.contactName} onChange={(e) => updateField('contactName', e.target.value)} />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="registrationNumber">Registration Number</Label>
          <Input id="registrationNumber" value={formData.registrationNumber} onChange={(e) => updateField('registrationNumber', e.target.value)} />
        </div>
      </CollapsibleSection>
      <CollapsibleSection title="Contact Details" defaultOpen>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="grid gap-2">
            <Label htmlFor="phone">Phone</Label>
            <Input id="phone" type="tel" value={formData.phone} onChange={(e) => updateField('phone', e.target.value)} />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="website">Website</Label>
            <Input id="website" type="url" placeholder="https://example.com" value={formData.website} onChange={(e) => updateField('website', e.target.value)} />
          </div>
        </div>
      </CollapsibleSection>
      <CollapsibleSection title="About">
        <div className="grid gap-2">
          <Label htmlFor="description">Description</Label>
          <Textarea id="description" rows={3} value={formData.description} onChange={(e) => updateField('description', e.target.value)} placeholder="Brief description of your company" />
        </div>
        <div className="grid gap-2">
          <Label htmlFor="logoUrl">Logo URL</Label>
          <Input id="logoUrl" type="url" value={formData.logoUrl} onChange={(e) => updateField('logoUrl', e.target.value)} placeholder="https://example.com/logo.png" />
        </div>
      </CollapsibleSection>
      <CollapsibleSection title="Location">
        <div className="grid gap-2">
          <Label htmlFor="address">Address</Label>
          <Input id="address" value={formData.address} onChange={(e) => updateField('address', e.target.value)} />
        </div>
        <div className="grid grid-cols-2 gap-4">
          <div className="grid gap-2">
            <Label htmlFor="city">City</Label>
            <Input id="city" value={formData.city} onChange={(e) => updateField('city', e.target.value)} />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="country">Country</Label>
            <Input id="country" value={formData.country} onChange={(e) => updateField('country', e.target.value)} placeholder="e.g., Germany" />
          </div>
        </div>
        <div className="grid gap-2">
          <Label>Service Regions</Label>
          <div className="flex flex-wrap gap-3">
            {REGIONS.map((r) => (
              <div key={r} className="flex items-center gap-1.5">
                <Checkbox id={`region-${r}`} checked={formData.regions.includes(r)} onCheckedChange={() => toggleMulti('regions', r)} />
                <Label htmlFor={`region-${r}`} className="text-sm font-normal">{r}</Label>
              </div>
            ))}
          </div>
        </div>
      </CollapsibleSection>
    </div>
  );

  const renderVendorDetailsStep = () => (
    <div className="space-y-5">
      <div className="grid gap-2">
        <Label>Vendor Types <span className="text-red-500">*</span></Label>
        <div className="flex flex-wrap gap-3">
          {VENDOR_TYPES.map((vt) => (
            <div key={vt} className="flex items-center gap-1.5">
              <Checkbox id={`vt-${vt}`} checked={formData.vendorTypes.includes(vt)} onCheckedChange={() => toggleMulti('vendorTypes', vt)} />
              <Label htmlFor={`vt-${vt}`} className="text-sm font-normal capitalize">{vt.toLowerCase().replace(/_/g, ' ')}</Label>
            </div>
          ))}
        </div>
      </div>
      <div className="grid gap-2">
        <Label>Project Types</Label>
        <div className="flex flex-wrap gap-3">
          {VENDOR_PROJECT_TYPES.map((pt) => (
            <div key={pt} className="flex items-center gap-1.5">
              <Checkbox id={`pt-${pt}`} checked={formData.projectTypes.includes(pt)} onCheckedChange={() => toggleProjectType(pt)} />
              <Label htmlFor={`pt-${pt}`} className="text-sm font-normal capitalize">{pt.toLowerCase().replace(/_/g, ' ')}</Label>
            </div>
          ))}
        </div>
      </div>
      <div className="grid gap-2">
        <Label htmlFor="yearsInBusiness">Years in Business</Label>
        <Input id="yearsInBusiness" type="number" min={0} value={formData.yearsInBusiness} onChange={(e) => updateField('yearsInBusiness', e.target.value)} />
      </div>
    </div>
  );

  const renderReviewStep = () => (
    <div className="space-y-4 text-sm">
      <CollapsibleSection title="Account">
        <dl className="grid grid-cols-2 gap-x-4 gap-y-2">
          <dt className="text-muted-foreground">Username:</dt><dd>{formData.username}</dd>
          <dt className="text-muted-foreground">Email:</dt><dd>{formData.email}</dd>
        </dl>
      </CollapsibleSection>
      <CollapsibleSection title="Company">
        <dl className="grid grid-cols-2 gap-x-4 gap-y-2">
          <dt className="text-muted-foreground">Legal Name:</dt><dd>{formData.legalName}</dd>
          <dt className="text-muted-foreground">Contact:</dt><dd>{formData.contactName || '—'}</dd>
          <dt className="text-muted-foreground">Phone:</dt><dd>{formData.phone || '—'}</dd>
          <dt className="text-muted-foreground">Website:</dt><dd>{formData.website || '—'}</dd>
          {formData.registrationNumber && <>
            <dt className="text-muted-foreground">Reg. Number:</dt><dd>{formData.registrationNumber}</dd>
          </>}
          {formData.description && <>
            <dt className="text-muted-foreground">Description:</dt><dd>{formData.description}</dd>
          </>}
          <dt className="text-muted-foreground">Location:</dt><dd>
            {[formData.address, formData.city, formData.country].filter(Boolean).join(', ') || '—'}
          </dd>
        </dl>
      </CollapsibleSection>
      <CollapsibleSection title="Vendor Profile">
        <dl className="grid grid-cols-2 gap-x-4 gap-y-2">
          <dt className="text-muted-foreground">Types:</dt>
          <dd className="capitalize">{formData.vendorTypes.map((v) => v.toLowerCase().replace(/_/g, ' ')).join(', ')}</dd>
          {formData.projectTypes.length > 0 && <>
            <dt className="text-muted-foreground">Projects:</dt>
            <dd className="capitalize">{formData.projectTypes.map((p) => p.toLowerCase().replace(/_/g, ' ')).join(', ')}</dd>
          </>}
          {formData.yearsInBusiness && <><dt className="text-muted-foreground">Years in Business:</dt><dd>{formData.yearsInBusiness}</dd></>}
        </dl>
      </CollapsibleSection>
    </div>
  );

  const stepContent = [renderAccountStep, renderCompanyStep, renderVendorDetailsStep, renderReviewStep][step]!();

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <Card className="w-full max-w-2xl">
        <CardHeader>
          <CardTitle className="text-xl">Register as Vendor</CardTitle>
          <CardDescription>Join the procurement network</CardDescription>
        </CardHeader>
        <CardContent className="space-y-5">
          {/* Step Indicators */}
          <div className="flex items-center gap-1">
            {STEPS.map((s, i) => (
              <div key={s.id} className="flex flex-1 items-center gap-1 last:flex-none last:w-auto">
                <div
                  className={`flex h-7 w-7 items-center justify-center rounded-full text-xs font-medium transition-colors ${
                    i <= step ? 'bg-primary text-primary-foreground' : 'bg-muted text-muted-foreground'
                  }`}
                >
                  {i < step ? <Check className="h-3.5 w-3.5" /> : s.id + 1}
                </div>
                <span className={`ml-1 text-xs font-medium ${i <= step ? 'text-foreground' : 'text-muted-foreground'} hidden sm:block`}>
                  {s.title}
                </span>
                {i < STEPS.length - 1 && <ChevronRight className="h-3 w-3 text-muted-foreground flex-shrink-0" />}
              </div>
            ))}
          </div>

          {error && <p className="rounded-md bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}

          {stepContent}

          {/* Navigation */}
          <div className="flex items-center justify-between">
            <Button variant="ghost" type="button" onClick={prevStep} disabled={step === 0 || pending}>
              <ChevronLeft className="mr-1 h-4 w-4" /> Back
            </Button>
            <div className="flex gap-2">
              {step < STEPS.length - 1 ? (
                <Button type="button" onClick={nextStep} disabled={pending}>
                  Next <ChevronRight className="ml-1 h-4 w-4" />
                </Button>
              ) : (
                <Button type="button" onClick={handleSubmit} disabled={pending}>
                  {pending ? 'Submitting...' : 'Submit Registration'}
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
      <div className="fixed bottom-4 left-4 text-sm">
        <Link href="/register" className="flex items-center gap-1 text-blue-600 hover:underline">
          <ArrowLeft className="h-4 w-4" /> Back to registration options
        </Link>
      </div>
    </main>
  );
}
