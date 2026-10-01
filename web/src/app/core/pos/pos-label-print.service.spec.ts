import { vi } from 'vitest';

import { scaleBarcodeDownIfWiderThanLabel } from './pos-label-print.service';

describe('scaleBarcodeDownIfWiderThanLabel', () => {
  it('does not scale when the barcode fits the label cell', () => {
    const label = document.createElement('article');
    label.className = 'label';
    Object.defineProperty(label, 'clientWidth', { value: 200 });

    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.classList.add('barcode-svg');
    label.append(svg);
    document.body.append(label);

    vi.spyOn(svg, 'getBoundingClientRect').mockReturnValue({
      width: 120,
      height: 40,
      top: 0,
      left: 0,
      right: 120,
      bottom: 40,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    } as DOMRect);

    scaleBarcodeDownIfWiderThanLabel(svg);

    expect(svg.style.transform).toBe('');

    label.remove();
  });

  it('applies uniform scale when the barcode is wider than the cell', () => {
    const label = document.createElement('article');
    label.className = 'label';
    Object.defineProperty(label, 'clientWidth', { value: 100 });

    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    label.append(svg);
    document.body.append(label);

    vi.spyOn(svg, 'getBoundingClientRect').mockReturnValue({
      width: 200,
      height: 40,
      top: 0,
      left: 0,
      right: 200,
      bottom: 40,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    } as DOMRect);

    scaleBarcodeDownIfWiderThanLabel(svg);

    expect(svg.style.transform).toBe('scale(0.5)');
    expect(svg.style.transformOrigin).toBe('top center');

    label.remove();
  });
});
