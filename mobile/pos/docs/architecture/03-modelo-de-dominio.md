# Modelo conceptual de dominio

Este modelo describe conceptos y relaciones. No es todavía el esquema de Room ni PostgreSQL; ambos pueden necesitar representaciones diferentes.

## Identidad y alcance

### `Site`

Representa una sede o sucursal. Todo dispositivo, turno, venta y movimiento de inventario pertenece a una sede. El nombre se alineará con la entidad equivalente del backend existente.

### `Device`

Tablet enrolada para una sede. Mantiene UUID técnico, código visible estable para folios, estado de autorización y secuencia local de eventos.

### `UserLocalSnapshot`

Copia local de un usuario central autorizado para la sede, con rol, estado y verificador de PIN. No es el registro maestro del usuario.

Roles: `CASHIER`, `MANAGER`, `SUPERADMIN`.

## Catálogo

### `Product`

Identidad maestra global del producto:

- ID global;
- nombre y descripción;
- código/SKU o barcode opcional y único;
- categoría textual en MVP;
- costo y precio base;
- unidad de venta: pieza o kilogramo;
- controla inventario o no;
- stock mínimo.

### `SiteProduct`

Configuración efectiva del producto en una sede:

- sede y producto;
- habilitado/no disponible;
- precio local opcional;
- precio efectivo resuelto;
- saldo central conocido y versión.

La tablet recibe una proyección de `Product + SiteProduct` lista para vender.

### `OperationalConfig`

Configuración versionada por sede: modo de inventario, límite negativo, umbrales de catálogo desactualizado, categorías de monto abierto y otras políticas operativas.

## Turno y caja

### `Shift`

Un turno pertenece a una tablet, sede y cajero. Solo puede existir uno activo por tablet.

Estados propuestos:

```text
OPEN → CASH_COUNT_SUBMITTED → OPEN (corrección)
                         └──→ CLOSED (PIN Manager)
```

Conserva fondo inicial, apertura, cierre, cajero, aprobador y totales congelados.

### `CashCountAttempt`

Cada conteo ciego enviado por el cajero. Conserva desglose, total, fecha, actor y resultado `SUBMITTED`, `REJECTED` o `APPROVED`. Los intentos rechazados nunca se eliminan.

### `ShiftClose`

Snapshot oficial del Corte Z: pagos por método, efectivo esperado y contado, diferencia, mermas, cancelaciones y descuentos. Existe uno aprobado por turno.

## Venta

### `Sale`

Venta identificada por UUID interno y folio visible. Pertenece a sede, dispositivo, turno y cajero.

Estados propuestos:

```text
DRAFT → PAYMENT_IN_PROGRESS → CONFIRMED → CANCELLED
             └──────────────→ DRAFT
```

`DRAFT` puede ser solo estado de sesión/UI si no se requiere recuperar carritos después de cerrar la app. `CONFIRMED` es inmutable. `CANCELLED` conserva la venta original y agrega una reversión.

### `SaleLine`

Snapshot de lo vendido:

- producto opcional;
- nombre y categoría capturados, más descripción snapshot o generada;
- tipo: catálogo, peso o monto abierto;
- cantidad exacta;
- precio unitario capturado;
- subtotal;
- indicador de stock negativo/no disponible;
- autorización asociada cuando aplique.

Cambiar el catálogo nunca modifica una línea confirmada.

### `SaleDiscount`

Descuento único de la venta, parcial o total, con importe, motivo y autorización de Manager/Superadmin. No hay descuentos por línea en el MVP.

### `Payment`

Componente de pago asociado a la venta:

- `CASH`;
- `EXTERNAL_CARD_MP`.

Una venta mixta contiene más de un componente. La suma aplicada cubre exactamente el total. La tarjeta externa se confirma manualmente.

### `Authorization`

Evidencia de un override: actor solicitante, actor autorizador, acción, motivo, entidad afectada, valores relevantes y fecha. Autorizar no cambia la sesión del cajero.

### `SaleCancellation`

Reversión total de una venta exclusivamente en efectivo, del turno activo y autorizada. Conserva motivo, aprobador, devolución de efectivo y movimientos inversos; no elimina la venta.

## Inventario

### `InventoryMovement`

Hecho inmutable que aumenta o disminuye inventario:

- venta;
- cancelación de venta;
- merma;
- reposición operativa;
- ajuste maestro;
- resolución de incidencia.

Incluye UUID, sede, producto, cantidad con signo, origen, dispositivo/usuario y fecha. La tablet envía movimientos, nunca un comando “establecer saldo a X”.

### `LocalStockBalance`

Proyección local para consulta rápida. Puede estar desactualizada o ser negativa. Se deriva de un snapshot central conocido más los movimientos locales posteriores.

## Infraestructura operativa

### `OutboxEvent`

Evento durable creado en la misma transacción que el hecho local. Incluye secuencia por dispositivo, tipo, versión de esquema, agregado, payload y estado de entrega.

Estados sugeridos:

- `PENDING`;
- `RETRY`;
- `ACKNOWLEDGED`;
- `ACKNOWLEDGED_REVIEW`;
- `BLOCKED_TECHNICAL`.

No existe un estado que descarte silenciosamente una venta cobrada.

### `PrintJob`

Intento durable de impresión vinculado a una venta. Estados: `PENDING`, `PRINTING`, `PRINTED`, `FAILED`. Una reimpresión crea otro intento marcado como duplicado.

### `SyncCursor`

Cursor opaco por flujo de descarga y sede/dispositivo. Solo avanza cuando el lote remoto se aplicó completamente en Room.

### `AuditEntry`

Bitácora append-only de acciones sensibles: autorizaciones, conteos, rechazos, cierres, cancelaciones, reimpresiones, limpieza y resoluciones.

### `SyncIncident`

Representación central de una venta recibida que requiere revisión. Solo Superadmin puede resolverla mediante clasificación/aceptación o vínculo a producto/movimiento. La resolución no modifica el payload original.

## Relaciones principales

```text
Site 1 ── N Device
Site 1 ── N Shift
Site 1 ── N SiteProduct N ── 1 Product
Device 1 ── N Shift
Shift 1 ── N Sale
Shift 1 ── N CashCountAttempt
Shift 1 ── 0..1 ShiftClose
Sale 1 ── N SaleLine
Sale 1 ── N Payment
Sale 1 ── N Authorization
Sale 1 ── 0..1 SaleCancellation
Sale 1 ── N InventoryMovement
Sale 1 ── N PrintJob
Hecho local 1 ── 1 OutboxEvent
```

## Invariantes principales

1. Una tablet solo tiene un turno activo.
2. Una venta confirmada no se edita ni elimina.
3. Confirmar venta crea pagos, movimientos, outbox y trabajo de impresión atómicamente.
4. El total pagado aplicado equivale al total final de la venta.
5. Un descuento requiere autorización y existe como máximo uno por venta MVP.
6. El monto abierto requiere importe positivo, categoría, descripción y autorización.
7. Solo una venta totalmente en efectivo puede cancelarse después del cobro en MVP.
8. Solo productos con inventario controlado generan movimientos de stock por venta.
9. Folio visible y UUID interno nunca se reutilizan.
10. Cerrar turno requiere conteo ciego y aprobación de Manager/Superadmin.
11. Una limpieza local no elimina hechos pendientes de sincronización.
12. Cada dato operativo pertenece a una sede.
