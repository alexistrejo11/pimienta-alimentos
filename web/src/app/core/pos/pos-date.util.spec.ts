import { localDateEndInstant, localDateStartInstant, todayInstantRange } from './pos-date.util';

const MEXICO = 'America/Mexico_City';

describe('pos date instants', () => {
  it('cuts a calendar day at Mexico City midnight, not UTC midnight', () => {
    expect(localDateStartInstant('2026-10-15', MEXICO)).toBe('2026-10-15T06:00:00.000Z');
    expect(localDateEndInstant('2026-10-15', MEXICO)).toBe('2026-10-16T06:00:00.000Z');
  });

  it('builds today from the client zone instead of the UTC date', () => {
    const evening = new Date('2026-10-15T05:30:00.000Z');
    expect(todayInstantRange(evening, MEXICO)).toEqual({
      from: '2026-10-14T06:00:00.000Z',
      to: '2026-10-15T06:00:00.000Z',
    });
  });
});
