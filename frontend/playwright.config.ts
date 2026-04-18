import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  timeout: 30_000,
  use: {
    baseURL: 'http://localhost:5273',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    viewport: { width: 390, height: 844 }, // iPhone 12 mobile viewport
  },
  projects: [
    { name: 'mobile', use: { ...devices['Pixel 5'], locale: 'fr-FR' } },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5273',
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
});
