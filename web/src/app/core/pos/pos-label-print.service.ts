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
    if (!popup) return;
    const { default: JsBarcode } = await import('jsbarcode');
    const content = labels.map((label, index) => `<article class="label"><strong>${escapeHtml(label.name)}</strong><svg id="barcode-${index}"></svg><span>${escapeHtml(label.sku)} · $${label.price.toFixed(2)}</span></article>`).join('');
    popup.document.write(`<!doctype html><html><head><title>Etiquetas POS</title><style>@page{margin:8mm}body{font-family:Arial,sans-serif;margin:0}.sheet{display:grid;grid-template-columns:repeat(3,1fr);gap:8mm}.label{break-inside:avoid;text-align:center;padding:4mm 2mm;border:1px dashed #aaa;min-height:30mm}.label strong{display:block;font-size:12px;margin-bottom:2mm}.label svg{width:100%;height:17mm}.label span{display:block;font-size:10px;margin-top:1mm}@media print{.label{border-color:#ddd}}</style></head><body><main class="sheet">${content}</main></body></html>`);
    popup.document.close();
    labels.forEach((label, index) => {
      const svg = popup.document.getElementById(`barcode-${index}`);
      if (svg) JsBarcode(svg, label.sku, { format: 'CODE128', displayValue: false, margin: 0, height: 48 });
    });
    popup.focus();
    window.setTimeout(() => popup.print(), 250);
  }
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>'"]/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' })[character] ?? character);
}
