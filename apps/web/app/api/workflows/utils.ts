import { NextRequest } from 'next/server';
import { IDENTITY_SERVICE_URL } from '@cerp/core-web/lib/env';
import type { WorkflowNote, WorkflowNoteType } from '@cerp/shared-types';

const SESSION_COOKIE = 'cerp_session';

export function getAuthHeaders(request: NextRequest): HeadersInit {
  const headers: HeadersInit = { 'Content-Type': 'application/json' };

  // Forward Authorization header if present (from client-side token)
  const authHeader = request.headers.get('authorization');
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }

  // Forward cookie-based auth (session cookie pattern used in the app)
  const sessionCookie = request.cookies.get(SESSION_COOKIE)?.value;
  if (sessionCookie) {
    headers['Cookie'] = `${SESSION_COOKIE}=${sessionCookie}`;
  }

  return headers;
}

/**
 * Get headers for backend API calls, including tenant ID and auth headers.
 *
 * <p>
 * The X-Tenant-Id header is used by Java services to set the TenantContext,
 * which controls data access in multi-tenant deployments.
 * </p>
 */
export function getBackendHeaders(request: NextRequest): HeadersInit {
  const headers: HeadersInit = { 'Content-Type': 'application/json' };

  // Forward Authorization header if present
  const authHeader = request.headers.get('authorization');
  if (authHeader) {
    headers['Authorization'] = authHeader;
  }

  // Forward cookie-based auth
  const sessionCookie = request.cookies.get(SESSION_COOKIE)?.value;
  if (sessionCookie) {
    headers['Cookie'] = `${SESSION_COOKIE}=${sessionCookie}`;
  }

  // Forward tenant ID from URL query param or header
  const searchParams = request.nextUrl.searchParams;
  const tenantId = searchParams.get('tenantId') || request.headers.get('x-tenant-id');
  if (tenantId) {
    headers['X-Tenant-Id'] = tenantId;
  }

  return headers;
}

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const DEFAULT_TENANT_ID = '00000000-0000-0000-0000-000000000001';

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3 || !parts[1]) return null;
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(Buffer.from(base64, 'base64').toString('utf-8')) as Record<string, unknown>;
  } catch {
    return null;
  }
}

export async function resolveUserIdByEmail(email: string): Promise<string | null> {
  try {
    const res = await fetch(
      `${IDENTITY_SERVICE_URL}/users?email=${encodeURIComponent(email)}`,
      { cache: 'no-store' },
    );
    if (!res.ok) return null;
    const data = (await res.json()) as Record<string, unknown>;
    return typeof data.id === 'string' ? data.id : null;
  } catch {
    return null;
  }
}

export async function resolveUserById(userId: string): Promise<{ id: string; email: string; name: string } | null> {
  try {
    const res = await fetch(
      `${IDENTITY_SERVICE_URL}/users/${encodeURIComponent(userId)}`,
      { cache: 'no-store' },
    );
    if (!res.ok) return null;
    const data = await res.json() as Record<string, unknown>;
    return {
      id: String(data.id ?? userId),
      email: String(data.email ?? ''),
      name: String(data.name ?? ''),
    };
  } catch {
    return null;
  }
}

export async function resolveUserLookup(
  userIds: string[],
): Promise<Record<string, { id: string; email: string; name: string }>> {
  const uniqueIds = [...new Set(userIds.filter(Boolean))];
  const results = await Promise.allSettled(
    uniqueIds.map(id => resolveUserById(id)),
  );
  const lookup: Record<string, { id: string; email: string; name: string }> = {};
  results.forEach((result, i) => {
    if (result.status === 'fulfilled' && result.value) {
      const id = uniqueIds[i];
      if (id) {
        lookup[id] = result.value;
      }
    }
  });
  return lookup;
}

/**
 * Extract the authenticated user's UUID, tenant ID, and roles from the session cookie.
 *
 * Priority:
 * 1. Dedicated `x-user-id` / `x-tenant-id` cookies (set at login in new sessions).
 * 2. `cerp_session` JWT — when `sub` is already a UUID it is used directly;
 *    when `sub` is an email (legacy dev token) the identity service is queried
 *    to obtain the real user UUID.
 */
export async function extractSessionUser(): Promise<{
  userId: string | null;
  tenantId: string;
  roles: string[];
}> {
  const { cookies } = await import('next/headers');
  const cookieStore = await cookies();

  const userIdCookie = cookieStore.get('x-user-id')?.value ?? null;
  const tenantIdCookie = cookieStore.get('x-tenant-id')?.value ?? null;

  // Priority 1: dedicated cookies — only use if both are present and userId is a UUID.
  if (userIdCookie && tenantIdCookie && UUID_REGEX.test(userIdCookie)) {
    return { userId: userIdCookie, tenantId: tenantIdCookie, roles: [] };
  }

  // Priority 2: JWT session cookie.
  const token = cookieStore.get('cerp_session')?.value;
  if (!token) return { userId: null, tenantId: tenantIdCookie ?? DEFAULT_TENANT_ID, roles: [] };

  const payload = decodeJwtPayload(token);
  if (!payload) return { userId: null, tenantId: tenantIdCookie ?? DEFAULT_TENANT_ID, roles: [] };

  let tenantId = tenantIdCookie ?? DEFAULT_TENANT_ID;
  if (typeof payload.tenantId === 'string' && payload.tenantId.trim()) {
    tenantId = payload.tenantId;
  }

  // Extract roles from JWT (comma-separated string or JSON array).
  let roles: string[] = [];
  if (typeof payload.roles === 'string' && payload.roles.trim()) {
    roles = payload.roles
      .split(',')
      .map((r) => r.trim())
      .filter(Boolean);
  } else if (Array.isArray(payload.roles)) {
    roles = (payload.roles as unknown[]).filter((r): r is string => typeof r === 'string');
  }

  // "internal_user_id" must be checked before "sub": Keycloak-issued tokens carry
  // Keycloak's own internal id as "sub", which matches no row in identity-service's
  // user_accounts (see RequestIdentity.creatorId on the backend) - "internal_user_id"
  // is the claim identity-service mints for exactly this purpose.
  const internalUserId =
    typeof payload.internal_user_id === 'string' ? payload.internal_user_id : null;
  if (internalUserId) return { userId: internalUserId, tenantId, roles };

  const sub = typeof payload.sub === 'string' ? payload.sub : null;
  if (!sub) return { userId: null, tenantId, roles };

  // New token: sub is already a UUID.
  if (UUID_REGEX.test(sub)) return { userId: sub, tenantId, roles };

  // Legacy token: sub is an email — resolve to UUID via identity service.
  const resolved = await resolveUserIdByEmail(sub);
  return { userId: resolved, tenantId, roles };
}

/** Raw shape workflow-service actually returns for a WorkflowNote (`workflow` is @JsonIgnore'd; `stepId` is a derived getter, not the full step entity). */
interface BackendWorkflowNote {
  id: string;
  stepId: string | null;
  authorId: string;
  note: string;
  type: string;
  createdAt: string;
}

/**
 * Translates workflow-service's WorkflowNote JSON into the frontend-facing shape. The backend
 * never echoes `workflowId` back (it's @JsonIgnore'd on the entity, and it's always the id
 * already in the route's own path, so callers supply it directly).
 */
export function toFrontendNote(raw: BackendWorkflowNote, workflowId: string): WorkflowNote {
  return {
    id: raw.id,
    workflowId,
    stepId: raw.stepId,
    content: raw.note,
    type: raw.type as WorkflowNoteType,
    createdBy: raw.authorId,
    createdAt: raw.createdAt,
  };
}
