import { test, expect } from '@playwright/test';

test('splash screen shows logo and 3 CTAs', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('main').getByRole('heading', { name: 'FUORI MARMO' })).toBeVisible();
  await expect(page.getByTestId('cta-activate')).toBeVisible();
  await expect(page.getByTestId('cta-register')).toBeVisible();
  await expect(page.getByTestId('cta-login')).toBeVisible();
});

test('legal page loads with draft banner', async ({ page, context }) => {
  // Force FR locale via localStorage before any page load
  await context.addInitScript(() => localStorage.setItem('fm.locale', 'fr'));
  await page.goto('/legal/cgu');
  await expect(page.locator('.fm-legal-banner').first()).toBeVisible();
});
