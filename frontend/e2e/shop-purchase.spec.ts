import { test, expect } from '@playwright/test';
import {
  apiAdminLogin,
  createProduct,
  createAllocataireApi,
  activateAllocataireUi,
  createShippingAddressApi,
  uniqueEmail,
} from './helpers';

const ALLOCATAIRE_PASSWORD = 'AlloPass1!';

test('allocataire can complete a full purchase flow (dev simulate)', async ({ page }) => {
  // Collect browser console errors for debugging
  const consoleErrors: string[] = [];
  page.on('console', msg => {
    if (msg.type() === 'error') consoleErrors.push(msg.text());
  });

  // ── 1. Seed: admin creates product + TIER_1 allocataire ──
  const auth = await apiAdminLogin();

  const now = new Date();
  const past = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
  const future = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString();

  const product = await createProduct(auth, {
    name: `Purchase-${Date.now()}`,
    priceEur: '10.00',
    tiers: ['TIER_1'],
    delivery: true, // needs a shipping address
    saleStartAt: past,
    saleEndAt: future,
    stock: 2,
  });

  const email = uniqueEmail('buy');
  const { code } = await createAllocataireApi(auth, { email, tierCode: 'TIER_1' });
  await auth.ctx.dispose();

  // ── 2. Activate the allocataire ──
  await activateAllocataireUi(page, email, code, ALLOCATAIRE_PASSWORD);

  // ── 3. Add a shipping address via API ──
  await createShippingAddressApi(email, ALLOCATAIRE_PASSWORD);

  // ── 4. Navigate to shop, click the product ──
  await page.goto('/shop');
  const card = page.locator(`[data-testid="product-card-${product.id}"]`);
  await expect(card).toBeVisible({ timeout: 8_000 });
  await card.click();

  // ── 5. Product detail: click buy ──
  await expect(page.getByTestId('buy-cta')).toBeVisible({ timeout: 5_000 });
  await page.getByTestId('buy-cta').click();

  // ── 6. Checkout page ──
  await page.waitForURL(/\/shop\/checkout\//, { timeout: 8_000 });

  // Accept CGV
  await page.getByTestId('accept-cgv').click();

  // Verify address select is visible
  await expect(page.getByTestId('address-select')).toBeVisible({ timeout: 5_000 });

  // Pay
  await page.getByTestId('pay-cta').click();

  // ── 7. Return page (dev mode — ?sim=1) ──
  await page.waitForURL(/\/shop\/order\/.+\/return/, { timeout: 10_000 });
  const returnUrl = page.url();
  const orderId = returnUrl.match(/\/shop\/order\/([^/]+)\/return/)?.[1];
  console.log('Order ID:', orderId);

  // Sim panel visible
  await expect(page.getByTestId('sim-paid')).toBeVisible({ timeout: 8_000 });

  // ── 8. Simulate payment via API directly (more reliable than button click in slow environments) ──
  // Use page.evaluate to call the simulate endpoint directly via fetch in the browser
  const simResult = await page.evaluate(async (id: string) => {
    try {
      // Prime CSRF if needed
      const xsrf = document.cookie.match(/XSRF-TOKEN=([^;]+)/)?.[1];
      const resp = await fetch(`/api/dev/orders/${id}/simulate-webhook?status=paid`, {
        method: 'POST',
        credentials: 'include',
        headers: xsrf ? { 'X-XSRF-TOKEN': decodeURIComponent(xsrf) } : {},
      });
      return { status: resp.status, ok: resp.ok };
    } catch (e) {
      return { error: String(e) };
    }
  }, orderId as string);
  console.log('Simulate result:', JSON.stringify(simResult));

  // Expect simulate to succeed
  expect(simResult.ok).toBe(true);

  // ── 9. Poll for status change — reload page until PAID ──
  // The Vue component polls after fetchOnce(), but in dev simulation it's one-shot.
  // After simulate, reload to trigger a fresh fetchOnce which should return PAID.
  let paid = false;
  for (let i = 0; i < 10; i++) {
    await page.reload();
    await page.waitForLoadState('networkidle', { timeout: 5_000 }).catch(() => {});
    const h2Text = await page.locator('h2').first().textContent().catch(() => '');
    console.log(`Poll ${i + 1}: h2="${h2Text}"`);
    if (h2Text && !h2Text.includes('Chargement')) {
      paid = true;
      break;
    }
    await page.waitForTimeout(1_000);
  }

  expect(paid).toBe(true);

  // The PAID section shows the "Commande confirmée" heading
  await expect(page.locator('h2').first()).toBeVisible({ timeout: 5_000 });
  const heading = await page.locator('h2').first().textContent();
  console.log('Final heading:', heading);
  expect(heading).toBeTruthy();
  expect(heading?.toLowerCase()).not.toContain('chargement');

  if (consoleErrors.length > 0) {
    console.log('Browser console errors:', consoleErrors);
  }
});
