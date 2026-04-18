import type { ApiError } from './types';

const BASE = '/api';

export class ApiException extends Error {
  status: number;
  payload: ApiError | null;
  constructor(status: number, payload: ApiError | null, message: string) {
    super(message);
    this.status = status;
    this.payload = payload;
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(^|;\\s*)' + name + '=([^;]+)'));
  return match ? decodeURIComponent(match[2]) : null;
}

async function primeCsrfCookie() {
  if (readCookie('XSRF-TOKEN')) return;
  // Spring sets the XSRF-TOKEN cookie on any GET; hit a permitAll endpoint.
  await fetch(BASE + '/legal/cgu', { credentials: 'include' }).catch(() => {});
}

async function request<T>(
  method: string,
  path: string,
  body?: unknown,
  opts: { raw?: boolean } = {}
): Promise<T> {
  const isMutation = method !== 'GET' && method !== 'HEAD';
  if (isMutation) await primeCsrfCookie();

  const headers: Record<string, string> = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (isMutation) {
    const csrf = readCookie('XSRF-TOKEN');
    if (csrf) headers['X-XSRF-TOKEN'] = csrf;
  }

  const res = await fetch(BASE + path, {
    method,
    credentials: 'include',
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (res.status === 204) return undefined as T;
  if (!res.ok) {
    let payload: ApiError | null = null;
    try { payload = await res.json(); } catch { /* noop */ }
    throw new ApiException(res.status, payload, payload?.message || res.statusText);
  }
  if (opts.raw) return (await res.text()) as unknown as T;
  return (await res.json()) as T;
}

export const api = {
  get:    <T>(p: string) => request<T>('GET', p),
  post:   <T>(p: string, body?: unknown) => request<T>('POST', p, body),
  patch:  <T>(p: string, body?: unknown) => request<T>('PATCH', p, body),
  put:    <T>(p: string, body?: unknown) => request<T>('PUT', p, body),
  delete: <T>(p: string) => request<T>('DELETE', p),
};
