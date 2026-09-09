# Modelo de dominio backend (POS)

Estado: **decidido** · alineado a [01](01-decisiones-abiertas.md) · 2026-09-08

Dominio **delgado**: holders de estado; workflow en use cases. Nombres en inglés en código. Contratos JSON: [contracts/](contracts/README.md).

## Identidad y sede

### `Headquarter` (existente, extendido)

Sede operativa. Todo dispositivo, turno, venta y movimiento POS pertenece a una.

Config POS (tabla propia o 1:1 `PosOperationalConfig`):

- moneda (`MXN`);
- umbrales de catálogo desactualizado (24 h / 72 h — la tablet los aplica; el servidor los publica);
- categorías de monto abierto;
- límite negativo por defecto de sede (puede refinarse por ítem en `HeadquarterItem`).

Al habilitar POS en una sede: crear/asegurar fila en `storage_locations` con `LocationType.POS`, `headquarter_id=<id>`, código estable p. ej. `POS-{id}`.

### `PosDevice` → tabla `pos_devices`

- `id` UUID
- `headquarterId` Long inmutable tras enrolar
- `visibleCode` estable (`T1`…)
- `status`: `PENDING` | `AUTHORIZED` | `REVOKED`
- `minAppVersion` / esquemas soportados
- última secuencia recibida (opcional)

### `PosOperator` → tabla `pos_operators`

- `id` Long (wire: string decimal)
- `userId` nullable
- `displayName`
- `posRole`: `CASHIER` | `MANAGER` | `SUPERADMIN`
- `pinHash` (nunca PIN en claro)
- `active`
- sedes vía `pos_operator_headquarter`

Roles POS **no** alias de `Role` web.

## Catálogo

### `Item` (global)

Nombre, SKU, barcode (**único global si not null; no reutilizable**), costo, precio base, unidad, `ItemCategory` de bodega.

### `HeadquarterItem` → tabla `headquarter_items`

`headquarterId`, `itemId`, `saleCategory`, `salePrice`, `available`, `stockPolicy` (`CONTROLLED` | `NOT_CONTROLLED`), `negativeStockLimit` opcional, `version` / `updatedAt`.

### Proyección bootstrap (plana)

Campos que recibe la tablet (ya resueltos):

| Campo wire | Origen |
|------------|--------|
| `id` | `Item.id` como string decimal |
| `sku`, `barcode`, `name`, `unit` | `Item` |
| `saleCategory` | `HeadquarterItem` |
| `priceCentavos` | efectivo sede |
| `costCentavos` | `Item.costPrice` |
| `available` | `HeadquarterItem` |
| `stockQuantity` | saldo en location POS (entero, no dinero) |
| `stockMinQuantity` | reorder / config |
| `stockPolicy` | `HeadquarterItem` |

`NOT_CONTROLLED` → sin movimiento de stock. `available=false` ≠ stock 0.

## Operación de caja (hechos inmutables)

### `PosShift`

Un activo por dispositivo. `OPEN` → conteos → `CLOSED`. Fondo inicial, cajero, timestamps. Totales oficiales en `PosShiftClose`.

### `PosSale`

- UUID interno + folio visible
- sede, device, shift, cashier (`PosOperator.id`)
- montos en centavos: gross, discount, total
- `CONFIRMED` | `CANCELLED`
- líneas snapshot (nombre, saleCategory, qty, unitPriceCentavos, subtotalCentavos, productId opcional, barcode crudo)
- pagos: `CASH` | `EXTERNAL_CARD_MP` | `CORTESIA`
- descuento único opcional con autorizador

Invariantes de aplicación:

1. Suma de pagos aplicados = total.
2. Cortesía: neto 0, un pago `CORTESIA`, descuento = bruto.
3. Sin `productId` → no mueve stock.
4. Solo `CONTROLLED` mueve stock.

### Otros hechos

`PosSaleCancellation`, `PosCashWithdrawal` (`SAFEKEEPING`), waste/restock, `PosCashCountAttempt`, `PosShiftClose`, audit/autorizaciones.

## Sync e inventario

### Sobre de evento

Ver [contracts/event-envelope.example.json](contracts/event-envelope.example.json). Campos: `eventId`, `eventType`, `schemaVersion`, `deviceId`, `siteId` (= headquarter id string), `deviceSequence`, `aggregateId`, `shiftId`, `occurredAt`, `payload`.

Tipos MVP: `SHIFT_OPENED`, `SALE_CONFIRMED`, `SALE_CANCELLED`, `WASTE_RECORDED`, `RESTOCK_RECORDED`, `CASH_WITHDRAWAL_RECORDED`, `CASH_COUNT_SUBMITTED`, `CASH_COUNT_REJECTED`, `SHIFT_CLOSED`, `TICKET_REPRINTED`, `OVERRIDE_AUTHORIZED`.

### Resultado

`ACCEPTED` | `DUPLICATE` | `REQUIRES_REVIEW`. Errores 5xx/red no definitivos. Schema ilegible → respuesta que la tablet marque `BLOCKED_TECHNICAL`.

### Flujo de aceptación (D10)

```text
eventId unique → venta + líneas → PosSaleInventoryService → proyecciones → incidente? → ACCEPTED
```

Cancelación: movimiento **inverso**, no borrar el original. Tablet no envía stock final.

### `PosSyncIncident`

Solo Superadmin (staff `ADMIN`) clasifica y acepta con nota. No muta el payload. No vincula líneas históricas a un `Item`.

## Invariantes de servidor

1. Unique `eventId`.
2. Unique `saleId` (UUID).
3. Device revocado: sin ingest ni download.
4. `siteId` del evento = sede del token; si no → **409** (no aplicar como venta normal).
5. Reenvío → `DUPLICATE`.
6. Deltas de catálogo no mutan ventas guardadas.
7. Stock central = suma de movimientos en location POS (puede ser negativo solo por ese camino).
