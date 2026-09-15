# Auditoría backend y POS

Estado: **borrador** (factual) · 2026-09-08

Revisión del código real. No aprueba contratos. Absorbe y actualiza el mapeo de Phase 0 del móvil (`mobile/pos/docs/technical/backend/backend-pos-mapping.md`).

## Resumen

El backend de producción sirve a la **web** (JWT de staff). El POS Android es **local-first** y todavía no habla con el servidor. El único rastro POS en Java es un `PosController` vacío, fuera de la estructura hexagonal.

```text
Web ──JWT──► Spring Boot /api/v1 ──► PostgreSQL
                 ▲
                 │  (no existe)
Tablet POS ──outbox local──┘
```

## Backend observado

Stack: Java 26, Spring Boot 4, `/api/v1`, PostgreSQL + Flyway (12 migraciones), Redis, JWT, S3.

Módulos: `account` (auth + user), `employees`, `contract`, `crm`, `task`, `headquarter`, `inventory`, `payroll`, `files`, `notification`. Paquete POS: `module.pos` con un controller vacío.

Arquitectura: hexagonal por módulo, dominio delgado (`BaseDomain` + `SafeBuilder` donde aplica), workflow en use cases, validación Jakarta en DTOs HTTP.

Auth: JWT Bearer (access ~15 min, refresh Redis). Roles `USER`, `SUPPORT`, `MANAGER`, `ADMIN`. Registration queda `PENDING_APPROVAL`. No hay PIN, ni rol `CASHIER`, ni identidad de dispositivo.

### Rutas relevantes (ya existen)

| Área | Base path |
|------|-----------|
| Auth | `/api/v1/auth` |
| Perfil | `/api/v1/users` |
| Gestión de usuarios | `/api/v1/users/management` |
| Sedes | `/api/v1/headquarters` |
| Ítems | `/api/v1/inventory/items` |
| Stock | `/api/v1/inventory/stock` |
| Ubicaciones | `/api/v1/inventory/locations` |
| Transacciones de inventario | `/api/v1/inventory/transactions` |
| Movimientos | `/api/v1/inventory/movements` |

No existen `/api/v1/pos/**`, bootstrap, eventos, enrolamiento ni incidencias de sync.

Security: casi todo `/api/v1/**` exige JWT de persona. Auth pública solo en `/api/v1/auth/**`. Una Device API no puede reutilizar ese modelo tal cual.

### `Headquarter`

Tabla `headquarters`: `id` Long, `name`, `address`, `description`, auditoría y soft delete. Usado por asistencia de empleados y tareas. **No tiene** moneda, políticas POS, catálogo por sede, dispositivos ni stock.

Candidato natural a `Site` del POS. Hay que extenderlo; hoy no es una sede de cafetería.

### `Item` + inventario de almacén

`Item` (tabla `inventory_items`):

- Identidad: `sku` unique, `barcode` indexado **sin unique**, `name`, `description`, `brand`
- Clasificación: `ItemCategory` de bodega (`RAW_MATERIAL`, `FINISHED_GOOD`, `CONSUMABLE`, …)
- Unidad: `PIECE`, `KG`, `GRAM`, …
- Precios: `costPrice` (`NUMERIC(19,6)`). El precio de venta efectivo vive en `headquarter_items.sale_price`.
- Reorden: `reorderPoint`, `reorderQuantity`
- Estado: `ACTIVE`, `DISCONTINUED`, `OUT_OF_STOCK`, `PENDING_APPROVAL`

Falta para POS: `saleCategory`, disponibilidad por sede, precio local, `stockPolicy` (`CONTROLLED` / `NOT_CONTROLLED`), `sellingEnabled`.

`Inventory` = cantidad de un ítem en una `StorageLocation`. **No hay FK a `Headquarter`**. Cantidades: `availableQuantity`, `reservedQuantity`, `inTransitQuantity`. `removeStock()` **lanza si no hay existencia**. `StorageLocation` también rechaza si no hay capacidad.

`InventoryMovement` es ledger append-only con tipos de almacén (`PURCHASE`, `SALE`, `SCRAP`, `TRANSFER`, ajustes…). `InventoryTransaction` agrupa movimientos bajo folio interno y workflow `DRAFT → PENDING → APPROVED → COMPLETED`.

`POST /api/v1/inventory/transactions/sale` (`SALE_DISPATCH`) aplica `removeStock` y completa al instante. **No es idempotente por UUID de venta POS** y **no admite stock negativo**. No debe ser el camino de sync.

### `User`

`Long` id, email, `passwordHash`, nombre, roles web, `AccountStatus`. Sin PIN, sin sede, sin snapshot para autenticación offline.

## POS observado

App nativa Kotlin + Compose, módulo `:app`, paquete `io.github.alexistrejo.pimienta.pos`.

Punto actual (docs de implementación, 2026-09-05; código alineado): **Fase 1** (caja local de efectivo) en curso. Phase 0 cerrada para desarrollo local. **Fase 4 (cloud) no empezada**. AGENTS.md del POS prohíbe añadir DI, Room extra o networking salvo que la tarea lo pida; Room ya está.

### Ya en Room

Sede (`site`), producto efectivo, usuarios locales con `pinHash`, dispositivo y secuencia, turno, venta, líneas snapshot, descuento, pagos, sangría, merma/movimientos, cancelación, conteo ciego, Corte Z, outbox, print jobs, marca de bootstrap.

Contrato local `pos-bootstrap` (debug, `schemaVersion` 2 en el seed): `site`, `device`, `users` (roles `CASHIER` / `MANAGER`), `products` con `saleCategory`, `price`/`cost` como texto decimal, `available`, `stock`, `stockPolicy`.

Outbox actual es delgado: `id`, `sequence`, `type`, `aggregateId`, `status`. **Sin payload JSON, schemaVersion, deviceId ni siteId**. Habrá que ampliarlo cuando exista Device API; no bloquea este corte de backend.

Dinero local: centavos `Long`. IDs: UUID `String`.

### Fases del móvil (contexto)

| Fase | Objetivo | Sync |
|------|----------|------|
| 0 | Descubrimiento, seed, Room | No |
| 1 | Cobro local efectivo/tarjeta manual | Outbox persistido, no enviado |
| 2 | Manager: monto abierto, merma, Corte Z, cancelación | Local |
| 3 | Periféricos | No |
| 4 | Enrolamiento, bootstrap, eventos, worker | **Aquí entra este contrato** |
| 5 | Piloto | — |

## Mapeo conceptual (sin contrato aprobado)

| Concepto POS | Backend hoy | Encaje |
|--------------|-------------|--------|
| `Site` | `Headquarter` | Candidato; faltan campos POS |
| `Product` | `Item` | Candidato a identidad maestra; faltan atributos de venta |
| `SiteProduct` | no existe | Precio, disponibilidad y stockPolicy por sede |
| Stock de caja | `Inventory` + `StorageLocation` | Hay que definir ubicación (o stock) por sede |
| `UserLocalSnapshot` | `User` | Faltan PIN, roles POS, alcance por sede |
| `Device` | no existe | Nuevo en `module.pos` |
| `Shift` / `Sale` / outbox | no existen | Nuevos en `module.pos` |
| Categoría de venta | `ItemCategory` (bodega) | **No es lo mismo**; no alias silencioso |

## Conflictos duros (detalle en 01)

1. Long vs UUID
2. Stock que no puede quedar negativo vs POS que sí
3. Inventario por ubicación de almacén vs inventario por sede
4. Precio/categoría/disponibilidad globales vs por sede
5. JWT de persona vs credencial de tablet
6. Roles web vs `CASHIER` / `MANAGER` / `SUPERADMIN`
7. `POST .../transactions/sale` vs ingesta de eventos
8. Admin API inventada en `/admin` vs rutas web ya existentes

## Qué no hay (y no debe improvisarse en código)

- Device API
- Bootstrap / deltas / cursores
- Idempotencia por `eventId`
- Incidencias `REQUIRES_REVIEW`
- PIN y enrolamiento
- Reportes de Corte Z / ventas POS en la web
