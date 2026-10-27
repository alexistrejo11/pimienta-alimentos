import { clientZone } from '../i18n/client-zone';

/** Rango del día actual en la zona del cliente, serializado como Instant UTC para la API. */
export function todayInstantRange(now = new Date(), timeZone = clientZone()): { from: string; to: string } {
  const today = calendarDate(now, timeZone);
  return { from: localDateStartInstant(today, timeZone), to: localDateEndInstant(today, timeZone) };
}

export function formatCentavos(centavos: number): string {
  return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(centavos / 100);
}

/** Inicio del día calendario (00:00) en la zona del cliente. */
export function localDateStartInstant(date: string, timeZone = clientZone()): string {
  return wallMidnightToInstant(date, timeZone);
}

/**
 * Fin del rango para APIs que usan `occurredAt < to` (límite superior exclusivo):
 * medianoche del día siguiente al `date` en hora local.
 */
export function localDateEndInstant(date: string, timeZone = clientZone()): string {
  return wallMidnightToInstant(nextIsoDate(date), timeZone);
}

/** Valor para `<input type="datetime-local">` al inicio del día local de hoy. */
export function todayDateTimeLocalStart(): string {
  return formatLocalDateTime(new Date(), 0, 0);
}

/** Valor para `<input type="datetime-local">` al final del día local de hoy (23:59). */
export function todayDateTimeLocalEnd(): string {
  return formatLocalDateTime(new Date(), 23, 59);
}

export function localDateTimeToInstant(value: string): string {
  return new Date(value).toISOString();
}

/**
 * Convierte el instante elegido en el límite exclusivo `to` de la API:
 * incluye el minuto completo seleccionado en `datetime-local`.
 */
export function localDateTimeToExclusiveEndInstant(value: string): string {
  const end = new Date(value);
  end.setMinutes(end.getMinutes() + 1, 0, 0);
  return end.toISOString();
}

function calendarDate(now: Date, timeZone: string): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now);
  const value = (type: Intl.DateTimeFormatPartTypes) => parts.find((part) => part.type === type)?.value ?? '';
  return `${value('year')}-${value('month')}-${value('day')}`;
}

function nextIsoDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  const next = new Date(Date.UTC(year, month - 1, day + 1));
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${next.getUTCFullYear()}-${pad(next.getUTCMonth() + 1)}-${pad(next.getUTCDate())}`;
}

/** Midnight of an ISO calendar date in `timeZone`, as a UTC instant. */
function wallMidnightToInstant(isoDate: string, timeZone: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  const utcGuess = Date.UTC(year, month - 1, day, 0, 0, 0);
  const offset = zoneOffsetMs(utcGuess, timeZone);
  let instant = utcGuess - offset;
  const corrected = zoneOffsetMs(instant, timeZone);
  if (corrected !== offset) {
    instant = utcGuess - corrected;
  }
  return new Date(instant).toISOString();
}

function zoneOffsetMs(epochMs: number, timeZone: string): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).formatToParts(new Date(epochMs));
  const value = (type: Intl.DateTimeFormatPartTypes) => Number(parts.find((part) => part.type === type)?.value ?? '0');
  let year = value('year');
  let month = value('month');
  let day = value('day');
  let hour = value('hour');
  if (hour === 24) {
    hour = 0;
    const rolled = new Date(Date.UTC(year, month - 1, day + 1));
    year = rolled.getUTCFullYear();
    month = rolled.getUTCMonth() + 1;
    day = rolled.getUTCDate();
  }
  return Date.UTC(year, month - 1, day, hour, value('minute'), value('second')) - epochMs;
}

function formatLocalDateTime(day: Date, hours: number, minutes: number): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${day.getFullYear()}-${pad(day.getMonth() + 1)}-${pad(day.getDate())}T${pad(hours)}:${pad(minutes)}`;
}
