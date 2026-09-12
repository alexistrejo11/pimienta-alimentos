# Plan de módulos (solo backend)

Estado: **decidido (orden)** · [01](01-decisiones-abiertas.md) cerrado · specs `02`–`06` alineadas · 2026-09-08

No incluye workers Android ni Fase 4 del POS. Implementar contra [contracts/](contracts/README.md).

**Progreso y checkboxes vivos:** [09-implementation-tracker.md](09-implementation-tracker.md) (marcar ahí; este doc es el orden/intención).

## Principio

Maestros → dispositivos/operadores → bootstrap → eventos/idempotencia → deltas → reportes. Cada corte: compila + IT viejos verdes.

## B0 — Docs y contratos

- [x] Decisiones D1–D10 en `01`.
- [x] Ejemplos en `contracts/`.
- [ ] Código: sustituir `PosController` vacío solo al empezar B2.

## B1 — Catálogo y sede (sin sync)

- Migración: `headquarter_items`, pos-settings, `storage_locations` + `LocationType.POS` + `headquarter_id`, unique barcode no reutilizable.
- Endpoints web pos-catalog / pos-settings.
- `PosSaleInventoryService` testeable sin HTTP device.

**Salida:** sede de cafetería configurable en PostgreSQL; tablet sigue con seed debug.

## B2 — Dispositivos y operadores

- Módulo `pos` hexagonal; reemplazar `PosController` plano.
- Tablas `pos_devices`, `pos_operators`, `pos_operator_headquarter`, `pos_enrollment_codes` (TTL 10 min).
- Access 15 min / refresh 90 d (techo 365); refresh hasheado y rotado.
- Reasignación = revoke + nuevo enroll.
- `POST enroll`, `refresh`, `GET me` + admin devices/operators.
- Security matchers + OpenAPI + IT (código expirado, revoke).

## B3 — Bootstrap

- `GET /pos/sync/bootstrap` = proyección plana + centavos ([pos-bootstrap.example.json](contracts/pos-bootstrap.example.json)).
- IT: otra sede no ve catálogo; revocado 403.

## B4 — Eventos

- Receipts unique `eventId`.
- `POST /pos/sync/events` + `PosSaleInventoryService`.
- `DUPLICATE` / `REQUIRES_REVIEW`.
- Prohibido `InventoryTransactionManagementUseCases.sale()`.

## B5 — Deltas

- `GET /pos/sync/changes?cursor=` según [sync-changes-response.example.json](contracts/sync-changes-response.example.json).

## B6 — Incidencias y reportes

- Accept incidents + reports bajo `/pos/admin/**`.

## B7 — Cliente móvil

Fase 4 en `mobile/pos`: HTTP, worker, outbox con payload, Keystore para refresh. Mapper desde seed decimal → contrato centavos.

## Definition of done

1. Enrolar device de sede de prueba.
2. Bootstrap con catálogo efectivo y operadores + `pinHash`.
3. Dos `SALE_CONFIRMED` iguales → una venta, segunda `DUPLICATE`, stock coherente (negativo permitido).
4. Incidencia resoluble por `ADMIN` sin mutar ticket.
5. IT MockMvc de lo anterior + inventory web intacto.
