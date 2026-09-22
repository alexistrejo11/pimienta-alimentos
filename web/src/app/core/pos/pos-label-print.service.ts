import { Injectable } from '@angular/core';

export interface PosLabel {
  sku: string;
  name: string;
  price: number;
}

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
          `<article class="label"><strong>${escapeHtml(label.name)}</strong><svg id="barcode-${index}"></svg><span>${escapeHtml(label.sku)} · $${label.price.toFixed(2)}</span></article>`,
      )
      .join('');

    const styles = `
@page{margin:8mm}
body{font-family:Arial,sans-serif;margin:0;color:#111}
.sheet{display:flex;flex-wrap:wrap;gap:6mm;align-content:flex-start}
.label{
  box-sizing:border-box;
  width:calc(33.333% - 4mm);
  break-inside:avoid;
  page-break-inside:avoid;
  text-align:center;
  padding:4mm 2mm;
  border:1px dashed #aaa;
  min-height:32mm;
}
.label strong{display:block;font-size:12px;margin-bottom:2mm;line-height:1.2}
.label svg{width:100%;height:17mm;display:block}
.label span{display:block;font-size:10px;margin-top:1mm}
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
      const svg = popup.document.getElementById(`barcode-${index}`);
      if (!svg || !label) continue;
      try {
        JsBarcode(svg, label.sku, {
          format: 'CODE128',
          displayValue: false,
          margin: 0,
          height: 48,
        });
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
