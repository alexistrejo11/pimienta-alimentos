const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

function canUseStorage(): boolean {
  return typeof localStorage !== 'undefined';
}

/** One-time migrate from sessionStorage (old tab sessions) to localStorage. */
function migrateFromSessionStorage(): void {
  if (typeof sessionStorage === 'undefined' || !canUseStorage()) return;
  const access = sessionStorage.getItem(ACCESS_TOKEN_KEY);
  const refresh = sessionStorage.getItem(REFRESH_TOKEN_KEY);
  if (!access && !refresh) return;
  if (access && !localStorage.getItem(ACCESS_TOKEN_KEY)) {
    localStorage.setItem(ACCESS_TOKEN_KEY, access);
  }
  if (refresh && !localStorage.getItem(REFRESH_TOKEN_KEY)) {
    localStorage.setItem(REFRESH_TOKEN_KEY, refresh);
  }
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
}

/**
 * Access / refresh JWT persistence in {@code localStorage} so the session
 * survives closing the browser. Prefer this helper over raw storage calls.
 */
export const authTokenStorage = {
  getAccessToken(): string | null {
    if (!canUseStorage()) return null;
    migrateFromSessionStorage();
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    if (!canUseStorage()) return null;
    migrateFromSessionStorage();
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  setTokens(accessToken: string, refreshToken: string): void {
    if (!canUseStorage()) return;
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(ACCESS_TOKEN_KEY);
      sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    }
  },

  setAccessToken(accessToken: string): void {
    if (!canUseStorage()) return;
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    }
  },

  clear(): void {
    if (canUseStorage()) {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      localStorage.removeItem(REFRESH_TOKEN_KEY);
    }
    if (typeof sessionStorage !== 'undefined') {
      sessionStorage.removeItem(ACCESS_TOKEN_KEY);
      sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    }
  },
};
