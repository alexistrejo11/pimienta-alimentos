# Contratos JSON (Device API)

Estado: **decidido** · schemaVersion de ejemplos = `1` · 2026-09-08

Alineados a [01-decisiones-abiertas.md](../01-decisiones-abiertas.md). Dinero: centavos enteros. IDs Long: string decimal. Hechos: UUID.

| Archivo | Uso |
|---------|-----|
| [device-enroll-request.example.json](device-enroll-request.example.json) | `POST /pos/devices/enroll` |
| [device-enroll-response.example.json](device-enroll-response.example.json) | respuesta enroll |
| [pos-bootstrap.example.json](pos-bootstrap.example.json) | `GET /pos/sync/bootstrap` |
| [sync-changes-response.example.json](sync-changes-response.example.json) | `GET /pos/sync/changes` |
| [event-envelope.example.json](event-envelope.example.json) | sobre de un evento |
| [sync-events-request.example.json](sync-events-request.example.json) | `POST /pos/sync/events` (incluye `SALE_CONFIRMED`) |
| [sync-events-response.example.json](sync-events-response.example.json) | resultados por `eventId` |
| [jwt-device-claims.example.json](jwt-device-claims.example.json) | claims del access token (documental) |

Reglas:

- No usar `float`/`double` ni JSON number decimal para dinero.
- `siteId` / `site.id` = `Headquarter.id` en string decimal.
- Productos del bootstrap son proyección plana (`inventory_items` + `headquarter_items` + stock en `storage_locations` tipo POS).
- Access device 15 min (900 s); refresh default 180 días (15552000 s); techo 365 días (31536000 s).
- Enrolamiento: código único, un uso, 10 minutos.
- El seed debug Android aún usa strings decimales; el cliente mapeará al hablar con este contrato.
