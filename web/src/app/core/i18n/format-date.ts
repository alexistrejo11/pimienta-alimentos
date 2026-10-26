/** Business zone for displayed instants. Date-only strings are never parsed as instants. */
export const DISPLAY_ZONE = 'America/Mexico_City';

/**
 * `YYYY-MM-DD` → `dd/MM/yy`. Splits the calendar date; does not call `Date`.
 * A time suffix, if present, is ignored so the written day does not shift.
 */
export function formatDateOnly(isoDate: string): string {
  const parts = calendarParts(isoDate);
  if (!parts) return isoDate;
  return `${parts.day}/${parts.month}/${parts.year.slice(-2)}`;
}

/** `YYYY-MM-DD` → `dd/MM/yyyy` for legal and HR dates. Does not call `Date`. */
export function formatDateOnlyFullYear(isoDate: string): string {
  const parts = calendarParts(isoDate);
  if (!parts) return isoDate;
  return `${parts.day}/${parts.month}/${parts.year}`;
}

/**
 * ISO-8601 instant → `dd/MM/yy HH:mm` in America/Mexico_City, 24-hour clock.
 * Empty values render as an em dash.
 */
export function formatDateTime(isoInstant: string | null | undefined): string {
  if (!isoInstant) return '—';
  const date = new Date(isoInstant);
  if (Number.isNaN(date.getTime())) return isoInstant;
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: DISPLAY_ZONE,
    day: '2-digit',
    month: '2-digit',
    year: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(date);
  const value = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? '';
  const day = value('day');
  const month = value('month');
  const year = value('year');
  const hour = value('hour');
  const minute = value('minute');
  if (!day || !month || !year || !hour || !minute) return isoInstant;
  return `${day}/${month}/${year} ${hour}:${minute}`;
}

function calendarParts(isoDate: string): { year: string; month: string; day: string } | null {
  const [year, month, day] = isoDate.split('T')[0]?.split('-') ?? [];
  if (!year || !month || !day) return null;
  return { year, month: month.slice(0, 2), day: day.slice(0, 2) };
}
