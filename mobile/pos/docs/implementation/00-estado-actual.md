# Estado actual de la implementación

Última revisión: 2026-09-11.

Este documento es el punto de entrada para saber qué está construido, qué está
en curso y qué sigue siendo una propuesta. Los documentos de arquitectura,
producto y workflows describen el sistema completo; no todos sus apartados
significan que ya existan en código.

## Punto actual

El POS local-first ya cubre **caja de efectivo**, **operación Manager
persistente**, **cola ESC/POS con fakes (Fase 3A)** y **sincronización Device
API en PRODUCTION (Fase 4 parcial)**.

Ninguna fase de 1 a 4 está formalmente cerrada: faltan huecos de producto,
hardware real, pruebas y validación manual. El detalle y los checklists viven
en [02-fases-y-checklists.md](02-fases-y-checklists.md).

## Ya implementado

### Caja local (núcleo Fase 1)

- Proyecto Android nativo con Kotlin, Compose y un único módulo `:app`.
- Modos `SANDBOX` (seed debug, sin red) y `PRODUCTION` (enrolamiento + sync).
- Catálogo real convertido a seed debug en `app/src/debug/assets/pos-bootstrap.json`.
- Room/SQLite con entidades de sede, catálogo, usuarios, dispositivo, turno,
  venta, líneas, pagos, descuentos, movimientos, sangrías, Outbox y PrintJob.
- Importación atómica e idempotente del bootstrap debug.
- Apertura de turno, autenticación local por PIN y venta por unidad.
- Efectivo con cambio, tarjeta externa registrada manualmente y confirmación
  atómica (venta, snapshots, pago, inventario controlado, Outbox, PrintJob).
- Folio asignado al confirmar; la UI aún no muestra el próximo folio antes
  del cobro.

### Manager local (núcleo Fase 2)

- Acceso por PIN de Manager/Superadmin desde Caja sin perder el carrito.
- Panel con Dashboard, Corte Z (incluye consulta de sangrías), Inventario,
  Historial y Estado; operaciones **sí** persisten en Room.
- Descuento único parcial o total autorizado; reposiciones y mermas.
- Sangrías de resguardo desde Modo Venta (creación) y consulta en Manager.
- Corte Z con conteo ciego, corrección y aprobación por PIN.
- Historial del turno con reimpresión de venta y cancelación de efectivo.

### Periféricos fake y cola de impresión (Fase 3A)

- Contratos de scanner, impresora, cajón y transporte.
- `FakeBarcodeScanner`, `FakeTicketPrinter`, encoder ESC/POS 58 mm.
- `PrintWorker` (WorkManager) que consume `PrintJob` desde Room.
- En SANDBOX imprime con fake; en PRODUCTION la impresora real aún no existe
  (`UnavailableTicketPrinter`).
- Scanner fake presente pero no cableado al flujo de venta.

### Sincronización cloud (Fase 4 parcial)

- Cliente `DeviceApi`, enrolamiento, refresh, bootstrap y deltas.
- `SyncWorker` que drena Outbox con resultados `ACCEPTED` / `DUPLICATE` /
  `REQUIRES_REVIEW` / `REJECTED` y aplica cambios de catálogo/operadores.
- Credenciales de dispositivo y espacios Room aislados por modo.
- Pruebas de contrato HTTP parciales (MockWebServer); falta E2E contra
  Spring Boot local y cierre formal del checklist de Fase 4.

## En curso / pendiente cercano

### Cerrar Fase 1 formalmente

- Mostrar el próximo folio antes de confirmar.
- Guardias de carrito cuando un producto se deshabilita tras agregarlo.
- Pruebas instrumentadas Room (venta atómica, folio, migración, reinicio).
- Pruebas Compose del happy path y cancelación de cobro.
- Guion manual en emulador y tablet.

### Completar Fase 2

- Monto abierto con categoría e importe.
- Producto pendiente de catálogo (barcode desconocido).
- Autorización genérica reutilizable (hoy hay diálogos PIN repetidos).
- Reimpresión operativa de comprobantes de sangría desde Manager.
- Pruebas de reglas, Room y UI para excepciones de Manager.

### Completar Fase 3A y abrir 3B

- Integrar scanner fake/real en venta; UI de Estado con errores claros.
- Validar supervivencia de PrintJob tras reinicio (código Room; falta prueba).
- Fase 3B: transportes USB/TCP reales y validación física del hub.

### Endurecer Fase 4

- Botón/acción de sync en UI que encole `SyncWorker` de forma confiable.
- Pruebas de contrato Android ↔ Spring Boot y guion de recuperación.
- Proyección central completa de eventos no-venta (lado backend, no móvil).

## Fuera del alcance del MVP actual

- Venta por peso.
- SDK/API de Mercado Pago (solo registro manual de terminal externa).
- Drivers reales de lector, impresora y cajón (Fase 3B).
- Validación física de hub, alimentación y tres periféricos simultáneos.
- Piloto endurecido (Fase 5).

## Cómo leer el resto de `docs/`

- `product/`: reglas y alcance del negocio; es la fuente de intención.
- `architecture/`: diseño objetivo y contratos conceptuales; mezcla decisiones
  actuales con evolución prevista y debe leerse junto con este estado.
- `ux/` y `workflows/`: comportamiento esperado; no garantizan pantalla
  construida.
- `implementation/`: estado, alcance, fases, checklists y validación.
- `integration/`: contrato Device API y plan del cliente Android (código
  parcialmente adelantado al checklist formal de Fase 4).
- `technical/`: decisiones de datos, seed, hardware y entorno.
