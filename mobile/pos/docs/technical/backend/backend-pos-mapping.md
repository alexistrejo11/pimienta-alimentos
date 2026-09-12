# Mapeo backend ↔ POS

Estado: descubrimiento de Phase 0, 2026-09-05. **Superseded as the backend
source of truth** by
[`backend/docs/v2/post_integration/`](../../../../../backend/docs/v2/post_integration/README.md)
(audit 2026-09-08). Keep this file as the Phase 0 note; do not extend it.

Este documento registra lo observado en `../../backend` antes de crear DTOs,
migraciones Room o cliente HTTP. No implica que el contrato POS ya esté
aprobado por backend.

## Entidades observadas

| Concepto POS | Backend observado | Estado |
|---|---|---|
| Sede | `module.headquarter.core.domain.Headquarter` | Candidato directo |
| Producto base | `module.inventory.core.domain.Item` | Candidato directo |
| Existencia por sede/ubicación | `module.inventory.core.domain.Inventory` + `StorageLocation` | Requiere definir ubicación operativa |
| Usuario | `module.account.user.core.domain.entities.User` | Requiere confirmar PIN local y roles |
| Categoría de venta | No existe como catálogo POS; `ItemCategory` es inventario | Pendiente de contrato |
| Precio local por sede | `Item.salePrice` es global en el modelo observado | Pendiente |

## Hallazgos relevantes

`Item` aporta `id`, `sku`, `name`, `barcode`, `category`, `unit`, `costPrice`,
`salePrice`, puntos de reorden y estado. El POS necesita además
`saleCategory`, `sellingEnabled` y precio efectivo por sede; no deben derivarse
silenciosamente hasta acordar el contrato `pos-bootstrap`.

La cantidad está fuera de `Item`, en `Inventory`, junto con su
`StorageLocation`. Esta versión del POS solo vende unidades enteras (`PIECE`).

## Rutas observadas

- `/api/v1/headquarters`
- `/api/v1/inventory/items`
- `/api/v1/inventory/stock`
- `/api/v1/inventory/locations`
- `/api/v1/users`

No se observó todavía un endpoint `/api/v1/pos/sync/bootstrap` ni un contrato
de eventos POS. La integración cloud queda fuera de este corte.

## Decisiones para el siguiente corte

1. Confirmar si `Headquarter` es la sede que debe viajar en el bootstrap.
2. Definir `saleCategory` separada de `ItemCategory`.
3. Definir precio efectivo y disponibilidad por sede.
4. Definir usuarios POS, PIN local, roles y revocación.
