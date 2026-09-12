# Integración POS ↔ Spring Boot

Esta carpeta es la fuente canónica para el contrato compartido y para el plan
de implementación de la integración. La documentación histórica de backend en
`backend/docs/v2/pos_integration/` no debe actualizarse ni usarse para tomar
decisiones nuevas.

## Estado comprobado — 2026-09-11

El backend ya implementa B1–B6: catálogo POS por sede, operadores y
dispositivos, enrolamiento y refresh, bootstrap, eventos idempotentes, deltas,
incidencias y reportes. Android mantiene dos espacios Room aislados: `SANDBOX`
(capacitación / playground) clonado desde `pos-training-bootstrap.json` sin red,
y `PRODUCTION` para enrolamiento y sincronización. La plantilla de capacitación
es inmutable; la base scratch se borra y se reimporta al entrar.
El cliente ya incluye `DeviceApi`, pantalla de enrolamiento, bootstrap/deltas
y `SyncWorker`; falta el cierre E2E formal del Corte IV / Fase 4.

## Documentos

- [Contrato implementado](01-contrato-implementado.md): rutas, autoridad de
  datos, seguridad y semántica que el cliente debe respetar.
- [Plan del cliente Android](02-plan-cliente-android.md): cortes para migrar
  Room, enrolar, sincronizar y comprobar recuperación.

## Límites

Cobrar nunca espera a la red. La UI lee Room; la red y WorkManager son efectos
asíncronos. Hardware (lector, impresora, cajón, ESC/POS y su validación física)
queda fuera de esta integración y no bloquea los workers de sincronización.
