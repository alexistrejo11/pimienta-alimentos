import type { Page, Request, Route } from '@playwright/test';

export const API_ORIGIN = 'http://api.e2e.test';
export const API_PREFIX = '/api/v1';
export const API_BASE = `${API_ORIGIN}${API_PREFIX}`;

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'OPTIONS' | 'HEAD';

export interface MockCall {
  method: string;
  path: string;
  url: string;
  search: string;
  query: Record<string, string>;
  json: unknown;
  multipartJson: unknown;
}

export interface StubOptions {
  times?: number;
  status?: number;
  json?: unknown;
  body?: string | Buffer;
  contentType?: string;
  headers?: Record<string, string>;
}

export type StubHandler = (route: Route, request: Request, call: MockCall) => Promise<void> | void;

interface Handler {
  method: HttpMethod | '*';
  matchPath: (path: string) => boolean;
  remaining: number | null;
  handle: StubHandler;
}

export function corsHeaders(request: Request): Record<string, string> {
  const origin = request.headers()['origin'] ?? '*';
  return {
    'Access-Control-Allow-Origin': origin,
    'Access-Control-Allow-Methods': 'GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD',
    'Access-Control-Allow-Headers': request.headers()['access-control-request-headers'] ?? '*',
    'Access-Control-Expose-Headers': '*',
    'Access-Control-Allow-Credentials': 'true',
    Vary: 'Origin',
  };
}

export function paged<T>(items: T[], page = 0, size = 20, total = items.length) {
  const totalPages = Math.max(1, Math.ceil(total / size));
  return {
    items,
    metadata: {
      pageNumber: page,
      pageSize: size,
      totalElements: total,
      totalPages,
      first: page === 0,
      last: page >= totalPages - 1,
      hasNext: page < totalPages - 1,
      hasPrevious: page > 0,
    },
  };
}

export function springPage<T>(content: T[], page = 0, size = 100) {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size,
    number: page,
    first: true,
    last: true,
    numberOfElements: content.length,
    empty: content.length === 0,
  };
}

export function apiErrorBody(
  errorCode: string,
  message: string,
  fieldErrors: { field: string; message: string }[] = [],
) {
  return { errorCode, message, traceId: 'e2e-trace', fieldErrors };
}

/** Parse JSON from multipart parts named `employee` (HR) or `data` (gastos). */
export function parseMultipartJsonPart(
  request: Request,
  names: string[] = ['employee', 'data'],
): unknown | null {
  const ct = request.headers()['content-type'] ?? '';
  if (!ct.includes('multipart/form-data')) {
    try {
      return request.postDataJSON();
    } catch {
      return null;
    }
  }
  const boundaryMatch = /boundary=(?:"([^"]+)"|([^;]+))/i.exec(ct);
  if (!boundaryMatch) return null;
  const boundary = boundaryMatch[1] ?? boundaryMatch[2];
  const buf = request.postDataBuffer();
  if (!buf) return null;
  const text = buf.toString('utf8');
  const parts = text.split(`--${boundary}`);
  for (const part of parts) {
    const headerEnd = part.indexOf('\r\n\r\n');
    if (headerEnd < 0) continue;
    const header = part.slice(0, headerEnd);
    const nameMatch = /name="([^"]+)"/.exec(header);
    if (!nameMatch || !names.includes(nameMatch[1])) continue;
    let body = part.slice(headerEnd + 4);
    if (body.endsWith('\r\n')) body = body.slice(0, -2);
    if (body.endsWith('--')) body = body.slice(0, -2);
    try {
      return JSON.parse(body);
    } catch {
      return body;
    }
  }
  return null;
}

function apiPath(url: URL): string {
  let p = url.pathname;
  if (p.startsWith(API_PREFIX)) {
    p = p.slice(API_PREFIX.length) || '/';
  }
  if (!p.startsWith('/')) p = `/${p}`;
  return p;
}

function queryOf(url: URL): Record<string, string> {
  const q: Record<string, string> = {};
  url.searchParams.forEach((value, key) => {
    q[key] = value;
  });
  return q;
}

export class MockApi {
  readonly calls: MockCall[] = [];
  readonly unmocked: { method: string; url: string }[] = [];
  private handlers: Handler[] = [];
  private waiters: Array<{
    pred: (call: MockCall) => boolean;
    resolve: (call: MockCall) => void;
  }> = [];

  constructor(private readonly page: Page) {}

  static async attach(page: Page): Promise<MockApi> {
    const api = new MockApi(page);
    await page.route('**/runtime-config.js', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/javascript; charset=utf-8',
        body: `window.__PIMIENTA_CONFIG__=${JSON.stringify({
          apiBaseUrl: API_BASE,
          release: 'e2e',
        })};`,
      });
    });
    await page.route(`${API_ORIGIN}/**`, (route) => api.onRoute(route));
    api.installDefaults();
    return api;
  }

  installDefaults(): void {
    this.stub('POST', '/telemetry/web/events', { status: 204 });
    this.stub('POST', '/auth/refresh', {
      json: {
        accessToken: 'e2e-access',
        refreshToken: 'e2e-refresh',
        tokenType: 'Bearer',
        expiresInSeconds: 3600,
      },
    });
    this.stub('POST', '/auth/logout', { status: 204 });
    this.stub('GET', '/employees/active', { json: paged([]) });
  }

  stub(method: HttpMethod, path: string | RegExp, options: StubOptions | StubHandler): void {
    const matchPath =
      typeof path === 'string'
        ? (p: string) => p === path
        : (p: string) => path.test(p);
    const handle: StubHandler =
      typeof options === 'function'
        ? options
        : async (route, request) => {
            await route.fulfill({
              status: options.status ?? 200,
              contentType:
                options.contentType ??
                (options.body ? 'text/plain' : 'application/json; charset=utf-8'),
              headers: { ...corsHeaders(request), ...(options.headers ?? {}) },
              body:
                options.body != null
                  ? options.body
                  : options.status === 204
                    ? ''
                    : JSON.stringify(options.json ?? {}),
            });
          };
    this.handlers.push({
      method,
      matchPath,
      remaining: typeof options === 'function' ? null : (options.times ?? null),
      handle,
    });
  }

  lastCall(method: string, path: string | RegExp): MockCall | undefined {
    for (let i = this.calls.length - 1; i >= 0; i--) {
      const c = this.calls[i];
      const pathOk = typeof path === 'string' ? c.path === path : path.test(c.path);
      if (c.method === method && pathOk) return c;
    }
    return undefined;
  }

  callsMatching(method: string, path: string | RegExp): MockCall[] {
    return this.calls.filter((c) => {
      const pathOk = typeof path === 'string' ? c.path === path : path.test(c.path);
      return c.method === method && pathOk;
    });
  }

  nextCall(method: string, path: string | RegExp, timeoutMs = 10_000): Promise<MockCall> {
    const pred = (c: MockCall) => {
      const pathOk = typeof path === 'string' ? c.path === path : path.test(c.path);
      return c.method === method && pathOk;
    };
    return new Promise((resolve, reject) => {
      const timer = setTimeout(() => {
        this.waiters = this.waiters.filter((w) => w.resolve !== resolveCall);
        reject(new Error(`Timed out waiting for ${method} ${path}`));
      }, timeoutMs);
      const resolveCall = (call: MockCall) => {
        clearTimeout(timer);
        resolve(call);
      };
      this.waiters.push({ pred, resolve: resolveCall });
    });
  }

  private async onRoute(route: Route): Promise<void> {
    const request = route.request();
    const method = request.method().toUpperCase() as HttpMethod;
    if (method === 'OPTIONS') {
      await route.fulfill({ status: 204, headers: corsHeaders(request), body: '' });
      return;
    }
    const url = new URL(request.url());
    const path = apiPath(url);
    let json: unknown = null;
    try {
      json = request.postDataJSON();
    } catch {
      json = null;
    }
    const call: MockCall = {
      method,
      path,
      url: request.url(),
      search: url.search,
      query: queryOf(url),
      json,
      multipartJson: parseMultipartJsonPart(request),
    };
    this.calls.push(call);
    const waiter = this.waiters.find((w) => w.pred(call));
    if (waiter) {
      this.waiters = this.waiters.filter((w) => w !== waiter);
      waiter.resolve(call);
    }

    for (let i = this.handlers.length - 1; i >= 0; i--) {
      const h = this.handlers[i];
      if (h.remaining === 0) continue;
      if (h.method !== '*' && h.method !== method) continue;
      if (!h.matchPath(path)) continue;
      if (h.remaining != null) h.remaining -= 1;
      await h.handle(route, request, call);
      return;
    }

    this.unmocked.push({ method, url: request.url() });
    await route.fulfill({
      status: 501,
      contentType: 'application/json; charset=utf-8',
      headers: corsHeaders(request),
      body: JSON.stringify(
        apiErrorBody('UNMOCKED', `Unmocked ${method} ${path}`),
      ),
    });
  }
}
