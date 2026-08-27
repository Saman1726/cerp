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
  return path.startsWith('/api/sagas') ||
         path.startsWith('/api/workflows') ||
         path.startsWith('/api/projects') ||
         path.startsWith('/api/vendors/') ||
         path.startsWith('/api/construction-sites');
}

// Whether a path should go through the Next.js API proxy instead of direct
// backend access. Some paths (e.g. /construction-sites) only need proxying
// from the browser side; the server calls the backend directly.
function shouldUseApiProxy(path: string, isServer: boolean): boolean {
  // For construction-sites, only use the Next.js API proxy from the browser.
  if (path.startsWith('/construction-sites')) {
    return !isServer;
  }
  // For already-proxied API paths, always use the proxy.
  return hasApiRouteProxy(path);
}

function baseUrlFor(path: string, opts: { isServer: boolean }): string {
  // Endpoints with Next.js API route proxies use relative paths.
  // This allows the browser to fetch from /api/sagas, /api/workflows, /api/projects,
  // /api/construction-sites — which are server-side proxied to the respective
  // backend services.
  if (shouldUseApiProxy(path, opts.isServer)) {
    return '';
  }

  // Other workflow service endpoints still route directly to the workflow service
  if (isWorkflowServicePath(path)) {
    return WORKFLOW_SERVICE_URL;
  }

  // Project service endpoints route to the project service (direct Docker access)
  // Note: /api/projects and /api/construction-sites paths are handled above
  if (isProjectServicePath(path)) {
    return PROJECT_SERVICE_URL;
  }

  return API_URL;
}

// Get the normalized path for the actual HTTP request
function normalizedPathFor(path: string, opts: { isServer: boolean }): string {
  // If path is already an API proxy route, keep as-is
  if (hasApiRouteProxy(path)) {
    return path;
  }

  // Handle construction-sites specially
  if (path.startsWith('/construction-sites')) {
    if (!opts.isServer) {
      // Browser calls the Next.js API route at /api/construction-sites
      return `/api${path}`;
    }
    // Server calls backend directly; ensure /api/v1 prefix
    return path.startsWith('/api/v1') ? path : `/api/v1${path}`;
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

// Reads the session cookie server-side, via next/headers - not document.cookie,
// which doesn't exist in Server Components/Actions at all, and couldn't see this
// cookie in the browser either since it's httpOnly (deliberately, to block XSS
// token theft). The dynamic import keeps a static `next/headers` import out of
// this file, which is also bundled into a few genuine Client Components (e.g.
// ProjectSelect) that call already-proxied /api/... paths and never hit this
// branch - see api-client.ts's git history for the 401 this was fixing.
async function getServerCookieMap(): Promise<Record<string, string>> {
  const { cookies } = await import('next/headers');
  const store = await cookies();
  const map: Record<string, string> = {};
  for (const { name, value } of store.getAll()) {
    map[name] = value;
  }
  return map;
}

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const isServer = typeof window === 'undefined';
  const cookieMap = isServer ? await getServerCookieMap() : getCookieMap();
  const headersInit = new Headers(init?.headers);
  headersInit.set('Content-Type', 'application/json');

  // Decode cookie values (document.cookie stores URL-encoded values; next/headers
  // already returns decoded values, but re-decoding a JWT/UUID is a no-op since
  // neither ever contains a '%' sequence for decodeURIComponent to act on).
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

  // Project service requires X-Tenant-Id header - fall back to cookie value if available,
  // then the shared default tenant (every registration lands there; see register-*/route.ts).
  if (isProjectService) {
    const rawTenantId = cookieMap['x-tenant-id'];
    tenantId = rawTenantId ? decodeValue(rawTenantId) : undefined;
    if (!tenantId) {
      tenantId = '00000000-0000-0000-0000-000000000001';
    }
    headersInit.set('X-Tenant-Id', tenantId);
  }

  const base = baseUrlFor(path, { isServer });
  const effectivePath = normalizedPathFor(path, { isServer });
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
