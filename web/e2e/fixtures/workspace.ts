import { expect, test as base, type Page } from '@playwright/test';

import { sedeCentro, sedeNorte } from '../data/headquarters';
import { adminUser, directorUser, employeeUser, managerUser, userDashboard } from '../data/users';
import { MockApi, paged, springPage, type MockCall } from '../mock-api/mock-api';
import type { UserResponse } from '../../src/app/core/model/account/user.dto';

type Role = 'admin' | 'director' | 'manager' | 'employee';

const profiles: Record<Role, UserResponse> = {
  admin: adminUser,
  director: directorUser,
  manager: managerUser,
  employee: employeeUser,
};

export async function seedSession(page: Page): Promise<void> {
  await page.addInitScript(() => {
    localStorage.setItem('accessToken', 'e2e-access');
    localStorage.setItem('refreshToken', 'e2e-refresh');
  });
}

export function stubWorkspaceBasics(api: MockApi, profile: UserResponse): void {
  api.stub('GET', '/users/me', { json: profile });
  api.stub('GET', '/users/me/dashboard', { json: userDashboard() });
  api.stub('GET', '/headquarters', { json: springPage([sedeCentro, sedeNorte]) });
  api.stub('GET', '/inventory/dashboard', {
    json: {
      skuCount: 0,
      lowStockCount: 0,
      outOfStockCount: 0,
      totalAvailableQuantity: 0,
      totalStockValue: 0,
    },
  });
  api.stub('GET', '/operating-expenses/summary', {
    json: { totalAmount: 0, topCategory: null, byDay: [] },
  });
  api.stub('GET', '/pos/admin/reports/summary', { json: { rows: [] } });
  api.stub('GET', '/pos/admin/reports/sales', { json: paged([]) });
  api.stub('GET', '/pos/admin/reports/products', { json: paged([]) });
  api.stub('GET', '/pos/admin/operators', { json: paged([]) });
  api.stub('GET', '/pos/admin/devices', { json: paged([]) });
  api.stub('GET', '/pos/admin/shifts', { json: paged([]) });
  api.stub('GET', '/opportunities', { json: paged([]) });
  api.stub('GET', '/projects', { json: paged([]) });
  api.stub('GET', '/products', { json: paged([]) });
  api.stub('GET', '/inventory/items', { json: paged([]) });
  api.stub('GET', '/contracts', { json: paged([]) });
  api.stub('GET', '/tasks', { json: paged([]) });
  api.stub('GET', '/employees', { json: paged([]) });
  api.stub('GET', '/payroll/periods', { json: paged([]) });
  api.stub('GET', '/payroll/records', { json: paged([]) });
  api.stub('GET', '/payroll/summary', {
    json: {
      from: null,
      to: null,
      employeesPaid: 0,
      totalGross: 0,
      totalNet: 0,
      totalDiscounts: 0,
      totalBonuses: 0,
      totalDebt: 0,
    },
  });
  api.stub('GET', '/headquarters/statistics', { json: { total: 2, active: 2, softDeleted: 0 } });
  api.stub('GET', /^\/headquarters\/\d+$/, async (route, request, call) => {
    const id = Number(call.path.split('/')[2]);
    const hq = id === sedeNorte.id ? sedeNorte : { ...sedeCentro, id };
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      headers: {
        'Access-Control-Allow-Origin': request.headers()['origin'] ?? '*',
        'Access-Control-Allow-Credentials': 'true',
      },
      body: JSON.stringify(hq),
    });
  });
}

export const test = base.extend<{ mockApi: MockApi }>({
  mockApi: async ({ page }, use) => {
    const mockApi = await MockApi.attach(page);
    await seedSession(page);
    stubWorkspaceBasics(mockApi, adminUser);
    await use(mockApi);
    const leftover = mockApi.unmocked.filter((c) => !c.url.includes('/telemetry/'));
    expect(leftover, `Unmocked API calls: ${JSON.stringify(leftover)}`).toEqual([]);
  },
});

export const guestTest = base.extend<{ mockApi: MockApi }>({
  mockApi: async ({ page }, use) => {
    const mockApi = await MockApi.attach(page);
    await use(mockApi);
    const leftover = mockApi.unmocked.filter((c) => !c.url.includes('/telemetry/'));
    expect(leftover, `Unmocked API calls: ${JSON.stringify(leftover)}`).toEqual([]);
  },
});

export async function loginAs(api: MockApi, role: Role): Promise<UserResponse> {
  const profile = profiles[role];
  api.stub('GET', '/users/me', { json: profile });
  return profile;
}

export function expectQuery(call: MockCall | undefined, key: string, value: string): void {
  expect(call, `missing call for query ${key}`).toBeTruthy();
  expect(call!.query[key]).toBe(value);
}

export { expect };
