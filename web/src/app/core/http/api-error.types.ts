/**
 * Mirrors {@link io.github.alexistrejo11.pimienta.shared.exception.api.FieldError}.
 */
export interface ApiFieldError {
  field: string;
  message: string;
}

/**
 * Mirrors {@link io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse}
 * (JSON body from {@link io.github.alexistrejo11.pimienta.config.web.GlobalExceptionHandler}).
 */
export interface ApiErrorResponse {
  errorCode: string;
  message: string;
  traceId: string;
  context?: Record<string, unknown> | null;
  fieldErrors?: ApiFieldError[] | null;
}
