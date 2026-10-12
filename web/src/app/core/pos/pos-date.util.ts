/** Rango del día actual en zona local, serializado como Instant UTC para la API. */
export function todayInstantRange(): { from: string; to: string } {
  const start = localDateTimeToInstant(todayDateTimeLocalStart());
  const end = localDateTimeToExclusiveEndInstant(todayDateTimeLocalEnd());
  return { from: start, to: end };
}

export function formatCentavos(centavos: number): string {
  return new Intl.NumberFormat('es-MX', { style: 'currency', currency: 'MXN' }).format(centavos / 100);
}

/** Inicio del día calendario local (00:00). */
export function localDateStartInstant(date: string): string {
  return localDateTimeToInstant(`${date}T00:00`);
}

/**
 * Fin del rango para APIs que usan `occurredAt < to` (límite superior exclusivo):
 * medianoche del día siguiente al `date` en hora local.
 */
export function localDateEndInstant(date: string): string {
  const [y, m, d] = date.split('-').map(Number);
  return new Date(y, m - 1, d + 1, 0, 0, 0, 0).toISOString();
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

function formatLocalDateTime(day: Date, hours: number, minutes: number): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${day.getFullYear()}-${pad(day.getMonth() + 1)}-${pad(day.getDate())}T${pad(hours)}:${pad(minutes)}`;
}
