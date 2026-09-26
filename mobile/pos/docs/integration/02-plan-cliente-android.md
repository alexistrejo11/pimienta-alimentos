# Plan de implementación del cliente Android

Objetivo: conectar el POS local-first al Device API sin introducir una ruta de
cobro dependiente de red ni tocar hardware.

**Estado (2026-09-11):** los cortes I–III están sustancialmente implementados
en código (`DeviceCredentials`, envelope de Outbox, enrolamiento,
`SyncWorker`, bootstrap/deltas). El Corte IV (validación E2E y recuperación)
sigue abierto. Este documento conserva el plan original; el tracking de
avance vive en [../implementation/](../implementation/).

## Corte I — contrato local durable

Migrar Room antes de cualquier llamada HTTP.

1. Evolucionar `DeviceEntity` para separar identidad enrolada, sede, estado y
   metadatos de compatibilidad de la identidad debug.
2. Crear almacenamiento protegido de tokens con Android Keystore; Room guarda
   únicamente estado no secreto y nunca refresh en texto plano.
3. Reemplazar la outbox delgada por filas inmutables con el envelope completo,
   payload JSON, `schemaVersion`, intento, próximo reintento, diagnóstico y
   estado (`PENDING`, `IN_FLIGHT`, `ACKNOWLEDGED`, `ACKNOWLEDGED_REVIEW`,
   `BLOCKED`). Conservar los UUID/sequence que ya existen.
4. Añadir estado de sincronización: bootstrap aplicado, cursor de cambios,
   última sincronización correcta y error visible.
5. Completar generadores de payload para todos los hechos locales. Incluir
   primero el payload exacto de `SALE_CONFIRMED`; los otros se enviarán como
   ledger sin prometer proyección de negocio central.

La migración debe preservar ventas, turnos, PrintJobs y eventos previos. Los
eventos creados antes del envelope se convierten mediante sus snapshots Room;
si no se puede reconstruir alguno, queda bloqueado y nunca se elimina.

## Corte II — enrolamiento y bootstrap

1. Implementar cliente HTTP, serialización y autenticación Bearer del
   dispositivo. Ningún composable llama la red directamente.
2. Construir pantalla/flujo administrativo de enrolamiento solo para una
   instalación sin bootstrap válido. Un POS ya enrolado puede iniciar offline.
3. Guardar identidad y tokens de forma atómica, descargar bootstrap y aplicar
   su proyección a Room en una transacción.
4. Mantener la plantilla `pos-training-bootstrap.json` (en `main/assets`) como
   fuente inmutable de capacitación: al entrar a `SANDBOX` se borra
   `pimienta-pos-training.db` y se clona la plantilla; nunca agenda
   sincronización ni envía eventos. Producción usa una base Room separada y el
   contrato remoto canónico usa centavos/`operators`. En release enrolado, el
   cambio a capacitación exige PIN de Manager/Superadmin de producción; en debug
   el playground es pinless y puede reiniciar la plantilla.
5. Ante 401/403, detener worker y pedir reenrolamiento; ante 409 del cursor,
   conservar hechos y ejecutar bootstrap de maestros.

### Monto abierto

El cliente persiste `allowOpenProducts`, `openAmountCategories` y `stockless`
desde el bootstrap y cada delta de `policies`. Antes de agregar la línea solicita una
categoría configurada y un importe positivo, sin PIN de Manager. El payload
conserva `lineType`, la identidad nula de producto y el snapshot generado.
`authorizedByOperatorId` y `authorizedAt` pueden ir nulos.

## Corte III — worker único y no bloqueante

1. Agregar WorkManager y un único `SyncWorker` serial por dispositivo. El
   botón "sincronizar ahora", inicio de app y conectividad sólo encolan ese
   mismo trabajo único.
2. En cada ejecución: renovar access si es necesario, push de outbox por
   `deviceSequence`, persistir cada resultado, después pull de deltas y
   finalmente actualizar diagnóstico.
3. Un timeout, falta de red o 5xx conserva la fila y usa backoff. Nunca se
   mantiene una transacción de venta abierta esperando al worker.
4. Un resultado terminal se persiste antes de sacar la fila de candidatos. La
   UI observa Room y puede vender durante sincronización.

## Corte IV — validación

- Room: migración desde la base actual, reconstrucción de envelope, aplicación
  atómica de bootstrap/deltas y cursor que no avanza parcialmente.
- Contrato: enrolar, bootstrap, reenviar la misma venta (`DUPLICATE`),
  `REQUIRES_REVIEW`, revocación y cursor 409 contra Spring Boot local.
- Recuperación: proceso muerto entre commit local y envío, pérdida de respuesta
  después del commit servidor y red interrumpida durante pull.
- Aceptación: venta offline, reconexión, cola drenada y catálogo actualizado
  sin cambiar snapshots ya cobrados.

## Orden y dependencias

El orden es I → II → III → IV. Periféricos se mantienen como pista independiente:
un PrintJob pendiente no bloquea sincronización y un SyncWorker no integra
impresora, scanner ni cajón.
