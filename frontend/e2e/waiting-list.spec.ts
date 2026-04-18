import { test, expect } from '@playwright/test';
import { uniqueEmail } from './helpers';

test('un visiteur peut s\'inscrire en liste d\'attente et voir son profil en attente', async ({ page }) => {
  const email = uniqueEmail('wl');
  await page.goto('/');
  await page.getByTestId('cta-register').click();
  await expect(page).toHaveURL(/\/register/);

  await page.getByTestId('reg-email').fill(email);
  await page.getByTestId('reg-first').fill('Alice');
  await page.getByTestId('reg-last').fill('Martin');
  await page.getByLabel(/pays/i).fill('France');
  await page.getByLabel(/ville/i).fill('Paris');
  await page.getByTestId('reg-password').fill('aVerySecurePass123!');
  await page.getByLabel(/accepte les conditions/i).check();
  await page.getByLabel(/accepte la politique/i).check();

  await page.getByTestId('reg-submit').click();

  await page.waitForURL(/\/profile/, { timeout: 10_000 });
  await expect(page.getByText(/en attente/i)).toBeVisible();
});
