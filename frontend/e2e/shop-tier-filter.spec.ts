import { test, expect } from '@playwright/test';
import { apiAdminLogin, createProduct, createAllocataireApi, activateAllocataireUi } from './helpers';
import { uniqueEmail } from './helpers';

test('TIER_3 allocataire does not see TIER_1-only products', async ({ page }) => {
  // Seed via admin API: one TIER_1-only product + one TIER_3 allocataire
  const auth = await apiAdminLogin();

  const now = new Date();
  const past = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
  const future = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString();

  // Create a TIER_1-only product with a valid sale window
  await createProduct(auth, {
    name: `T1-only-${Date.now()}`,
    priceEur: '99.00',
    tiers: ['TIER_1'],
    delivery: false,
    saleStartAt: past,
    saleEndAt: future,
  });

  // Create a TIER_3 allocataire
  const email = uniqueEmail('t3');
  const { code } = await createAllocataireApi(auth, { email, tierCode: 'TIER_3' });
  await auth.ctx.dispose();

  // Activate the allocataire
  const password = 'AlloPass1!';
  await activateAllocataireUi(page, email, code, password);

  // Navigate to /shop as TIER_3 — must NOT see TIER_1 products
  await page.goto('/shop');

  // Wait for loading to complete (either empty text or product cards appear)
  await page.waitForTimeout(2_000);

  // No product cards should be visible for a TIER_3 user viewing TIER_1-only products
  await expect(page.locator('[data-testid^="product-card-"]')).toHaveCount(0, { timeout: 5_000 });
});

test('TIER_1 allocataire sees TIER_1 products in shop', async ({ page }) => {
  // Seed via admin API
  const auth = await apiAdminLogin();

  const now = new Date();
  const past = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
  const future = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString();

  const productName = `T1-visible-${Date.now()}`;
  await createProduct(auth, {
    name: productName,
    priceEur: '120.00',
    tiers: ['TIER_1'],
    delivery: false,
    saleStartAt: past,
    saleEndAt: future,
  });

  const email = uniqueEmail('t1');
  const { code } = await createAllocataireApi(auth, { email, tierCode: 'TIER_1' });
  await auth.ctx.dispose();

  const password = 'AlloPass1!';
  await activateAllocataireUi(page, email, code, password);

  await page.goto('/shop');

  // Wait for at least one product card to appear
  await expect(page.locator('[data-testid^="product-card-"]').first()).toBeVisible({ timeout: 8_000 });
});
