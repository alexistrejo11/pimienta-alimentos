# Fase 0: contrato cerrado

Estado: **cerrado para implementar** · 2026-09-16

Este documento fija las decisiones que deben respetar las siguientes fases de
backend, web y Android. El código actual implementa una parte del contrato,
pero no se considera conforme hasta completar las correcciones descritas aquí.

## 1. Deltas descendentes

La ruta pública se mantiene:

```http
GET /api/v1/pos/sync/changes?cursor={cursor}
```

El cursor objetivo será opaco y basado en una secuencia monotónica por sede,
no en `Instant`, `updatedAt` ni en la hora local del servidor. La respuesta
debe incluir operaciones ordenadas y un `nextCursor` que represente un límite
estable de lectura.

Cada mutación relevante y su registro en el feed deben confirmarse en la misma
transacción. Una tablet no puede perder una operación por una carrera entre la
lectura del catálogo y la creación del watermark.

Las operaciones mínimas son `product`, `operator` y `policies`; cada una admite
`upsert` y las entidades aplicables admiten `deactivate`.

El servidor devolverá `409 POS_SYNC_CURSOR_INVALID` cuando el cursor sea
inválido, pertenezca a otra sede, sea anterior al historial retenido o no sea
compatible con el `schemaVersion`. La tablet debe conservar sus hechos locales
y ejecutar bootstrap; nunca debe borrar ventas, turnos, impresión ni outbox.

Un lote debe tener límite explícito. Si quedan operaciones, la tablet aplica el
lote y continúa desde `nextCursor`. Una operación desconocida, incompleta o de
un schema no soportado hace fallar el lote y el cursor no avanza.

## 2. Autoridad de inventario

`headquarter_items` es la autoridad de configuración comercial por sede:

- precio de venta;
- categoría de venta;
- disponibilidad;
- política de stock;
- límite de stock negativo.

La cantidad que consume el POS es la fila de `inventory_stock` asociada a la
ubicación canónica `POS` de la sede. El servidor aplica movimientos, no saldos
enviados por la tablet. Si `headquarter_pos_settings.stockless` es verdadero,
esas ventas y cancelaciones POS **no** generan movimiento.

Una entrada `IN`, merma `OUT`, transferencia, ajuste, conteo o venta solo debe
producir un cambio de stock para tablets cuando modifica esa ubicación POS. El
feed publica el producto proyectado completo, incluido `stockQuantity`.

Las ventas offline de varias tablets se acumulan centralmente. La tablet debe
mantener separada la existencia central conocida de los movimientos locales aún
no confirmados para no sobreescribir visualmente ventas pendientes al aplicar
un delta.

## 3. Excepciones de catálogo

`PENDING_CATALOG` y `OPEN_AMOUNT` son hechos distintos y no se convierten entre
sí automáticamente.

### `OPEN_AMOUNT`

- requiere `allowOpenProducts=true`;
- requiere una categoría incluida en `openAmountCategories`;
- requiere importe positivo y cantidad `1`;
- usa `productId=null` y `rawBarcode=null`;
- usa `stockPolicy=NOT_CONTROLLED`;
- no requiere PIN; el cajero confirma categoría e importe;
- `authorizedByOperatorId` y `authorizedAt` pueden ser nulos;
- no genera movimiento de inventario;
- el backend conserva la venta y la acepta (`ACCEPTED`), sin incidencia.

La descripción se genera como `Producto abierto · {categoría}`. No se captura
nota o descripción libre en caja. El monto abierto no se cataloga después.

### `PENDING_CATALOG`

Un barcode desconocido conserva el flujo rápido existente: barcode crudo,
importe y descripción generada, sin categoría manual, PIN ni movimiento de
inventario. El servidor acepta la venta (`ACCEPTED`) con `productId` nulo. La
acción de monto abierto puede ofrecerse como alternativa, pero debe ser
explícita y no borrar la evidencia del barcode.

## 4. Configuración web y ruta

La configuración y catálogo POS permanecen separados:

- `/app/pos/configuracion`: políticas POS, incluido monto abierto.
- `/app/pos/catalogo`: catálogo operativo por sede.

`/app/pos/sedes` no es la ruta canónica actual. Si producto exige esa URL, se
implementará como alias o redirección a las superficies anteriores; no se
duplicará el flujo ni el estado.

El editor de categorías permitidas debe enviar una lista normalizada, sin
duplicados ni valores vacíos. Con monto abierto activo, la lista debe contener
al menos una categoría válida.

## 5. Criterios de aceptación de Fase 0

- Un cambio confirmado en catálogo, operador, política o stock POS aparece en
  exactamente un intervalo posterior del feed.
- Bootstrap y deltas no pierden cambios concurrentes.
- Un cursor inválido o retenido provoca bootstrap sin perder hechos locales.
- La aplicación Android no avanza el cursor si no entiende una operación.
- La cantidad tablet-visible proviene de `inventory_stock` en la ubicación POS.
- `OPEN_AMOUNT` y `PENDING_CATALOG` tienen payloads y reglas diferentes.
- La UI web puede habilitar/deshabilitar monto abierto y administrar categorías
  válidas sin depender de valores que el backend descarte.
