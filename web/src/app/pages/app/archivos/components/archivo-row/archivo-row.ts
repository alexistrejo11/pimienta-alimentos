import { Component, input, output } from '@angular/core';

import type { FileAssetResponse } from '../../../../../core/model/files/file.dto';
import { FILE_CATEGORY_LABELS } from '../../../../../core/model/files/file.enums';

/**
 * Fila de la tabla de archivos.
 * Renderiza metadatos del activo y acciones de descarga / eliminación.
 */
@Component({
  selector: '[app-archivo-row]',
  
  templateUrl: './archivo-row.html',
})
export class ArchivoRowComponent {
  readonly archivo = input.required<FileAssetResponse>();
  readonly canDelete = input(false);
  readonly downloading = input(false);

  readonly download = output<string>();
  readonly deleteFile = output<string>();

  categoryLabel(category: string): string {
    return FILE_CATEGORY_LABELS[category as keyof typeof FILE_CATEGORY_LABELS] ?? category;
  }

  fileIcon(contentType: string): string {
    if (contentType.includes('pdf')) return 'picture_as_pdf';
    if (
      contentType.includes('spreadsheet') ||
      contentType.includes('excel') ||
      contentType.includes('csv')
    )
      return 'table_chart';
    if (contentType.includes('image')) return 'image';
    if (contentType.includes('word') || contentType.includes('document')) return 'article';
    return 'description';
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  formatDate(iso: string): string {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('es-MX', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  onDownload(): void {
    this.download.emit(this.archivo().id);
  }

  onDelete(): void {
    this.deleteFile.emit(this.archivo().id);
  }
}
