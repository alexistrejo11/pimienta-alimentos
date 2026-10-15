import { businessToday, formatIsoDate, formatMxn } from './business-date';
import { scaledReceiptSize } from './receipt-image';

describe('expense helpers', () => {
  it('formats the business day without shifting the calendar date', () => {
    const evening = new Date('2026-10-15T05:30:00.000Z');
    expect(businessToday(evening)).toBe('2026-10-14');
    expect(formatIsoDate('2026-10-14')).toBe('14/10/2026');
  });

  it('formats pesos', () => {
    expect(formatMxn(12.5)).toContain('12.50');
  });

  it('caps the longest side at 1920 and does not upscale', () => {
    expect(scaledReceiptSize(2000, 800)).toEqual({ width: 1920, height: 768 });
    expect(scaledReceiptSize(800, 2000)).toEqual({ width: 768, height: 1920 });
    expect(scaledReceiptSize(100, 40)).toEqual({ width: 100, height: 40 });
    expect(scaledReceiptSize(1920, 800)).toEqual({ width: 1920, height: 800 });
  });
});
