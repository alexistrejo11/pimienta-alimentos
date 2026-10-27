import { DISPLAY_ZONE } from './format-date';

/**
 * IANA zone sent as `X-Timezone`.
 * The browser uses `Intl`; server render uses {@link DISPLAY_ZONE}.
 */
export function clientZone(): string {
  if (typeof window === 'undefined') {
    return DISPLAY_ZONE;
  }
  const zone = Intl.DateTimeFormat().resolvedOptions().timeZone;
  return zone || DISPLAY_ZONE;
}
