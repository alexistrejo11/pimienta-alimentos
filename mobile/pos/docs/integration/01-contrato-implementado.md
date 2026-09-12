# Contrato implementado de Device API

Estado: **implementado en Spring Boot y en el cliente Android
(`PRODUCTION`)**. El cierre formal E2E y el checklist de Fase 4 siguen
abiertos; ver [../implementation/00-estado-actual.md](../implementation/00-estado-actual.md).

## Autoridad y formatos

- `Headquarter` es la sede POS. Sus IDs y los de maestros `Long` viajan como
  strings decimales; los hechos y el dispositivo usan UUID.
- `Item` es maestro global. `headquarter_items` aporta precio efectivo,
  categoría de venta, disponibilidad y política de stock por sede.
- La ubicación `POS` por sede es la única que permite stock negativo en el
  flujo POS. La tablet nunca envía un saldo final.
- Todo importe remoto usa centavos enteros. Los snapshots de venta son
  inmutables; una revisión no modifica el ticket.
- Operadores POS (`CASHIER`, `MANAGER`, `SUPERADMIN`) son independientes de
  roles web. El bootstrap entrega su verificador de PIN, nunca un PIN plano.

## Device API

| Ruta | Uso del cliente |
|---|---|
| `POST /api/v1/pos/devices/enroll` | Canjear código de un solo uso de 10 min por identidad de tablet, sede y tokens. |
| `POST /api/v1/pos/devices/refresh` | Rotar refresh token. El access dura 15 min; refresh 90 días, máximo 365. |
| `GET /api/v1/pos/devices/me` | Consultar estado, sede, versión mínima y schemas soportados. |
| `GET /api/v1/pos/sync/bootstrap` | Descargar snapshot plano y atómico de la sede. |
| `GET /api/v1/pos/sync/changes?cursor=` | Descargar `upsert`/`deactivate`; cursor ajeno o inválido devuelve 409 y exige bootstrap. |
| `POST /api/v1/pos/sync/events` | Enviar lote ordenado por secuencia y recibir resultado individual. |

El access JWT representa al dispositivo (`typ=device`, `scope=pos:sync`), no
al cajero. Revocación devuelve 401/403 al reconectar: se detiene sync y la app
deja de usar esas credenciales. Reasignar una tablet significa revocarla y
enrolarla de nuevo; no hay transferencia in-place.

## Bootstrap y deltas

El bootstrap contiene `site`, `device`, `operators`, `products`, categorías de
monto abierto, políticas y `cursors.changes`. Productos usan
`priceCentavos`, `costCentavos`, `stockQuantity`, `stockMinQuantity` y
`negativeStockLimit`; no los campos decimales del seed debug.

Se aplica por completo dentro de una transacción Room. Los hechos locales,
turnos, tickets, impresión y outbox no se borran ni se regeneran. En deltas,
la app guarda `nextCursor` solo después de aplicar todo el lote. Un cambio de
catálogo no modifica snapshots de ventas ni un carrito activo.

## Envelope y resultados

Cada evento persistido localmente debe conservar exactamente:

```text
eventId, eventType, schemaVersion, deviceId, siteId, deviceSequence,
aggregateId, shiftId, occurredAt, payload JSON
```

`eventId` se reutiliza en cada reintento. `deviceSequence` es estrictamente
local a una tablet; no existe orden global. Los estados terminales son:

- `ACCEPTED` y `DUPLICATE`: reconocer el evento local.
- `REQUIRES_REVIEW`: reconocerlo y conservar el `incidentId`; nunca deshacer
  la venta local.
- `REJECTED`: conservarlo como bloqueo técnico/administrativo y mostrarlo.

Timeout, red y 5xx no son resultados: se reintentan. La respuesta puede mezclar
estados, por lo que Room se actualiza fila por fila.

## Alcance semántico actual

El servidor guarda cualquier evento recibido en el ledger, pero **solo
`SALE_CONFIRMED` tiene hoy proyección de negocio completa**: persistencia de
venta/líneas/pagos, movimiento POS de inventario e incidencia. Los demás
eventos locales (`SHIFT_*`, merma, reposición, sangría, cancelación,
reimpresión) pueden reconocerse como ledger, pero no deben anunciarse como
reportes o proyecciones centrales completas hasta que exista su procesador
backend y su payload versionado.

Por eso el primer cliente debe drenar toda la outbox en orden —para no dejar
huecos ni evidencia pendiente— y mostrar que los eventos no-venta están
"recibidos en ledger". La siguiente ampliación de negocio se especificará por
tipo de evento antes de modificar backend o móvil.

## No entra

No hay gateway LAN, comunicación tablet-a-tablet, WebSockets obligatorios,
sync de carritos, Mercado Pago, CFDI ni integración de hardware. El flujo web
admin continúa usando sus endpoints existentes; los recursos POS nuevos viven
bajo `/api/v1/pos/admin/**`.
