# Telemetry integration follow-ups

## 2026-09-13: P2 health persistence and observability

- Health snapshots use server receipt time because the existing POST health contract does not carry a client timestamp.
- Dashboard online status uses a 45-minute freshness window (three missed 15-minute WorkManager reports). Confirm this with operations before treating it as a production SLO.
- Snapshot retention/archival is not included in P2 and should be scheduled before production volume grows substantially.

## 2026-09-16: Ingestion authorization and Grafana consumption

- Staff `MANAGER` JWTs can ingest web telemetry; device JWTs cannot. Public anonymous browser events remain intentionally disabled.
- Local Grafana dashboards and Prometheus alert rules are development-only. Production still needs private scrape of the management port, auth, TLS, and retention.
- No extra follow-ups from the ingestion integration pass beyond the existing snapshot-retention note.
