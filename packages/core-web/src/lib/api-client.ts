import { cookies, headers } from 'next/headers';
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
  const requestHeaders = await headers();
  const headersInit = new Headers(init?.headers);
  headersInit.set('Content-Type', 'application/json');

  const token = store.get(SESSION_COOKIE)?.value;
  if (token) headersInit.set('Authorization', `Bearer ${token}`);

  const isProjectService = isProjectServicePath(path);
  let tenantId: string | undefined;

  // Project service requires X-Tenant-Id header - read from incoming request
  // headers first, fall back to cookie value if available, then default.
  if (isProjectService) {
    tenantId =
      requestHeaders.get('X-Tenant-Id') ||
      store.get('x-tenant-id')?.value ||
      'default';
    headersInit.set('X-Tenant-Id', tenantId);
  }

  const base = baseUrlFor(path);
  const effectivePath = normalizedPathFor(path);
  const fullUrl = `${base}${effectivePath}`;

  console.log('[apiClient] REQUEST:', {
    path,
    base,
    effectivePath,
    fullUrl,
    isProjectService,
    tenantId,
    hasAuthorization: !!token,
  });

  const res = await fetch(fullUrl, { ...init, headers: headersInit, cache: 'no-store' });

  const responseHeaders: Record<string, string> = {};
  res.headers.forEach((value, key) => {
    responseHeaders[key] = value;
  });

  console.log('[apiClient] RESPONSE:', {
    fullUrl,
    status: res.status,
    statusText: res.statusText,
    headers: responseHeaders,
  });

  if (!res.ok) {
    const body = await res.json().catch(() => undefined);
    console.error('[apiClient] API ERROR:', { fullUrl, status: res.status, body });
    throw new ApiClientError(res.status, body?.message ?? res.statusText, body);
  }

  const data = res.status === HTTP_NO_CONTENT ? (undefined as T) : await res.json();
  console.log('[apiClient] SUCCESS:', { fullUrl, hasData: !!data });
  return data;
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
