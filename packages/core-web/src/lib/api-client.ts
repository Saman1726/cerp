import { cookies } from 'next/headers';
import { API_URL } from './env';
import { SESSION_COOKIE, TENANT_COOKIE } from './session-cookies';

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

// Server-only: relies on next/headers, so this can only be called from
// Server Components, Route Handlers, or Server Actions - by design, all
// authenticated data-fetching for this scaffold stays server-side.
async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const store = await cookies();
  const headers = new Headers(init?.headers);
  headers.set('Content-Type', 'application/json');

  const token = store.get(SESSION_COOKIE)?.value;
  const tenant = store.get(TENANT_COOKIE)?.value;
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (tenant) headers.set('X-Tenant-Id', tenant);

  const res = await fetch(`${API_URL}${path}`, { ...init, headers, cache: 'no-store' });

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
  patch: <TBody, T>(path: string, body: TBody) =>
    apiFetch<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path: string) => apiFetch<void>(path, { method: 'DELETE' }),
};
