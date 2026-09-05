# Cliente Android

## Tecnología propuesta

- Kotlin.
- Jetpack Compose para UI.
- Room sobre SQLite para persistencia.
- Coroutines y Flow para trabajo asíncrono y estado observable.
- WorkManager para activar trabajo durable de sincronización después de cierres o reinicios.
- Cliente HTTP con serialización explícita y contratos versionados.
- Android Keystore para proteger credenciales del dispositivo y material sensible local.

La elección de librerías concretas de inyección, HTTP, serialización e impresión se hará al preparar la implementación; no afecta los límites descritos aquí.

## Modularización gradual

El proyecto actual tiene un solo módulo `:app`. Para evitar sobrearquitectura, la primera implementación conserva ese módulo físico y crea límites lógicos mediante paquetes. La estructura se extrae a módulos Gradle solo cuando exista una razón concreta: compilación lenta, reutilización, dependencia de fabricante o equipo trabajando de forma independiente.

Objetivo de evolución, no punto de partida obligatorio:

```text
:app       composición, navegación, Compose y ViewModels
:domain    reglas y casos de uso puros de Kotlin
:data      Room, API, repositorios, outbox y sincronización
:hardware  escáner, impresora y adaptadores Android
```

Las funcionalidades (`sale`, `shift`, `inventory`, `auth`, `admin`) empiezan como paquetes del módulo `:app`. La regla de dependencia se mantiene incluso antes de extraer módulos: el dominio no debe conocer Android, Room, HTTP ni fabricantes; los adaptadores concretos quedan hacia el borde.

## Capa de presentación

Responsabilidades:

- renderizar estado observable;
- transformar gestos y lecturas en intenciones de usuario;
- mostrar advertencias y solicitudes de autorización;
- conservar la pantalla única del flujo feliz;
- impedir dobles confirmaciones mientras una acción local está en curso.

La presentación no calcula totales definitivos, no modifica stock directamente y no decide si una acción requiere Manager. Esas reglas pertenecen al dominio.

Pantallas/áreas iniciales:

- acceso y cambio rápido de usuario;
- POS de pantalla dividida;
- apertura y cierre de turno;
- historial y reimpresión;
- administración local de mermas/reposiciones;
- estado de sincronización y periféricos;
- enrolamiento inicial del dispositivo.

## Capa de dominio

Contiene casos de uso explícitos, por ejemplo:

- `OpenShift` y `SubmitCashCount`;
- `ApproveShiftClose` y `RejectCashCount`;
- `AddProductToCart` y `AddWeightedLabelToCart`;
- `AddOpenAmountWithAuthorization`;
- `ApplySaleDiscountWithAuthorization`;
- `ConfirmSale`;
- `CancelCashSale`;
- `RecordWaste` y `RecordRestock`;
- `RequestTicketReprint`.

Cada caso de uso valida invariantes y delega la persistencia a una unidad transaccional. Los resultados esperados se modelan como resultados de negocio, no como excepciones HTTP o mensajes del driver.

## Capa de datos

Responsabilidades:

- exponer repositorios que leen primero y siempre de Room;
- separar modelos de dominio, entidades Room y DTOs de red;
- ejecutar transacciones atómicas;
- persistir outbox y trabajos de impresión;
- aplicar snapshots y deltas centrales;
- conservar cursores, secuencias y estado de sincronización.

Repositorios conceptuales:

- `CatalogRepository`;
- `SaleRepository`;
- `ShiftRepository`;
- `InventoryRepository`;
- `UserRepository`;
- `DeviceRepository`;
- `SyncRepository`;
- `PrintJobRepository`.

No se requiere que cada repositorio sea una clase única ni que refleje una tabla. Es un límite de responsabilidad.

## Persistencia y atomicidad

Operaciones que deben ser transaccionales:

- confirmar venta, pagos, movimientos, outbox e impresión;
- cancelar venta en efectivo y crear movimientos inversos;
- registrar merma/reposición junto con su evento;
- aprobar el cierre junto con bitácora y evento;
- aplicar un lote de cambios remotos junto con su nuevo cursor.

Los importes monetarios deben persistirse sin coma flotante; se recomienda una unidad menor entera cuando la precisión del negocio lo permita. Las cantidades por peso requieren un decimal exacto con escala definida. Fechas y horas se conservan como instantes, además de la sede/zona horaria necesaria para reportes.

## Motor de sincronización

El procesamiento tiene dos activadores complementarios:

- intento inmediato mientras la aplicación está activa y hay conectividad;
- WorkManager como garantía durable para reanudar y reintentar.

La cola real vive en Room. WorkManager no es la cola ni la fuente de verdad. Solo debe existir un drenador activo por dispositivo.

## Periféricos

### Escáner

Contrato conceptual:

```text
BarcodeScanner.start()
BarcodeScanner.events
BarcodeScanner.stop()
```

El adaptador normaliza lecturas USB/serial/SPP y entrega cadenas completas, sin hacer consultas de catálogo ni modificar el carrito. El parser de etiquetas de peso es independiente del transporte físico.

### Impresora

Contrato conceptual:

```text
TicketPrinter.status
TicketPrinter.print(renderedTicket)
```

Un servicio de aplicación renderiza el ticket y crea un `PrintJob`. El adaptador traduce el documento al protocolo USB o TCP/IP. Reimprimir crea un nuevo intento vinculado a la misma venta, no una nueva venta.

## Seguridad local

- PIN nunca almacenado en texto legible.
- Verificadores de PIN protegidos localmente y asociados a usuarios sincronizados.
- Límite de intentos y bloqueo temporal.
- Credencial del dispositivo protegida con Android Keystore.
- Acciones sensibles registran actor de sesión y actor autorizador por separado.
- `clean DB` se rechaza mientras exista evidencia no sincronizada.
- Revocaciones se aplican al reconectar; una tablet completamente offline no puede conocer cambios centrales nuevos.

## Referencias técnicas

- [Guía de arquitectura offline-first de Android](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Recomendaciones de arquitectura Android](https://developer.android.com/topic/architecture/recommendations)
- [Persistencia con Room](https://developer.android.com/training/data-storage/room)
- [Trabajo durable con WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent)
