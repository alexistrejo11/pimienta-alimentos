# Device API

Estado: **decidido** · alineado a [01](01-decisiones-abiertas.md) · 2026-09-08

Prefijo: `/api/v1/pos`. Auth: JWT de **dispositivo** (`typ=device`, `scope=pos:sync`), excepto enroll. Errores: `ApiErrorResponse`. Ejemplos canónicos: [contracts/](contracts/README.md).

## Enrolamiento y sesión

### `POST /api/v1/pos/devices/enroll`

Código de un solo uso, **TTL 10 minutos** → access (**15 min**) + refresh (**90 días**, techo 365) hasheado/rotado + dispositivo (`pos_devices`) + sede + pistas de config.

Request: [contracts/device-enroll-request.example.json](contracts/device-enroll-request.example.json)

Response: [contracts/device-enroll-response.example.json](contracts/device-enroll-response.example.json)

Efectos: código consumido, device `AUTHORIZED` en `pos_devices`, sede fijada. Código reusado o expirado (>10 min) → 409/400. Device ya enrolado → 409.

**Reasignación de tablet:** revocar el device en admin + generar código nuevo + enroll en la tablet. No hay endpoint de “transfer”.

### `POST /api/v1/pos/devices/refresh`

Body: `{ "refreshToken": "..." }`. Rota refresh (nuevo TTL hasta 90 días / techo 365). `REVOKED` → 401/403.

### `GET /api/v1/pos/devices/me`

`AUTHORIZED` | `REVOKED`, sede, `visibleCode`, `minAppVersion`, `eventSchemaVersions`.

Claims access (no se envían al cliente como doc aparte; van dentro del JWT):

```text
deviceId, headquarterId, typ=device, scope=pos:sync
```

El token **no** representa a un cajero ni da permisos admin.

## Descarga de maestros

### `GET /api/v1/pos/sync/bootstrap`

Snapshot atómico, **plano**, de la sede del device. Dinero en **centavos enteros**.

Ejemplo: [contracts/pos-bootstrap.example.json](contracts/pos-bootstrap.example.json)

Reglas:

- Solo operadores autorizados en esa sede y productos con fila `HeadquarterItem` (o política explícita de inclusión).
- `site.id` = `Headquarter.id` como string decimal.
- `priceCentavos` = efectivo de sede; `costCentavos` desde `Item`.
- `stockQuantity` = saldo central en location POS (informativo; no es comando).
- Nunca `number` flotante para dinero.
- PIN hashes de debug del seed Android no van a producción.

### `GET /api/v1/pos/sync/changes?cursor=`

Ejemplo: [contracts/sync-changes-response.example.json](contracts/sync-changes-response.example.json)

Operaciones `upsert` | `deactivate` sobre products, operators, policies, stock. Incluye `nextCursor`.

La tablet guarda `nextCursor` solo tras aplicar el lote entero en Room. Cursor inválido / de otra sede → **409** → re-bootstrap.

## Ingesta de eventos

### `POST /api/v1/pos/sync/events`

Lote ordenado por `deviceSequence`. Respuesta por `eventId` (puede mezclar estados).

Request: [contracts/sync-events-request.example.json](contracts/sync-events-request.example.json)

Response: [contracts/sync-events-response.example.json](contracts/sync-events-response.example.json)

`siteId` debe coincidir con `headquarterId` del JWT → si no, **409** para ese evento (o rechazo de lote; preferir por evento).

Procesamiento **por evento** en transacción:

```text
Registrar eventId
  → Persistir venta original + líneas snapshot
  → PosSaleInventoryService (si CONTROLLED)
  → Proyecciones
  → Incidencia si corresponde
  → ACCEPTED | REQUIRES_REVIEW
```

Reintento mismo `eventId` → `DUPLICATE` + resultado previo.

**Prohibido** mapear a `POST /api/v1/inventory/transactions/sale`.

Huecos de `deviceSequence`: MVP los acepta; son observabilidad.

## Payload `SALE_CONFIRMED`

Ver ejemplo embebido en el request de eventos. Mínimo: sale UUID, folio, shift, cashier, líneas snapshot, pagos, totales en centavos, flags de excepción (stock negativo, no disponible, barcode pendiente).

Precio distinto al maestro **no** se reescribe; si hay política de review → `REQUIRES_REVIEW`.

## Versionado

- `/api/v1` para breaking del API.
- `schemaVersion` por `eventType`.
- Campos nuevos opcionales.
- `devices/me` informa `minAppVersion`.

## Fuera de Device API

CRUD de ítems, conteos, operadores (alta), reportes, resolución de incidencias → [06-admin-api-web.md](06-admin-api-web.md).
