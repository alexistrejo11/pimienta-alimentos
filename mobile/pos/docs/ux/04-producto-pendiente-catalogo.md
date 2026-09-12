# Producto pendiente de catálogo

## Propósito

Permitir que un cajero venda sin detener la fila cuando el lector entrega un barcode que no existe en el catálogo local. No crea un `Product`, no pide texto libre y no modifica inventario.

Se diferencia de [Monto abierto manual](02-monto-abierto.md): aquí existe evidencia física en forma de barcode escaneado, por lo que no requiere PIN de Manager. El cajero todavía debe confirmar el importe; un escaneo por sí solo nunca agrega una línea cobrable.

## Flujo

```text
Escáner entrega barcode desconocido
        ↓
Superficie temporal con barcode capturado
        ↓
Cajero captura importe con numpad y confirma
        ↓
Carrito recibe: Producto pendiente de catálogo · {barcode}
```

```text
┌──────────────────────────────────────────────────────────────────┐
│                 PRODUCTO PENDIENTE DE CATÁLOGO                    │
├──────────────────────────────────────────────────────────────────┤
│ Código escaneado                                                   │
│ 7501234567890                                                      │
│                                                                    │
│ Importe                                    Numpad                  │
│ $ 25.00                               [7] [8] [9]                 │
│                                        [4] [5] [6]                 │
│ Producto pendiente · 7501234567890      [1] [2] [3]                 │
│                                        [0] [.] [Borrar]            │
│                                                                    │
│ [Cancelar]                              [Agregar al carrito]       │
└──────────────────────────────────────────────────────────────────┘
```

No hay teclado alfanumérico, selector de categoría ni creación de producto local. Hasta que llegue el delta de catálogo con el producto real, cada lectura del mismo barcode vuelve a pedir importe; el MVP no memoriza precios provisionales.

## Datos y efectos

La línea confirmada conserva:

- `productId` ausente;
- `sourceBarcode` crudo;
- tipo `PENDING_CATALOG` u origen `UNKNOWN_BARCODE`;
- descripción generada, importe, cajero, sede, tablet, turno y fecha;
- categoría de reporte `Pendiente de catálogo`.

No genera movimiento de inventario. Aparece en ticket e historial como fue cobrada y se sincroniza como parte de la venta inmutable.

## Resolución central prospectiva

Superadmin consulta los barcodes pendientes en la Web Central y puede crear el producto maestro con nombre, barcode único, precio, disponibilidad y política de inventario. Esa publicación se sincroniza a las tablets y habilita el reconocimiento del código en **ventas posteriores**.

Las líneas históricas pendientes no se renombran, no reciben `productId`, no se vinculan al producto recién creado y no descuentan inventario hacia atrás. Si hace falta ajustar existencias, se registra un conteo físico o ajuste maestro separado y auditado.
