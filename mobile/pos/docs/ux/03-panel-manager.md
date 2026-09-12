# Panel local de Manager

## Propósito

Permitir supervisar y operar una tablet durante el turno sin depender de internet. No pretende ser un ERP, un panel de configuración global ni un sustituto de la web administrativa.

El acceso requiere PIN de Manager o Superadmin. Entrar y salir del panel no cambia la identidad del cajero que mantiene el turno ni elimina un carrito en curso. El **conteo ciego** es la excepción: el cajero titular puede iniciarlo desde la acción `Cerrar turno` de la caja, pero la revisión, corrección y aprobación siguen requiriendo Manager/Superadmin.

## Alcance local

El panel contiene un dashboard y cuatro secciones operativas:

1. **Dashboard / Resumen del día:** métricas locales de la tablet y cinco productos más vendidos.
2. **Caja y Corte Z:** preparación, validación, cierre y consulta de sangrías del turno activo.
3. **Inventario operativo:** reposiciones, mermas y movimientos recientes.
4. **Historial local:** tickets originados en esta tablet, reimpresión y cancelación permitida.
5. **Estado y periféricos:** sincronización, impresora, lector y diagnóstico básico.

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

## Estado y periféricos

```text
┌──────────────────────────────────────────────────────────────────────┐
│ ESTADO Y PERIFÉRICOS                                                  │
│ Sincronización: 12 eventos pendientes · última sync hace 3 h         │
│ [Intentar sincronizar ahora]                                         │
│                                                                      │
│ Impresora: lista / sin papel / desconectada                          │
│ [Imprimir ticket de prueba] [Imprimir + abrir cajón*]                │
│ Lector: conectado / sin lectura reciente        [Probar lectura]     │
│                                                                      │
│ Las pruebas no alteran ventas ni eliminan eventos pendientes.         │
└──────────────────────────────────────────────────────────────────────┘
```

La acción combinada de prueba de impresión y apertura de cajón queda pendiente de validar con el modelo físico. Si el adaptador y la impresora soportan el pulso de apertura, se habilita; si no, el botón permanece deshabilitado y la impresión de prueba continúa disponible. El POS usa USB o TCP/IP para impresión según la restricción técnica ya acordada; no se asume Bluetooth como ruta principal.

## Pendiente de diseño

- pantalla de enrolamiento inicial y recuperación de dispositivo;
- URL/SSO definitivo para abrir Web Central;
- captura detallada de denominaciones (la primera implementación persiste el total contado y deja el campo preparado).
