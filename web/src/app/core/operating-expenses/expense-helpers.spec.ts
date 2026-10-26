import {
  BUSINESS_ZONE,
  businessMonthToDate,
  businessToday,
  businessWeekToDate,
  businessYesterday,
  formatCompactDate,
  formatIsoDate,
  formatMxn,
} from './business-date';
import { scaledReceiptSize } from './receipt-image';

describe('expense helpers', () => {
  it('formats the business day without shifting the calendar date', () => {
    const evening = new Date('2026-10-15T05:30:00.000Z');
    expect(businessToday(evening, BUSINESS_ZONE)).toBe('2026-10-14');
    expect(businessYesterday(evening, BUSINESS_ZONE)).toBe('2026-10-13');
    expect(formatIsoDate('2026-10-14')).toBe('14/10/2026');
    expect(formatCompactDate('2026-10-14')).toBe('14/10/26');
  });

  it('builds week and month ranges on the business calendar', () => {
    const wednesday = new Date('2026-10-15T05:30:00.000Z');
    expect(businessWeekToDate(wednesday, BUSINESS_ZONE)).toEqual({ from: '2026-10-12', to: '2026-10-14' });
    expect(businessMonthToDate(wednesday, BUSINESS_ZONE)).toEqual({ from: '2026-10-01', to: '2026-10-14' });

    const monday = new Date('2026-10-13T05:30:00.000Z');
    expect(businessToday(monday, BUSINESS_ZONE)).toBe('2026-10-12');
    expect(businessWeekToDate(monday, BUSINESS_ZONE)).toEqual({ from: '2026-10-12', to: '2026-10-12' });

    const newYear = new Date('2026-01-01T18:00:00.000Z');
    expect(businessYesterday(newYear, BUSINESS_ZONE)).toBe('2025-12-31');
    expect(businessWeekToDate(newYear, BUSINESS_ZONE)).toEqual({ from: '2025-12-29', to: '2026-01-01' });
    expect(businessMonthToDate(newYear, BUSINESS_ZONE)).toEqual({ from: '2026-01-01', to: '2026-01-01' });
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
