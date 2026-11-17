import type { HeadQuarterResponse } from '../../src/app/core/model/headquarter/headquarter.dto';

const stamp = '2026-01-10T08:00:00Z';

export function headquarter(overrides: Partial<HeadQuarterResponse> = {}): HeadQuarterResponse {
  return {
    id: 10,
    name: 'Sede Centro',
    address: 'Calle 1',
    description: 'Principal',
    createdAt: stamp,
    updatedAt: stamp,
    deletedAt: null,
    version: 0,
    ...overrides,
  };
}

export const sedeCentro = headquarter();
export const sedeNorte = headquarter({ id: 11, name: 'Sede Norte', address: 'Calle 2' });
