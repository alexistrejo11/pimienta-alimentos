import { HttpInterceptorFn } from '@angular/common/http';

const TRACE_ID_HEADER = 'X-Trace-Id';

function ssrTraceId(): string | undefined {
  const als = (globalThis as Record<symbol, { getStore?: () => { traceId?: string } } | undefined>)[
    Symbol.for('pimienta.traceAls')
  ];
  return als?.getStore?.()?.traceId;
}

/** Sends a canonical UUID on every API call so logs and telemetry share one id. */
export const traceIdInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.includes('/api/')) {
    return next(req);
  }
  if (req.headers.get(TRACE_ID_HEADER)) {
    return next(req);
  }
  const traceId = ssrTraceId() ?? crypto.randomUUID();
  return next(req.clone({ setHeaders: { [TRACE_ID_HEADER]: traceId } }));
};
