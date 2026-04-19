import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './helpers';

test('admin can navigate to the orders dashboard and see the orders table', async ({ page }) => {
  // Log in as admin
  await loginAsAdmin(page);

  // Navigate to the admin orders page
  await page.goto('/admin/orders');

  // The admin orders view renders a <table> (visible even when empty — the thead is always rendered)
  await expect(page.locator('table')).toBeVisible({ timeout: 8_000 });
});

test('admin orders table headers are rendered correctly', async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto('/admin/orders');

  // Wait for the table
  await expect(page.locator('table')).toBeVisible({ timeout: 8_000 });

  // The AdminOrdersView thead contains: Date, Acheteur, Produit, Total, Statut
  const headers = page.locator('thead th');
  await expect(headers).toHaveCount(5, { timeout: 5_000 });
});
