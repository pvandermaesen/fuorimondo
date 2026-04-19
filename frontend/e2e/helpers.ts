import { request, APIRequestContext, Page } from '@playwright/test';

const API_BASE = process.env.VITE_API_URL ? `${process.env.VITE_API_URL}/api` : 'http://localhost:8080/api';

export async function resetDb() {
  // Dev admin is seeded at app startup; we just ensure no leftover users of our test emails.
  // We do this via admin login + delete but our admin CRUD doesn't expose DELETE.
  // For now the E2E tests use unique timestamps to avoid collisions.
}

export function uniqueEmail(prefix: string): string {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@fm-e2e.test`;
}

/**
 * Reads the XSRF-TOKEN from the Set-Cookie headers of a response.
 * Returns undefined if not present.
 */
function extractXsrfFromSetCookie(headers: Array<{ name: string; value: string }>): string | undefined {
  for (const h of headers) {
    if (h.name.toLowerCase() === 'set-cookie') {
      const m = h.value.match(/XSRF-TOKEN=([^;]+)/);
      if (m) return decodeURIComponent(m[1]);
    }
  }
  return undefined;
}

/**
 * Creates an admin-authenticated APIRequestContext.
 * The context has the session cookie + CSRF token stored.
 * Use getCsrf(ctx) to read the CSRF value for mutation requests.
 */
export async function apiAdminLogin(): Promise<{ ctx: APIRequestContext; csrf: string }> {
  const ctx = await request.newContext();

  // Prime CSRF cookie — use full URL since no baseURL set
  const r1 = await ctx.get(`${API_BASE}/legal/cgu`);
  let csrf = extractXsrfFromSetCookie(r1.headersArray());

  // Login
  const loginRes = await ctx.post(`${API_BASE}/auth/login`, {
    headers: {
      'Content-Type': 'application/json',
      ...(csrf ? { 'X-XSRF-TOKEN': csrf } : {}),
    },
    data: { email: 'admin@fuorimondo.local', password: 'Admin!Password123' },
  });
  if (!loginRes.ok() && loginRes.status() !== 204) {
    throw new Error(`apiAdminLogin failed: ${loginRes.status()} — ${await loginRes.text()}`);
  }

  // After login Spring may rotate the CSRF token — re-fetch it
  const r2 = await ctx.get(`${API_BASE}/legal/cgu`);
  const newCsrf = extractXsrfFromSetCookie(r2.headersArray());
  if (newCsrf) csrf = newCsrf;

  if (!csrf) throw new Error('apiAdminLogin: could not obtain XSRF token');
  return { ctx, csrf };
}

/**
 * Creates a product via the admin REST API (JSON body, no photo).
 * Requires an authenticated { ctx, csrf } from apiAdminLogin().
 */
export async function createProduct(
  auth: { ctx: APIRequestContext; csrf: string },
  data: {
    name: string;
    priceEur: string;
    tiers: string[];
    delivery: boolean;
    saleStartAt: string;
    saleEndAt?: string | null;
    stock?: number | null;
    description?: string | null;
  }
) {
  const payload = {
    name: data.name,
    description: data.description ?? null,
    priceEur: data.priceEur,
    delivery: data.delivery,
    weightKg: null,
    tiers: data.tiers,
    saleStartAt: data.saleStartAt,
    saleEndAt: data.saleEndAt ?? null,
    stock: data.stock ?? null,
  };

  const res = await auth.ctx.post(`${API_BASE}/admin/products`, {
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': auth.csrf,
    },
    data: payload,
  });
  if (!res.ok()) {
    throw new Error(`createProduct failed: ${res.status()} — ${await res.text()}`);
  }
  return await res.json();
}

/**
 * Creates an allocataire via the admin API and returns { user, code }.
 * Requires an authenticated { ctx, csrf } from apiAdminLogin().
 */
export async function createAllocataireApi(
  auth: { ctx: APIRequestContext; csrf: string },
  opts: {
    email: string;
    tierCode: 'TIER_1' | 'TIER_2' | 'TIER_3';
  }
) {
  const res = await auth.ctx.post(`${API_BASE}/admin/users`, {
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': auth.csrf,
    },
    data: {
      email: opts.email,
      firstName: 'Allo',
      lastName: 'Cat',
      civility: 'NONE',
      birthDate: null,
      phone: '',
      country: 'FR',
      city: 'Paris',
      tierCode: opts.tierCode,
      locale: 'FR',
      adminNotes: '',
    },
  });
  if (!res.ok()) {
    throw new Error(`createAllocataireApi failed: ${res.status()} — ${await res.text()}`);
  }
  return await res.json() as { user: { id: string; email: string }; code: string };
}

/**
 * Creates a shipping address for the given user via the REST API.
 * Handles its own login session internally.
 */
export async function createShippingAddressApi(userEmail: string, userPassword: string) {
  const userCtx = await request.newContext();

  // prime CSRF
  const r1 = await userCtx.get(`${API_BASE}/legal/cgu`);
  let csrf = extractXsrfFromSetCookie(r1.headersArray());

  // login
  await userCtx.post(`${API_BASE}/auth/login`, {
    headers: {
      'Content-Type': 'application/json',
      ...(csrf ? { 'X-XSRF-TOKEN': csrf } : {}),
    },
    data: { email: userEmail, password: userPassword },
  });

  // re-fetch CSRF after login
  const r2 = await userCtx.get(`${API_BASE}/legal/cgu`);
  const newCsrf = extractXsrfFromSetCookie(r2.headersArray());
  if (newCsrf) csrf = newCsrf;

  if (!csrf) throw new Error('createShippingAddressApi: could not obtain XSRF token');

  const res = await userCtx.post(`${API_BASE}/me/addresses`, {
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': csrf,
    },
    data: {
      type: 'SHIPPING',
      fullName: 'Allo Cat',
      street: '1 Rue de la Paix',
      streetExtra: '',
      postalCode: '75001',
      city: 'Paris',
      country: 'France',
      isDefault: true,
    },
  });
  if (!res.ok()) {
    await userCtx.dispose();
    throw new Error(`createShippingAddressApi failed: ${res.status()} — ${await res.text()}`);
  }
  const body = await res.json();
  await userCtx.dispose();
  return body;
}

/** Logs in to the UI as admin and waits for /profile redirect. */
export async function loginAsAdmin(page: Page) {
  await page.goto('/login');
  await page.getByLabel(/email/i).fill('admin@fuorimondo.local');
  await page.getByLabel(/mot de passe/i).fill('Admin!Password123');
  await page.getByTestId('login-submit').click();
  await page.waitForURL(/\/profile/, { timeout: 10_000 });
}

/** Logs in to the UI as a given user and waits for /profile redirect. */
export async function loginAs(page: Page, email: string, password: string) {
  await page.goto('/login');
  await page.getByLabel(/email/i).fill(email);
  await page.getByLabel(/mot de passe/i).fill(password);
  await page.getByTestId('login-submit').click();
  await page.waitForURL(/\/profile/, { timeout: 10_000 });
}

/** Logs out via the burger menu logout button. */
export async function logout(page: Page) {
  await page.getByTestId('burger').click();
  await page.getByTestId('logout').click();
  await page.waitForURL(/\/(login|\?|$)/, { timeout: 5_000 });
}

/**
 * Activates an allocataire account through the UI: enters the code, sets a password,
 * then clicks the "go to profile" button. Lands on /profile.
 */
export async function activateAllocataireUi(page: Page, email: string, code: string, password = 'AlloPass1!') {
  await page.goto('/activate');
  await page.getByTestId('act-email').fill(email);
  await page.getByTestId('act-code').fill(code);
  await page.getByTestId('act-verify').click();
  // Step 2: set password
  await page.getByTestId('act-password').fill(password);
  await page.getByTestId('act-submit').click();
  // Step 3: confirmation → go to profile
  await page.getByTestId('act-goto-profile').click();
  await page.waitForURL(/\/profile/, { timeout: 10_000 });
}

/**
 * Creates a TIER_1 allocataire via admin UI, returns { email, code }.
 * Admin must already be logged in on `page`.
 */
export async function createAllocataireUi(
  page: Page,
  opts: { tierCode?: 'TIER_1' | 'TIER_2' | 'TIER_3' } = {}
): Promise<{ email: string; code: string }> {
  const email = uniqueEmail('allo');
  const tier = opts.tierCode ?? 'TIER_1';

  await page.goto('/admin/users/create');
  await page.getByTestId('new-email').fill(email);
  await page.getByTestId('new-first').fill('Allo');
  await page.getByTestId('new-last').fill('Cat');
  await page.getByLabel(/pays/i).fill('France');
  await page.getByLabel(/ville/i).fill('Paris');

  // Select the tier from the FmSelect dropdown (label is "Tier" in admin)
  await page.getByLabel(/tier/i).selectOption(tier);

  await page.getByTestId('admin-create-submit').click();
  await page.getByTestId('generated-code').waitFor({ state: 'visible', timeout: 5_000 });
  const code = (await page.getByTestId('generated-code').textContent()) ?? '';

  return { email, code };
}
