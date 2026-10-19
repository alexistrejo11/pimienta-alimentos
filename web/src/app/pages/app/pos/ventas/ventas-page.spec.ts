import { posDiscountModeLabel } from '../../../../core/i18n/enum-labels';
import { posSalesIncludeFilter } from './ventas-page';

describe('Ventas POS discount filter', () => {
  it('sends discountedOnly for any discounted or courtesy ticket', () => {
    const filter = posSalesIncludeFilter('discounted');
    expect(filter.discountedOnly).toBe(true);
    expect(filter.courtesyOnly).toBe(false);
  });

  it('sends courtesyOnly only for full courtesies', () => {
    const filter = posSalesIncludeFilter('courtesy');
    expect(filter.courtesyOnly).toBe(true);
    expect(filter.discountedOnly).toBe(false);
  });

  it('labels the three discount modes in Spanish', () => {
    expect(posDiscountModeLabel('FULL_COURTESY')).toBe('Cortesía total');
    expect(posDiscountModeLabel('AT_COST')).toBe('Cobro al costo');
    expect(posDiscountModeLabel('TARGET_AMOUNT')).toBe('Monto a cobrar');
    expect(posDiscountModeLabel(null)).toBe('Descuento');
  });
});
