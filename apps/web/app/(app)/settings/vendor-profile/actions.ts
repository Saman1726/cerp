'use server';

import { cookies } from 'next/headers';

// API types
interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: string;
}

// Vendor profile response from backend
export interface VendorProfileData {
  id: string;
  vendorId: string;
  legalName: string;
  companyName?: string;
  contactPerson?: string;
  contactEmail?: string;
  contactPhone?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  taxProfile?: Record<string, unknown>;
  categories: Array<{
    categoryId: string;
    categoryName: string;
    categoryCode: string;
  }>;
  status: 'pending' | 'approved' | 'rejected';
  ratingAvg?: number;
  avatarUrl?: string;
  createdAt: string;
  updatedAt?: string;
}

// Category from backend
export interface CategoryData {
  id: string;
  code: string;
  name: string;
  description: string | null;
  icon: string | null;
  displayOrder: number;
  groupId: string | null;
  groupName: string | null;
}

// Category group from backend
export interface CategoryGroupData {
  id: string;
  name: string;
  description: string | null;
  displayOrder: number;
  isActive: boolean;
}

/**
 * Fetch the current user's vendor profile.
 */
export async function getVendorProfile(): Promise<ApiResponse<VendorProfileData>> {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get('session_token')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    const response = await fetch(
      `${process.env.CERP_API_URL || 'http://localhost:8080'}/api/vendors/profile`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        cache: 'no-store',
      }
    );

    if (!response.ok) {
      return { success: false, error: `Failed to fetch profile: ${response.status}` };
    }

    const data = await response.json();
    return { success: true, data };
  } catch {
    return { success: false, error: 'Failed to fetch vendor profile' };
  }
}

/**
 * Fetch all available categories grouped by category group.
 */
export async function getCategories(): Promise<ApiResponse<CategoryData[]>> {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get('session_token')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    const response = await fetch(
      `${process.env.CERP_API_URL || 'http://localhost:8080'}/api/vendors/categories`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        cache: 'no-store',
      }
    );

    if (!response.ok) {
      return { success: false, error: `Failed to fetch categories: ${response.status}` };
    }

    const data = await response.json();
    return { success: true, data };
  } catch {
    return { success: false, error: 'Failed to fetch categories' };
  }
}

/**
 * Update the current user's vendor profile.
 */
export async function updateVendorProfile(data: {
  legalName?: string;
  companyName?: string;
  contactPerson?: string;
  contactEmail?: string;
  contactPhone?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  postalCode?: string;
  city?: string;
  countryCode?: string;
  categoryIds?: string[];
}): Promise<ApiResponse<VendorProfileData>> {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get('session_token')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    const response = await fetch(
      `${process.env.CERP_API_URL || 'http://localhost:8080'}/api/vendors/profile`,
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
        error: errorData.error || `Failed to update profile: ${response.status}`,
      };
    }

    const result = await response.json();
    return { success: true, data: result };
  } catch {
    return { success: false, error: 'Failed to update vendor profile' };
  }
}