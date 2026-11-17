import type { Page } from '@playwright/test';

/** Native `window.confirm` used by dirtyFormGuard and delete flows. Register before the click. */
export const ConfirmDialog = {
  once(page: Page, accept = true): Promise<string> {
    return new Promise((resolve) => {
      page.once('dialog', async (dialog) => {
        const message = dialog.message();
        if (accept) await dialog.accept();
        else await dialog.dismiss();
        resolve(message);
      });
    });
  },
};
