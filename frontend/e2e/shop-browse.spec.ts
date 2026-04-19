import { test, expect } from '@playwright/test';
import { loginAsAdmin, apiAdminLogin, createAllocataireApi, activateAllocataireUi, uniqueEmail } from './helpers';

test('allocataire sees the shop page after login', async ({ page }) => {
  // Seed: create a TIER_1 allocataire via admin API
  const auth = await apiAdminLogin();
  const email = uniqueEmail('browse');
  const { code } = await createAllocataireApi(auth, { email, tierCode: 'TIER_1' });
  await auth.ctx.dispose();

  // Activate the allocataire through the UI
  await activateAllocataireUi(page, email, code);

  // Navigate to /shop — allocataire should see the shop page
  // (may be empty state if no products are seeded for this user's tier, but the page must load)
  await page.goto('/shop');
  await expect(page.locator('h2').first()).toBeVisible({ timeout: 5_000 });
  // The shop title is rendered in the i18n locale — just verify something is shown
  const title = await page.locator('h2').first().textContent();
  expect(title).toBeTruthy();
});

test('admin sees shop page (empty — no tier code assigned to admin)', async ({ page }) => {
  // Admin account has no tierCode so /api/products returns [] for admin.
  // The shop page should render with the empty-state message.
  await loginAsAdmin(page);
  await page.goto('/shop');

  // h2 with the shop title should be visible
  await expect(page.locator('h2').first()).toBeVisible({ timeout: 5_000 });
  // Loading should finish and no product cards should be visible
  await expect(page.locator('[data-testid^="product-card-"]')).toHaveCount(0, { timeout: 5_000 });
});
