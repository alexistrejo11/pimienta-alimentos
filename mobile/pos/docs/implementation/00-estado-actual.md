# Estado actual de la implementación

Última revisión: 2026-09-05.

Este documento es el punto de entrada para saber qué está construido, qué está
en curso y qué sigue siendo una propuesta. Los documentos de arquitectura,
producto y workflows describen el sistema completo; no todos sus apartados
significan que ya existan en código.

## Punto actual

El proyecto se encuentra en **Fase 1: caja local de efectivo**, con el flujo
local principal implementado y en revisión. Phase 0 está cerrada para el
desarrollo local.

## Ya implementado

- Proyecto Android nativo con Kotlin, Compose y un único módulo `:app`.
- Configuración reproducible con Gradle Wrapper y JDK documentado.
- Catálogo real convertido desde `docs/technical/data/deprecated_product_data.json` a seed
  debug en `app/src/debug/assets/pos-bootstrap.json`.
- Room/SQLite local con migración inicial.
- Entidades locales para sede, catálogo, usuarios, dispositivo, turno, venta,
  líneas, pagos, movimientos, Outbox y trabajos de impresión.
- Importación atómica e idempotente del bootstrap.
- Reglas locales de dinero exacto y confirmación atómica de venta.
- Apertura de turno, autenticación local por PIN y venta de unidad por pieza.
- Efectivo, cambio y tarjeta externa registrada manualmente.
- Outbox y PrintJob persistidos como pendientes.
- Pruebas unitarias iniciales para dinero y compilación verificada.

## En curso: cerrar Fase 1

El checklist completo y el criterio de salida están en
[02-fases-y-checklists.md](02-fases-y-checklists.md). En resumen, falta completar
folio visible, guardias del carrito ante cambios de catálogo, historial local,
reimpresión pendiente, pruebas instrumentadas Room, pruebas Compose y el guion
manual en emulador/tablet.

## Siguiente fase

Fase 2: operación local de Manager. Incluye monto abierto, autorizaciones,
descuentos, reposiciones, mermas, cancelación de efectivo y Corte Z.

### Prototipo visual de Fase 2 en curso

La app ya ofrece un acceso local por PIN de Manager/Superadmin desde Caja y un
panel navegable para evaluar Corte Z, Inventario, Historial y Estado. Este
subcorte conserva el carrito y no persiste operaciones de Manager: no cierra
turnos, no altera inventario, no cancela ventas ni crea eventos o trabajos de
impresión. Fase 1 y Fase 2 continúan abiertas; el detalle está en
`02-fases-y-checklists.md`.

## Fuera del alcance actual

- Venta por peso.
- Integración real con backend y sincronización cloud.
- SDK/API de Mercado Pago.
- Drivers de lector, impresora y cajón.
- Validación física de hardware.

## Cómo leer el resto de `docs/`

- `product/`: reglas y alcance del negocio; es la fuente de intención.
- `architecture/`: diseño objetivo y contratos conceptuales; mezcla decisiones
  actuales con evolución prevista y debe leerse junto con este estado.
- `ux/`: flujos y wireframes objetivo, no necesariamente pantallas construidas.
- `workflows/`: comportamiento end-to-end esperado y decisiones abiertas.
- `implementation/`: estado, alcance, fases, checklists y validación
  por fases.
