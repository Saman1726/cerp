import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';

/**
 * Decode a JWT token payload and extract the user role.
 * JWT format: header.payload.signature (base64url-encoded JSON)
 */
function decodeJwtRole(token: string): string | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;

    const payload = parts[1] ?? '';
    if (!payload) return null;

    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = Buffer.from(base64, 'base64').toString('utf-8');
    const parsed = JSON.parse(jsonPayload) as Record<string, unknown>;

    if (parsed && typeof parsed === 'object') {
      if (
        parsed.user &&
        typeof parsed.user === 'object' &&
        'role' in parsed.user
      ) {
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

export default async function SettingsPage() {
  const cookieStore = await cookies();
  const token = cookieStore.get('cerp_session')?.value;

  let targetPath = '/settings/profile';

  if (token) {
    const role = decodeJwtRole(token);
    if (role === 'vendor') {
      targetPath = '/settings/vendor-profile';
    } else if (role === 'company') {
      targetPath = '/settings/organisation';
    }
  }

  redirect(targetPath);
}

export const metadata = {
  title: 'Settings',
  description: 'Organisation, team, and account settings.',
};