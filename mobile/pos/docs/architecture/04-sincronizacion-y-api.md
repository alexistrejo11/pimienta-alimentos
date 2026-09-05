# Sincronización y contrato preliminar de API

## Alcance

Este documento define capacidades y formas generales. Las rutas, nombres de campos y separación final de DTOs deben validarse contra el backend existente antes de implementar.

La API se divide conceptualmente en:

- **Device API:** usada por tablets enroladas para sincronizar.
- **Admin API:** usada por la web central para datos maestros, seguridad y reportes.

## Reglas del protocolo

1. Toda petición de tablet se autentica como dispositivo enrolado y autorizado.
2. Toda petición y entidad operativa incluye la sede asignada; el cliente no puede cambiar libremente de sede.
3. Cada evento tiene UUID único y secuencia monotónica por dispositivo.
4. Reenviar el mismo evento produce el mismo resultado, no un duplicado.
5. El backend responde individualmente por evento aunque se envíen lotes.
6. Una venta cobrada se recibe intacta; una inconsistencia de negocio la marca para revisión.
7. Errores de transporte/servidor se reintentan; resultados finales confirmados no.
8. Los cambios maestros se descargan con cursor opaco y versión de esquema.

## Sobre de evento

Forma conceptual:

```json
{
  "eventId": "uuid",
  "eventType": "SALE_CONFIRMED",
  "schemaVersion": 1,
  "deviceId": "uuid",
  "siteId": "uuid",
  "deviceSequence": 1842,
  "aggregateId": "sale-uuid",
  "shiftId": "shift-uuid",
  "occurredAt": "2026-09-04T15:23:12Z",
  "payload": {}
}
```

`occurredAt` ayuda a reconstruir la operación, pero el orden causal confiable dentro de una tablet lo aporta `deviceSequence`. El servidor registra además su propia fecha de recepción.

## Respuesta por evento

```json
{
  "eventId": "uuid",
  "status": "ACCEPTED",
  "serverReceivedAt": "2026-09-04T15:24:01Z",
  "incidentId": null,
  "message": null
}
```

Estados definitivos:

- `ACCEPTED`: recibido y aplicado normalmente.
- `DUPLICATE`: ya había sido recibido; equivale a éxito idempotente.
- `REQUIRES_REVIEW`: recibido y conservado, pero requiere resolución de Superadmin.

Estados/fallos no definitivos:

- error HTTP temporal, timeout o falta de red: mantener y reintentar;
- `401/403` por dispositivo revocado: detener sincronización y cerrar/bloquear sesión;
- esquema no soportado o payload técnicamente ilegible: conservar como `BLOCKED_TECHNICAL`, mostrar diagnóstico administrativo y no borrar. Requiere corregir compatibilidad, no alterar la venta.

## Orden y lotes

La tablet envía eventos por `deviceSequence`. El backend no necesita imponer un orden total entre Tablet A y B, pero sí debe poder detectar huecos o recibir reintentos sin duplicar.

Una respuesta de lote puede mezclar resultados. La tablet actualiza cada fila de outbox de forma independiente; no marca todo el lote como exitoso si solo una parte fue aceptada.

Eventos mínimos del MVP:

- `SHIFT_OPENED`;
- `SALE_CONFIRMED`;
- `SALE_CANCELLED`;
- `WASTE_RECORDED`;
- `RESTOCK_RECORDED`;
- `CASH_COUNT_SUBMITTED`;
- `CASH_COUNT_REJECTED`;
- `SHIFT_CLOSED`;
- `TICKET_REPRINTED`;
- `OVERRIDE_AUTHORIZED`.

No es obligatorio que cada evento corresponda a un endpoint distinto. Para el MVP, un endpoint de ingesta por lotes reduce superficie y conserva un protocolo uniforme.

## Descarga de datos maestros

### Bootstrap

Después del enrolamiento, la tablet descarga un snapshot consistente de su sede:

- identidad y configuración de sede;
- catálogo efectivo;
- inventario central conocido;
- usuarios autorizados y verificadores de PIN;
- políticas operativas y categorías de monto abierto;
- cursores iniciales.

El snapshot debe aplicarse de forma atómica en Room. Si falla a la mitad, la tablet conserva la última versión válida.

### Deltas

La tablet solicita cambios posteriores usando un cursor opaco:

```text
GET /api/v1/pos/sync/changes?cursor={cursor}
```

La respuesta incluye operaciones `upsert`/desactivación y `nextCursor`. La tablet guarda `nextCursor` únicamente después de aplicar todo el lote. No se usa la hora del dispositivo como cursor de sincronización.

## Endpoints preliminares de Device API

### Enrolamiento y sesión de dispositivo

- `POST /api/v1/pos/devices/enroll` — intercambia código de vinculación por identidad/credencial de dispositivo y configuración de sede.
- `POST /api/v1/pos/devices/refresh` — renueva credenciales cuando corresponda.
- `GET /api/v1/pos/devices/me` — obtiene estado de autorización, sede y versión mínima compatible.

### Sincronización

- `GET /api/v1/pos/sync/bootstrap` — snapshot inicial completo de la sede.
- `GET /api/v1/pos/sync/changes?cursor=...` — cambios incrementales de catálogo, stock, usuarios y configuración.
- `POST /api/v1/pos/sync/events` — ingesta idempotente de eventos en lote con resultado individual.

Para el MVP, estas rutas cubren el cliente. Un endpoint específico como `/sales/sync` puede existir internamente, pero no es necesario si la ingesta de eventos está bien tipada y versionada.

## Endpoints preliminares de Admin API

Estas capacidades pueden reutilizar endpoints existentes del backend; la lista identifica necesidades, no obliga a duplicarlas.

### Catálogo y sede

- `GET/POST /api/v1/admin/products`
- `GET/PATCH /api/v1/admin/products/{productId}`
- `GET/PUT /api/v1/admin/sites/{siteId}/products/{productId}` — disponibilidad y precio local.
- `GET/PUT /api/v1/admin/sites/{siteId}/settings` — política de stock y umbrales.

### Inventario

- `GET /api/v1/admin/sites/{siteId}/inventory`
- `POST /api/v1/admin/sites/{siteId}/inventory/adjustments`
- `GET /api/v1/admin/sites/{siteId}/inventory/movements`

### Usuarios y dispositivos

- `GET/POST /api/v1/admin/users`
- `PATCH /api/v1/admin/users/{userId}` — rol, PIN o estado.
- `POST /api/v1/admin/devices/enrollment-codes`
- `GET /api/v1/admin/devices`
- `POST /api/v1/admin/devices/{deviceId}/revoke`

### Incidencias

- `GET /api/v1/admin/sync-incidents`
- `GET /api/v1/admin/sync-incidents/{incidentId}`
- `POST /api/v1/admin/sync-incidents/{incidentId}/accept`
- `POST /api/v1/admin/sync-incidents/{incidentId}/link-product`

### Reportes

- `GET /api/v1/admin/reports/sales`
- `GET /api/v1/admin/reports/products`
- `GET /api/v1/admin/reports/waste-cancellations`
- `GET /api/v1/admin/reports/shift-closes`

Los filtros mínimos son sede y rango de fechas; según el reporte también turno, producto, motivo y estado de sincronización.

## Idempotencia central

El backend mantiene una restricción única por `eventId`, y una venta mantiene unicidad por su UUID. El procesamiento debe ocurrir dentro de una transacción central:

```text
Registrar recepción única del evento
          ↓
Persistir venta/hecho original
          ↓
Aplicar movimientos y proyecciones
          ↓
Crear incidencia si corresponde
          ↓
Confirmar resultado
```

Si la tablet pierde la respuesta después del commit, reenvía el mismo evento y recibe `DUPLICATE` con el resultado previamente registrado.

## Conflictos y autoridad

- Las ventas confirmadas, pagos y snapshots de líneas enviados por caja no se sobrescriben.
- El backend puede clasificar una venta para revisión, no reescribirla.
- El inventario central se calcula aplicando movimientos; nunca acepta como autoridad un saldo local enviado por una tablet.
- Catálogo, usuarios y configuración central sí reemplazan sus proyecciones locales al aplicar un delta más nuevo.
- Un carrito activo conserva sus snapshots aunque el catálogo cambie.

## Compatibilidad y versionado

- Versión mayor en la ruta (`/api/v1`) para cambios incompatibles del contrato.
- `schemaVersion` por tipo de evento para evolución gradual.
- Campos nuevos deben ser opcionales o tener valores por defecto compatibles.
- El backend informa una versión mínima de app; la política de actualización obligatoria debe definirse antes de producción.
- Los eventos pendientes creados por una versión anterior deben seguir siendo aceptables durante la ventana de soporte acordada.

## Fuera del contrato MVP

- Comunicación directa entre tablets.
- Servidor o gateway LAN.
- Integración API/SDK con Mercado Pago.
- WebSockets obligatorios para vender.
- Sincronización genérica de tablas.
- Facturación electrónica.
