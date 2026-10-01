import { Injectable } from '@angular/core';

export interface PosLabel {
  sku: string;
  name: string;
  price: number;
}

/** Target bar height on paper; width follows JsBarcode output (no CSS stretch). */
export const POS_LABEL_BARCODE_HEIGHT_MM = 18;

/** Module width in px for JsBarcode (1–1.5 keeps bars scannable on office printers). */
export const POS_LABEL_BARCODE_MODULE_WIDTH = 1.25;

/** Opens a print-only sheet with internal SKU Code 128 labels. */
@Injectable({ providedIn: 'root' })
export class PosLabelPrintService {
  async print(labels: PosLabel[]): Promise<void> {
    if (!labels.length || typeof window === 'undefined') return;
    // Open synchronously so the browser keeps the user-gesture popup grant.
    const popup = window.open('', '_blank', 'popup,width=900,height=700');
    if (!popup) {
      window.alert('Permita ventanas emergentes para imprimir etiquetas.');
      return;
    }

    const { default: JsBarcode } = await import('jsbarcode');
    const content = labels
      .map(
        (label, index) =>
          `<article class="label"><strong>${escapeHtml(label.name)}</strong><div class="barcode"><svg class="barcode-svg" id="barcode-${index}"></svg></div><span>${escapeHtml(label.sku)} · $${label.price.toFixed(2)}</span></article>`,
      )
      .join('');

    const styles = `
@page{size:letter;margin:8mm}
body{font-family:Arial,sans-serif;margin:0;color:#111}
.sheet{
  display:grid;
  grid-template-columns:repeat(3,1fr);
  gap:4mm 3mm;
  align-content:start;
}
.label{
  box-sizing:border-box;
  break-inside:avoid;
  page-break-inside:avoid;
  text-align:center;
  padding:3mm 2mm;
  border:1px dashed #aaa;
}
.label strong{
  display:block;
  font-size:10px;
  margin-bottom:1.5mm;
  line-height:1.15;
  max-height:2.4em;
  overflow:hidden;
}
.barcode{
  display:flex;
  justify-content:center;
  align-items:flex-start;
  min-height:${POS_LABEL_BARCODE_HEIGHT_MM}mm;
  margin:0 auto;
}
.barcode-svg{
  display:block;
  height:${POS_LABEL_BARCODE_HEIGHT_MM}mm;
  width:auto;
  max-width:none;
}
.label span{display:block;font-size:9px;margin-top:1mm;line-height:1.2}
@media print{
  .label{border-color:#ddd}
}
`;

    popup.document.write(
      `<!doctype html><html><head><title>Etiquetas POS</title><style>${styles}</style></head><body><main class="sheet">${content}</main></body></html>`,
    );
    popup.document.close();

    for (let index = 0; index < labels.length; index++) {
      const label = labels[index];
      const svgNode = popup.document.getElementById(`barcode-${index}`);
      if (!svgNode || !label || svgNode.tagName.toLowerCase() !== 'svg') continue;
      const svg = svgNode as unknown as SVGSVGElement;
      try {
        JsBarcode(svg, label.sku, {
          format: 'CODE128',
          displayValue: false,
          margin: 4,
          width: POS_LABEL_BARCODE_MODULE_WIDTH,
          height: 40,
        });
        svg.setAttribute('preserveAspectRatio', 'xMidYMid meet');
        scaleBarcodeDownIfWiderThanLabel(svg);
      } catch {
        const fallback = popup.document.createElement('span');
        fallback.textContent = label.sku;
        fallback.setAttribute('style', 'display:block;font-size:11px;font-weight:600;margin:4mm 0');
        svg.replaceWith(fallback);
      }
    }

    popup.focus();
    await waitForPaint(popup);
    popup.print();
  }
}

/**
 * If the SKU is long, the natural width at 18mm height may exceed the grid cell.
 * Uniform scale keeps bar proportions; never set a fixed CSS width on the SVG.
 */
export function scaleBarcodeDownIfWiderThanLabel(svg: SVGSVGElement): void {
  const label = svg.closest('.label');
  if (!(label instanceof HTMLElement)) return;

  svg.style.transform = '';
  svg.style.transformOrigin = 'top center';

  const availableWidth = label.clientWidth;
  if (availableWidth <= 0) return;

  const renderedWidth = svg.getBoundingClientRect().width;
  if (renderedWidth <= availableWidth) return;

  const scale = availableWidth / renderedWidth;
  svg.style.transform = `scale(${scale})`;
}

function waitForPaint(popup: Window): Promise<void> {
  return new Promise((resolve) => {
    popup.requestAnimationFrame(() => {
      popup.requestAnimationFrame(() => {
        window.setTimeout(resolve, 50);
      });
    });
  });
}

function escapeHtml(value: string): string {
  return value.replace(
    /[&<>'"]/g,
    (character) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character] ??
      character,
  );
}
