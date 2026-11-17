import type { Page } from '@playwright/test';

/** Native `window.prompt` (usuarios Bloquear). Register before the click. */
export const PromptDialog = {
  once(page: Page, text: string | null): Promise<string> {
    return new Promise((resolve) => {
      page.once('dialog', async (dialog) => {
        const message = dialog.message();
        if (text == null) await dialog.dismiss();
        else await dialog.accept(text);
        resolve(message);
      });
    });
  },
};
