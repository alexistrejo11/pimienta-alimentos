import { DISPLAY_ZONE } from './format-date';
import { clientZone } from './client-zone';

describe('clientZone', () => {
  it('uses the browser zone when window exists', () => {
    expect(clientZone()).toBe(Intl.DateTimeFormat().resolvedOptions().timeZone);
  });

  it('uses America/Mexico_City when there is no window', () => {
    const descriptor = Object.getOwnPropertyDescriptor(globalThis, 'window');
    // @ts-expect-error SSR has no window.
    delete globalThis.window;
    try {
      expect(clientZone()).toBe(DISPLAY_ZONE);
    } finally {
      if (descriptor) {
        Object.defineProperty(globalThis, 'window', descriptor);
      }
    }
  });
});
