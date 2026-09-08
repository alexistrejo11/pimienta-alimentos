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

Snapshot oficial del Corte Z: pagos por método, efectivo esperado y contado, diferencia, mermas, cancelaciones, descuentos y total/cantidad de sangrías. Existe uno aprobado por turno. El snapshot conserva los totales usados para el cierre y referencias a los registros de sangría del turno.

### `CashWithdrawal` (sangría)

Hecho inmutable de salida física de efectivo para resguardo durante un turno abierto. En el MVP su único tipo/motivo permitido es `SAFEKEEPING` / `RESGUARDO_EFECTIVO`; no modela gastos, proveedores ni ingresos de caja.

Conserva UUID técnico, folio visible consecutivo por tablet y turno, sede, dispositivo, turno, importe positivo, motivo, fecha/hora, cajero titular del turno y Manager/Superadmin que lo autorizó. Al confirmarse crea también `AuditEntry`, `OutboxEvent` y `PrintJob` en la misma transacción local. El saldo teórico de efectivo se deriva de las ventas y estas sangrías; no existe un saldo editable de cajón.

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
- tipo: catálogo, peso, monto abierto manual o pendiente de catálogo;
- barcode de origen opcional para una línea pendiente de catálogo;
- cantidad exacta;
- precio unitario capturado;
- subtotal;
- indicador de stock negativo/no disponible;
- autorización asociada cuando aplique.

Cambiar el catálogo nunca modifica una línea confirmada.

### `SaleDiscount`

Descuento único de la venta, parcial o total, con importe fijo en pesos, motivo obligatorio y autorización de Manager/Superadmin. No hay porcentajes ni descuentos por línea en el MVP. Si cubre el total, la venta queda marcada como cortesía.

### `Payment`

Componente de pago asociado a la venta:

- `CASH`;
- `EXTERNAL_CARD_MP`;
- `CORTESIA`.

Una venta mixta contiene más de un componente. La suma aplicada cubre exactamente el total. La tarjeta externa se confirma manualmente.

`CORTESIA` es el único componente de una venta cuyo total neto es cero: no representa un ingreso ni efectivo en cajón y debe coincidir con un descuento por el total bruto. Aun así, la venta y sus movimientos de inventario son reales.

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

Intento durable de impresión vinculado a un documento operativo. Incluye `documentType` (`SALE`, `CASH_WITHDRAWAL` o `SHIFT_CLOSE`), `documentId`, versión de plantilla, tipo de intento (`ORIGINAL` o `DUPLICATE`) y estado. Estados: `PENDING`, `PRINTING`, `PRINTED`, `FAILED`. Una reimpresión crea otro intento marcado como duplicado. Los comandos ESC/POS generados no se persisten.

### `SyncCursor`

Cursor opaco por flujo de descarga y sede/dispositivo. Solo avanza cuando el lote remoto se aplicó completamente en Room.

### `AuditEntry`

Bitácora append-only de acciones sensibles: autorizaciones, conteos, rechazos, cierres, cancelaciones, reimpresiones, limpieza y resoluciones.

### `SyncIncident`

Representación central de una venta recibida que requiere revisión. Solo Superadmin puede resolverla mediante clasificación o aceptación con una nota de auditoría. La resolución no modifica el payload original ni vincula retrospectivamente líneas pendientes a un producto.

## Relaciones principales

```text
Site 1 ── N Device
Site 1 ── N Shift
Site 1 ── N SiteProduct N ── 1 Product
Device 1 ── N Shift
Shift 1 ── N Sale
Shift 1 ── N CashCountAttempt
Shift 1 ── 0..1 ShiftClose
Shift 1 ── N CashWithdrawal
Sale 1 ── N SaleLine
Sale 1 ── N Payment
Sale 1 ── N Authorization
Sale 1 ── 0..1 SaleCancellation
Sale 1 ── N InventoryMovement
Sale 1 ── N PrintJob
CashWithdrawal 1 ── N PrintJob
ShiftClose 1 ── N PrintJob
Hecho local 1 ── 1 OutboxEvent
```

## Invariantes principales

1. Una tablet solo tiene un turno activo.
2. Una venta confirmada no se edita ni elimina.
3. Confirmar venta crea pagos, movimientos, outbox y trabajo de impresión atómicamente.
4. El total pagado aplicado equivale al total final de la venta.
5. Un descuento requiere autorización y existe como máximo uno por venta MVP.
6. Una venta confirmada no permite cambiar precios ni descuentos; toda modificación posterior sigue el flujo de cancelación autorizado.
7. El monto abierto manual requiere importe positivo, categoría, descripción y autorización. Una línea pendiente de catálogo requiere barcode crudo e importe positivo, usa categoría generada y no tiene `productId`.
8. Solo una venta totalmente en efectivo puede cancelarse después del cobro en MVP.
9. Solo productos con inventario controlado generan movimientos de stock por venta.
10. Folio visible y UUID interno nunca se reutilizan.
11. Cerrar turno requiere conteo ciego y aprobación de Manager/Superadmin.
12. Una limpieza local no elimina hechos pendientes de sincronización.
13. Cada dato operativo pertenece a una sede.
14. Una sangría solo se registra sobre un turno abierto, tiene importe positivo, usa el motivo de resguardo y no puede editarse ni eliminarse tras confirmarse.
