# Blueprint técnico

## Punto de partida observado

El proyecto actual es un scaffold Android nativo con Kotlin y Jetpack Compose:

- un único módulo Gradle, `:app`;
- `minSdk 26`;
- catálogo de versiones en `gradle/libs.versions.toml`;
- pantalla `MainActivity` de ejemplo;
- pruebas unitarias e instrumentadas de ejemplo.

No hay todavía persistencia, red, DI ni integración de periféricos. Este plan no exige agregarlas todas a la vez.

## Estructura inicial de archivos

Durante las primeras fases se conserva un único módulo y se usan paquetes explícitos:

```text
app/src/main/java/io/github/alexistrejo/pimienta/pos/
├── app/
│   ├── PosApplication.kt
│   ├── AppContainer.kt
│   └── navigation/
├── core/
│   ├── model/
│   ├── money/
│   ├── time/
│   └── result/
├── data/
│   ├── local/
│   │   ├── db/
│   │   ├── entity/
│   │   ├── dao/
│   │   └── transaction/
│   ├── repository/
│   ├── seed/
│   ├── sync/
│   └── remote/                 # fase de backend, no antes
├── domain/
│   ├── sale/
│   ├── shift/
│   ├── inventory/
│   ├── auth/
│   └── printing/
├── hardware/
│   ├── scanner/                # fase de periféricos
│   └── printer/                # fase de periféricos
├── feature/
│   ├── access/
│   ├── shift/
│   ├── sale/
│   ├── manager/
│   └── status/
└── ui/
    ├── theme/
    ├── components/
    └── state/
```

La estructura no obliga a crear carpetas vacías. Cada paquete aparece cuando exista una clase real que le corresponda.

## Responsabilidad de cada límite

- `feature`: Compose, ViewModels, eventos de usuario y estado de pantalla.
- `domain`: reglas de negocio y casos de uso sin referencias a Compose, Room, HTTP o Android.
- `data/local`: persistencia Room, DAOs y transacciones.
- `data/repository`: puente entre dominio y almacenamiento; en fase posterior combina local/remoto.
- `data/sync`: Outbox, cursores, workers y políticas de reintento.
- `hardware`: implementación de contratos para lector e impresora; no decide reglas de venta.
- `app`: composición de dependencias manual, navegación y configuración de aplicación.

## Inyección de dependencias

La fase inicial usa un `AppContainer` manual y explícito. Es suficiente para crear base de datos, repositorios, casos de uso y adaptadores sin añadir un framework de DI.

Hilt u otro framework solo se evaluará si el grafo de dependencias o las pruebas dejan de ser mantenibles. No se añade por anticipación.

## Dependencias por fase

Las versiones se resuelven en `libs.versions.toml` y deben ser compatibles con el AGP/Kotlin existentes al momento de implementarse. No se deben copiar números de versión obsoletos a código o documentación.

### Base de fase local

- `lifecycle-viewmodel-compose` y estado lifecycle-aware para ViewModels Compose.
- `navigation-compose` solo si la navegación real ya supera una actividad/panel controlable manualmente.
- Room runtime, KTX y compilador mediante KSP para base local.
- `kotlinx-coroutines-test` para reglas y casos de uso asíncronos.
- Room testing para verificar transacciones y queries relevantes.

### Fase de trabajo durable

- WorkManager KTX para reanudar impresión y sincronización durables.

Aunque el worker se active después, las tablas `outbox_event` y `print_job` se crean desde la primera fase de venta confirmada. Así ningún hecho histórico requiere una migración artificial para poder sincronizarse.

### Fase de backend

- un cliente HTTP y serialización JSON elegidos tras revisar el contrato existente de Spring Boot;
- autenticación/credenciales de dispositivo mediante APIs de Android Keystore, sin dependencia adicional si no se necesita;
- no se añade SDK de Mercado Pago en MVP.

### Fase de periféricos

- Fase 3A: contratos, fakes, encoder ESC/POS, perfiles, estados y worker de
  impresión, sin depender de un modelo físico.
- Fase 3B: APIs Android para USB, red TCP/IP o Bluetooth serial únicamente según
  el hardware validado.
- El transporte se implementa separado del protocolo: por ejemplo,
  `TcpPrintTransport` puede enviar ESC/POS por RAW TCP y un transporte USB puede
  enviar los mismos bytes por endpoints bulk.
- La detección USB puede identificar VID/PID e interfaces, pero no garantiza que
  el equipo entienda ESC/POS. La configuración debe confirmar el perfil mediante
  una prueba de impresión.
- SDK de fabricante solo si la prueba con hardware demuestra que aporta una
  integración estable y el transporte/protocolo común no es suficiente.
- No se agrega una dependencia Bluetooth genérica sin conocer el lector real.

## Reglas de construcción

1. Ninguna pantalla consulta directamente una API.
2. Ningún composable escribe directamente en Room.
3. La confirmación de venta vive en un caso de uso y una transacción local.
4. Todas las cantidades monetarias usan representación exacta, nunca `Float` o `Double`.
5. Todo hecho confirmado crea una fila de auditoría/Outbox dentro de la misma transacción cuando aplique.
6. Las dependencias nuevas requieren una necesidad de fase y una prueba asociada.
7. Los ejemplos y datos seed no se empaquetan en builds de producción.

## Extracción futura de módulos

No se extraen módulos Gradle al inicio. Los primeros candidatos, si hacen falta, son `:domain` para pruebas JVM rápidas y `:hardware` para aislar SDKs de periféricos. La decisión se revisa después de completar sincronización, no antes.
