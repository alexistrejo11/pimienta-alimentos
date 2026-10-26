import { formatNumber, registerLocaleData } from '@angular/common';
import localeEsMx from '@angular/common/locales/es-MX';

import { formatDateOnly, formatDateOnlyFullYear, formatDateTime } from './format-date';

registerLocaleData(localeEsMx);

describe('format-date', () => {
  it('splits a calendar date without moving it to the previous day', () => {
    expect(formatDateOnly('2026-10-15')).toBe('15/10/26');
    expect(formatDateOnlyFullYear('2026-10-15')).toBe('15/10/2026');
    expect(formatDateOnly('2026-10-15T00:00:00.000Z')).toBe('15/10/26');
  });

  it('renders instants in America/Mexico_City with a 24-hour clock', () => {
    expect(formatDateTime('2026-10-15T05:30:00.000Z')).toBe('14/10/26 23:30');
    expect(formatDateTime('2026-10-15T06:00:00.000Z')).toBe('15/10/26 00:00');
    expect(formatDateTime(null)).toBe('—');
  });

  it('keeps es-MX thousands as commas and decimals as dots', () => {
    expect(formatNumber(1234567.89, 'es-MX', '1.2-2')).toBe('1,234,567.89');
  });
});
