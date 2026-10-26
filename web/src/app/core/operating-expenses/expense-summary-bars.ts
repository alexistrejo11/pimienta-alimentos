import { eachCalendarDay } from './business-date';
import { formatDateOnly } from '../i18n/format-date';
import type { OperatingExpenseDayTotal } from '../model/operating-expense/expense.dto';

export interface DailyExpenseBar {
  date: string;
  label: string;
  heightPercent: number;
  title: string;
}

/**
 * Bars for a multi-day range of at most 31 days. Single days, longer ranges, and empty
 * summaries return no bars. Missing days in `byDay` are drawn at height 0.
 */
export function dailyExpenseBars(
  from: string,
  to: string,
  byDay: readonly OperatingExpenseDayTotal[],
  formatAmount: (amount: number | string) => string,
): DailyExpenseBar[] {
  const days = eachCalendarDay(from, to);
  if (days.length <= 1 || days.length > 31 || byDay.length === 0) return [];
  const rows = new Map(byDay.map((row) => [row.occurredOn.slice(0, 10), row]));
  const totals = days.map((date) => numeric(rows.get(date)?.totalAmount));
  const max = Math.max(...totals, 0);
  return days.map((date, index) => {
    const total = totals[index];
    const count = rows.get(date)?.count ?? 0;
    const label = dayMonth(date);
    const noun = count === 1 ? 'gasto' : 'gastos';
    return {
      date,
      label,
      heightPercent: max > 0 && total > 0 ? Math.max(8, Math.round((total / max) * 100)) : 0,
      title: `${label}: ${formatAmount(total)} · ${count} ${noun}`,
    };
  });
}

function dayMonth(iso: string): string {
  const [day, month] = formatDateOnly(iso).split('/');
  return day && month ? `${day}/${month}` : iso;
}

function numeric(amount: number | string | undefined): number {
  const value = typeof amount === 'number' ? amount : Number(amount);
  return Number.isFinite(value) ? value : 0;
}
