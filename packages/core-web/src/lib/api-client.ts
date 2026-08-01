import { API_URL, PROJECT_SERVICE_URL, WORKFLOW_SERVICE_URL } from './env';
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

// Detect whether a path belongs to the workflow service so we can route
// to the correct backend (workflow-service-java on port 8089).
// Workflow service uses /api/v1/ prefix for backend endpoints
// Also matches /api/workflows/ and /api/sagas/ prefixes for Next.js API routes
function isWorkflowServicePath(path: string): boolean {
  return path.startsWith('/api/sagas') ||
         path.startsWith('/api/v1/workflow') ||
         path.startsWith('/api/v1/organisations') ||
         path.startsWith('/api/v1/template-phase-mappings') ||
         path.startsWith('/api/workflows/');
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

// Paths that have Next.js API route proxies (server-side proxying).
// These use relative URLs so Next.js can intercept and proxy to the backend.
function hasApiRouteProxy(path: string): boolean {
  return path.startsWith('/api/sagas') || path.startsWith('/api/workflows') || path.startsWith('/api/projects') || path.startsWith('/api/vendors/');
}

function baseUrlFor(path: string): string {
  // Endpoints with Next.js API route proxies use relative paths.
  // This allows the browser to fetch from /api/sagas, /api/workflows, /api/projects
  // which are server-side proxied to the respective backend services.
  if (hasApiRouteProxy(path)) {
    return '';
  }

  // Other workflow service endpoints still route directly to the workflow service
  if (isWorkflowServicePath(path)) {
    return WORKFLOW_SERVICE_URL;
  }

  // Project service endpoints route to the project service (direct Docker access)
  // Note: /api/projects paths are handled by hasApiRouteProxy above
  if (isProjectServicePath(path)) {
    return PROJECT_SERVICE_URL;
  }

  return API_URL;
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

// Helper to parse document.cookie into a key-value map
function getCookieMap(): Record<string, string> {
  const cookieMap: Record<string, string> = {};
  if (typeof document !== 'undefined') {
    document.cookie.split(';').forEach((cookie) => {
      const separatorIndex = cookie.indexOf('=');
      if (separatorIndex === -1) return;
      const cookieKey = cookie.slice(0, separatorIndex).trim();
      const cookieValue = cookie.slice(separatorIndex + 1);
      if (cookieKey) {
        cookieMap[cookieKey] = cookieValue;
      }
    });
  }
  return cookieMap;
}

// Client-compatible: uses document.cookie for browser-side cookie access.
// This allows the API client to be used in Client Components.
async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const cookieMap = getCookieMap();
  const headersInit = new Headers(init?.headers);
  headersInit.set('Content-Type', 'application/json');

  // Decode cookie values (document.cookie stores URL-encoded values)
  const decodeValue = (value: string): string => {
    try {
      return decodeURIComponent(value);
    } catch {
      return value;
    };
  };

  const rawToken = cookieMap[SESSION_COOKIE];
  const token = rawToken ? decodeValue(rawToken) : undefined;
  if (token) headersInit.set('Authorization', `Bearer ${token}`);

  const isProjectService = isProjectServicePath(path);
  let tenantId: string | undefined;

  // Project service requires X-Tenant-Id header - fall back to cookie value if available, then default.
  if (isProjectService) {
    const rawTenantId = cookieMap['x-tenant-id'];
    tenantId = rawTenantId ? decodeValue(rawTenantId) : undefined;
    if (!tenantId) {
      tenantId = 'default';
    }
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
