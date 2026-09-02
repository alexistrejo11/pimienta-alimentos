/**
 * Base path for the Spring Boot API (see {@code @RequestMapping} on controllers).
 * Overridden at Docker build time via `--define __API_BASE_URL__=...`.
 */
declare const __API_BASE_URL__: string | undefined;

export const API_BASE_URL =
  typeof __API_BASE_URL__ !== 'undefined' && __API_BASE_URL__
    ? __API_BASE_URL__
    : 'http://localhost:8080/api/v1';
