import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  // Todas las rutas del workspace (/app/**) se renderizan en el cliente.
  // Son rutas autenticadas que dependen de llamadas HTTP al backend;
  // hacer prerender en el servidor causaría timeouts al no haber backend disponible.
  { path: 'app/**', renderMode: RenderMode.Client },
  // El resto de las rutas públicas (landing, portal, etc.) se prerenderiza.
  {
    path: '**',
    renderMode: RenderMode.Prerender,
  },
];
