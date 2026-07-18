import { cookies } from 'next/headers';
import { API_URL, PROJECT_SERVICE_URL } from './env';
import { SESSION_COOKIE } from './session-cookies';

const HTTP_NO_CONTENT = 204;

export class ApiClientError extends Error {
  constructor(
    public status: number,
    message: string,
    public body?: unknown,
  ) {
    super(message);
  }
}

// Detect whether a path belongs to the project service so we can route
// to the correct internal backend (the API gateway currently does not
// proxy project endpoints).
function isProjectServicePath(path: string): boolean {
  return (
    path.includes('/api/projects/') ||
    path.includes('/api/v1/projects') ||
    path.includes('/api/v1/construction-sites') ||
    path.startsWith('/construction-sites')
  );
}

function baseUrlFor(path: string): string {
  if (!isProjectServicePath(path)) {
    return API_URL;
  }

  return PROJECT_SERVICE_URL;
}

// Get the normalized path for the actual HTTP request
function normalizedPathFor(path: string): string {
  if (!isProjectServicePath(path)) {
    return path;
  }

  // Add /api/v1 prefix if missing for construction-sites paths
  if (path.includes('/construction-sites') && !path.startsWith('/api/v1')) {
    return `/api/v1${path}`;
  }

  return path;
}

// Server-only: relies on next/headers, so this can only be called from
// Server Components, Route Handlers, or Server Actions - by design, all
// authenticated data-fetching for this scaffold stays server-side.
async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const store = await cookies();
  const headers = new Headers(init?.headers);
  headers.set('Content-Type', 'application/json');

  const token = store.get(SESSION_COOKIE)?.value;
  if (token) headers.set('Authorization', `Bearer ${token}`);

  // Project service requires X-Tenant-Id header
  if (isProjectServicePath(path)) {
    const tenantId = store.get('x-tenant-id')?.value;
    headers.set('X-Tenant-Id', tenantId || 'default');
  }

  const base = baseUrlFor(path);
  const effectivePath = normalizedPathFor(path);
  const res = await fetch(`${base}${effectivePath}`, { ...init, headers, cache: 'no-store' });

  if (!res.ok) {
    const body = await res.json().catch(() => undefined);
    throw new ApiClientError(res.status, body?.message ?? res.statusText, body);
  }
  return res.status === HTTP_NO_CONTENT ? (undefined as T) : res.json();
}

export const apiClient = {
  get: <T>(path: string) => apiFetch<T>(path, { method: 'GET' }),
  post: <TBody, T>(path: string, body: TBody) =>
    apiFetch<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <TBody, T>(path: string, body: TBody) =>
    apiFetch<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: <TBody, T>(path: string, body: TBody) =>
    apiFetch<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path: string) => apiFetch<void>(path, { method: 'DELETE' }),
};
