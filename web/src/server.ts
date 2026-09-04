import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import { join } from 'node:path';

const browserDistFolder = join(import.meta.dirname, '../browser');

function parseAllowedHosts(): string[] {
  const raw = process.env['ALLOWED_HOSTS'] ?? process.env['NG_ALLOWED_HOSTS'] ?? '';
  return raw
    .split(',')
    .map((h) => h.trim())
    .filter(Boolean);
}

function requireEnv(name: string): string {
  const value = process.env[name]?.trim();
  if (!value) {
    console.error(`Missing required environment variable: ${name}`);
    process.exit(1);
  }
  return value;
}

const allowedHosts = parseAllowedHosts();
if (allowedHosts.length && !process.env['NG_ALLOWED_HOSTS']) {
  process.env['NG_ALLOWED_HOSTS'] = allowedHosts.join(',');
}

const app = express();
const angularApp = new AngularNodeAppEngine(
  allowedHosts.length ? { allowedHosts } : undefined,
);

/**
 * Runtime config for the browser bundle (API base URL).
 * Must stay ahead of static + SSR handlers.
 */
app.get('/runtime-config.js', (_req, res) => {
  const apiBaseUrl = process.env['API_BASE_URL']?.trim();
  if (!apiBaseUrl) {
    res.status(500).type('text/plain').send('API_BASE_URL is not configured');
    return;
  }

  res
    .type('application/javascript; charset=utf-8')
    .set('Cache-Control', 'no-store')
    .send(
      `window.__PIMIENTA_CONFIG__=${JSON.stringify({ apiBaseUrl })};`,
    );
});

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point, or it is ran via PM2.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url) || process.env['pm_id']) {
  requireEnv('API_BASE_URL');
  requireEnv('ALLOWED_HOSTS');

  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
