# Impacto en módulos existentes

Estado: **decidido** · alineado a [01](01-decisiones-abiertas.md) · 2026-09-08

La **web actual no se rompe**. Campos nuevos opcionales o tablas nuevas. Compras, transferencias y `SALE_DISPATCH` de almacén conservan su semántica (incl. rechazo por stock insuficiente).

## Tablas (nombres congelados)

| Tabla | Acción |
|-------|--------|
| `headquarter_items` | **Nueva** — efectivo POS por sede |
| `pos_operators` | **Nueva** — operadores de caja |
| `pos_devices` | **Nueva** — tablets enroladas |
| `pos_operator_headquarter` | **Nueva** — N:N operador↔sede |
| `pos_enrollment_codes` | **Nueva** — códigos un uso, TTL 10 min |
| `storage_locations` | **Reusada** — filas `LocationType.POS` + `headquarter_id` |
| `inventory_items` | Alter: unique barcode `WHERE barcode IS NOT NULL` |

## `headquarter`

Hoy: nombre, dirección, descripción.

Cambios:

- `PosOperationalConfig` (1:1 o embebido) — moneda, umbrales catálogo, categorías monto abierto, límite negativo default.
- Al habilitar caja: insert en `storage_locations` (`type=POS`, `headquarter_id`, código `POS-{id}`).

API web:

- `GET/PUT /api/v1/headquarters/{id}/pos-settings`
- `GET/PUT /api/v1/headquarters/{id}/pos-catalog/{itemId}` (backed by `headquarter_items`)

## `inventory` — `Item`

- Sin reutilizar `ItemCategory` como categoría de venta.
- Efectivo de caja → **`headquarter_items`**.
- Precio base y costo siguen en `Item`.
- Barcode: único global, **no reutilizable**, 409 en conflicto. Deshabilitado conserva el código. Limpiar duplicados antes del índice.

## `inventory` — `storage_locations` y stock

Migración:

1. Ampliar enum/CHECK: `LocationType.POS`.
2. Columna `headquarter_id` nullable; **NOT NULL** cuando `type = POS` (CHECK o partial).
3. Location POS: no usar `occupiedCapacity` como hard fail.

Bootstrap stock = `availableQuantity` de `(item_id, location POS de la sede)`.

### `PosSaleInventoryService`

- movimiento ligado a `eventId` / sale UUID;
- negativo **solo** en location POS;
- no llama `Inventory.removeStock()`;
- no acepta stock final desde tablet;
- idempotente por evento.

`POST /inventory/transactions/sale` sin cambios.

## `account.user`

Sin PIN en `User`. Puente opcional: `pos_operators.user_id`.

## `config.security`

| Ruta | Auth |
|------|------|
| `POST /api/v1/pos/devices/enroll` | Público + STRICT; código válido ≤ **10 min**, un uso |
| `POST /api/v1/pos/devices/refresh` | Refresh device rotado |
| Resto Device `/api/v1/pos/**` (no admin) | JWT `typ=device`, `scope=pos:sync` |
| `/api/v1/pos/admin/**` | JWT staff; incidencias `ADMIN` |

| Token device | TTL |
|--------------|-----|
| Access | **15 min** |
| Refresh | **180 días** default; techo **365 días** |

Reasignar tablet: `POST .../devices/{id}/revoke` + nuevo código de enrolamiento. No hay transfer in-place.

## Flyway

Después de `V12`, por corte: B1 (`headquarter_items`, location POS, barcode) → B2 (`pos_devices`, `pos_operators`, enrollment codes) → B4 (hechos/receipts).

## Tests

IT inventory actuales verdes. Nuevos: negativo POS, idempotencia, device revocado, barcode 409, enroll code expirado a los 10 min.
