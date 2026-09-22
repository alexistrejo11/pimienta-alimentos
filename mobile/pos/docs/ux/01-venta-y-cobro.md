# Pantalla de venta y cobro

## Propósito

Permitir al cajero completar una venta habitual desde una pantalla fija en orientación horizontal, sin navegar entre pantallas ni esperar red. El flujo feliz es: agregar artículos, elegir/capturar pago y confirmar cobro.

## Idioma visible

Toda etiqueta, mensaje, estado, acción y error mostrado al personal en la tablet se redacta en **español de México**. Los nombres de producto, categoría o marca recibidos desde catálogo se conservan tal como fueron capturados en el dato maestro. Los identificadores técnicos, nombres de roles internos y estados de API no se exponen como texto de interfaz.

## Diseño estructural

La pantalla usa un layout dividido fijo en tablet de 10–12 pulgadas, aproximadamente 60 % catálogo y 40 % carrito/cobro.

En orientación vertical no se comprime esta división hasta volverla ilegible: catálogo y carrito se alternan mediante dos controles visibles, mientras que el cobro ocupa el área completa. Esta adaptación conserva el mismo carrito y no crea una segunda venta.

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Estado: sincronización · tablet · turno · cajero · impresora       [ Bloquear caja ] [ Admin ] │
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

Siempre visible. Expone estado de sincronización, cantidad de eventos pendientes, identidad de tablet, turno activo, cajero, estado de impresora, **Bloquear caja** y acceso a Admin.

El acceso a Admin solicita PIN de Manager/Superadmin antes de abrir el panel local. No debe interrumpir ni perder un carrito activo.

**Bloquear caja** muestra una pantalla de privacidad y solicita el PIN del cajero responsable para volver. No cierra el turno, no muestra selección de perfil y no equivale a Corte Z. Si el cajero estaba en cobro, el panel vuelve a carrito y descarta solamente el borrador de pago no confirmado.

```text
┌──────────────────────────────────────────────────────────────┐
│                         Pimienta POS                          │
│                                                              │
│                         Caja bloqueada                        │
│            Turno activo · Cajero responsable: Marco          │
│                                                              │
│                         PIN [ • • • • ]                      │
│                                                              │
│                    [ Desbloquear caja ]                       │
│                                                              │
│ El turno y el carrito continúan resguardados en la tablet.    │
└──────────────────────────────────────────────────────────────┘
```

### Categorías y búsqueda

Las categorías son el método principal para seleccionar productos sin código. El escáner agrega directamente al carrito sin cambiar de vista.

La búsqueda por texto es una vía secundaria. En modo cajero no debe quedar un campo de texto enfocado permanentemente ni abrir el teclado del sistema durante el flujo normal. Si se habilita, debe ser una acción explícita y controlada.

### Grid de productos

Cada tarjeta debe mostrar nombre, precio efectivo y el estado relevante:

- producto preparado: no muestra una cantidad de stock artificial;
- producto controlado normal: puede mostrar disponibilidad;
- stock bajo/cero/negativo: muestra advertencia, sin bloquear por defecto;
- no disponible por administración: permanece visible pero no permite agregar;
- monto abierto: tarjeta diferenciada que inicia su flujo de categoría e importe.

Tocar una tarjeta agrega una unidad. Si la línea ya existe en el carrito, aumenta su cantidad y conserva el precio capturado en la línea. Si un producto pasa a no disponible mientras ya está en el carrito, esa línea puede cobrarse, pero no aumentarse sin override.

Un barcode escaneado sin coincidencia no se trata como monto abierto manual: abre el flujo de [Producto pendiente de catálogo](04-producto-pendiente-catalogo.md), con el código capturado y un importe numérico por confirmar.

### Carrito

Muestra líneas de venta, cantidad, precio capturado, subtotal por línea, controles de aumentar/disminuir/eliminar, descuento total si existe y total final. No hay modales de navegación para editar una cantidad o quitar una línea.

El botón **Descuento / cortesía** abre una superficie temporal, no una pantalla de navegación. Solicita importe fijo en pesos, motivo obligatorio y PIN de Manager/Superadmin. Al aprobarse, el carrito muestra el total bruto, el descuento y el total neto. Si el descuento cubre todo el total, el botón de confirmación pasa a **Confirmar cortesía e imprimir**; no se presenta efectivo ni tarjeta. Esta acción solo está disponible antes de confirmar el cobro.

Limpiar carrito solo afecta una venta no confirmada. No equivale a cancelación postventa.

### Sangría desde la barra de caja

La barra superior operativa del POS incluye el botón **Sangría** mientras exista un turno abierto. Está fuera del panel derecho de pago: una sangría no es un cobro al cliente. El cajero puede iniciarla sin abandonar ni perder su carrito; se abre un modal temporal con importe numérico, el motivo fijo `Resguardo de efectivo` y el PIN de Manager/Superadmin para firmar la acción.

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Pimienta POS · Turno 104 · Marco       [Sangría] [Bloquear caja] [Admin]   │
└────────────────────────────────────────────────────────────────────────────┘

                 ┌──────────────────────────────────────────────┐
                 │ REGISTRAR SANGRÍA · TURNO 104                 │
                 │ Motivo: Resguardo de efectivo                 │
                 │                                              │
                 │ Importe: $ [ 1,500.00 ]                      │
                 │                                              │
                 │ PIN Manager / Superadmin: [ • • • • ]        │
                 │                                              │
                 │ [Cancelar] [Confirmar e imprimir comprobante]│
                 └──────────────────────────────────────────────┘
```

No se permite abrir este modal durante la confirmación atómica de una venta. Si existe un borrador de pago, el cajero debe volver al carrito primero; no hay pago ni efectivo registrado hasta confirmar la venta. Al confirmar una sangría, el modal se cierra, el carrito permanece intacto y se muestra el comprobante pendiente o impreso según corresponda.

## Estado de cobro

Al tocar **Cobrar**, el panel derecho reemplaza temporalmente el carrito. El catálogo izquierdo permanece visible: no es una nueva pantalla ni un modal de pantalla completa.

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

### Producto olvidado durante cobro

Mientras el cobro sea un borrador —es decir, antes de que el cajero pulse **Confirmar cobro e imprimir**— el catálogo y el escáner siguen pudiendo recibir una selección de producto. Si ocurre:

1. se cancela el borrador de cobro sin crear pago, venta ni movimiento de inventario;
2. se agrega el artículo escaneado o tocado al carrito con sus reglas normales;
3. el panel derecho vuelve automáticamente a **Carrito**;
4. se muestra un aviso breve: `Se agregó {producto}. Total actualizado.`

No se abre una confirmación ni se obliga al cajero a pulsar primero **Volver al carrito**. El método de pago elegido y, para efectivo, el importe capturado pueden conservarse únicamente como borrador de interfaz para cuando el cajero vuelva a cobrar; no son un pago registrado ni afectan la caja.

Al pulsar **Confirmar cobro e imprimir**, la interfaz entra en estado de confirmación atómica y bloquea nuevas lecturas, toques de producto y modificaciones hasta obtener el resultado local. Esto evita alterar una venta que ya está siendo registrada.

Con tarjeta externa, el POS no puede saber si la terminal física ya aprobó un cargo. La regla operativa es que, una vez aprobado en Mercado Pago, el cajero confirme inmediatamente la venta en el POS y no agregue más artículos. Si se detecta un producto faltante antes de esa aprobación, se usa el retorno automático al carrito.

## Teclado y escáner

El modo cajero se diseña como una experiencia táctil: no depende de un teclado físico ni del teclado nativo de Android. PIN, importes, efectivo y conteo de caja usan numpad integrado.

El lector se prefiere por USB/serial/SPP o SDK del fabricante, para entregar una lectura completa sin tratarlo como teclado de propósito general. Si se usa HID como respaldo, se valida con el modelo real de lector y tablet; la aplicación no debe asumir que puede desactivar globalmente el teclado Android sin efectos secundarios.

## Flujos excepcionales permitidos

Los diálogos/superficies temporales son válidos solo para acciones que requieren confirmación o autorización:

- PIN de Manager/Superadmin;
- monto abierto;
- producto pendiente de catálogo por barcode desconocido;
- autorización de sobregiro de inventario;
- forzar venta de producto no disponible;
- descuento o cortesía;
- cancelación postventa en efectivo.

No se usan para navegar entre catálogo, carrito y cobro.

## Monto abierto

El monto abierto manual es una excepción de catálogo que se documenta en [Monto abierto](02-monto-abierto.md). Usa categoría y numpad, sin PIN; el POS genera su descripción sin requerir teclado alfanumérico. Un barcode no encontrado usa el flujo separado de [Producto pendiente de catálogo](04-producto-pendiente-catalogo.md).

## Pendiente de diseñar

- panel local de Manager;
- apertura, conteo ciego, corrección y aprobación de Corte Z;
- historial, reimpresión y cancelación de venta en efectivo;
- registro de merma y reposición;
- enrolamiento, acceso y cambio de usuario;
- estados de impresora, sincronización y recuperación de errores.
