/** Business calendar used by the expense API. */
export const BUSINESS_ZONE = 'America/Mexico_City';

/** Today as `YYYY-MM-DD` in the business zone. */
export function businessToday(now = new Date(), timeZone = BUSINESS_ZONE): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now);
}

/** Renders an ISO date without shifting the calendar day. */
export function formatIsoDate(iso: string): string {
  const [year, month, day] = iso.split('-');
  if (!year || !month || !day) return iso;
  return `${day}/${month}/${year}`;
}

export function formatMxn(amount: number | string): string {
  const value = typeof amount === 'number' ? amount : Number(amount);
  if (!Number.isFinite(value)) return '—';
  return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(value);
}
