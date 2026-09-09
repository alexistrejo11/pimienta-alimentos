# Admin API (Web Central)

Estado: **decidido** · alineado a [01](01-decisiones-abiertas.md) · 2026-09-08

**No** duplicar CRUD bajo `/api/v1/admin`. La web sigue en `/api/v1/inventory`, `/users`, `/headquarters`. Recursos **nuevos** de POS (devices, codes, incidents, reports, operators) viven en `/api/v1/pos/admin/**` con JWT de **staff**.

## Catálogo y sede

| Necesidad | Encaje |
|-----------|--------|
| Alta/edición maestro | `GET/POST/PUT /api/v1/inventory/items` |
| Efectivo por sede (`HeadquarterItem`) | `GET/PUT /api/v1/headquarters/{id}/pos-catalog/{itemId}` (+ list) |
| Políticas de caja | `GET/PUT /api/v1/headquarters/{id}/pos-settings` |
| Categorías monto abierto | dentro de pos-settings |

Barcode duplicado o intento de reutilizar uno ya asignado → **409**. Descontinuar (`DISCONTINUED`) ≠ `available=false` por sede. Barcodes **no reutilizables**.

Importes en API web de inventario pueden seguir en `BigDecimal`; el mapper a Device API convierte a centavos.

## Inventario maestro

| Necesidad | Encaje |
|-----------|--------|
| Stock / movimientos por sede | Filtrar por location `POS` + `headquarterId` |
| Conteo, ajuste, compra | `POST /api/v1/inventory/transactions/*` **como hoy** |
| Merma/reposición ya sync | Lectura de movements origen POS |

Ajustes Superadmin **nunca** reescriben una `PosSale`.

## Operadores y dispositivos

| Necesidad | Encaje |
|-----------|--------|
| Staff web | `/api/v1/users/management` (sin cambios de modelo PIN) |
| Operadores (`pos_operators`), PIN write-only, sedes | `/api/v1/pos/admin/operators` (+ assign headquarter) |
| Vincular `user_id` opcional | mismo recurso |
| Códigos de enrolamiento | `POST /api/v1/pos/admin/enrollment-codes` `{ "headquarterId": 42 }` — único, **10 min** TTL, un uso |
| Listar / revocar tablets (`pos_devices`) | `GET /api/v1/pos/admin/devices`, `POST .../devices/{id}/revoke` |
| Reasignar tablet | **Revocar** + emitir código nuevo + enroll (no transfer) |

Roles asignables: `CASHIER`, `MANAGER`, `SUPERADMIN`. No auto-mapear desde `ADMIN` web.

Revocación de device es inmediata en servidor; offline no se entera hasta reconectar.

## Incidencias

```text
GET  /api/v1/pos/admin/sync-incidents
GET  /api/v1/pos/admin/sync-incidents/{incidentId}
POST /api/v1/pos/admin/sync-incidents/{incidentId}/accept
```

`accept`: etiqueta + nota obligatoria. No muta payload. No crea `productId` histórico.

Solo staff `ADMIN`. Manager web no resuelve sync incidents.

## Reportes

```text
GET /api/v1/pos/admin/reports/sales
GET /api/v1/pos/admin/reports/products
GET /api/v1/pos/admin/reports/waste-cancellations
GET /api/v1/pos/admin/reports/shift-closes
```

Filtros: `headquarterId`, fechas; opcional shift, producto, motivo. Solo hechos `ACCEPTED` (y aceptados en review). Montos en centavos o decimal documentado — preferir centavos en JSON POS-admin para consistencia.

Paginación: `PageableRequest` / `PagedResponse`.

## Qué no es esto

- `POST /pos/devices/enroll`, bootstrap, events → Device API.
- Panel Manager de la tablet → 100% local.

## Web Angular

Sin diseño de pantallas aquí. Cuando existan estos endpoints, `web/` los consume.
