# Validación y pruebas

Este documento define cómo se valida una fase. El detalle de estado y las
tareas concretas viven en [02-fases-y-checklists.md](02-fases-y-checklists.md).

## Pruebas unitarias

- Dinero exacto, totales y cambio.
- Snapshot de precio.
- Guardias de disponibilidad y stock controlado.
- Autorización y permisos.
- Estados de turno y Corte Z cuando corresponda.

## Pruebas Room

- Importación atómica del bootstrap.
- Unicidad de folios y eventos.
- Confirmación atómica de venta.
- Persistencia de Outbox y PrintJob.
- Migraciones y recuperación tras reinicio.

## Pruebas Compose

- Agregar producto y editar carrito.
- Cobro en efectivo y cambio.
- Cancelación de intento de pago.
- Producto no disponible y advertencias de inventario.

## Validación manual

- Build desde Android Studio y Gradle Wrapper.
- Flujo de apertura, venta y cobro en emulador.
- Orientación, objetivos táctiles y reinicio.
- Tablet real cuando el hardware esté definido.

## Criterio general

Una fase se cierra cuando cumple su checklist, sus reglas críticas tienen
pruebas automatizadas proporcionales y el guion manual relevante pasa. Las
métricas de rendimiento se definirán en Phase 5, cuando existan flujos
completos y hardware objetivo para medirlos.
