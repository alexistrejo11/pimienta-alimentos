# Fases y estrategia de pruebas

## Estrategia de entrega

Se implementa por cortes verticales, no por capas aisladas ni por todas las pantallas de una vez. Cada corte deja una operación útil sobre Room y datos reales/sanitizados; las capacidades de cloud y hardware se integran después sin cambiar el significado de una venta local.

## Fase 0: descubrimiento técnico y preparación

Objetivo: convertir la documentación actual en un backlog implementable y conocer las restricciones reales.

Entregables:

- inspección/mapeo del backend y datos existentes;
- definición del snapshot `pos-bootstrap` de debug;
- identificación de tablet, lector, impresora y hub;
- decisión de variante de build para datos debug;
- criterios medibles de rendimiento y compatibilidad;
- actualización de dependencias mediante catálogo de versiones.

No se construye UI de negocio todavía. Esta fase evita que Room, DTOs o drivers nazcan con supuestos incorrectos.

## Fase 1: caja local de efectivo

Objetivo: una cajera puede abrir turno, vender y confirmar efectivo sin red usando datos reales/sanitizados.

Incluye:

- base Room y migraciones iniciales;
- importación de seed debug;
- selección de usuario/PIN local y apertura de turno;
- pantalla de venta, categorías, carrito, snapshot de precio y producto no disponible;
- numpad de efectivo, cambio y confirmación local;
- folio, venta, pagos, movimientos y Outbox/PrintJob creados atómicamente;
- historial local básico y estado de operación pendiente;
- pruebas de unidad, Room e interfaz para happy path.

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

## Fase 5: endurecimiento y piloto

Objetivo: operar en una tablet real durante periodos controlados sin perder evidencia.

Incluye:

- pruebas de apagado/reinicio durante venta, impresión y sync;
- políticas de catálogo antiguo, revocación y recuperación de dispositivo;
- observabilidad de eventos pendientes y errores;
- revisión de rendimiento y uso de batería;
- piloto con datos reales y retroalimentación de cajeros;
- correcciones de UX surgidas del uso, antes de sumar nuevas capacidades.

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
