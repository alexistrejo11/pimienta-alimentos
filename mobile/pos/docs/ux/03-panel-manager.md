# Panel local de Manager

## Propósito

Permitir supervisar y operar una tablet durante el turno sin depender de internet. No pretende ser un ERP, un panel de configuración global ni un sustituto de la web administrativa.

El acceso requiere PIN de Manager o Superadmin. Entrar y salir del panel no cambia la identidad del cajero que mantiene el turno ni elimina un carrito en curso. El **conteo ciego** es la excepción: el cajero titular puede iniciarlo desde la acción `Cerrar turno` de la caja, pero la revisión, corrección y aprobación siguen requiriendo Manager/Superadmin.

**Impresión y dispositivos** (ver por dónde sale el ticket, elegir o quitar la térmica Bluetooth de respaldo, dar permiso USB, probar ticket/cajón/lectura) no es exclusivo de Manager: cualquier cajero lo abre desde **Acciones → Dispositivos** o el banner sin turno. La sección Manager **Estado** reutiliza el mismo panel.

## Alcance local

El panel contiene un dashboard y cuatro secciones operativas:

1. **Dashboard / Resumen del día:** métricas locales de la tablet y cinco productos más vendidos.
2. **Caja y Corte Z:** preparación, validación, cierre y consulta de sangrías del turno activo.
3. **Inventario operativo:** reposiciones, mermas y movimientos recientes.
4. **Historial local:** tickets originados en esta tablet, reimpresión y cancelación permitida.
5. **Estado:** aplicación, sincronización y el mismo panel de **Impresión y dispositivos** que el cajero.

La acción **Abrir Web Central** permanece fija en el encabezado y se ofrece de nuevo desde Inventario/Estado. Requiere internet y no sustituye las operaciones locales.

No incluye clientes, fiados, apartados, proveedores, compras, CSV, catálogo maestro, ajustes masivos, configuración por sede, reportes consolidados, licencias ni administración de usuarios. Esas funciones pertenecen a la web central.

## Wireframe estructural

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│ [Volver a caja]   Panel local de Manager · Tablet T1 · Turno 104                 │
├───────────────────┬──────────────────────────────────────────────────────────────┤
│ Resumen del día   │ Sección activa                                                │
│ Caja y Corte Z    │                                                              │
│ Inventario        │                                                              │
│ Historial         │ Contenido local de la tablet                                  │
│ Estado            │                                                              │
│                   │                                                              │
└───────────────────┴──────────────────────────────────────────────────────────────┘
```

Las secciones pueden ser navegación lateral en orientación horizontal. No deben convertirse en una barra superior con decenas de módulos.

## Caja y Corte Z

Esta sección reúne el efectivo operativo y el cierre, sin convertirlos en la misma acción. Una sangría mantiene el turno abierto; el Corte Z lo cierra. El cierre conserva el doble control ya definido: cajero cuenta, Manager aprueba. La pantalla cambia por estado; no muestra toda la información a todos los actores al mismo tiempo.

### 1. Turno abierto: efectivo y sangrías

Esta vista solo aparece a Manager/Superadmin. Muestra el resumen del turno y el efectivo teórico para supervisar resguardos, pero nunca se reutiliza como pantalla de conteo del cajero.

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ CAJA Y CORTE Z · TURNO 104 · ABIERTO                                        │
│ Cajero: Marco · Apertura: 08:00 · Fondo inicial: $500.00                    │
├────────────────────────────────────────────────────────────────────────────┤
│ Efectivo teórico en cajón: $2,800.00                                       │
│ Sangrías de resguardo: 2 · $1,500.00        [Ver detalle]                    │
│                                                                            │
│ Resumen comercial                                                     │
│ Venta bruta  $4,500.00 · Descuentos $350.00 · Venta neta $4,150.00          │
│ Efectivo $3,800.00 · Tarjeta $350.00 · Cortesías $0.00                      │
│                                                                            │
│ [Solicitar cierre / iniciar conteo ciego]                                  │
└────────────────────────────────────────────────────────────────────────────┘
```

El efectivo teórico se calcula, no se edita. Sus componentes se pueden desplegar: fondo inicial, efectivo neto cobrado, devoluciones de efectivo por cancelación y sangrías confirmadas. Tarjeta y cortesías son visibles en el resumen comercial, pero no participan en el efectivo del cajón.

### 2. Consulta de sangrías de resguardo

Una sangría se inicia desde el botón fijo **Sangría** de la barra de caja, no desde el Panel de Manager. El cajero captura el importe en un modal y Manager/Superadmin la firma con PIN. Esta sección solamente permite supervisar el total, consultar el detalle y reimprimir comprobantes.

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ SANGRÍAS DE RESGUARDO · TURNO 104                                          │
│ Total retirado: $1,500.00 · 2 registros                 [Ver detalle]      │
│                                                                            │
│ Las nuevas sangrías se registran desde la barra de caja.                    │
│ Este panel es de consulta y reimpresión.                                    │
└────────────────────────────────────────────────────────────────────────────┘
```

Cada sangría confirmada persiste `CashWithdrawal`, auditoría, evento de outbox y trabajo de impresión de manera atómica. Si existe hardware de cajón compatible, su apertura puede ejecutarse como efecto posterior; una falla de periférico no revierte una sangría ya confirmada.

### 3. Detalle de sangrías del turno

El total no sustituye los registros. La lista permite verificar cuánto se retiró, cuándo, quién lo autorizó y si se imprimió/sincronizó. Los renglones son de solo lectura: una sangría confirmada no se edita ni se elimina.

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ SANGRÍAS DE RESGUARDO · TURNO 104                                          │
│ Total retirado: $1,500.00 · 2 registros                                    │
├────────────────────────────────────────────────────────────────────────────┤
│ 10:18 · SG-T1-104-0001 · $1,000.00                                        │
│ Marco (cajero) · Autorizó: Laura · Impreso · Sincronizado                  │
│ [Reimprimir comprobante]                                                   │
│                                                                            │
│ 12:04 · SG-T1-104-0002 · $500.00                                          │
│ Marco (cajero) · Autorizó: Laura · Impresión pendiente · En cola           │
│ [Reimprimir comprobante]                                                   │
└────────────────────────────────────────────────────────────────────────────┘
```

El folio `SG-{tablet}-{turno}-{consecutivo}` es visible para búsqueda y comprobantes; el UUID técnico se usa para sincronización idempotente. El formato exacto se alinea con el de los folios de ticket antes de implementación.

### 4. Conteo ciego del cajero

El cajero inicia el cierre y captura el desglose físico con numpad. **No ve el efectivo esperado, las sangrías calculadas ni la diferencia**. Solo ve el valor de su propio conteo. Puede abandonar antes de enviarlo sin cerrar el turno.

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ CONTEO CIEGO · CAJERO · TURNO 104                                          │
│ Cuenta el efectivo físico del cajón. El esperado se verá al validar.        │
├───────────────────────────────────────┬────────────────────────────────────┤
│ Denominación         Cantidad          │ Numpad táctil                       │
│ $0.50                 [   0 ]          │ [7] [8] [9]                         │
│ $1.00                 [   0 ]          │ [4] [5] [6]                         │
│ $5.00                 [   0 ]          │ [1] [2] [3]                         │
│ $10.00                [   0 ]          │ [0] [⌫] [Limpiar]                    │
│ $20 · $50 · $100 · $200 · $500          │                                      │
│                                       │ Total de mi conteo: $2,800.00        │
├───────────────────────────────────────┴────────────────────────────────────┤
│ [Cancelar y volver a caja]                    [Enviar a validación]         │
└────────────────────────────────────────────────────────────────────────────┘
```

### 5. Validación de Manager/Superadmin

Solo tras enviar el conteo se muestra al Manager/Superadmin el desglose comercial y de arqueo. Puede aprobar con PIN o devolverlo para corrección. Cada intento queda en bitácora; devolverlo no borra el intento anterior.

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ VALIDAR CORTE Z · MANAGER · TURNO 104                                      │
├────────────────────────────────────────────────────────────────────────────┤
│ RESUMEN COMERCIAL                                                           │
│ Venta bruta                  $4,500.00                                     │
│ (-) Descuentos               $  350.00                                     │
│ (=) Venta neta               $4,150.00                                     │
│     Efectivo $3,800.00 · Tarjeta $350.00 · Cortesías $0.00                │
│                                                                            │
│ ARQUEO DE EFECTIVO                                                         │
│ Fondo inicial                $  500.00                                     │
│ (+) Efectivo neto cobrado    $3,800.00                                     │
│ (-) Devoluciones efectivo    $    0.00                                     │
│ (-) Sangrías (2)             $1,500.00  [Ver detalle]                      │
│ (=) Efectivo esperado        $2,800.00                                     │
│ Conteo físico enviado        $2,780.00                                     │
│ DIFERENCIA                   -$   20.00  Faltante                          │
│                                                                            │
│ Mermas · Cancelaciones · Descuentos: [Ver resumen de auditoría]            │
│ [Devolver para corregir]              [Aprobar con PIN y cerrar turno]     │
└────────────────────────────────────────────────────────────────────────────┘
```

Al aprobar, el POS congela el corte, registra el evento de cierre e intenta imprimirlo. La falta de red o impresión no bloquea el cierre; impresión y sincronización quedan pendientes si fallan.

El comprobante de Corte Z contiene esos mismos dos bloques, además de mermas, cancelaciones y estado de sincronización. No se muestran gastos, pagos a proveedores ni ingresos adicionales porque no forman parte del MVP definido.

## Inventario operativo

Un Manager puede registrar hechos operativos, no editar el inventario maestro.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ INVENTARIO OPERATIVO                                                   │
│ [Registrar reposición]   [Registrar merma]                            │
│                                                                      │
│ Producto: [buscar o seleccionar]                                     │
│ Cantidad: [numpad]       Motivo: [botones predefinidos]              │
│                                                                      │
│ Últimos movimientos de esta tablet                                   │
│ 09:42 · Refresco +12 · Reabastecimiento                              │
│ 10:06 · Muffin -2 · Producto dañado                                  │
└──────────────────────────────────────────────────────────────────────┘
```

Una reposición o merma se aplica localmente y genera su evento de sincronización. Los productos sin inventario controlado pueden registrar merma operativa, pero no modifican stock.

## Historial local

Muestra únicamente ventas creadas por la tablet, incluyendo las pendientes de sincronizar.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ HISTORIAL DE ESTA TABLET · TURNO 104                                  │
│ [Buscar por folio]                                                    │
│                                                                      │
│ 10:22 · T1-104-0023 · $115.00 · Efectivo · Impreso                   │
│          [Reimprimir] [Cancelar]                                     │
│ 10:19 · T1-104-0022 · $76.00  · Tarjeta externa · Impreso            │
│          [Reimprimir] [No cancelable en MVP]                         │
└──────────────────────────────────────────────────────────────────────┘
```

Cancelar está disponible solo para venta totalmente en efectivo, durante el turno actual y con autorización de Manager/Superadmin. No se ofrece para tarjeta externa.

## Estado e impresión y dispositivos

La configuración de periféricos vive en **Impresión y dispositivos**. No requiere PIN y la usa cualquier cajero, así que el panel responde una sola pregunta: **¿por dónde sale el siguiente ticket y qué hago si no sale?** No muestra direcciones MAC, nombres internos de error ni conceptos de cola.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ IMPRESIÓN Y DISPOSITIVOS                                              │
│ Revisa la impresora y el lector de esta caja                         │
│                                                                      │
│ ● Impresora                                                          │
│   Lista · por cable USB                                              │
│   Si desconectas el cable, se imprimirá por Bluetooth (POS-5890A).   │
│ ● Lector                                                             │
│   Detectado                                                          │
│                                                                      │
│ Impresora Bluetooth                                                  │
│ Ahora se imprime por cable. La impresora elegida aquí se usa cuando  │
│ desconectas el cable.                                                │
│ POS-5890A · Elegida · en espera mientras haya cable                  │
│ ZJ-5802                                           [Usar esta]        │
│ Lector emparejado: Shawty BT.                                        │
│ [Quitar impresora Bluetooth]                                         │
│                                                                      │
│ Probar · Las pruebas no registran ventas.                            │
│ [Imprimir prueba y abrir cajón] [Probar lectura]                     │
│ Prueba enviada por cable USB. Revisa el ticket y el cajón.           │
│                                                                      │
│ [Avanzado]  → Tickets por imprimir: 2 · con error: 0                 │
│               [Reintentar tickets pendientes]                        │
└──────────────────────────────────────────────────────────────────────┘
```

### Filas de estado

Cada equipo es una fila con punto de color, nombre del equipo, estado en palabras y, si hace falta, **un** botón para resolverlo. Las filas no son botones ni interruptores; por eso no usan el relleno rojo de marca, que queda reservado para acciones.

| Punto | Significado |
|-------|-------------|
| Verde (acento) | Listo para usar. |
| Gris | Se puede usar, pero hay algo que revisar (en espera, respaldo activo, entrenamiento). |
| Rojo (error) | No va a imprimir o leer hasta que el cajero haga algo. |

Estados de la impresora (la lógica vive en `printerStatusLine`):

| Situación | Texto principal | Punto | Botón |
|-----------|-----------------|-------|-------|
| Cable USB con permiso | Lista · por cable USB | Verde | — |
| Cable sin permiso, Bluetooth elegido y encendido | Imprimiendo por Bluetooth · POS-5890A | Gris | Dar permiso USB |
| Cable sin permiso, sin Bluetooth usable | Cable conectado · falta permiso | Rojo | Dar permiso USB |
| Sin cable, Bluetooth apagado | Bluetooth apagado | Rojo | Abrir ajustes de Bluetooth |
| Sin cable, Bluetooth ya imprimió | Lista · por Bluetooth | Verde | — |
| Sin cable, Bluetooth vinculado | Encontrada · por Bluetooth | Verde | — |
| Sin cable, Bluetooth sin actividad aún | En espera · por Bluetooth | Gris | — (usar Imprimir prueba) |
| Sin cable, Bluetooth falló | Bluetooth sin respuesta | Rojo | — |
| Nada configurado | Sin impresora (· modo entrenamiento) | Rojo (gris en entrenamiento) | — |

### Cable y Bluetooth

- **El cable gana** siempre que la impresora USB tenga permiso. No hay que elegirlo: basta con conectarlo.
- **Bluetooth es el respaldo.** La impresora elegida en la sección Bluetooth se usa cuando no hay cable. Mientras hay cable, la fila de esa impresora dice "Elegida · en espera mientras haya cable" para no contradecir la ruta real.
- **Un permiso USB pendiente no bloquea la caja.** Si el cajero cancela el aviso de Android y hay una impresora Bluetooth elegida con el radio encendido, los tickets salen por Bluetooth y la fila ofrece **Dar permiso USB** para volver a pedirlo sin desconectar el cable. Sin Bluetooth usable, la fila queda en rojo con el mismo botón.
- **Desconectar el cable** regresa a Bluetooth sin intervención. El indicador de impresora en la barra de venta ya cambia entre "Impresora USB en línea" e "Impresora Bluetooth en línea", así que el cambio queda visible sin avisos extra.

### Pruebas y Avanzado

Las pruebas (**Imprimir prueba y abrir cajón**, **Probar lectura**) muestran el resultado en español con la siguiente acción ("La impresora no tiene papel. Cambia el rollo y vuelve a probar."), nunca el código interno. Las pruebas no registran ventas.

La cola de impresión (tickets por imprimir, con error, reintentar) queda plegada bajo **Avanzado**: es información de soporte, no algo que el cajero deba vigilar.

Manager **Estado** añade versión de app, actualizaciones y sincronización encima de este mismo bloque.

La acción combinada de prueba de impresión y apertura de cajón queda pendiente de validar con el modelo físico. El cable ocupa el único USB-C de la tablet a través del hub; Bluetooth es el camino normal con la tablet cargando.

## Pendiente de diseño

- pantalla de enrolamiento inicial y recuperación de dispositivo;
- URL/SSO definitivo para abrir Web Central;
- captura detallada de denominaciones (la primera implementación persiste el total contado y deja el campo preparado).
