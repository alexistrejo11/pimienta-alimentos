# Panel local de Manager

## Propósito

Permitir supervisar y operar una tablet durante el turno sin depender de internet. No pretende ser un ERP, un panel de configuración global ni un sustituto de la web administrativa.

El acceso requiere PIN de Manager o Superadmin. Entrar y salir del panel no cambia la identidad del cajero que mantiene el turno ni elimina un carrito en curso.

## Alcance local

El panel contiene cuatro secciones:

1. **Corte Z:** preparación, validación y consulta de cierre del turno activo.
2. **Inventario operativo:** reposiciones y mermas registradas durante el turno.
3. **Historial local:** tickets originados en esta tablet, reimpresión y cancelación permitida.
4. **Estado y periféricos:** sincronización, impresora, lector y diagnóstico básico.

No incluye clientes, fiados, apartados, proveedores, compras, CSV, catálogo maestro, ajustes masivos, configuración por sede, reportes consolidados, licencias ni administración de usuarios. Esas funciones pertenecen a la web central.

## Wireframe estructural

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│ [Volver a caja]   Panel local de Manager · Tablet T1 · Turno 104                 │
├───────────────────┬──────────────────────────────────────────────────────────────┤
│ Corte Z           │ Sección activa                                                │
│ Inventario        │                                                              │
│ Historial         │ Contenido local de la tablet                                  │
│ Estado            │                                                              │
│                   │                                                              │
└───────────────────┴──────────────────────────────────────────────────────────────┘
```

Las secciones pueden ser navegación lateral en orientación horizontal. No deben convertirse en una barra superior con decenas de módulos.

## Corte Z

El cierre conserva el doble control ya definido: cajero cuenta, Manager aprueba. La pantalla cambia por estado; no muestra toda la información a todos los actores al mismo tiempo.

### 1. Turno abierto

```text
┌──────────────────────────────────────────────────────────────────────┐
│ CORTE Z · TURNO ABIERTO                                                │
│ Cajero: Marco · Apertura: 08:00 · Fondo inicial: $500.00              │
│                                                                      │
│ [Iniciar conteo ciego]                                                │
│                                                                      │
│ Resumen operativo                                                     │
│ Ventas por pago · mermas · descuentos · cancelaciones                 │
└──────────────────────────────────────────────────────────────────────┘
```

### 2. Conteo ciego del cajero

El cajero captura desglose de monedas/billetes con numpad. **No ve el efectivo esperado ni la diferencia**. Puede enviar el conteo para validación.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ CONTEO CIEGO · CAJERO                                                 │
│ $0.50 [ ]  $1 [ ]  $5 [ ]  $10 [ ]  ...  $500 [ ]                     │
│                                                                      │
│ Total contado: $2,920.00                                              │
│                                                                      │
│ [Cancelar]                                      [Enviar a validación]│
└──────────────────────────────────────────────────────────────────────┘
```

### 3. Validación de Manager

Solo tras enviar el conteo se muestra al Manager el resumen, efectivo esperado, contado y diferencia. Puede aprobar con PIN o devolverlo para corrección. Cada intento queda en bitácora.

```text
┌──────────────────────────────────────────────────────────────────────┐
│ VALIDAR CORTE · MANAGER                                               │
│ Efectivo esperado: $2,920.00   Contado: $2,900.00                    │
│ Diferencia: -$20.00                                                   │
│ Ventas · descuentos · mermas · cancelaciones                          │
│                                                                      │
│ [Corregir conteo]                  [Aprobar con PIN y cerrar turno]  │
└──────────────────────────────────────────────────────────────────────┘
```

Al aprobar, el POS congela el corte, registra el evento de cierre e intenta imprimirlo. La falta de red o impresión no bloquea el cierre; impresión y sincronización quedan pendientes si fallan.

Retiros, gastos de caja e ingresos adicionales no aparecen porque no forman parte del MVP definido.

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
│ Impresora: lista / sin papel / desconectada    [Imprimir prueba]     │
│ Lector: conectado / sin lectura reciente        [Probar lectura]     │
│                                                                      │
│ Las pruebas no alteran ventas ni eliminan eventos pendientes.         │
└──────────────────────────────────────────────────────────────────────┘
```

La apertura de cajón no se incluye hasta confirmar que existe hardware compatible y cuál será su conexión. El POS usa USB o TCP/IP para impresión según la restricción técnica ya acordada; no se asume Bluetooth como ruta principal.

## Pendiente de diseño

- flujo de PIN para entrar al panel y volver al cajero;
- detalle de un ticket y acciones de reimpresión/cancelación;
- captura exacta de denominaciones del conteo ciego;
- pantalla de enrolamiento inicial y recuperación de dispositivo;
- comportamiento cuando no hay turno activo.
