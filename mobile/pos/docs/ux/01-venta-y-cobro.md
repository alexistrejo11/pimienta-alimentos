# Pantalla de venta y cobro

## Propósito

Permitir al cajero completar una venta habitual desde una pantalla fija en orientación horizontal, sin navegar entre pantallas ni esperar red. El flujo feliz es: agregar artículos, elegir/capturar pago y confirmar cobro.

## Diseño estructural

La pantalla usa un layout dividido fijo en tablet de 10–12 pulgadas, aproximadamente 60 % catálogo y 40 % carrito/cobro.

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Estado: sincronización · tablet · turno · cajero · impresora                         [ Admin ] │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ Categorías: [Todos] [Desayunos] [Bebidas] [Snacks]                   [Buscar explícitamente]  │
├─────────────────────────────────────────────┬───────────────────────────────────────────────────┤
│ Catálogo y selección                         │ Carrito activo o panel de cobro                   │
│                                             │                                                   │
│ [Producto] [Producto] [Producto]             │ Próximo folio · artículos                         │
│ [Producto] [Producto] [No disponible]        │ Líneas, cantidades, eliminación y totales         │
│ [Monto abierto]                              │                                                   │
│                                             │ [Limpiar carrito]                                 │
│                                             │ [Cobrar total]                                    │
└─────────────────────────────────────────────┴───────────────────────────────────────────────────┘
```

El identificador de folio visible se genera al confirmar la venta. Antes de hacerlo, el carrito muestra únicamente el próximo folio como referencia visual.

## Áreas y responsabilidades

### Barra de estado

Siempre visible. Expone estado de sincronización, cantidad de eventos pendientes, identidad de tablet, turno activo, cajero, estado de impresora y acceso a Admin.

El acceso a Admin solicita PIN de Manager/Superadmin antes de abrir el panel local. No debe interrumpir ni perder un carrito activo.

### Categorías y búsqueda

Las categorías son el método principal para seleccionar productos sin código. El escáner agrega directamente al carrito sin cambiar de vista.

La búsqueda por texto es una vía secundaria. En modo cajero no debe quedar un campo de texto enfocado permanentemente ni abrir el teclado del sistema durante el flujo normal. Si se habilita, debe ser una acción explícita y controlada.

### Grid de productos

Cada tarjeta debe mostrar nombre, precio efectivo y el estado relevante:

- producto preparado: no muestra una cantidad de stock artificial;
- producto controlado normal: puede mostrar disponibilidad;
- stock bajo/cero/negativo: muestra advertencia, sin bloquear por defecto;
- no disponible por administración: permanece visible pero no permite agregar;
- monto abierto: tarjeta diferenciada que inicia su flujo autorizado.

Tocar una tarjeta agrega una unidad. Si la línea ya existe en el carrito, aumenta su cantidad y conserva el precio capturado en la línea. Si un producto pasa a no disponible mientras ya está en el carrito, esa línea puede cobrarse, pero no aumentarse sin override.

### Carrito

Muestra líneas de venta, cantidad, precio capturado, subtotal por línea, controles de aumentar/disminuir/eliminar, descuento total si existe y total final. No hay modales de navegación para editar una cantidad o quitar una línea.

Limpiar carrito solo afecta una venta no confirmada. No equivale a cancelación postventa.

## Estado de cobro

Al tocar **Cobrar**, el panel derecho reemplaza temporalmente el carrito. El catálogo izquierdo no desaparece, pero no debe aceptar nuevas lecturas o adiciones mientras existe un intento de pago activo.

```text
┌──────────────────────────────────────────────────────────────────┐
│ [Volver al carrito]                                               │
│                                                                  │
│ TOTAL A COBRAR: $115.50                                         │
│                                                                  │
│ [Efectivo] [Tarjeta externa]                                    │
│                                                                  │
│ Efectivo recibido             Denominaciones                    │
│ $200.00                       [$0.50] [$1] [$5] [$10] ...       │
│                                                                  │
│ Numpad táctil                 CAMBIO: $84.50                    │
│ [7] [8] [9]                                                   │
│ [4] [5] [6]                 Ticket automático                 │
│ [1] [2] [3]                                                   │
│ [0] [.] [Borrar]                                                │
│                                                                  │
│ [Cancelar intento]       [Confirmar cobro e imprimir]           │
└──────────────────────────────────────────────────────────────────┘
```

Reglas del estado:

- efectivo usa numpad y botones de denominaciones; el cambio es visible y grande;
- tarjeta externa se confirma en POS solo después de aprobación en la terminal física;
- pago mixto no aparece hasta que el cliente confirme que forma parte del alcance;
- cancelar intento devuelve al carrito intacto;
- confirmar crea la venta localmente y encola impresión/sincronización;
- una falla de impresión muestra un aviso y deja la reimpresión pendiente, sin revertir el cobro.

## Teclado y escáner

El modo cajero se diseña como una experiencia táctil: no depende de un teclado físico ni del teclado nativo de Android. PIN, importes, efectivo y conteo de caja usan numpad integrado.

El lector se prefiere por USB/serial/SPP o SDK del fabricante, para entregar una lectura completa sin tratarlo como teclado de propósito general. Si se usa HID como respaldo, se valida con el modelo real de lector y tablet; la aplicación no debe asumir que puede desactivar globalmente el teclado Android sin efectos secundarios.

## Flujos excepcionales permitidos

Los diálogos/superficies temporales son válidos solo para acciones que requieren confirmación o autorización:

- PIN de Manager/Superadmin;
- monto abierto;
- autorización de sobregiro de inventario;
- forzar venta de producto no disponible;
- descuento o cortesía;
- cancelación postventa en efectivo.

No se usan para navegar entre catálogo, carrito y cobro.

## Monto abierto

El monto abierto es una excepción autorizada que se documenta en [Monto abierto](02-monto-abierto.md). Usa categoría, numpad y PIN; el POS genera su descripción sin requerir teclado alfanumérico.

## Pendiente de diseñar

- panel local de Manager;
- apertura, conteo ciego, corrección y aprobación de Corte Z;
- historial, reimpresión y cancelación de venta en efectivo;
- registro de merma y reposición;
- enrolamiento, acceso y cambio de usuario;
- estados de impresora, sincronización y recuperación de errores.
