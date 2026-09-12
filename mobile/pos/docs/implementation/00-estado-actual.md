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
- Modos `SANDBOX` (capacitación/playground, sin red) y `PRODUCTION` (enrolamiento + sync).
- Plantilla de capacitación en `app/src/main/assets/pos-training-bootstrap.json`
  (empaquetada en debug y release); la DB scratch se resetea al entrar.
- Room/SQLite con entidades de sede, catálogo, usuarios, dispositivo, turno,
  venta, líneas, pagos, descuentos, movimientos, sangrías, Outbox y PrintJob.
- Importación atómica de la plantilla de capacitación (`TrainingBootstrapImporter`).
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
- **Producto pendiente de catálogo:** modal con barcode + importe, línea
  `PENDING_CATALOG` sin inventario.

### Periféricos USB (Fases 3A/3B)

- Contratos de scanner, impresora, cajón y transporte.
- `FakeBarcodeScanner`, `HidKeyboardBarcodeScanner`, `MultiplexBarcodeScanner`.
- `UsbPrintTransport`, `UsbTicketPrinter`, perfil `POS-5890A` / CP850 / cajón.
- `PrintWorker` + `PrinterFactory` (fake en SANDBOX, USB en PRODUCTION).
- Scanner cableado a venta; barcode desconocido abre modal de producto pendiente.
- Panel Estado: cola de impresión, sync, prueba fake/HID e impresión de prueba.

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
- Autorización genérica reutilizable (hoy hay diálogos PIN repetidos por flujo).
- Reimpresión operativa de comprobantes de sangría desde Manager.
- Pruebas de reglas, Room y UI para excepciones de Manager.

### Validar Fase 3B en mesa

- Prueba física Rikkai + hub + Shawty S0024 + POS-5890A (30 min, acentos, cajón).
- Registrar VID/PID USB reales tras la mesa.
- Validar supervivencia de PrintJob tras reinicio (falta prueba explícita).

### Endurecer Fase 4

- Botón/acción de sync en UI que encole `SyncWorker` (implementado en PRODUCTION).
- Pruebas de contrato Android ↔ Spring Boot y guion de recuperación.
- Proyección central completa de eventos no-venta (lado backend, no móvil).

## Fuera del alcance del MVP actual

- Venta por peso.
- SDK/API de Mercado Pago (solo registro manual de terminal externa).
- Bluetooth como ruta principal de periféricos.
- Validación física de hub, alimentación y tres periféricos simultáneos (código listo; falta mesa).
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
