# Visión general

## Estilo arquitectónico

Se propone un **monolito modular offline-first** en ambos extremos:

- una aplicación Android nativa con módulos técnicos pequeños y funcionalidades organizadas por paquetes;
- el backend Spring Boot existente, extendido con un contexto POS claramente delimitado;
- PostgreSQL como persistencia central consolidada;
- Room/SQLite como persistencia operativa de cada tablet.

No se propone un servidor local en la LAN para el MVP. Cada tablet conserva autonomía total y sincroniza directamente con el backend cloud cuando hay internet.

```text
┌────────────────────────────┐          ┌────────────────────────────┐
│ Tablet A                   │          │ Backend central            │
│ UI → Dominio → Room        │──HTTPS──►│ Spring Boot                │
│          └→ Outbox         │          │ PostgreSQL                 │
│          └→ Print Queue    │◄──delta──│ Catálogo / usuarios / cfg. │
└────────────────────────────┘          └────────────────────────────┘

┌────────────────────────────┐                     ▲
│ Tablet B                   │─────────────────────┘
│ UI → Dominio → Room        │
│          └→ Outbox         │
│          └→ Print Queue    │
└────────────────────────────┘
```

## Fuentes de verdad

La expresión “fuente de verdad” depende del tipo de dato:

- **Operación activa de una tablet:** Room es la fuente inmediata. La UI no consulta la red para vender.
- **Venta ya cobrada pero no sincronizada:** la copia local es la evidencia autoritativa hasta ser recibida por el backend.
- **Catálogo, usuarios, configuración y disponibilidad administrativa:** el backend es la fuente maestra; la tablet mantiene una réplica filtrada por sede.
- **Inventario consolidado y reportes multisede:** el backend calcula el estado central a partir de todos los movimientos recibidos.

No se replica una base de datos completa. Se intercambian snapshots/deltas de datos maestros y eventos inmutables de operación.

## Componentes principales

### Aplicación POS Android

Responsable de autenticar localmente, abrir turnos, construir y cobrar ventas, imprimir tickets, registrar inventario operativo y conservar una cola durable de sincronización.

### Base local Room/SQLite

Contiene el catálogo efectivo de la sede, usuarios autorizados, configuración vigente, turno activo, ventas, pagos, movimientos, bitácora, eventos pendientes y trabajos de impresión. Una actualización remota siempre se guarda primero aquí antes de reflejarse en UI.

### Motor de sincronización

Envía eventos locales con idempotencia y descarga cambios centrales mediante cursores/versiones. Los reintentos sobreviven a reinicios. Conserva el orden causal por dispositivo, pero no presupone un orden global entre tablets.

### Adaptadores de periféricos

Aíslan lector e impresora detrás de contratos internos. El dominio solicita “leer código” o “imprimir ticket”; no conoce USB, serial, SPP, TCP/IP ni comandos específicos del fabricante.

### Backend Spring Boot

Autentica dispositivos, recibe eventos idempotentes, consolida ventas e inventario, distribuye catálogo/configuración/usuarios, administra incidencias y sirve reportes. Se mantiene como una sola aplicación desplegable con límites internos explícitos.

### Web administrativa

Opera sobre el backend central. Gestiona catálogo maestro, configuración por sede, usuarios, dispositivos, inventario maestro, incidencias y reportes consolidados. No forma parte del camino crítico de cobro.

## Flujo de confirmación de venta

La confirmación ejecuta una sola transacción local:

```text
Validar carrito y autorización
          ↓
Crear venta + líneas snapshot + pagos
          ↓
Crear movimientos locales de inventario
          ↓
Crear evento Outbox + trabajo de impresión
          ↓
COMMIT local → venta confirmada
```

La impresión y la red ocurren después del `COMMIT`. Su falla no revierte un cobro que ya ocurrió.

## Decisiones para evitar complejidad innecesaria

- Sin microservicios para el MVP.
- Sin servidor LAN obligatorio.
- Sin event sourcing completo: se conservan hechos importantes y una bitácora, pero el estado operativo también vive en tablas consultables.
- Sin sincronización genérica tabla-a-tabla.
- Sin base compartida entre tablets.
- Sin un módulo Gradle por pantalla.
- Sin API/SDK de Mercado Pago en la primera versión.
- Sin lógica de negocio dentro de drivers de hardware, componentes Compose o DTOs de red.

## Evolución prevista

El diseño permite añadir otra sede, integrar pagos, incorporar un gateway LAN opcional o separar un módulo del backend si la escala lo justifica. Ninguna de esas posibilidades se implementa anticipadamente.
