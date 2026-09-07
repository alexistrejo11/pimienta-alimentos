# Alcance y entregables de la versión actual

## Objetivo

Construir la primera operación local demostrable del POS Android para vender
productos por unidad sin depender de red.

## Dentro del alcance

- POS Android nativo offline-first.
- Catálogo local basado en datos reales sanitizados.
- Productos vendidos por unidad (`PIECE`).
- Categorías, precios, disponibilidad y stock local.
- Apertura de turno y autenticación local por PIN.
- Venta local en efectivo con cálculo de cambio.
- Registro manual de tarjeta externa, sin SDK de Mercado Pago.
- Room/SQLite local y migraciones.
- Seed debug e importación transaccional.
- Persistencia de Outbox y PrintJob pendientes.
- Pruebas unitarias, Room, Compose y validación manual gradual.

## Fuera del alcance actual

- Venta por peso.
- Backend y contrato remoto de bootstrap.
- Sincronización cloud.
- Integración con Mercado Pago.
- Drivers de lector, impresora o cajón.
- Validación física del hub y periféricos.
- Métricas de rendimiento definitivas.

## Entregables de la versión

1. Fase 0 cerrada para desarrollo local.
2. Fase 1: caja local de efectivo.
3. Catálogo real importable en debug.
4. Base local durable y preparada para hechos operativos.
5. Evidencia de pruebas y checklist de cierre actualizado.
