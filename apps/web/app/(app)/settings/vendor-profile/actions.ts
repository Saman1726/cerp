'use server';

import { cookies } from 'next/headers';

const DEFAULT_VENDOR_TENANT_ID = '00000000-0000-0000-0000-000000000001';

/**
 * vendor-service resolves "my profile" by X-User-Id ("internal_user_id" claim - the token's
 * own "sub" is Keycloak's internal id, not identity.user_accounts.id - see KeycloakBootstrap
 * in identity-service). X-Tenant-Id is still sent since Vendor.tenantId is NOT NULL and
 * vendor-service defaults new profiles to the shared tenant when the JWT carries no tenant
 * claim (TODO: remove the default-tenant fallback once identity-service issues one).
 */
function resolveTenantAndUserHeaders(token: string): { tenantId: string; userId?: string } {
  let tenantId: string | undefined;
  let userId: string | undefined;
  try {
    const parts = token.split('.');
    if (parts.length === 3 && parts[1]) {
      const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString('utf-8'));
      tenantId = payload.tenantId || payload.tenant_id || payload.tid;
      userId = payload.internal_user_id;
    }
  } catch {
    // fall through to defaults
  }
  return { tenantId: tenantId || process.env.VENDOR_TENANT_ID || DEFAULT_VENDOR_TENANT_ID, userId };
}

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
 * Fetch the current vendor's profile from Vendor Service.
 */
export async function getVendorProfile(): Promise<ApiResponse<VendorProfileData>> {
  const startTime = Date.now();
  console.log('[VendorProfile] === getVendorProfile() called ===');
  console.log('[VendorProfile] Current timestamp:', new Date().toISOString());
  console.log('[VendorProfile] VENDOR_SERVICE_URL env:', process.env.VENDOR_SERVICE_URL || '(not set, using default)');

  try {
    console.log('[VendorProfile] Step 1: Reading cookie store...');
    const cookieStore = await cookies();
    console.log('[VendorProfile] Step 2: Cookie store obtained');

    // Read all cookies for debugging
    const allCookies = await cookieStore.getAll();
    console.log('[VendorProfile] All cookies:', allCookies.map(c => ({ name: c.name, value: c.value ? c.value.substring(0, 30) + '...' : 'empty' })).join('; '));

    const token = cookieStore.get('cerp_session')?.value;
    console.log('[VendorProfile] Step 3: cerp_session token present:', !!token);
    if (token) {
      console.log('[VendorProfile] Step 3a: Token length:', token.length);
      // Decode JWT payload for debugging (format: header.payload.signature)
      const parts = token.split('.');
      if (parts.length === 3 && parts[1]) {
        try {
          const payload = JSON.parse(Buffer.from(parts[1], 'base64').toString('utf-8'));
          console.log('[VendorProfile] Step 3b: JWT payload:', JSON.stringify(payload, null, 2));
        } catch (e) {
          console.log('[VendorProfile] Step 3b: Could not decode JWT payload:', e);
        }
      }
    } else {
      // Also check for refresh cookie
      const refreshToken = cookieStore.get('cerp_refresh')?.value;
      console.log('[VendorProfile] Step 3c: No cerp_session! cerp_refresh present:', !!refreshToken);
    }

    if (!token) {
      console.log('[VendorProfile] Step 4: NO TOKEN - returning Unauthorized');
      return { success: false, error: 'Unauthorized' };
    }

    // Build the URL with debugging
    const baseUrl = process.env.VENDOR_SERVICE_URL || 'http://localhost:8085';
    const targetUrl = `${baseUrl}/vendors/profile`;
    console.log('[VendorProfile] Step 4: Full target URL:', targetUrl);

    // Prepare headers
    const requestHeaders: Record<string, string> = {
      Authorization: `Bearer ${token.substring(0, 20)}...[token masked]...`,
      'Content-Type': 'application/json',
    };
    console.log('[VendorProfile] Step 5: Request headers (masked):', JSON.stringify(requestHeaders));

    console.log('[VendorProfile] Step 6: Making fetch() request...');
    const { tenantId, userId } = resolveTenantAndUserHeaders(token);
    const fetchHeaders: Record<string, string> = {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'X-Tenant-Id': tenantId,
    };
    if (userId) {
      fetchHeaders['X-User-Id'] = userId;
    }
    console.log('[VendorProfile] Step 5c: Added X-Tenant-Id/X-User-Id headers:', tenantId, userId);

    // Call Vendor Service directly on port 8085 - use POST endpoint
    const response = await fetch(targetUrl, {
      method: 'POST',
      headers: fetchHeaders,
      cache: 'no-store',
    });
    console.log('[VendorProfile] Step 7: fetch() completed');
    console.log('[VendorProfile] Step 7a: Response status:', response.status);
    console.log('[VendorProfile] Step 7b: Response status text:', response.statusText);
    console.log('[VendorProfile] Step 7c: Response headers:', Object.fromEntries(response.headers.entries()));
    console.log('[VendorProfile] Step 7d: Response ok:', response.ok);
    console.log('[VendorProfile] Step 7e: Response redirected:', response.redirected);

    if (!response.ok) {
      console.log('[VendorProfile] Step 8: Response NOT OK! Status:', response.status);
      // Try to get error body
      try {
        const errorBody = await response.text();
        console.log('[VendorProfile] Step 8a: Error response body:', errorBody);
      } catch (e) {
        console.log('[VendorProfile] Step 8a: Could not read error body:', e);
      }
      return { success: false, error: `Failed to fetch Vendor profile: ${response.status}` };
    }

    console.log('[VendorProfile] Step 9: Response OK! Parsing JSON...');
    const responseText = await response.text();
    console.log('[VendorProfile] Step 9a: Raw response text length:', responseText.length);
    console.log('[VendorProfile] Step 9b: Raw response text (first 500 chars):', responseText.substring(0, 500));

    const data = JSON.parse(responseText);
    console.log('[VendorProfile] Step 10: Parsed JSON data');
    console.log('[VendorProfile] Step 10a: Data keys:', Object.keys(data));
    console.log('[VendorProfile] Step 10b: Data success:', data.success);
    console.log('[VendorProfile] Step 10c: Data error:', data.error);
    if (data.data) {
      console.log('[VendorProfile] Step 10d: Data.data keys:', Object.keys(data.data));
      console.log('[VendorProfile] Step 10e: Data.data.categories count:', data.data.categories?.length || 0);
    }

    const elapsed = Date.now() - startTime;
    console.log('[VendorProfile] === getVendorProfile() completed in', elapsed, 'ms ===');
    console.log('[VendorProfile] Returning:', JSON.stringify({ success: true, data: data }));
    return { success: true, data };
  } catch (error) {
    const elapsed = Date.now() - startTime;
    console.error('[VendorProfile] === EXCEPTION in getVendorProfile() after', elapsed, 'ms ===');
    console.error('[VendorProfile] Error type:', error instanceof Error ? error.constructor.name : typeof error);
    console.error('[VendorProfile] Error message:', error instanceof Error ? error.message : String(error));
    console.error('[VendorProfile] Error stack:', error instanceof Error ? error.stack : 'N/A');
    return { success: false, error: 'Failed to fetch vendor profile' };
  }
}

/**
 * Fetch all available categories from Vendor Service.
 */
export async function getCategories(): Promise<ApiResponse<CategoryData[]>> {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get('cerp_session')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    // Call Vendor Service directly on port 8085
    const response = await fetch(
      `${process.env.VENDOR_SERVICE_URL || 'http://localhost:8085'}/vendors/categories`,
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
 * Update the current vendor's profile.
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
    const token = cookieStore.get('cerp_session')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    const { tenantId, userId } = resolveTenantAndUserHeaders(token);
    const fetchHeaders: Record<string, string> = {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'X-Tenant-Id': tenantId,
    };
    if (userId) {
      fetchHeaders['X-User-Id'] = userId;
    }

    // Call Vendor Service directly on port 8085
    const response = await fetch(
      `${process.env.VENDOR_SERVICE_URL || 'http://localhost:8085'}/vendors/profile`,
      {
        method: 'PATCH',
        headers: fetchHeaders,
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