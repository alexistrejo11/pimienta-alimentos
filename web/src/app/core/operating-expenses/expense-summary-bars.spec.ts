import { dailyExpenseBars } from './expense-summary-bars';

describe('daily expense bars', () => {
  const money = (amount: number | string) => `$${amount}`;

  it('hides a single day, an empty summary, and ranges longer than 31 days', () => {
    const oneDay = [{ occurredOn: '2026-10-14', totalAmount: 10, count: 1 }];
    expect(dailyExpenseBars('2026-10-14', '2026-10-14', oneDay, money)).toEqual([]);
    expect(dailyExpenseBars('2026-10-12', '2026-10-14', [], money)).toEqual([]);
    expect(dailyExpenseBars('2026-10-01', '2026-11-15', oneDay, money)).toEqual([]);
    expect(dailyExpenseBars('', '', oneDay, money)).toEqual([]);
  });

  it('zero-fills the range and scales bars to the largest day', () => {
    const bars = dailyExpenseBars(
      '2026-10-12',
      '2026-10-14',
      [
        { occurredOn: '2026-10-12', totalAmount: '10.00', count: 1 },
        { occurredOn: '2026-10-14', totalAmount: 30, count: 2 },
      ],
      money,
    );

    expect(bars.map((bar) => bar.date)).toEqual(['2026-10-12', '2026-10-13', '2026-10-14']);
    expect(bars.map((bar) => bar.label)).toEqual(['12/10', '13/10', '14/10']);
    expect(bars.map((bar) => bar.heightPercent)).toEqual([33, 0, 100]);
    expect(bars[0].title).toBe('12/10: $10 · 1 gasto');
    expect(bars[1].title).toBe('13/10: $0 · 0 gastos');
    expect(bars[2].title).toBe('14/10: $30 · 2 gastos');
  });
});
