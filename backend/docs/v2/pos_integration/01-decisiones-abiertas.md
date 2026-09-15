# Decisiones abiertas

Estado: **decidido** · 2026-09-08

Corte 1 cerrado. Los docs `02`–`06` y `contracts/` pueden alinearse a esto; no reabrir opciones sin una nota de cambio.

Numeración de este archivo = D1…D10 del plan original. El mensaje de cierre usó otra secuencia (catálogo como “D1”, etc.); abajo cada ítem lleva la letra elegida del plan original.

## Resumen consolidado

```text
Item global
  + headquarter_items (efectivo por sede)
  + storage_locations con LocationType.POS
  + pos_operators (+ pos_operator_headquarter)
  + pos_devices
  + JWT device (access 15m / refresh 90d, máx 365d)
  + enroll code 10 min; reasignar = revoke + enroll
  + API /api/v1/pos/**
  + eventos idempotentes con UUID
  + dinero en centavos enteros
  + barcode único global no reutilizable
```

No se migra todo el backend a UUID ni se duplican CRUDs bajo `/admin`. El contrato POS queda listo para multisede, offline y stock negativo solo en el flujo POS.

---

## D1. Sede POS = `Headquarter`

**Estado:** `decidido` · 2026-09-08 · **A**

`Site` (POS) y `Headquarter` son el mismo concepto. Se reusa y se extiende; no hay `pos_site`.

En bootstrap el campo puede seguir llamándose `site`; su `id` es el de `Headquarter` (formato en D2).

---

## D2. Identificadores públicos

**Estado:** `decidido` · 2026-09-08 · **A** (confirmado en el consolidado)

- Hechos POS con UUID: `eventId`, `saleId`, `deviceId`, `shiftId`, etc.
- Maestros web siguen en `Long`: `Item`, `Headquarter`, `User`, `PosOperator.id` puede ser Long o UUID de servidor — si es Long, en el wire POS se serializa como string decimal (`"42"`).
- No migrar catálogo/sedes/usuarios web a UUID.

Formato canónico en bootstrap/deltas para IDs Long: **string decimal**. Elegir un solo estilo y no mezclar con UUID en el mismo campo.

---

## D3. Catálogo de venta vs `Item`

**Estado:** `decidido` · 2026-09-08 · **B** (`headquarter_items`)

### `Item` (global)

Conserva datos maestros:

- nombre;
- SKU / barcode;
- costo;
- unidad;
- categoría de **bodega** (`ItemCategory`).

El precio de venta no vive en `Item`; se define por sede en `headquarter_items.sale_price`.

### Tabla `headquarter_items` (efectivo por sede)

| Campo | Notas |
|-------|--------|
| `headquarter_id` | FK sede |
| `item_id` | FK ítem |
| `sale_category` | Categoría de venta POS; **no** es `ItemCategory` |
| `sale_price` | Precio efectivo (centavos en el DTO POS; BigDecimal en persistencia) |
| `available` | Disponibilidad administrativa para caja |
| `stock_policy` | `CONTROLLED` \| `NOT_CONTROLLED` |
| `negative_stock_limit` | Si aplica a esa sede/ítem |
| `version` / `updated_at` | Para deltas |

`saleCategory` vive en la config POS por sede. Fallback opcional a un valor global del producto **solo si** se añade después un campo global explícito; no reutilizar `ItemCategory`.

La tablet recibe un **bootstrap plano ya proyectado** (joins resueltos en el servidor).

Descartado: meter solo campos POS globales en `Item`, o un catálogo POS copiado aparte.

---

## D4. Stock de cafetería y negativo

**Estado:** `decidido` · 2026-09-08 · **A+** (ubicación POS canónica + flujo especializado)

- Existe una fila en `storage_locations` canónica por sede: `type = POS` (`LocationType.POS`), con `headquarter_id` obligatorio en ese tipo, código estable p. ej. `POS-{id}`.
- Las ventas POS generan movimientos **solo** sobre esa ubicación.
- **No** llamar al `removeStock()` / `SALE_DISPATCH` web si rechazan saldo insuficiente.

### `PosSaleInventoryService` (nombre de aplicación)

Use case / servicio de aplicación POS que:

1. Acepta stock negativo **únicamente** en la ubicación POS;
2. Registra el movimiento en el ledger central;
3. Aplica idempotencia por `eventId`;
4. **No** acepta que la tablet envíe “stock final”;
5. Calcula el saldo a partir de movimientos.

Las ventas web normales siguen rechazando inventario insuficiente. La excepción es solo para eventos POS autorizados.

---

## D5. Unicidad de barcode

**Estado:** `decidido` · 2026-09-08 · unique global · **no reutilizable**

```sql
UNIQUE (barcode) WHERE barcode IS NOT NULL
```

- Unicidad **global**, no limitada a productos activos.
- Un ítem deshabilitado / soft-deleted **conserva** su barcode.
- El barcode **no es reutilizable**: no hay flujo de reasignación a otro ítem. Si se necesita un código nuevo en el negocio, se usa un barcode distinto.
- API: **409 Conflict** si ya existe.
- Antes del índice: limpiar duplicados existentes.

---

## D6. Identidad de cajero / manager POS

**Estado:** `decidido` · 2026-09-08 · **B ligera** (tabla `pos_operators`)

### `pos_operators`

- `id`
- `user_id` nullable (vínculo opcional a `User` web)
- `display_name`
- `pos_role`
- `pin_hash`
- `active`

### `pos_operator_headquarter`

- `operator_id`
- `headquarter_id`

Roles POS **independientes** de roles web:

- `CASHIER`
- `MANAGER`
- `SUPERADMIN`

No asumir que `ADMIN` web = `SUPERADMIN` POS.

Ventajas retenidas: cajeros sin cuenta web; PIN ≠ password; mismo humano puede tener web + POS sin duplicar identidad lógica; multisede vía la tabla de autorización.

---

## D7. Autenticación de Device API

**Estado:** `decidido` · 2026-09-08 · **A**

JWT de dispositivo + refresh.

Claims del access token:

- `deviceId`
- `headquarterId`
- `typ` = `device`
- `scope` = `pos:sync`

Enrolamiento: código de un solo uso, **TTL 10 minutos**, → access + refresh + dispositivo + sede + config inicial.

TTL de tokens de dispositivo:

| Token | TTL |
|-------|-----|
| Access | **15 minutos** |
| Refresh | **90 días** por defecto; **máximo 365 días** (techo de política; no emitir por encima) |

- Refresh tokens **hasheados** en backend y **rotados** en cada refresh.
- La tablet los protege con Android Keystore (regla de cliente; el backend no almacena el refresh en claro).
- El token de dispositivo **no** es un cajero y **no** da permisos admin web.

**Reasignación de tablet / dispositivo:** no hay “transferencia” in-place. Flujo obligatorio: **revocar** el device actual + **nuevo enrolamiento** (código fresco de 10 min) en la tablet destino. El device revocado deja de sync (401/403).

Revocado → 401/403; la tablet detiene sync.

---

## D8. Superficie Admin vs Device

**Estado:** `decidido` · 2026-09-08 · **A** (con namespace Device explícito)

### Device API — `/api/v1/pos/**`

- enrolamiento;
- bootstrap;
- deltas;
- eventos;
- estado del dispositivo.

### Web Central

Sigue usando endpoints existentes de inventario, usuarios, sedes y productos (extendidos donde haga falta). **No** segunda API CRUD bajo `/admin/pos`.

Recursos admin *nuevos* que no existen hoy (devices, enrollment codes, sync incidents, reportes POS) pueden vivir bajo `/api/v1/pos/admin/**` con JWT de **staff**, sin duplicar CRUD de `Item`/`Headquarter`. Ver [06-admin-api-web.md](06-admin-api-web.md).

---

## D9. Representación de dinero en JSON POS

**Estado:** `decidido` · 2026-09-08 · **B** (centavos enteros; cambia la recomendación tentativa A)

Todo el contrato POS (bootstrap, deltas, eventos) usa enteros en centavos:

```json
{
  "priceCentavos": 4500,
  "totalCentavos": 9000
}
```

- Sin `float` / `double` / number JSON decimal ambiguo.
- Alineado al modelo Android (`Long` centavos).
- Persistencia / dominio inventario puede seguir en `BigDecimal`; el mapper POS hace `4500` → `BigDecimal("45.00")`.
- Productos por peso: **cantidad** (y unidad) en campos separados; no mezclar precio, peso y unidad en un solo valor.

El seed local `pos-bootstrap.json` (texto decimal) deberá mapearse o migrarse cuando el cliente hable con el servidor real.

---

## D10. Relación evento POS ↔ movimiento de inventario

**Estado:** `decidido` · 2026-09-08 · **A** (nunca vía use case web de sale)

Flujo remoto:

```text
Registrar eventId
        ↓
Persistir venta original
        ↓
Persistir líneas snapshot
        ↓
Crear movimiento de inventario POS (PosSaleInventoryService)
        ↓
Actualizar proyecciones
        ↓
Crear incidencia si corresponde
        ↓
Responder ACCEPTED
```

Restricciones:

- `eventId` único;
- `saleId` único;
- movimiento vinculado al evento;
- una transacción central por evento;
- reintento → `DUPLICATE` con el resultado previo;
- la venta original **nunca** se sobrescribe;
- venta / línea `NOT_CONTROLLED` o sin stock → **no** crea movimiento;
- cancelación → movimiento **inverso**, no borra el original.

Prohibido: `InventoryTransactionManagementUseCases.sale()` / `POST .../transactions/sale` como camino de sync.

---

## D11. Nombres de tablas y esquema congelados

**Estado:** `decidido` · 2026-09-08

### Tablas nuevas (POS / catálogo por sede)

| Tabla | Rol |
|-------|-----|
| `headquarter_items` | Efectivo POS por sede (`HeadquarterItem`) |
| `pos_operators` | Cajeros/managers POS (`PosOperator`) |
| `pos_devices` | Tablets enroladas (`PosDevice`) |

Tablas auxiliares esperadas en el mismo módulo (no renombrar sin nota): `pos_operator_headquarter`, `pos_enrollment_codes`, receipts/eventos/`pos_*` de hechos según B2–B4.

### Inventario existente

| Objeto | Cambio |
|--------|--------|
| Tabla `storage_locations` | Se **reusa**; no se crea tabla de stock POS paralela |
| Enum `LocationType` | Añadir valor **`POS`** (+ CHECK SQL) |
| Columna `headquarter_id` | En `storage_locations`, requerida cuando `type = POS` |

### Tokens y enrolamiento (resumen operativo)

| Parámetro | Valor |
|-----------|-------|
| Access token device | 15 min |
| Refresh token device | 90 días (default); techo 365 días |
| Código de enrolamiento | único, un uso, **10 min** TTL |
| Reasignar tablet | **revocar** + **nuevo enrolamiento** |
| Barcode | único global, **no reutilizable** |

---

## Pendiente menor

Ninguno bloqueante de diseño. Detalle de columnas Flyway se define en B1/B2.

## Siguiente paso

Implementación backend según [07-plan-de-modulos.md](07-plan-de-modulos.md) (B1 en adelante). Marcar avance en [09-implementation-tracker.md](09-implementation-tracker.md). Contratos en [contracts/](contracts/README.md).
