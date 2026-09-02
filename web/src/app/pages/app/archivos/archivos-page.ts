import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { ArchivoRowComponent } from './components/archivo-row/archivo-row';
import { FileService } from '../../../core/files/file.service';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { UserProfileService } from '../../../core/user/user-profile.service';
import { FILE_MODULE_OPTIONS, FileCategory } from '../../../core/model/files/file.enums';
import { parseApiError, ParsedApiError } from '../../../core/http/parse-api-error';
import { FileAssetResponse } from '../../../core/model/files/file.dto';
import { PageMetadata } from '../../../core/model';



const MAX_FILE_BYTES = 50 * 1024 * 1024;

/**
 * Gestor de archivos de la empresa: listado, carga y descarga según rol.
 * Admin usa /files/management; manager usa /files/resources por módulo.
 */
@Component({
  selector: 'app-archivos-page',
  
  imports: [PageHeaderComponent, DataStateComponent, ArchivoRowComponent, FormsModule],
  templateUrl: './archivos-page.html',
})
export class ArchivosPageComponent implements OnInit {
  private readonly files = inject(FileService);
  private readonly profile = inject(UserProfileService);

  readonly moduleOptions = FILE_MODULE_OPTIONS;

  readonly profileLoading = signal(true);
  readonly isAdmin = signal(false);
  readonly userId = signal<number | null>(null);

  readonly loading = signal(false);
  readonly error = signal<ParsedApiError | null>(null);
  readonly items = signal<FileAssetResponse[]>([]);
  readonly metadata = signal<PageMetadata | null>(null);
  readonly currentPage = signal(0);

  readonly showUpload = signal(false);
  readonly uploading = signal(false);
  readonly uploadError = signal<string | null>(null);
  readonly uploadSuccess = signal<string | null>(null);

  readonly downloadingId = signal<string | null>(null);
  readonly deletingId = signal<string | null>(null);

  // ── Filtros ───────────────────────────────────────────────────────────────
  filterCategory: FileCategory | '' = '';
  filterModule = 'crm';
  filterEntityType = '';
  filterEntityId = '';
  filterName = '';
  filterContentType = '';
  filterCreatedFrom = '';
  filterCreatedTo = '';

  // ── Upload form ─────────────────────────────────────────────────────────────
  uploadCategory: FileCategory = 'COMPANY';
  uploadModule = 'crm';
  uploadDescription = '';
  selectedFile: File | null = null;

  readonly canDelete = computed(() => this.isAdmin());

  ngOnInit(): void {
    this.profileLoading.set(true);
    this.profile
      .getProfile()
      .pipe(finalize(() => this.profileLoading.set(false)))
      .subscribe({
        next: (user) => {
          this.isAdmin.set(user.roles.includes('ROLE_ADMIN'));
          this.userId.set(user.id);
          this.reloadList();
        },
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  toggleUpload(): void {
    this.showUpload.update((v) => !v);
    this.uploadError.set(null);
    this.uploadSuccess.set(null);
  }

  applyFilters(): void {
    this.currentPage.set(0);
    this.reloadList();
  }

  reloadList(): void {
    if (this.profileLoading()) return;

    if (!this.isAdmin() && !this.filterModule.trim()) {
      this.error.set(null);
      this.items.set([]);
      this.metadata.set(null);
      return;
    }

    this.error.set(null);
    this.loading.set(true);

    const page = this.currentPage();
    const size = 20;

    if (this.isAdmin()) {
      this.files
        .searchManagement({
          page,
          size,
          category: this.filterCategory || undefined,
          module: this.filterModule.trim() || undefined,
          entityType: this.filterEntityType.trim() || undefined,
          entityId: this.parseOptionalInt(this.filterEntityId),
          originalNameContains: this.filterName.trim() || undefined,
          contentTypeContains: this.filterContentType.trim() || undefined,
          createdFrom: this.toIsoStart(this.filterCreatedFrom),
          createdTo: this.toIsoEnd(this.filterCreatedTo),
        })
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (p) => {
            this.items.set(p.items);
            this.metadata.set(p.metadata);
          },
          error: (err: unknown) => this.error.set(parseApiError(err)),
        });
    } else {
      this.files
        .searchResources({
          page,
          size,
          module: this.filterModule.trim(),
          entityType: this.filterEntityType.trim() || undefined,
          entityId: this.parseOptionalInt(this.filterEntityId),
          originalNameContains: this.filterName.trim() || undefined,
          contentTypeContains: this.filterContentType.trim() || undefined,
          createdFrom: this.toIsoStart(this.filterCreatedFrom),
          createdTo: this.toIsoEnd(this.filterCreatedTo),
        })
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (p) => {
            this.items.set(p.items);
            this.metadata.set(p.metadata);
          },
          error: (err: unknown) => this.error.set(parseApiError(err)),
        });
    }
  }

  prevPage(): void {
    const meta = this.metadata();
    if (meta?.hasPrevious) {
      this.currentPage.update((p) => p - 1);
      this.reloadList();
    }
  }

  nextPage(): void {
    const meta = this.metadata();
    if (meta?.hasNext) {
      this.currentPage.update((p) => p + 1);
      this.reloadList();
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.selectedFile = file;
    this.uploadError.set(null);
    if (file && file.size > MAX_FILE_BYTES) {
      this.uploadError.set('El archivo supera el límite de 50 MB.');
      this.selectedFile = null;
      input.value = '';
    }
  }

  submitUpload(): void {
    this.uploadError.set(null);
    this.uploadSuccess.set(null);

    if (!this.selectedFile) {
      this.uploadError.set('Selecciona un archivo para subir.');
      return;
    }
    if (this.selectedFile.size > MAX_FILE_BYTES) {
      this.uploadError.set('El archivo supera el límite de 50 MB.');
      return;
    }

    if (this.isAdmin() && this.uploadCategory === 'RESOURCE' && !this.uploadModule.trim()) {
      this.uploadError.set('Los archivos de tipo Recurso requieren un módulo.');
      return;
    }
    if (!this.isAdmin() && !this.filterModule.trim()) {
      this.uploadError.set('Selecciona un módulo antes de subir.');
      return;
    }

    this.uploading.set(true);

    const description = this.uploadDescription.trim() || undefined;
    const uploadedByUserId = this.userId() ?? undefined;

    const upload$ = this.isAdmin()
      ? this.files.uploadManagement(this.selectedFile, {
          category: this.uploadCategory,
          module:
            this.uploadCategory === 'RESOURCE' ? this.uploadModule.trim() : undefined,
          description,
          uploadedByUserId,
        })
      : this.files.uploadResource(this.selectedFile, {
          module: this.filterModule.trim(),
          description,
          uploadedByUserId,
        });

    upload$
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: (asset) => {
          this.uploadSuccess.set(`"${asset.originalName}" subido correctamente.`);
          this.selectedFile = null;
          this.uploadDescription = '';
          this.showUpload.set(false);
          this.reloadList();
        },
        error: (err: unknown) => this.uploadError.set(parseApiError(err).message),
      });
  }

  downloadFile(id: string): void {
    this.downloadingId.set(id);
    const download$ = this.isAdmin()
      ? this.files.getManagementDownloadUrl(id)
      : this.files.getResourceDownloadUrl(id);

    download$
      .pipe(finalize(() => this.downloadingId.set(null)))
      .subscribe({
        next: (res) => window.open(res.url, '_blank', 'noopener,noreferrer'),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  deleteFile(id: string): void {
    const item = this.items().find((f) => f.id === id);
    const name = item?.originalName ?? 'este archivo';
    if (!confirm(`¿Eliminar "${name}"? Esta acción no se puede deshacer.`)) return;

    this.deletingId.set(id);
    this.files
      .delete(id)
      .pipe(finalize(() => this.deletingId.set(null)))
      .subscribe({
        next: () => this.reloadList(),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }

  private parseOptionalInt(value: string): number | undefined {
    const trimmed = value.trim();
    if (!trimmed) return undefined;
    const n = Number(trimmed);
    return Number.isNaN(n) ? undefined : n;
  }

  private toIsoStart(date: string): string | undefined {
    if (!date.trim()) return undefined;
    return `${date.trim()}T00:00:00`;
  }

  private toIsoEnd(date: string): string | undefined {
    if (!date.trim()) return undefined;
    return `${date.trim()}T23:59:59`;
  }
}
