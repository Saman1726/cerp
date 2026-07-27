'use client';

import { useState, useEffect } from 'react';
import {
  Building2,
  Mail,
  Phone,
  Globe,
  MapPin,
  User,
  Tag,
  Save,
  Loader2,
  CheckCircle2,
  XCircle,
} from 'lucide-react';
import { PageHeader } from '@cerp/core-web/shell';
import { Input } from '@cerp/core-web/components/ui/input';
import { Label } from '@cerp/core-web/components/ui/label';
import { Button } from '@cerp/core-web/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@cerp/core-web/components/ui/card';
import { Separator } from '@cerp/core-web/components/ui/separator';
import { Badge } from '@cerp/core-web/components/ui/badge';
import { CategorySelector } from '@cerp/core-web/vendors';
import type { CategoryData, VendorProfileData } from './actions';
import { getVendorProfile, getCategories, updateVendorProfile } from './actions';

export default function VendorProfilePage() {
  const [profile, setProfile] = useState<VendorProfileData | null>(null);
  const [categories, setCategories] = useState<CategoryData[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [saveMessage, setSaveMessage] = useState('');

  // Form state
  const [formData, setFormData] = useState({
    legalName: '',
    companyName: '',
    contactPerson: '',
    contactEmail: '',
    contactPhone: '',
    website: '',
    addressLine1: '',
    addressLine2: '',
    postalCode: '',
    city: '',
    countryCode: '',
    categoryIds: [] as string[],
  });

  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  // Fetch profile and categories on mount
  useEffect(() => {
    async function fetchData() {
      try {
        const [profileRes, categoriesRes] = await Promise.all([
          getVendorProfile(),
          getCategories(),
        ]);

        if (profileRes.success && profileRes.data) {
          const p = profileRes.data;
          setProfile(p);
          setFormData({
            legalName: p.legalName || '',
            companyName: p.companyName || '',
            contactPerson: p.contactPerson || '',
            contactEmail: p.contactEmail || '',
            contactPhone: p.contactPhone || '',
            website: p.website || '',
            addressLine1: p.addressLine1 || '',
            addressLine2: p.addressLine2 || '',
            postalCode: p.postalCode || '',
            city: p.city || '',
            countryCode: p.countryCode || '',
            categoryIds: p.categories.map((c) => c.categoryId),
          });
        } else {
          setSaveStatus('error');
          setSaveMessage(profileRes.error || 'Failed to load vendor profile');
        }

        if (categoriesRes.success && categoriesRes.data) {
          setCategories(categoriesRes.data);
        }
      } catch (error) {
        setSaveStatus('error');
        setSaveMessage('Failed to load data');
        console.error('Error fetching data:', error);
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, []);

  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.legalName.trim()) {
      errors.legalName = 'Legal name is required';
    }

    if (formData.contactEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.contactEmail)) {
      errors.contactEmail = 'Invalid email format';
    }

    if (formData.website && !/^https?:\/\/.+/.test(formData.website)) {
      errors.website = 'URL must start with http:// or https://';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleInputChange = (field: string, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (formErrors[field]) {
      setFormErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  };

  const handleCategoryChange = (selectedIds: string[]) => {
    setFormData((prev) => ({ ...prev, categoryIds: selectedIds }));
    if (formErrors.categoryIds) {
      setFormErrors((prev) => {
        const next = { ...prev };
        delete next.categoryIds;
        return next;
      });
    }
  };

  const handleSave = async () => {
    if (!validateForm()) return;

    setSaving(true);
    setSaveStatus('idle');

    try {
      const result = await updateVendorProfile(formData);

      if (result.success && result.data) {
        setSaveStatus('success');
        setSaveMessage('Profile updated successfully');
        setProfile(result.data);

        // Clear success message after 3 seconds
        setTimeout(() => {
          setSaveStatus('idle');
          setSaveMessage('');
        }, 3000);
      } else {
        setSaveStatus('error');
        setSaveMessage(result.error || 'Failed to update profile');
      }
    } catch {
      setSaveStatus('error');
      setSaveMessage('An unexpected error occurred');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="text-center">
          <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-muted-foreground" />
          <p className="text-muted-foreground">Loading vendor profile...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Vendor Profile"
        description="Manage your vendor profile, contact information, and business categories."
      />

      {/* Status message */}
      {saveStatus !== 'idle' && (
        <div
          className={`flex items-center gap-2 p-3 rounded-lg ${
            saveStatus === 'success'
              ? 'bg-green-50 text-green-800 border border-green-200'
              : 'bg-red-50 text-red-800 border border-red-200'
          }`}
        >
          {saveStatus === 'success' ? (
            <CheckCircle2 className="h-5 w-5 shrink-0" />
          ) : (
            <XCircle className="h-5 w-5 shrink-0" />
          )}
          <span className="text-sm">{saveMessage}</span>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-3">
        {/* Main form */}
        <div className="lg:col-span-2 space-y-6">
          {/* Business Information */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Building2 className="h-5 w-5" />
                Business Information
              </CardTitle>
              <CardDescription>Your company{'\''}s legal and business details.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="legalName">Legal Name *</Label>
                <Input
                  id="legalName"
                  placeholder="Enter your legal company name"
                  value={formData.legalName}
                  onChange={(e) => handleInputChange('legalName', e.target.value)}
                  aria-invalid={!!formErrors.legalName}
                  aria-describedby={formErrors.legalName ? 'legalName-error' : undefined}
                />
                {formErrors.legalName && (
                  <p id="legalName-error" className="text-sm text-destructive">{formErrors.legalName}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="companyName">Trading Name / DBA</Label>
                <Input
                  id="companyName"
                  placeholder="How your business is commonly known"
                  value={formData.companyName}
                  onChange={(e) => handleInputChange('companyName', e.target.value)}
                />
              </div>

              <Separator />

              {/* Contact Information */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium">Contact Information</h4>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="contactPerson" className="flex items-center gap-2">
                      <User className="h-4 w-4" />
                      Contact Person
                    </Label>
                    <Input
                      id="contactPerson"
                      placeholder="Primary contact person"
                      value={formData.contactPerson}
                      onChange={(e) => handleInputChange('contactPerson', e.target.value)}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="contactEmail" className="flex items-center gap-2">
                      <Mail className="h-4 w-4" />
                      Email
                    </Label>
                    <Input
                      id="contactEmail"
                      type="email"
                      placeholder="contact@company.com"
                      value={formData.contactEmail}
                      onChange={(e) => handleInputChange('contactEmail', e.target.value)}
                      aria-invalid={!!formErrors.contactEmail}
                      aria-describedby={formErrors.contactEmail ? 'contactEmail-error' : undefined}
                    />
                    {formErrors.contactEmail && (
                      <p id="contactEmail-error" className="text-sm text-destructive">{formErrors.contactEmail}</p>
                    )}
                  </div>
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="contactPhone" className="flex items-center gap-2">
                      <Phone className="h-4 w-4" />
                      Phone
                    </Label>
                    <Input
                      id="contactPhone"
                      type="tel"
                      placeholder="+1 (555) 000-0000"
                      value={formData.contactPhone}
                      onChange={(e) => handleInputChange('contactPhone', e.target.value)}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="website" className="flex items-center gap-2">
                      <Globe className="h-4 w-4" />
                      Website
                    </Label>
                    <Input
                      id="website"
                      type="url"
                      placeholder="https://www.company.com"
                      value={formData.website}
                      onChange={(e) => handleInputChange('website', e.target.value)}
                      aria-invalid={!!formErrors.website}
                      aria-describedby={formErrors.website ? 'website-error' : undefined}
                    />
                    {formErrors.website && (
                      <p id="website-error" className="text-sm text-destructive">{formErrors.website}</p>
                    )}
                  </div>
                </div>
              </div>

              <Separator />

              {/* Address */}
              <div className="space-y-4">
                <h4 className="text-sm font-medium flex items-center gap-2">
                  <MapPin className="h-4 w-4" />
                  Address
                </h4>

                <div className="space-y-2">
                  <Label htmlFor="addressLine1">Street Address</Label>
                  <Input
                    id="addressLine1"
                    placeholder="123 Main Street"
                    value={formData.addressLine1}
                    onChange={(e) => handleInputChange('addressLine1', e.target.value)}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="addressLine2">Address Line 2</Label>
                  <Input
                    id="addressLine2"
                    placeholder="Suite, Unit, Building, etc."
                    value={formData.addressLine2}
                    onChange={(e) => handleInputChange('addressLine2', e.target.value)}
                  />
                </div>

                <div className="grid gap-4 sm:grid-cols-3">
                  <div className="space-y-2">
                    <Label htmlFor="city">City</Label>
                    <Input
                      id="city"
                      placeholder="City"
                      value={formData.city}
                      onChange={(e) => handleInputChange('city', e.target.value)}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="postalCode">Postal Code</Label>
                    <Input
                      id="postalCode"
                      placeholder="12345"
                      value={formData.postalCode}
                      onChange={(e) => handleInputChange('postalCode', e.target.value)}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor="countryCode">Country Code</Label>
                    <Input
                      id="countryCode"
                      placeholder="US"
                      value={formData.countryCode}
                      onChange={(e) => handleInputChange('countryCode', e.target.value)}
                    />
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Categories */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Tag className="h-5 w-5" />
                Business Categories
              </CardTitle>
              <CardDescription>
                Select the construction categories that describe your business. You can select multiple categories.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <CategorySelector
                categories={
                  categories.map((c) => ({
                    id: c.id,
                    code: c.code as never,
                    name: c.name,
                    description: c.description,
                    icon: c.icon,
                    displayOrder: c.displayOrder,
                    groupId: c.groupId,
                    groupName: c.groupName,
                  }))
                }
                selectedCategoryIds={formData.categoryIds}
                onChange={handleCategoryChange}
                error={formErrors.categoryIds}
              />
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Status */}
          <Card>
            <CardHeader>
              <CardTitle>Profile Status</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {profile?.status && (
                <div className="space-y-2">
                  <span className="text-sm text-muted-foreground">Vendor Status:</span>
                  <Badge
                    variant={
                      profile.status === 'approved'
                        ? 'success'
                        : profile.status === 'rejected'
                          ? 'destructive'
                          : 'secondary'
                    }
                  >
                    {profile.status}
                  </Badge>
                </div>
              )}

              {profile?.ratingAvg != null && (
                <div className="space-y-2">
                  <span className="text-sm text-muted-foreground">Rating:</span>
                  <div className="flex items-center gap-2">
                    <span className="text-2xl font-bold">{profile.ratingAvg.toFixed(1)}</span>
                    <span className="text-sm text-muted-foreground">/ 5</span>
                  </div>
                </div>
              )}

              <Separator />

              <div className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Created:</span>
                  <span>
                    {profile?.createdAt
                      ? new Date(profile.createdAt).toLocaleDateString()
                      : '—'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Last updated:</span>
                  <span>
                    {profile?.updatedAt
                      ? new Date(profile.updatedAt).toLocaleDateString()
                      : '—'}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Save button */}
          <Button
            className="w-full gap-2"
            size="lg"
            onClick={handleSave}
            disabled={saving}
          >
            {saving ? (
              <>
                <Loader2 className="h-4 w-4 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="h-4 w-4" />
                Save Changes
              </>
            )}
          </Button>

          <p className="text-xs text-center text-muted-foreground">
            Changes will be reviewed by your administrator.
          </p>
        </div>
      </div>
    </div>
  );
}