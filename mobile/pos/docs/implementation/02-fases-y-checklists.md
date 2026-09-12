# Fases y checklists de implementación

## Estrategia de entrega

Se implementa por cortes verticales, no por capas aisladas ni por todas las pantallas de una vez. Cada corte deja una operación útil sobre Room y datos reales/sanitizados; las capacidades de cloud y hardware se integran después sin cambiar el significado de una venta local.

## Fase 0: descubrimiento técnico y preparación

Objetivo: convertir la documentación actual en un backlog implementable y conocer las restricciones reales.

**Estado:** cerrada para el desarrollo local. Las validaciones físicas y el contrato remoto quedan diferidos a sus fases correspondientes; no bloquean la caja local.

### Alcance y resultados documentados

- Se inspeccionó y documentó el mapeo preliminar entre backend y POS en [backend-pos-mapping.md](../technical/backend/backend-pos-mapping.md).
- Se definió el contrato local versionado `pos-bootstrap` en [local-pos-bootstrap-contract.md](local-pos-bootstrap-contract.md).
- Se creó un snapshot sanitizado, reproducible y exclusivo de `debug`****, derivado del catálogo legado.
- Se preparó Room e importador local para cargar sede, catálogo y marca de snapshot.
- Se eligió un hub USB-C con Power Delivery como conexión propuesta para periféricos futuros.

No se construye UI de negocio todavía. Esta fase evita que Room, DTOs o drivers nazcan con supuestos incorrectos.

### Checklist de cierre

- [x] Identificar módulos backend relacionados con sede, inventario y usuarios.
- [x] Registrar el mapeo preliminar y sus incompatibilidades.
- [x] Definir el contrato local debug para categorías de venta, precio efectivo y disponibilidad por unidad.
- [x] Definir un snapshot `pos-bootstrap` versionado y mantenerlo fuera de producción mediante `src/debug/assets`.
- [x] Convertir el catálogo legacy a un seed reproducible con escenarios de stock normal, bajo, cero, negativo y no controlado.
- [x] Añadir usuarios de demostración con roles POS separados de datos reales.
- [x] Añadir importador debug y Room sobre el contrato local.
- [x] Verificar el build en la computadora de desarrollo.
- [x] Elegir hub USB-C con PD como punto único de conexión propuesto.
- [ ] Confirmar modelos exactos de tablet, lector, impresora, hub, cables y cargador — diferido a Fase 3.
- [ ] Ejecutar la prueba física de carga simultánea y tres periféricos — diferida a Fase 3.
- [ ] Acordar el endpoint remoto y contrato backend `pos-bootstrap` — diferido a Fase 4.

## Fase 1: caja local de efectivo

Objetivo: una cajera puede abrir turno, vender y confirmar efectivo sin red usando datos reales/sanitizados.

**Estado:** núcleo operativo listo; cierre formal pendiente. El historial y la
reimpresión viven en el panel Manager (Fase 2) y ya funcionan sobre Room.
Faltan folio previo al cobro, guardias de carrito, pruebas Room/Compose y
guion manual.

### Alcance y trabajo realizado

- Se amplió Room con migración inicial y registros locales de usuario, dispositivo, turno, venta, líneas, pago, movimiento de inventario, Outbox y PrintJob.
- La aplicación importa usuarios y dispositivo de demostración, autentica perfil/PIN y abre un único turno local.
- Se construyó la vista de venta en español: catálogo filtrable, carrito editable, cobro de efectivo con numpad/billetes/cambio y registro manual de tarjeta externa.
- La vista se adapta a orientación horizontal y vertical, y ofrece comparación persistente de tema claro/oscuro solo en `debug`.
- La confirmación crea de manera atómica venta, snapshots de líneas/precios, pago, movimientos de inventario controlado, Outbox y trabajo de impresión pendiente.
- El folio se asigna y persiste al confirmar; la etiqueta de carrito aún no muestra el próximo número.
- Historial del turno y reimpresión de venta están disponibles desde Manager (no como pantalla aislada de Fase 1).
- Se añadió cobertura unitaria para conversión y formato de importes exactos; la compilación `:app:compileDebugKotlin` fue verificada.

### Checklist de salida

- [x] Base Room y migración inicial para hechos operativos locales.
- [x] Importación de seed debug con catálogo, usuarios y dispositivo de demostración.
- [x] Selección de usuario/PIN local y apertura de turno con fondo inicial.
- [x] Pantalla de venta con categorías, búsqueda, carrito y snapshot de precio.
- [x] Estados visuales para producto no disponible y advertencia de inventario.
- [x] Numpad de efectivo, denominaciones, cambio y registro manual de tarjeta externa sin SDK.
- [x] Folio, venta, pagos, movimientos, Outbox y PrintJob creados atómicamente al confirmar.
- [x] Estado visible de operación pendiente y tema claro/oscuro de demostración.
- [x] Prueba unitaria de importes exactos y compilación de la aplicación.
- [x] Historial local básico del turno (vía panel Manager).
- [x] Reimpresión pendiente sin modificar venta, pago ni folio.
- [ ] Mostrar el próximo folio antes de confirmar.
- [ ] Cubrir la advertencia/guardia de producto que se deshabilita durante un carrito activo (línea cobrable; no aumentar cantidad).
- [ ] Mostrar en historial el estado local de impresión y sincronización por venta.
- [ ] Pruebas instrumentadas Room para transacción, unicidad, migración y persistencia tras reinicio.
- [ ] Pruebas de interfaz Compose para el happy path y cancelación de intento de pago.
- [ ] Guion manual en emulador y tablet para orientación, objetivos táctiles y recuperación tras reinicio.

### Trabajo restante para cerrar Fase 1

Fase 1 no se considera terminada hasta completar estos puntos:

1. **Folio visible:** mostrar el próximo folio antes de confirmar y conservar
   el folio definitivo en la venta confirmada.
2. **Carrito ante cambios de catálogo:** si un producto se deshabilita después
   de entrar al carrito, la línea existente puede cobrarse, pero no puede
   aumentarse ni agregarse a otro carrito sin autorización.
3. **Historial enriquecido:** mostrar en cada venta del turno su estado local
   de impresión y sincronización.
4. **Persistencia Room:** probar transacción de venta, unicidad de folio y
   eventos, migración y recuperación después de reiniciar la aplicación.
5. **Pruebas Compose:** cubrir agregar producto, editar carrito, cobro en
   efectivo, cambio y cancelación de intento de pago.
6. **Validación manual:** ejecutar el flujo en emulador y tablet, incluyendo
   orientación, objetivos táctiles, reinicio y recuperación de estado pendiente.

### Criterio de salida de Fase 1

La fase queda cerrada cuando una cajera puede abrir turno, vender productos por
unidad y confirmar efectivo sin red; la venta queda almacenada de forma
atómica; el historial permite recuperar la evidencia; los trabajos de
impresión/sincronización quedan pendientes sin perderse; y las pruebas
automatizadas y manuales anteriores pasan.

No incluye todavía red real, terminal de Mercado Pago, sincronización activa ni hardware final. El resultado sí conserva sus eventos pendientes desde el primer cobro.

**Límite de sangrías:** Fase 1 no crea ni consulta sangrías. La barra de
venta conserva exclusivamente el flujo de venta/cobro y el Panel
Manager/Admin no muestra un historial parcial o calculado de retiros. La
operación se incorpora completa en Fase 2 para que el retiro, su auditoría,
el comprobante y el arqueo nunca queden desalineados.

## Fase 2: operación local de Manager

Objetivo: completar la operación de turno y excepciones que no dependen del backend.

**Estado:** núcleo persistente implementado (panel, Corte Z, inventario,
historial, descuentos, sangrías). **Producto pendiente de catálogo** ya está
en venta (modal + línea `PENDING_CATALOG`). Faltan monto abierto, reimpresión
de sangría en UI, autorización PIN genérica y pruebas.

Incluye:

- autorización genérica por PIN;
- monto abierto con categoría e importe;
- producto pendiente de catálogo por barcode desconocido, con importe y auditoría de excepción;
- descuento único parcial o total autorizado;
- reposiciones y mermas locales;
- sangrías de resguardo locales, con comprobante y auditoría;
- historial de turno, reimpresión como trabajo pendiente y cancelación de efectivo permitida;
- Corte Z con conteo ciego, corrección y aprobación;
- estados de impresora/sincronización visibles aunque sean pendientes locales.

### Sangría de caja: reparto por modo

La sangría pertenece al turno activo y se implementa como un registro
persistente e inmutable, no como un contador editable.

- **Modo Venta:** la cajera solo puede **registrar** una sangría desde el
  botón fijo de la barra de caja. Captura importe positivo con numpad y el
  motivo fijo `RESGUARDO_EFECTIVO`; un Manager/Superadmin firma con PIN. No
  se muestra el acumulado ni el historial de sangrías en este modo, y la
  operación no altera el carrito, el borrador de pago ni el turno.
- **Modo Manager/Admin:** es de **consulta** para sangrías: muestra cantidad,
  total y detalle del turno, incluidos folio, importe, cajero, autorizador,
  fecha/hora y estados de impresión/sincronización. Puede solicitar
  reimpresión; no permite crear, editar ni eliminar sangrías.
- La confirmación local crea atómicamente `CashWithdrawal`, evidencia de
  autorización, trabajo de impresión y evento de outbox. El total se deriva
  de los registros confirmados y resta del efectivo esperado del Corte Z.

### Estado del panel Manager

Desde Caja se solicita PIN de Manager/Superadmin para abrir el panel local sin
cambiar el cajero ni perder el carrito. El panel presenta Dashboard, Corte Z
(incluye la consulta de sangrías), Inventario, Historial y Estado, con
navegación lateral en horizontal y selector compacto en vertical.

**Estado actual:** el panel consulta Room y **persiste** sus operaciones
locales. Corte Z, inventario, historial (reimpresión/cancelación), sangrías y
descuentos ya escriben hechos, Outbox y PrintJob. ESC/POS + PrintWorker
(Fase 3A) y SyncWorker (Fase 4) ya existen; el hardware físico (3B) no.

#### Checklist del panel operativo

- [x] Acceso desde Caja con PIN de Manager/Superadmin y regreso que conserva el carrito.
- [x] Panel local con Caja/Corte Z (incluye sangrías), Inventario, Historial y Estado navegables.
- [x] Dashboard local con métricas del día y productos más vendidos.
- [x] Conteo ciego que oculta efectivo esperado hasta validación.
- [x] Corrección auditada y aprobación de Corte Z con PIN.
- [x] Inventario operativo persistente con movimientos recientes.
- [x] Historial del turno con reimpresión y cancelación de efectivo autorizada.
- [x] Modo de solo lectura cuando no hay turno activo.
- [x] Adaptación de navegación a orientación horizontal y vertical.
- [x] Límites de persistencia visibles en cada sección.
- [ ] Validación manual en emulador y tablet para recoger mejoras de UX.

### Checklist de salida

- [ ] Autorización genérica por PIN y evidencia de autorizador (hoy hay diálogos PIN repetidos por flujo).
- [ ] Monto abierto con categoría, importe y autorización por línea.
- [x] Producto pendiente de catálogo con barcode crudo, importe y ausencia de movimiento de inventario.
- [x] Descuento único parcial o total autorizado y persistido con la venta.
- [x] Reposición, merma, historial, reimpresión de venta y cancelación de efectivo.
- [x] Sangría de resguardo persistente, auditable y con outbox/PrintJob pendiente.
- [x] Modal de sangría en Modo Venta: importe positivo, motivo fijo y PIN de
  Manager/Superadmin, sin exponer total ni historial al cajero.
- [x] Consulta de sangrías en Modo Manager/Admin: cantidad, total y detalle
  por turno, sin acciones de creación o edición.
- [ ] Reimpresión operativa de comprobantes de sangría desde la UI de Manager.
- [x] Corte Z con conteo ciego, corrección y aprobación.
- [ ] Pruebas de reglas, Room y UI para las excepciones de Manager.

## Fase 3A: base de periféricos sin hardware

Objetivo: dejar lista una integración verificable con fakes y protocolos puros,
sin inventar soporte para una marca o modelo que todavía no fue validado.

**Estado:** pipeline listo y cableado a venta/Estado. SANDBOX usa fakes;
PRODUCTION usa `UsbTicketPrinter` + perfil POS-5890A. Falta prueba de reinicio
y validación física en tablet (Fase 3B).

Incluye:

- contratos de `BarcodeScanner`, `TicketPrinter`, `CashDrawer` y estado de periféricos;
- separación entre transporte, protocolo ESC/POS y perfil de capacidades;
- fakes permanentes para scanner, impresora y cajón;
- encoder ESC/POS puro con plantilla de 58 mm y code page configurable;
- `PrintWorker` durable que consume `PrintJob` desde Room;
- estados de impresión, errores clasificables, reintentos y reimpresión duplicada;
- pruebas unitarias de renderer, perfiles, fakes y recuperación de `PrintJob`;
- pantalla de Estado conectada a los estados reales de la cola, aunque el
  transporte físico todavía no exista.

### Checklist de salida

- [x] Contratos de scanner, impresora, cajón y transporte definidos sin Android.
- [x] `FakeBarcodeScanner` permite inyectar lecturas completas.
- [x] `FakeTicketPrinter` simula éxito, desconexión, timeout y falta de papel.
- [x] Encoder ESC/POS probado para 58 mm, importes, acentos y `ñ`.
- [x] Perfil genérico declara code page, corte y cajón como capacidades.
- [x] Worker procesa `PENDING`, `PRINTING`, `PRINTED` y `FAILED` sin perder trabajos.
- [x] Reimpresión crea un trabajo duplicado sin crear otra venta.
- [ ] Reinicio de aplicación conserva los trabajos pendientes (Room los guarda; falta prueba automatizada/manual explícita).
- [x] La UI de Estado muestra pendientes/fallidos, encola impresión/sync y permite prueba de scanner fake.
- [x] Scanner HID + fake multiplexados hacia la venta; barcode desconocido abre modal de producto pendiente.

### Criterio de salida

La aplicación puede demostrar, usando fakes, que una venta confirmada genera un
ticket, que un fallo se reintenta, que una reimpresión no duplica el cobro y que
el trabajo sobrevive a un reinicio. Esta fase no declara compatibilidad con un
hardware concreto.

## Fase 3B: periféricos reales

Objetivo: validar la operación sobre hardware antes de integrar cloud.

**Estado:** código USB/HID implementado (`UsbPrintTransport`, `HidKeyboardBarcodeScanner`,
perfil POS-5890A, cajón ESC/POS). Falta validación física en la Rikkai S25 con
hub + Shawty S0024 + POS-5890A.

Incluye:

- modelos reales de tablet, hub, cargador, lector e impresora confirmados;
- lector real por la conexión/protocolo validado;
- transporte USB o TCP/IP real conectado al encoder ESC/POS;
- perfiles de capacidades para los dispositivos validados;
- adapter específico solo si el protocolo común no es suficiente;
- cola durable de impresión, prueba y reimpresión;
- pruebas de desconexión/reconexión con hub USB-C y alimentación;
- validación de orientación horizontal, objetivos táctiles y teclado nativo.

No se habilita Bluetooth como ruta principal salvo que el hardware validado lo requiera y sea estable.

### Checklist de salida

- [x] Modelos de tablet, lector, impresora, hub, cables y cargador confirmados (documentados; falta mesa).
- [x] Transporte USB y protocolo ESC/POS seleccionados para el kit documentado.
- [x] Lector HID y adapter USB de impresora integrados en código.
- [ ] Detección asistida, permisos y selección de perfil verificadas en hardware real.
- [ ] Impresión, reimpresión y recuperación de desconexión verificadas.
- [ ] Prueba física de alimentación y tres periféricos simultáneos.
- [ ] Validación de orientación, objetivos táctiles y comportamiento de teclado/lector.

## Fase 4: sincronización con Spring Boot

Objetivo: convertir la cola existente en integración cloud idempotente.

**Estado:** cliente Android sustancialmente implementado en modo
`PRODUCTION` (`DeviceApi`, enrolamiento, bootstrap/deltas, `SyncWorker` y
outbox). El contrato backend está documentado en
[../integration/01-contrato-implementado.md](../integration/01-contrato-implementado.md).
Falta cierre formal: E2E contra Spring Boot local, UX de sync confiable y
proyección central completa de eventos no-venta (lado servidor).

Incluye:

- enrolamiento de dispositivo y credenciales;
- bootstrap y deltas de catálogo, usuarios y configuración;
- endpoint de eventos por lotes e idempotencia central;
- worker de envío y reintentos;
- respuestas `ACCEPTED`, `DUPLICATE`, `REQUIRES_REVIEW` y bloqueo técnico;
- reportes web consumiendo datos ya sincronizados;
- pruebas de contrato Android ↔ Spring Boot.

### Checklist de salida

- [x] Contrato remoto de bootstrap y eventos documentado e implementado en Spring Boot.
- [x] Enrolamiento, credenciales y bootstrap/deltas implementados en el cliente Android.
- [x] Drenado idempotente de Outbox con reintentos y orden por `deviceSequence` (`SyncWorker`).
- [x] Manejo local de `ACCEPTED`, `DUPLICATE`, `REQUIRES_REVIEW` y `REJECTED`/bloqueo técnico.
- [ ] Acción de UI que encole sync de forma confiable (hoy el arranque PRODUCTION sí encola; el panel Estado es parcial).
- [ ] Pruebas de contrato E2E Android ↔ Spring Boot local y guion de recuperación.
- [ ] Reportes web consumiendo datos ya sincronizados validados en piloto.

## Fase 5: endurecimiento y piloto

Objetivo: operar en una tablet real durante periodos controlados sin perder evidencia.

Incluye:

- pruebas de apagado/reinicio durante venta, impresión y sync;
- políticas de catálogo antiguo, revocación y recuperación de dispositivo;
- observabilidad de eventos pendientes y errores;
- revisión de rendimiento y uso de batería;
- piloto con datos reales y retroalimentación de cajeros;
- correcciones de UX surgidas del uso, antes de sumar nuevas capacidades.

### Checklist de salida

- [ ] Pruebas de apagado/reinicio durante venta, impresión y sincronización.
- [ ] Políticas de catálogo antiguo, revocación y recuperación validadas.
- [ ] Observabilidad de pendientes y errores disponible.
- [ ] Rendimiento, batería y periféricos revisados en tablet real.
- [ ] Piloto controlado, guion de aceptación y correcciones de UX completados.

## Pirámide de pruebas

### Pruebas unitarias JVM

Cubren reglas de alto riesgo y rápidas de ejecutar:

- dinero, cambio y totales;
- precios snapshot;
- guardias de inventario y sobregiro;
- autorización y permisos;
- monto abierto;
- estados de turno/Corte Z;
- decisiones de reintento y resultados de sincronización.

### Pruebas de base local

Pruebas instrumentadas sobre Room para:

- transacción completa de confirmación de venta;
- unicidad de folio/UUID/evento;
- persistencia de Outbox y PrintJob tras reinicio;
- migraciones;
- aplicación atómica de bootstrap/deltas;
- conteos y auditoría de turno.

### Pruebas de UI Compose

Cubren flujos visibles críticos:

- agregar producto y actualizar carrito;
- cobro efectivo y cambio;
- solicitud de PIN;
- producto no disponible/sobregiro;
- regreso de intento de pago cancelado;
- conteo ciego sin exponer efectivo esperado.

### Pruebas de contrato e integración

Con backend de desarrollo/local:

- bootstrap compatible;
- evento reenviado sin duplicar venta;
- orden por dispositivo;
- negocio marcado `REQUIRES_REVIEW` sin alterar venta;
- deltas aplicados sin modificar snapshots confirmados.

### Pruebas manuales

No se eliminan: cubren lo que emulador y tests automáticos no pueden garantizar.

- emulador Android Studio para iteración de UI, rotación, proceso muerto y navegación;
- tablet real para táctil, tamaño, brillo, orientación y rendimiento;
- lector, impresora, hub, alimentación y red real;
- prueba de fila: varias ventas consecutivas, impresora sin papel, red apagada y vuelta de red;
- revisión con cajeros/Manager usando un guion de aceptación por fase.

## Definición de terminado por fase

Una fase no termina solo porque “se ve” la pantalla. Debe:

1. cumplir sus criterios de aceptación documentados;
2. tener pruebas automatizadas para reglas y transacciones nuevas;
3. pasar su guion manual en el entorno relevante;
4. no introducir datos mock como fuente de demostración;
5. registrar decisiones y desviaciones en documentación.
