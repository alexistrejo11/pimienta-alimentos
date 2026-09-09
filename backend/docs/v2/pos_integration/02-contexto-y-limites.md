# Contexto acotado y límites

Estado: **decidido** · alineado a [01-decisiones-abiertas.md](01-decisiones-abiertas.md) · 2026-09-08

El backend sigue siendo **un monolito hexagonal**. El POS no es un microservicio. Se añade un bounded context `module.pos` y se **extienden** headquarter e inventory. Operadores POS son entidad propia (`PosOperator`), con vínculo opcional a `User`. No se replica Room ni se sincronizan tablas genéricas.

## Autoridad por tipo de dato

| Dato | Autoridad | La otra parte |
|------|-----------|----------------|
| Venta cobrada, pagos, líneas snapshot, folio local | Tablet hasta ACK; después el payload original es inmutable en servidor | Backend clasifica, no reescribe importes |
| Turno, conteo, Corte Z, sangría, merma, reposición operativa | Hechos de tablet | Backend almacena y reporta |
| Catálogo maestro (`Item`) + efectivo por sede (`HeadquarterItem`) | Backend | Tablet recibe proyección plana |
| `PosOperator`, PIN hashes, roles POS, dispositivos | Backend | Snapshot local para offline |
| Inventario consolidado | Backend vía movimientos en location POS | Tablet proyecta saldo local para UI |

## Qué nace en `module.pos`

Paquete: `io.github.alexistrejo11.pimienta.module.pos`, layout hexagonal (preferir `infrastructure/adapter` como inventory).

Agregados / tablas:

- `PosDevice` — tablet enrolada, sede fija, `visibleCode`, estado, secuencia vista en servidor
- `PosEnrollmentCode` — código de vinculación de un solo uso
- Refresh de dispositivo (Redis o tabla; **hasheado**, rotado)
- `PosOperator` + `pos_operator_headquarter`
- `PosOperationalConfig` — políticas por sede
- `PosShift`, `PosCashCountAttempt`, `PosShiftClose`, `PosCashWithdrawal`
- `PosSale`, líneas, pagos, descuento, cancelación
- Recepción idempotente (`eventId` unique)
- `PosSyncIncident` — `REQUIRES_REVIEW`
- Cursores opacos emitidos por el servidor
- `PosSaleInventoryService` — aplicación de stock POS (no el sale web)

El `PosController` vacío actual **no** es el diseño. Sustituirlo por controllers thin + `Doc*` cuando se implemente.

## Qué se reusa (no se duplica)

| Módulo | Uso POS |
|--------|---------|
| `headquarter` | Sede = `Site` (D1) |
| `inventory` `Item` | Maestro global (D3) |
| `inventory` stock + movements | Ledger en location `POS` por sede (D4, D10) |
| `account.user` | Solo si `PosOperator.user_id` no es null |
| Security / rate limit / errores / paginación | Igual que `/api/v1` |
| `files` | Fuera del MVP |

Tabla nueva / extendida:

| Tabla | Nota |
|-------|------|
| `headquarter_items` | Catálogo efectivo por sede |
| `pos_operators` | Operadores de caja |
| `pos_devices` | Tablets |
| `storage_locations` | Reusada; filas `LocationType.POS` + `headquarter_id` |

## Qué no se toca en el MVP POS

CRM, contracts, payroll, tasks, notifications (salvo aviso futuro de incidencia), files management, attendance.

La web de almacén (`InventoryTransactionController`) permanece. El POS **no** usa `POST .../transactions/sale`.

## Dos superficies, un deploy

```text
Device API        /api/v1/pos/**           JWT typ=device, scope=pos:sync
POS admin (nuevo) /api/v1/pos/admin/**     JWT staff (ADMIN para incidencias)
Web existente     /api/v1/inventory|users|headquarters|…   JWT staff
```

Device API no lista todas las sedes ni crea ítems. Web no ingiere el outbox de la tablet. No hay CRUD duplicado bajo `/admin/pos` para productos/sedes.

## Límites explícitos

- Sin event sourcing completo.
- Sin sync tabla-a-tabla.
- Sin LAN entre tablets.
- Una sede por dispositivo; el cliente no cambia de sede.
- Sin orden global entre tablets; `deviceSequence` es por dispositivo.
- Dinero en el wire POS: **centavos enteros** (D9).
- IDs de hechos: UUID; IDs de maestros Long como string decimal en el wire (D2).

## Relación con la web

Web Central administra maestros e incidencias. Pantallas nuevas son trabajo de `web/`; el backend deja endpoints — [06-admin-api-web.md](06-admin-api-web.md).
