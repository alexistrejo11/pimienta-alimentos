import { AbstractControl } from '@angular/forms';

/** Clears dirty/touched so CanDeactivate does not treat a saved or hydrated form as unsaved. */
export function markFormPristine(form: AbstractControl): void {
  form.markAsPristine();
  form.markAsUntouched();
}
