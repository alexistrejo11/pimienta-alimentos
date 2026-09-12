# Observability Implementation Plan

Status: planned. The target architecture is documented in [observability.md](observability.md). Complete phases in order; each phase is independently reviewable and deployable.

## Phase 0: Rules and Baseline

- [x] Confirm the architectural direction: backend metrics use Prometheus pull; browser and POS telemetry use authenticated push to the backend.
- [x] Define the telemetry payload limits, rate limits, event severity vocabulary, and redaction policy in [observability.md](observability.md).
- [x] Document allowed Loki labels and prohibited Prometheus labels in [observability.md](observability.md).
- [x] Define the rule that telemetry is best effort and must not affect business operations or POS sales.
- [x] Record the pre-observability baseline: backend already had Actuator, JSON logging, MDC `traceId`, and an audit interceptor; Angular only logged to the console; POS had no telemetry.
- [ ] Confirm log retention, metric retention, dashboard access roles, and alert recipients for local and production environments.
- [ ] Define the production observability network boundary and restrict public access to Grafana, Loki, and `/actuator/prometheus`.
- [ ] Record baseline API request/error rates from a representative production period.

**Exit condition:** security, retention, cardinality, ownership, and alerting decisions are explicit before production data starts being retained. The technical implementation may proceed in parallel, but production rollout remains blocked by the unchecked operational decisions above.

## Phase 1: Backend Logging and Metrics Foundation

- [x] Keep Spring Boot Actuator and Micrometer Prometheus registry enabled.
- [x] Move production management endpoints to a separate internal container port; do not publish that port through the application proxy.
- [x] Preserve `X-Trace-Id` request/response correlation and include it in structured backend logs through MDC.
- [x] Replace string-embedded audit JSON with structured audit fields.
- [x] Change audit selection to mutations and security-sensitive actions only; exclude ordinary reads.
- [x] Remove raw query strings and unsafe headers from audit metadata; avoid storing exception messages.
- [x] Add unit coverage for audit filtering and redaction; local `dev` permits Prometheus scraping while production keeps Actuator protected and isolated on its internal management port.

**Exit condition:** backend logs are safe JSON, audit scope is intentional, and Prometheus can scrape the backend privately.

## Phase 2: Local Observability Stack

- [x] Create `observability/compose.yaml` with Prometheus, Loki, Grafana, and Grafana Alloy.
- [x] Configure the stack to join `pimienta-net` and discover local backend and web containers.
- [x] Add Prometheus scrape configuration for the backend Actuator endpoint.
- [x] Configure Alloy to parse backend and SSR stdout JSON logs and send them to Loki.
- [x] Add persisted local volumes, a committed `.env.example`, and a README with start, stop, reset, and access instructions.
- [x] Provision Grafana data sources and an initial backend dashboard.
- [x] Verify backend logs appear in Loki and the backend scrape target is healthy in Prometheus; Grafana datasources and the backend dashboard are provisioned.

**Exit condition:** a developer can start the local stack independently and inspect backend metrics and logs in Grafana.

## Phase 3: Backend Telemetry Ingestion

- [x] Define versioned DTOs for browser events, POS log batches, and POS health snapshots.
- [x] Add a dedicated telemetry module or adapter with thin controllers, validation, payload limits, and independent rate limits.
- [x] Add authenticated web ingestion for workspace events; anonymous public-site ingestion is not enabled in this phase.
- [x] Add POS ingestion protected by device JWT scope; derive device and site identity from authentication.
- [x] Normalize accepted events to structured `web-client` and `pos-client` log records for Alloy/Loki.
- [x] Add failure-safe behavior: telemetry ingestion errors never fail a business request or POS sync event batch.
- [x] Add Micrometer counters for ingestion outcomes and bounded POS health states.
- [x] Add unit coverage for client event metric emission and bounded source handling.
- [ ] Add integration tests for authorization, validation, rate limits, identity derivation, redaction, and metric aggregation.

**Exit condition:** clients have secure server-owned endpoints and the backend exposes ingestion and fleet-health metrics without high-cardinality labels.

## Phase 4: Angular Browser Telemetry

- [x] Add a browser-only telemetry service that respects SSR and hydration boundaries.
- [x] Capture Angular uncaught errors, network errors, HTTP 5xx responses, and failed write operations.
- [x] Include route, browser metadata, timestamp, safe error summary, stack trace cap, and available `X-Trace-Id`.
- [x] Redact line breaks and cap error text before network output; do not send request or response bodies.
- [x] Use a direct `HttpBackend` client so telemetry requests bypass API logging and session interceptors, preventing recursive reports.
- [x] Send asynchronously and swallow telemetry transport errors; never block rendering, navigation, authentication, or a user operation.
- [x] Add unit coverage for authenticated event delivery and the no-token fail-closed behavior.
- [ ] Add further unit tests for redaction, browser-only execution, error classification, and recursion prevention.

**Exit condition:** a controlled browser error and an API 5xx appear in Loki with useful, non-sensitive context.

## Phase 5: Android POS Telemetry

- [x] Add a versioned Room telemetry entity, DAO, migration, maximum queue size, and oldest-first retention.
- [x] Implement a small POS telemetry logger for warning/error and selected lifecycle events.
- [x] Capture sync failures and state transitions without storing tokens, PINs, sales payloads, or full server responses.
- [x] Add a compact device health snapshot: app version, sync state, outbox count/age, and hardware error counters when available.
- [x] Extend `SyncWorker` to upload telemetry batches and health snapshots after business event synchronization, using the existing authenticated API client.
- [x] Preserve offline-first behavior: a telemetry failure retries independently and never blocks sales or the POS outbox.
- [x] Add unit coverage for telemetry transport authentication failures and unavailable endpoints; compile instrumented Room coverage for bounded retention and batches.
- [ ] Run the Room instrumentation test on an emulator or physical Android device; this requires `adb` and is not available in the current environment.

**Exit condition:** a disconnected tablet retains telemetry locally and uploads bounded batches after reconnecting without affecting sales.

## Phase 6: Dashboards, Alerts, and Production Rollout

- [ ] Provision API, backend error, browser error, POS fleet-health, and telemetry-ingestion Grafana dashboards.
- [ ] Create alert rules for unavailable API scrape, elevated 5xx rate, stale POS synchronization, aging POS outbox, and abnormal telemetry rejection rate.
- [ ] Configure production Loki/Prometheus retention, persistent storage, backup, TLS, and role-restricted Grafana access.
- [ ] Deploy observability infrastructure separately from application services with production-specific secrets and endpoints.
- [ ] Run a production smoke test: scrape API metrics, inspect a redacted backend error, receive a browser event, and receive a POS event.
- [ ] Write a short operational runbook for alert triage, dashboard ownership, and retention changes.

**Exit condition:** production telemetry is private, durable, queryable, actionable, and verified end-to-end.

## Deferred: Distributed Tracing

- [ ] Reassess whether logs plus metrics and `X-Trace-Id` leave unresolved diagnosis gaps.
- [ ] If justified, introduce OpenTelemetry tracing with sampling, exporter configuration, context propagation, and trace data retention.
- [ ] Do not add OpenTelemetry merely to replace the logging/metrics baseline.
