/**
 * Mirrors {@link io.github.alexistrejo11.pimienta.shared.web.PageMetadata} (backend JSON).
 */
export interface PageMetadata {
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

/**
 * Mirrors {@link io.github.alexistrejo11.pimienta.shared.web.PagedResponse} — used by most list endpoints.
 */
export interface PagedResponse<T> {
  items: T[];
  metadata: PageMetadata;
}

/**
 * Response shape for {@code GET /api/v1/headquarters} (Spring Data {@link org.springframework.data.domain.Page}).
 * Other paginated resources use {@link PagedResponse} instead.
 */
export interface SpringDataPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}
