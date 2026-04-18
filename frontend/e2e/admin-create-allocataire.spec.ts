import { test, expect } from '@playwright/test';
import { uniqueEmail } from './helpers';

test('admin se connecte, crée un allocataire, le code est affiché', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel(/email/i).fill('admin@fuorimondo.local');
  await page.getByLabel(/mot de passe/i).fill('Admin!Password123');
  await page.getByTestId('login-submit').click();

  await page.waitForURL(/\/profile/, { timeout: 10_000 });

  // Open burger, go to admin users
  await page.getByTestId('burger').click();
  await page.getByRole('link', { name: /Utilisateurs/i }).click();
  await page.waitForURL(/\/admin\/users/);

  await page.getByTestId('admin-create-btn').click();
  await page.waitForURL(/\/admin\/users\/create/);

  const email = uniqueEmail('allo');
  await page.getByTestId('new-email').fill(email);
  await page.getByTestId('new-first').fill('Bob');
  await page.getByTestId('new-last').fill('Dupont');
  await page.getByLabel(/pays/i).fill('France');
  await page.getByLabel(/ville/i).fill('Lyon');

  await page.getByTestId('admin-create-submit').click();

  await expect(page.getByTestId('generated-code')).toBeVisible({ timeout: 5_000 });
  const code = await page.getByTestId('generated-code').textContent();
  expect(code).toMatch(/^[A-HJ-KM-NP-Z2-9]{6}$/);
});
