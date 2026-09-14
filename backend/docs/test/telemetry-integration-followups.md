# Telemetry integration follow-ups

## 2026-09-13: P2 health persistence and observability

- Health snapshots use server receipt time because the existing POST health contract does not carry a client timestamp.
- Dashboard online status uses a five-minute freshness window; this should become a product/configuration decision if device polling intervals change.
- Snapshot retention/archival is not included in P2 and should be scheduled before production volume grows substantially.
