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

**Estado:** en curso. El flujo principal demostrable existe localmente; faltan pruebas automatizadas de Room/UI, historial y validación manual en tablet.

### Alcance y trabajo realizado

- Se amplió Room con migración inicial y registros locales de usuario, dispositivo, turno, venta, líneas, pago, movimiento de inventario, Outbox y PrintJob.
- La aplicación importa usuarios y dispositivo de demostración, autentica perfil/PIN y abre un único turno local.
- Se construyó la vista de venta en español: catálogo filtrable, carrito editable, cobro de efectivo con numpad/billetes/cambio y registro manual de tarjeta externa.
- La vista se adapta a orientación horizontal y vertical, y ofrece comparación persistente de tema claro/oscuro solo en `debug`.
- La confirmación crea de manera atómica venta, snapshots de líneas/precios, pago, movimientos de inventario controlado, Outbox y trabajo de impresión pendiente.
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
- [ ] Mostrar el próximo folio antes de confirmar y cubrir la advertencia de producto que se deshabilita durante un carrito activo.
- [ ] Historial local básico y reimpresión pendiente.
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
3. **Historial local:** consultar ventas del dispositivo/turno y mostrar su
   estado local de impresión y sincronización.
4. **Reimpresión pendiente:** crear y reintentar un trabajo de impresión sin
   modificar la venta, el pago ni el folio original.
5. **Persistencia Room:** probar transacción de venta, unicidad de folio y
   eventos, migración y recuperación después de reiniciar la aplicación.
6. **Pruebas Compose:** cubrir agregar producto, editar carrito, cobro en
   efectivo, cambio y cancelación de intento de pago.
7. **Validación manual:** ejecutar el flujo en emulador y tablet, incluyendo
   orientación, objetivos táctiles, reinicio y recuperación de estado pendiente.

### Criterio de salida de Fase 1

La fase queda cerrada cuando una cajera puede abrir turno, vender productos por
unidad y confirmar efectivo sin red; la venta queda almacenada de forma
atómica; el historial permite recuperar la evidencia; los trabajos de
impresión/sincronización quedan pendientes sin perderse; y las pruebas
automatizadas y manuales anteriores pasan.

No incluye todavía red real, terminal de Mercado Pago, sincronización activa ni hardware final. El resultado sí conserva sus eventos pendientes desde el primer cobro.

## Fase 2: operación local de Manager

Objetivo: completar la operación de turno y excepciones que no dependen del backend.

Incluye:

- autorización genérica por PIN;
- monto abierto con categoría e importe;
- descuento único parcial o total autorizado;
- reposiciones y mermas locales;
- historial de turno, reimpresión como trabajo pendiente y cancelación de efectivo permitida;
- Corte Z con conteo ciego, corrección y aprobación;
- estados de impresora/sincronización visibles aunque sean pendientes locales.

### Subcorte en curso: prototipo visual de Manager

Este subcorte permite evaluar la UX antes de implementar las transacciones de
Fase 2. Desde Caja se solicita PIN de Manager/Superadmin para abrir el panel
local sin cambiar el cajero ni perder el carrito. El panel presenta Corte Z,
Inventario, Historial y Estado, con navegación lateral en horizontal y selector
compacto en vertical.

**Límites explícitos:** el PIN valida el acceso, pero las acciones visuales no
cierran turno, no modifican existencias, no cancelan ventas y no crean eventos
de sincronización ni trabajos de impresión. Los datos de ejemplo se distinguen
de los datos locales reales disponibles.

#### Checklist del prototipo visual

- [x] Acceso desde Caja con PIN de Manager/Superadmin y regreso que conserva el carrito.
- [x] Panel local con Corte Z, Inventario, Historial y Estado navegables.
- [x] Conteo ciego visual que oculta efectivo esperado hasta validación.
- [x] Adaptación de navegación a orientación horizontal y vertical.
- [x] Límites de persistencia visibles en cada sección.
- [ ] Validación manual en emulador y tablet para recoger mejoras de UX.

### Checklist de salida

- [ ] Autorización genérica por PIN y evidencia de autorizador.
- [ ] Monto abierto con categoría, importe y autorización por línea.
- [ ] Descuento único autorizado.
- [ ] Reposición, merma, historial, reimpresión y cancelación de efectivo.
- [ ] Corte Z con conteo ciego, corrección y aprobación.
- [ ] Pruebas de reglas, Room y UI para las excepciones de Manager.

## Fase 3: periféricos reales

Objetivo: validar la operación sobre hardware antes de integrar cloud.

Incluye:

- lector real por la conexión/protocolo decidido;
- integración de periféricos conforme a los modelos reales;
- impresora USB o TCP/IP real;
- cola durable de impresión, prueba y reimpresión;
- pruebas de desconexión/reconexión con hub USB-C y alimentación;
- validación de orientación horizontal, objetivos táctiles y teclado nativo.

No se habilita Bluetooth como ruta principal salvo que el hardware validado lo requiera y sea estable.

### Checklist de salida

- [ ] Modelos de tablet, lector, impresora, hub, cables y cargador confirmados.
- [ ] Lector e impresora integrados contra el hardware validado.
- [ ] Impresión, reimpresión y recuperación de desconexión verificadas.
- [ ] Prueba física de alimentación y tres periféricos simultáneos.
- [ ] Validación de orientación, objetivos táctiles y comportamiento de teclado/lector.

## Fase 4: sincronización con Spring Boot

Objetivo: convertir la cola existente en integración cloud idempotente.

Incluye:

- enrolamiento de dispositivo y credenciales;
- bootstrap y deltas de catálogo, usuarios y configuración;
- endpoint de eventos por lotes e idempotencia central;
- worker de envío y reintentos;
- respuestas `ACCEPTED`, `DUPLICATE`, `REQUIRES_REVIEW` y bloqueo técnico;
- reportes web consumiendo datos ya sincronizados;
- pruebas de contrato Android ↔ Spring Boot.

### Checklist de salida

- [ ] Contrato remoto de bootstrap y eventos aprobado.
- [ ] Enrolamiento, credenciales y bootstrap/deltas implementados.
- [ ] Drenado idempotente de Outbox con reintentos y orden por dispositivo.
- [ ] Manejo de `ACCEPTED`, `DUPLICATE`, `REQUIRES_REVIEW` y bloqueo técnico.
- [ ] Pruebas de contrato Android ↔ Spring Boot y reportes web con datos sincronizados.

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
