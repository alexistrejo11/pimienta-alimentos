# Monto abierto manual

## Propósito

Permitir que la caja termine una venta manual excepcional sin obligar al cajero a escribir texto ni abandonar la pantalla de venta.

No es un producto maestro ni una forma de editar catálogo desde la tablet. Es una línea excepcional, autorizada y marcada para revisión posterior.

## Regla funcional

La tarjeta fija **Monto abierto** aparece en el grid de venta. Al tocarla, abre una superficie temporal de operación —no una pantalla de navegación— que solicita:

1. categoría predefinida;
2. importe positivo con numpad táctil;
3. autorización por PIN de Manager o Superadmin.

No pide descripción libre. El POS construye la descripción visible con el formato `Producto abierto · {categoría}`. Por ejemplo: `Producto abierto · Snacks`.

## Wireframe estático

```text
┌──────────────────────────────────────────────────────────────────────┐
│                  AGREGAR PRODUCTO ABIERTO                             │
├──────────────────────────────────────────────────────────────────────┤
│ Categoría                                                             │
│ [Comida]   [Bebida]   [Snack]   [Varios]                              │
│                                                                      │
│ Importe                                      Numpad                  │
│ $ 45.00                                  [7] [8] [9]                 │
│                                          [4] [5] [6]                 │
│ Descripción generada                     [1] [2] [3]                 │
│ Producto abierto · Snack                 [0] [.] [Borrar]            │
│                                                                      │
│ [Cancelar]                    [Solicitar PIN de Manager]             │
└──────────────────────────────────────────────────────────────────────┘

Después de tocar **Solicitar PIN de Manager**, la misma superficie conserva categoría e importe y muestra un teclado de cuatro dígitos. Si la autorización es válida, agrega una línea al carrito:

```text
1 × Producto abierto · Snack                              $45.00
    Excepción autorizada · no afecta inventario
```

Cancelar no agrega nada y devuelve al carrito intacto.

## Datos registrados

- importe capturado;
- categoría seleccionada;
- descripción generada;
- cajero que solicitó la acción;
- Manager/Superadmin que autorizó;
- sede, tablet, turno y fecha;
- indicador de excepción y estado de revisión.

La línea aparece en ticket e historial, no modifica inventario y no puede transformarse en un producto real desde la tablet.

## Revisión posterior

Solo Superadmin, desde la web central, puede clasificar y aceptar la venta con nota de auditoría. Un Manager puede consultar el historial local, pero no reescribe una venta ya cobrada. La venta no se vincula posteriormente a un producto ni genera inventario histórico; una corrección de existencias se hace mediante conteo físico o ajuste maestro independiente.

## Límites del MVP

- No hay texto libre, foto, variantes ni combos dentro de monto abierto.
- No se permite cambiar precio o categoría después de confirmar la venta.
- La autorización es por cada línea de monto abierto.
