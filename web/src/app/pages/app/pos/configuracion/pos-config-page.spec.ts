import { FormControl, FormGroup } from '@angular/forms';

import { openAmountSettingsValidator } from './pos-config-page';

describe('Open Product settings validation', () => {
  function form(enabled: boolean, categories: string[]): FormGroup {
    return new FormGroup({
      allowOpenProducts: new FormControl(enabled),
      openAmountCategories: new FormControl(categories),
    }, { validators: openAmountSettingsValidator });
  }

  it('requires one category when monto abierto is enabled', () => {
    expect(form(true, []).hasError('openAmountCategoriesRequired')).toBe(true);
    expect(form(false, []).valid).toBe(true);
  });

  it('rejects duplicates ignoring case and categories over 64 characters', () => {
    expect(form(false, ['Bebidas', ' bebidas ']).hasError('duplicateOpenAmountCategories')).toBe(true);
    expect(form(false, ['x'.repeat(65)]).hasError('openAmountCategoryTooLong')).toBe(true);
  });
});
