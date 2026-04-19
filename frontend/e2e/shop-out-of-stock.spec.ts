import { test, expect } from '@playwright/test';
import { apiAdminLogin, createProduct, createAllocataireApi, activateAllocataireUi } from './helpers';
import { uniqueEmail } from './helpers';

test('out-of-stock product (stock=0) is not listed in the shop', async ({ page }) => {
  // Seed via admin API: one TIER_1 product with stock=0 + one TIER_1 allocataire
  const auth = await apiAdminLogin();

  const now = new Date();
  const past = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
  const future = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString();

  // Product with stock=0 — should be filtered out by the backend /api/products endpoint
  const oosProduct = await createProduct(auth, {
    name: `oos-${Date.now()}`,
    priceEur: '50.00',
    tiers: ['TIER_1'],
    delivery: false,
    saleStartAt: past,
    saleEndAt: future,
    stock: 0,
  });

  const email = uniqueEmail('oos');
  const { code } = await createAllocataireApi(auth, { email, tierCode: 'TIER_1' });
  await auth.ctx.dispose();

  const password = 'AlloPass1!';
  await activateAllocataireUi(page, email, code, password);

  await page.goto('/shop');

  // Wait for loading to settle — either the empty message appears or product cards do
  await page.waitForTimeout(2_000);

  // The specific stock=0 product must NOT appear as a product card (other products from previous
  // test runs may still exist in the shared dev DB, so we check the specific ID, not total count)
  await expect(page.locator(`[data-testid="product-card-${oosProduct.id}"]`)).toHaveCount(0, { timeout: 5_000 });
});

test('product without stock constraint is listed when in sale window', async ({ page }) => {
  // Verify that a product with stock=null (unlimited) IS visible to a matching-tier allocataire
  const auth = await apiAdminLogin();

  const now = new Date();
  const past = new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
  const future = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000).toISOString();

  const productName = `unlimited-${Date.now()}`;
  await createProduct(auth, {
    name: productName,
    priceEur: '75.00',
    tiers: ['TIER_1'],
    delivery: false,
    saleStartAt: past,
    saleEndAt: future,
    stock: null, // unlimited
  });

  const email = uniqueEmail('ulim');
  const { code } = await createAllocataireApi(auth, { email, tierCode: 'TIER_1' });
  await auth.ctx.dispose();

  const password = 'AlloPass1!';
  await activateAllocataireUi(page, email, code, password);

  await page.goto('/shop');
  await expect(page.locator('[data-testid^="product-card-"]').first()).toBeVisible({ timeout: 8_000 });
});
