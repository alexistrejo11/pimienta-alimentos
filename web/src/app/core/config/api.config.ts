/**
 * Browser-reachable API base (see {@code @RequestMapping} on controllers).
 * Resolved at runtime:
 * - Server / Docker: {@code process.env.API_BASE_URL}
 * - Browser: {@code window.__PIMIENTA_CONFIG__.apiBaseUrl} from {@code /runtime-config.js}
 * - Local {@code ng serve} fallback: localhost API
 */
export type PimientaRuntimeConfig = {
  apiBaseUrl: string;
  release?: string;
};

declare global {
  var __PIMIENTA_CONFIG__: PimientaRuntimeConfig | undefined;
}

function resolveApiBaseUrl(): string {
  const fromEnv =
    typeof process !== 'undefined' ? process.env?.['API_BASE_URL']?.trim() : undefined;
  if (fromEnv) {
    return fromEnv;
  }

  const fromWindow = globalThis.__PIMIENTA_CONFIG__?.apiBaseUrl?.trim();
  if (fromWindow) {
    return fromWindow;
  }

  return 'http://localhost:8080/api/v1';
}

export const API_BASE_URL = resolveApiBaseUrl();
export const WEB_RELEASE = resolveRelease();

function resolveRelease(): string {
  const fromEnv =
    typeof process !== 'undefined' ? process.env?.['WEB_RELEASE']?.trim() : undefined;
  if (fromEnv) {
    return fromEnv;
  }
  const fromWindow = globalThis.__PIMIENTA_CONFIG__?.release?.trim();
  if (fromWindow) {
    return fromWindow;
  }
  return '2.1.0';
}
