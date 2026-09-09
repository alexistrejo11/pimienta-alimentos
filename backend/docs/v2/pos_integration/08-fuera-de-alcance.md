# Fuera de alcance

Estado: **decidido para este corte de docs** · 2026-09-08

No entra en la preparación del backend POS ni en el MVP de Device API. Si más adelante se aprueba, se escribe un spec propio **antes** de código.

## Sync y topología

- Microservicios o módulo desplegable aparte
- Servidor / gateway LAN
- Comunicación directa entre tablets
- Replicación genérica de tablas o de PostgreSQL a Room
- WebSockets u obligatoriedad de red para cobrar
- Sync bidireccional de carritos o drafts

## Pagos y fiscal

- SDK/API Mercado Pago
- Conciliación automática de terminal
- Devoluciones electrónicas de tarjeta
- Pago mixto (reservado en producto; espera al cliente)
- Facturación electrónica, desglose de IVA, CFDI
- Fiados, clientes, cuentas por cobrar, monederos

## Inventario avanzado

- Transferencias entre sedes
- Recetas, producción, combos, modificadores, tamaños, extras
- Órdenes de compra y recepción detallada como parte del POS
- El cajero creando productos maestros desde la tablet

## Caja

- Gastos, pagos a proveedores, ingresos extra, sangrías que no sean resguardo
- Corrección de una sangría ya confirmada (producto: decisión pendiente; no especificar aquí)
- Cancelación postventa de tarjeta desde el POS

## Plataforma

- Imágenes pesadas de producto en bootstrap (files/S3 puede esperar)
- Política de update forzoso de APK en producción (hay que definirla antes de prod, no para el primer bootstrap)
- Retención/purge de historial local (es regla de tablet)
- Hardware, ESC/POS, cajón, parser de etiquetas por peso

## Documentación retirada

`docs/project/` (showcase YAML) se eliminó. No regenerarlo. OpenAPI vive en Swagger. Estas specs POS viven solo en `docs/v2/pos_integration/`.
