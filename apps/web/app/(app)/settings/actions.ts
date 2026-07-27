'use server';

import { cookies } from 'next/headers';

interface UserRoleResponse {
  success: boolean;
  role?: string;
  error?: string;
}

/**
 * Decode a JWT token payload and extract the user role.
 * JWT format: header.payload.signature (base64url-encoded JSON)
 */
function decodeJwtRole(token: string): string | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;

    // Decode the payload (second part)
    const payload = parts[1] ?? '';
    // Replace JWT-specific chars with base64-compatible chars
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );

    const parsed = JSON.parse(jsonPayload);

    // The JWT contains the user object with role
    // Check common locations: user.role, sub (if it's a user object), or top-level role
    if (parsed && typeof parsed === 'object') {
      if (parsed.user && typeof parsed.user === 'object' && 'role' in parsed.user) {
        return (parsed.user as { role: string }).role;
      }
      if ('role' in parsed) {
        return (parsed as { role: string }).role;
      }
    }
  } catch {
    // If decoding fails, return null
  }
  return null;
}

export async function getUserRole(): Promise<UserRoleResponse> {
  try {
    const cookieStore = await cookies();
    // Cookie name is 'cerp_session' as defined in session-cookies.ts
    const token = cookieStore.get('cerp_session')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    const role = decodeJwtRole(token);

    if (!role) {
      return { success: false, error: 'Could not extract role from token' };
    }

    return { success: true, role };
  } catch {
    return { success: false, error: 'Failed to get user role' };
  }
}