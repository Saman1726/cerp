'use server';

import { cookies } from 'next/headers';

interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: string;
}

export interface UserProfileData {
  id: string;
  email: string;
  name: string;
  status?: string;
  createdAt: string;
  updatedAt?: string;
}

/**
 * Fetch the current user's profile from Identity Service.
 */
export async function getUserProfile(): Promise<ApiResponse<UserProfileData>> {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get('cerp_session')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    // Call Identity Service directly on port 8090
    const response = await fetch(
      `${process.env.IDENTITY_SERVICE_URL || 'http://localhost:8090'}/users/profile`,
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
    return { success: false, error: 'Failed to fetch user profile' };
  }
}

/**
 * Update the current user's profile.
 */
export async function updateUserProfile(data: {
  name?: string;
}): Promise<ApiResponse<UserProfileData>> {
  try {
    const cookieStore = await cookies();
    const token = cookieStore.get('cerp_session')?.value;

    if (!token) {
      return { success: false, error: 'Unauthorized' };
    }

    // Call Identity Service directly on port 8090
    const response = await fetch(
      `${process.env.IDENTITY_SERVICE_URL || 'http://localhost:8090'}/users/profile`,
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
    return { success: false, error: 'Failed to update user profile' };
  }
}