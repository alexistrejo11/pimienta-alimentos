# Tracker de implementación POS (backend)

Estado: **activo** · fuente de progreso · 2026-09-08

Documento **vivo** para trackear alcance y avance. El orden y la intención de cada corte viven en [07-plan-de-modulos.md](07-plan-de-modulos.md). Aquí solo se marca qué está hecho, qué falta y qué queda fuera.

**Cómo usarlo:** al cerrar un ítem, cambia `[ ]` → `[x]` y anota fecha/PR si quieres. No reabrir D1–D10 sin nota en [01](01-decisiones-abiertas.md). Fuera de alcance: [08](08-fuera-de-alcance.md).

## Mapa rápido

| Fase | Nombre | Estado | Specs | Empezar cuando… |
|------|--------|--------|-------|-----------------|
| **B0** | Docs y contratos | casi cerrado | `00`–`08`, `contracts/` | — |
| **B1** | Catálogo y sede (sin Device API) | **hecho** | `03`, `04`, `06` | — |
| **B2** | Dispositivos y operadores | **hecho** | `05`, `06`, D6–D7 | B1 verde |
| **B3** | Bootstrap | **hecho** | `05`, contracts bootstrap | B2 verde |
| **B4** | Eventos + inventario POS | **hecho** | `05`, D4, D10 | B3 verde |
| **B5** | Deltas | **hecho** | `05`, contracts changes | B4 verde |
| **B6** | Incidencias y reportes | **hecho** | `06` | B4+ (puede solaparse con B5) |
| **B7** | Cliente móvil | fuera de este repo backend | `mobile/pos` Fase 4 | B3 mínimo; ideal B5 |

Principio (de `07`): maestros → devices/operators → bootstrap → eventos/idempotencia → deltas → reportes. Cada corte: **compila + IT viejos verdes**.

---

## B0 — Docs y contratos

Specs listas; no hay más diseño bloqueante.

- [x] Decisiones D1–D10 (+ D11 tablas) en `01`
- [x] Diseño `02`–`06` alineado
- [x] Ejemplos en `contracts/`
- [x] Fuera de alcance en `08`
- [x] Plan de cortes en `07`
- [x] Sustituir `PosController` vacío — **solo al empezar B2** (no hacerlo en B1)

**Salida B0:** contrato servidor listo; código POS sigue siendo stub.

---

## B1 — Catálogo y sede (sin sync)

Extiende `headquarter` + `inventory`. **No** Device API todavía. Tablet sigue con seed debug.

### Migraciones / modelo

- [x] Tabla `headquarter_items` (`HeadquarterItem`: sale_category, sale_price, available, stock_policy, negative_stock_limit, version/updated_at)
- [x] Pos-settings / `PosOperationalConfig` por sede (moneda, umbrales catálogo, categorías monto abierto, límite negativo default)
- [x] `LocationType.POS` + `headquarter_id` en `storage_locations` (código estable `POS-{id}`)
- [x] Unique barcode global `WHERE barcode IS NOT NULL` (no reutilizable); limpiar duplicados antes del índice
- [x] Al habilitar POS en sede: asegurar location `POS` canónica

### API web (staff JWT)

- [x] `GET/PUT /api/v1/headquarters/{id}/pos-settings`
- [x] List + `GET/PUT /api/v1/headquarters/{id}/pos-catalog/{itemId}`
- [x] Barcode conflicto → **409**
- [x] OpenAPI `Doc*` + IT MockMvc de catálogo/settings

### Aplicación inventario POS (sin HTTP device)

- [x] `PosSaleInventoryService` (o equivalente en application) **testeable sin** `/pos/sync/**`
- [x] Acepta negativo solo en location `POS`; no usa `InventoryTransactionManagementUseCases.sale()`

**Salida B1:** sede de cafetería configurable en PostgreSQL; stock POS de sede existe; tablet aún offline/seed.

**Refs:** [03](03-modelo-de-dominio-backend.md), [04](04-impacto-en-modulos-existentes.md), [06](06-admin-api-web.md) § catálogo.

---

## B2 — Dispositivos y operadores

Módulo `pos` hexagonal real. Reemplaza el `PosController` plano.

### Persistencia

- [x] Tablas `pos_devices`, `pos_operators`, `pos_operator_headquarter`, `pos_enrollment_codes` (TTL **10 min**, un uso)
- [x] Refresh tokens hasheados + rotación (Redis o tabla)

### Auth device

- [x] Access **15 min** / refresh **90 d** (techo **365 d**); claims `deviceId`, `headquarterId`, `typ=device`, `scope=pos:sync`
- [x] Security matchers: Device JWT ≠ staff JWT
- [x] Reasignación = **revoke** + nuevo enroll (sin transfer)

### Endpoints

- [x] `POST /api/v1/pos/devices/enroll`
- [x] `POST /api/v1/pos/devices/refresh`
- [x] `GET /api/v1/pos/devices/me`
- [x] Admin: operators CRUD/assign, `POST .../enrollment-codes`, devices list + revoke
- [x] OpenAPI + IT (código expirado, reusado, revoke → 401/403)

**Salida B2:** tablet de prueba puede enrolarse y renovar tokens; sin bootstrap aún.

**Refs:** [05](05-device-api.md), [06](06-admin-api-web.md) § operadores/devices, contracts enroll/JWT.

---

## B3 — Bootstrap

- [x] `GET /api/v1/pos/sync/bootstrap` — proyección plana, dinero en **centavos**
- [x] Solo catálogo/operadores de la sede del device
- [x] IT: otra sede no ve catálogo; device `REVOKED` → 403
- [x] Alinear a [contracts/pos-bootstrap.example.json](contracts/pos-bootstrap.example.json)

**Salida B3:** tablet enrolada puede bajar maestros reales (aún sin subir ventas).

---

## B4 — Eventos e idempotencia

- [x] Persistencia receipts / hechos con `eventId` unique (+ `saleId` unique)
- [x] `POST /api/v1/pos/sync/events` — lote por `deviceSequence`, respuesta por `eventId`
- [x] Pipeline: registrar eventId → venta+líneas snapshot → `PosSaleInventoryService` → proyecciones → incidencia si aplica
- [x] `ACCEPTED` / `DUPLICATE` / `REQUIRES_REVIEW`
- [x] `siteId` ≠ JWT headquarter → rechazo (preferir por evento)
- [x] **Prohibido** llamar sale web / `removeStock` que rechaza insuficiente
- [x] IT: dos `SALE_CONFIRMED` iguales → una venta, segunda `DUPLICATE`, stock coherente (negativo permitido en POS)

**Salida B4:** Definition of done parcial (enrolar + bootstrap + venta idempotente).

**Refs:** [05](05-device-api.md) § eventos, D4/D10, contracts events.

---

## B5 — Deltas

- [x] `GET /api/v1/pos/sync/changes?cursor=`
- [x] Ops `upsert` | `deactivate`; `nextCursor`
- [x] Cursor inválido / otra sede → **409** → re-bootstrap
- [x] Contrato: [sync-changes-response.example.json](contracts/sync-changes-response.example.json)

**Salida B5:** sync incremental sin re-bootstrap completo siempre.

---

## B6 — Incidencias y reportes admin

- [x] `GET/POST accept` sync-incidents (`ADMIN` web; no muta ticket)
- [x] Reports: sales, products, waste-cancellations, shift-closes (hechos `ACCEPTED`)
- [x] Paginación estándar `PageableRequest` / `PagedResponse`
- [x] IT accept + report smoke

**Salida B6:** ops puede resolver review y consultar reportes POS.

**Refs:** [06](06-admin-api-web.md) § incidencias/reportes.

---

## B7 — Cliente móvil (no es código de este backend)

Trackear en `mobile/pos` (Fase 4). Backend solo necesita B3+ estable.

- [ ] HTTP client + worker + outbox con payload
- [ ] Keystore para refresh
- [ ] Mapper seed decimal → contrato centavos
- [ ] Enroll → bootstrap → events → changes

---

## Definition of done (backend listo para mobile serio)

Checklist global — marcar cuando B1–B6 cumplan el escenario:

1. [ ] Enrolar device de sede de prueba
2. [ ] Bootstrap con catálogo efectivo + operadores + `pinHash`
3. [x] Dos `SALE_CONFIRMED` iguales → una venta, segunda `DUPLICATE`, stock coherente (negativo OK en POS)
4. [x] Incidencia resoluble por `ADMIN` sin mutar ticket
5. [ ] IT MockMvc de lo anterior + inventario web (`SALE_DISPATCH`) intacto

---

## Alcance congelado (no implementar “de paso”)

Si aparece en una PR sin spec nuevo, **rechazar** o abrir nota en `08` / decisión:

- Mercado Pago, CFDI, fiados, pago mixto
- Microservicios, WebSockets, sync de carritos
- Transferencias entre sedes, recetas/combos, imágenes S3 en bootstrap
- CRUD `/admin` duplicado de Item/Headquarter
- Usar `InventoryTransactionManagementUseCases.sale()` para sync POS
- Networking Android antes de B7

---

## Notas de progreso

| Fecha | Fase | Nota |
|-------|------|------|
| 2026-09-08 | B0 | Specs + este tracker creados; código aún stub (`PosController` vacío) |
| 2026-09-08 | B1 | Catálogo/sede: V13, pos-settings, pos-catalog, LocationType.POS, barcode 409, PosSaleInventoryUseCases |
| 2026-09-08 | B2 | Devices/operators: V14, enroll/refresh/me, admin operators/codes/devices, device JWT ≠ staff, ITs verdes |
| 2026-09-08 | B3 | Bootstrap: `GET /pos/sync/bootstrap` proyección plana + centavos, HQ isolation + revoked 403 ITs |
| 2026-09-08 | B4 | Events: V15 ledger/sales/incidents, `POST /pos/sync/events`, SALE_CONFIRMED + PosSaleInventory, ACCEPTED/DUPLICATE/REQUIRES_REVIEW/REJECTED, ITs verdes |
| 2026-09-08 | B5 | Deltas: V16 tombstones, `GET /pos/sync/changes`, real bootstrap cursor, upsert/deactivate, cursor 409, ITs verdes |
| 2026-09-08 | B6 | Incidencias/reportes: V17 accept_label, admin sync-incidents list/get/accept (ADMIN), reports sales/products/waste/shift (ACCEPTED), ITs verdes |
