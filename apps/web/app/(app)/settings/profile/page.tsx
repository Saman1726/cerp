'use client';

import { useState, useEffect } from 'react';
import { User, Save, Loader2, CheckCircle2, XCircle } from 'lucide-react';
import { PageHeader } from '@cerp/core-web/shell';
import { Input } from '@cerp/core-web/components/ui/input';
import { Label } from '@cerp/core-web/components/ui/label';
import { Button } from '@cerp/core-web/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@cerp/core-web/components/ui/card';
import { Avatar, AvatarFallback } from '@cerp/core-web/components/ui/avatar';
import { Separator } from '@cerp/core-web/components/ui/separator';
import type { UserProfileData } from './actions';
import { getUserProfile, updateUserProfile } from './actions';

export default function UserProfilePage() {
  const [profile, setProfile] = useState<UserProfileData | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveStatus, setSaveStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [saveMessage, setSaveMessage] = useState('');

  const [formData, setFormData] = useState({
    name: '',
  });

  const [formErrors, setFormErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    async function fetchData() {
      try {
        const profileRes = await getUserProfile();

        if (profileRes.success && profileRes.data) {
          const p = profileRes.data;
          setProfile(p);
          setFormData({
            name: p.name || '',
          });
        } else {
          setSaveStatus('error');
          setSaveMessage(profileRes.error || 'Failed to load profile');
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

    if (!formData.name.trim()) {
      errors.name = 'Name is required';
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

  const handleSave = async () => {
    if (!validateForm()) return;

    setSaving(true);
    setSaveStatus('idle');

    try {
      const result = await updateUserProfile(formData);

      if (result.success && result.data) {
        setSaveStatus('success');
        setSaveMessage('Profile updated successfully');
        setProfile(result.data);

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
          <p className="text-muted-foreground">Loading profile...</p>
        </div>
      </div>
    );
  }

  const getInitials = (name: string) =>
    name
      .split(' ')
      .map((n) => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Personal Profile"
        description="Manage your personal information and preferences."
      />

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
        <div className="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <User className="h-5 w-5" />
                Personal Information
              </CardTitle>
              <CardDescription>Your account details and contact information.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" value={profile?.email ?? ''} disabled />
                <p className="text-xs text-muted-foreground">Email cannot be changed.</p>
              </div>

              <Separator />

              <div className="space-y-2">
                <Label htmlFor="name">Full Name *</Label>
                <Input
                  id="name"
                  placeholder="Your full name"
                  value={formData.name}
                  onChange={(e) => handleInputChange('name', e.target.value)}
                  aria-invalid={!!formErrors.name}
                />
                {formErrors.name && (
                  <p className="text-sm text-destructive">{formErrors.name}</p>
                )}
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Account Info</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col items-center gap-4">
              <Avatar className="h-24 w-24">
                <AvatarFallback className="text-2xl">{profile ? getInitials(profile.name) : 'U'}</AvatarFallback>
              </Avatar>

              <Separator className="w-full" />

              <div className="space-y-2 text-sm w-full">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Status:</span>
                  <span>{profile?.status ?? 'active'}</span>
                </div>
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

          <Button className="w-full gap-2" size="lg" onClick={handleSave} disabled={saving}>
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
        </div>
      </div>
    </div>
  );
}