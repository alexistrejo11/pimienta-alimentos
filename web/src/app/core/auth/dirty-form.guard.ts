import type { CanDeactivateFn } from '@angular/router';

type FormPage = {
  form?: { dirty: boolean };
  loading?: () => boolean;
  saving?: () => boolean;
  submitting?: () => boolean;
};

/** Protects routed reactive forms without coupling the guard to each page class. */
export const dirtyFormGuard: CanDeactivateFn<FormPage> = (component) => {
  if (!component.form?.dirty) return true;
  const busy = component.loading?.() || component.saving?.() || component.submitting?.();
  if (busy) return true;
  return globalThis.confirm?.('Hay cambios sin guardar. ¿Deseas salir sin guardar?') ?? true;
};
