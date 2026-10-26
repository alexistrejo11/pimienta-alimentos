import { clientZone } from '../i18n/client-zone';
import { formatDateOnly, formatDateOnlyFullYear } from '../i18n/format-date';

/** Business calendar used by the expense API. */
export const BUSINESS_ZONE = 'America/Mexico_City';

export interface BusinessDateRange {
  from: string;
  to: string;
}

interface CalendarDay {
  year: number;
  month: number;
  day: number;
}

/** Today as `YYYY-MM-DD` in the client zone. Pass `BUSINESS_ZONE` to pin Mexico City. */
export function businessToday(now = new Date(), timeZone = clientZone()): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now);
}

/** Yesterday as `YYYY-MM-DD` in the business zone. */
export function businessYesterday(now = new Date(), timeZone = clientZone()): string {
  return formatDay(shiftDays(parseDay(businessToday(now, timeZone)), -1));
}

/** Monday through today, as `YYYY-MM-DD` in the business zone. */
export function businessWeekToDate(now = new Date(), timeZone = clientZone()): BusinessDateRange {
  const today = parseDay(businessToday(now, timeZone));
  return { from: formatDay(shiftDays(today, -mondayIndex(today))), to: formatDay(today) };
}

/**
 * Inclusive calendar days from `from` through `to` as `YYYY-MM-DD`.
 * Returns an empty list when either value is not a date or `from` is after `to`.
 */
export function eachCalendarDay(from: string, to: string): string[] {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(from) || !/^\d{4}-\d{2}-\d{2}$/.test(to) || from > to) return [];
  const days: string[] = [];
  let cursor = parseDay(from);
  for (let i = 0; i < 366; i++) {
    const iso = formatDay(cursor);
    days.push(iso);
    if (iso === to) return days;
    cursor = shiftDays(cursor, 1);
  }
  return days;
}

/** First of the current month through today, as `YYYY-MM-DD` in the business zone. */
export function businessMonthToDate(now = new Date(), timeZone = clientZone()): BusinessDateRange {
  const today = parseDay(businessToday(now, timeZone));
  return { from: formatDay({ year: today.year, month: today.month, day: 1 }), to: formatDay(today) };
}

/** Renders an ISO date as `dd/MM/yyyy` without shifting the calendar day. */
export function formatIsoDate(iso: string): string {
  return formatDateOnlyFullYear(iso);
}

/** Renders an ISO date as `dd/MM/yy` without parsing it as an instant. */
export function formatCompactDate(iso: string): string {
  return formatDateOnly(iso);
}

function parseDay(iso: string): CalendarDay {
  const [year, month, day] = iso.split('-').map(Number);
  return { year, month, day };
}

function formatDay(day: CalendarDay): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${day.year}-${pad(day.month)}-${pad(day.day)}`;
}

function shiftDays(day: CalendarDay, delta: number): CalendarDay {
  const utc = new Date(Date.UTC(day.year, day.month - 1, day.day + delta));
  return { year: utc.getUTCFullYear(), month: utc.getUTCMonth() + 1, day: utc.getUTCDate() };
}

/** Monday = 0 … Sunday = 6. */
function mondayIndex(day: CalendarDay): number {
  const sunday0 = new Date(Date.UTC(day.year, day.month - 1, day.day)).getUTCDay();
  return sunday0 === 0 ? 6 : sunday0 - 1;
}

export function formatMxn(amount: number | string): string {
  const value = typeof amount === 'number' ? amount : Number(amount);
  if (!Number.isFinite(value)) return '—';
  return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(value);
}
