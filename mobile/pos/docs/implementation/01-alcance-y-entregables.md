# Alcance y entregables de la versión actual

## Objetivo

Operar el POS Android local-first: vender sin depender de red, completar la
operación Manager en Room, preparar impresión ESC/POS con fakes y sincronizar
hechos hacia Spring Boot en modo `PRODUCTION` sin bloquear el cobro.

## Dentro del alcance

- POS Android nativo offline-first (`SANDBOX` y `PRODUCTION`).
- Catálogo local basado en datos reales sanitizados (seed debug) o bootstrap remoto.
- Productos vendidos por unidad (`PIECE`).
- Categorías, precios, disponibilidad y stock local.
- Apertura de turno y autenticación local por PIN.
- Venta local en efectivo con cálculo de cambio.
- Registro manual de tarjeta externa, sin SDK de Mercado Pago.
- Panel Manager: Corte Z, inventario, historial, sangrías, descuentos.
- Room/SQLite local y migraciones.
- Outbox y PrintJob pendientes; PrintWorker con fake en SANDBOX.
- Device API: enrolamiento, bootstrap/deltas y SyncWorker.
- Pruebas unitarias, Room, Compose y validación manual gradual.

## Fuera del alcance actual

- Venta por peso.
- Integración con Mercado Pago (SDK/API).
- Drivers reales de lector, impresora o cajón (USB/TCP) — Fase 3B.
- Validación física del hub y periféricos.
- Cierre formal de piloto endurecido (Fase 5).
- Métricas de rendimiento definitivas.

## Entregables de la versión

1. Fase 0 cerrada para desarrollo local.
2. Fase 1: caja local de efectivo (núcleo listo; cierre formal pendiente).
3. Fase 2: operación Manager persistente (núcleo listo; excepciones abiertas).
4. Fase 3A: cola ESC/POS + fakes (núcleo listo; UI/scanner pendientes).
5. Fase 4 parcial: cliente Device API + SyncWorker (E2E formal pendiente).
6. Catálogo real importable en debug y bootstrap remoto en PRODUCTION.
7. Evidencia de pruebas y checklist de cierre actualizado en
   [02-fases-y-checklists.md](02-fases-y-checklists.md).
