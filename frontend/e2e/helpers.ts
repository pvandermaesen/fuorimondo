import { request } from '@playwright/test';

const API = process.env.VITE_API_URL ? `${process.env.VITE_API_URL}/api` : 'http://localhost:8081/api';

export async function resetDb() {
  // Dev admin is seeded at app startup; we just ensure no leftover users of our test emails.
  // We do this via admin login + delete but our admin CRUD doesn't expose DELETE.
  // For now the E2E tests use unique timestamps to avoid collisions.
}

export function uniqueEmail(prefix: string): string {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@fm-e2e.test`;
}

export async function apiAdminLogin() {
  const ctx = await request.newContext();
  const r = await ctx.get(`${API}/legal/cgu`);
  const csrf = (r.headersArray().find(h => h.name.toLowerCase() === 'set-cookie')?.value.match(/XSRF-TOKEN=([^;]+)/) || [])[1];
  await ctx.post(`${API}/auth/login`, {
    headers: { 'Content-Type': 'application/json', ...(csrf ? { 'X-XSRF-TOKEN': decodeURIComponent(csrf) } : {}) },
    data: { email: 'admin@fuorimondo.local', password: 'Admin!Password123' },
  });
  return ctx;
}
