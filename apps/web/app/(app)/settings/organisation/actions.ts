'use server';

import { cookies } from 'next/headers';

interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: string;
}

export interface CompanyProfileData {
  id: string;
  tenantId: string;
  name: string;
  legalName: string;
  description?: string;
  industry?: string;
  companySize?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  phone?: string;
  email?: string;
  taxId?: string;
  vatNumber?: string;
  registrationNumber?: string;
  avatarUrl?: string;
  createdAt: string;
  updatedAt?: string;
}

export async function getCompanyProfile(): Promise<ApiResponse<CompanyProfileData>> {
  try {
    const cookieStore = await cookies();
    // Cookie name is 'cerp_session' as defined in session-cookies.ts
    const token = cookieStore.get('cerp_session')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    const response = await fetch(
      `${process.env.CERP_API_URL || 'http://localhost:8080'}/api/organisation/profile`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        cache: 'no-store',
      }
    );

    if (!response.ok) {
      return { success: false, error: `Failed to fetch organisation profile: ${response.status}` };
    }

    const data = await response.json();
    return { success: true, data };
  } catch {
    return { success: false, error: 'Failed to fetch organisation profile' };
  }
}

export async function updateCompanyProfile(data: {
  name?: string;
  legalName?: string;
  description?: string;
  industry?: string;
  companySize?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  phone?: string;
  email?: string;
  taxId?: string;
  vatNumber?: string;
  registrationNumber?: string;
}): Promise<ApiResponse<CompanyProfileData>> {
  try {
    const cookieStore = await cookies();
    // Cookie name is 'cerp_session' as defined in session-cookies.ts
    const token = cookieStore.get('cerp_session')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    const response = await fetch(
      `${process.env.CERP_API_URL || 'http://localhost:8080'}/api/organisation/profile`,
      {
        method: 'PATCH',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(data),
        cache: 'no-store',
      }
    );

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      return {
        success: false,
        error: errorData.error || `Failed to update organisation profile: ${response.status}`,
      };
    }

    const result = await response.json();
    return { success: true, data: result };
  } catch {
    return { success: false, error: 'Failed to update organisation profile' };
  }
}