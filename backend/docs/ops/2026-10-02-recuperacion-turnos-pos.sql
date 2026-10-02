-- Recuperación de turnos POS rechazados en la tablet T2 (2026-10-02)
--
-- Contexto:
--   1. El cierre de 4bd3932c se rechazó porque cashExpectedCentavos salió negativo
--      (retiro SG-T2-4BD3-0453 duplicado por doble toque). El turno quedó OPEN en servidor.
--   2. Las aperturas de b31633e3 (secuencia 575) y f1534cb1 (secuencia 1007) chocaron con
--      ux_pos_shifts_active_device, el lote respondió HTTP 400 y nunca se guardaron.
--   3. Sus retiros, arqueos y cierres quedaron REJECTED ("El turno debe abrirse antes...").
--
-- Qué hace:
--   A. Cierra 4bd3932c con el cierre corregido (sin el retiro duplicado).
--   B. Anula el retiro duplicado SG-T2-4BD3-0453 (borra el movimiento; el evento queda DUPLICATE).
--   C. Reconstruye b31633e3 y f1534cb1: apertura en las secuencias faltantes, turno cerrado,
--      retiros y arqueos desde el payload de sus eventos rechazados.
--   D. Verifica que todo cuadre. NO hace COMMIT: revisar la salida y luego COMMIT o ROLLBACK.
--
-- Uso (psql interactivo):
--   \set ON_ERROR_STOP on
--   \i backend/docs/ops/2026-10-02-recuperacion-turnos-pos.sql
--   -- revisar la salida
--   COMMIT;   -- o ROLLBACK;

BEGIN;

-- ---------------------------------------------------------------------------
-- 0. Verificaciones previas: si algo no coincide con el diagnóstico, se aborta.
-- ---------------------------------------------------------------------------
DO $$
DECLARE
  dev CONSTANT uuid := '829012b4-3a6f-4436-84e5-1ba076baed6b';
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pos_shifts
                 WHERE shift_id = '4bd3932c-3520-4c4c-91e7-5806e5c08098' AND status = 'OPEN') THEN
    RAISE EXCEPTION '4bd3932c ya no está OPEN';
  END IF;

  IF EXISTS (SELECT 1 FROM pos_shifts
             WHERE shift_id IN ('b31633e3-8b42-4622-927b-d879d818ba9b',
                                'f1534cb1-8b85-40c9-bbc4-d9279523dc9e')) THEN
    RAISE EXCEPTION 'b31633e3 o f1534cb1 ya existen en pos_shifts';
  END IF;

  IF EXISTS (SELECT 1 FROM pos_sync_events
             WHERE device_id = dev AND device_sequence IN (575, 1007)) THEN
    RAISE EXCEPTION 'Las secuencias 575 o 1007 ya no están vacías';
  END IF;

  IF (SELECT shift_id FROM pos_sync_events WHERE device_id = dev AND device_sequence = 576)
       IS DISTINCT FROM 'b31633e3-8b42-4622-927b-d879d818ba9b' THEN
    RAISE EXCEPTION 'La secuencia 576 no pertenece a b31633e3';
  END IF;

  IF (SELECT shift_id FROM pos_sync_events WHERE device_id = dev AND device_sequence = 1008)
       IS DISTINCT FROM 'f1534cb1-8b85-40c9-bbc4-d9279523dc9e' THEN
    RAISE EXCEPTION 'La secuencia 1008 no pertenece a f1534cb1';
  END IF;

  IF (SELECT count(*) FROM pos_sync_events
      WHERE status = 'REJECTED' AND shift_id = '4bd3932c-3520-4c4c-91e7-5806e5c08098') <> 1
     OR (SELECT count(*) FROM pos_sync_events
         WHERE status = 'REJECTED' AND shift_id = 'b31633e3-8b42-4622-927b-d879d818ba9b') <> 10
     OR (SELECT count(*) FROM pos_sync_events
         WHERE status = 'REJECTED' AND shift_id = 'f1534cb1-8b85-40c9-bbc4-d9279523dc9e') <> 3 THEN
    RAISE EXCEPTION 'El número de eventos rechazados por turno no es 1 / 10 / 3';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pos_cash_movements
                 WHERE movement_id = 'd59c454a-fc5d-4e37-b6c2-0882e0ab0242'
                   AND folio = 'SG-T2-4BD3-0453' AND amount_centavos = 260000) THEN
    RAISE EXCEPTION 'No se encontró el retiro duplicado SG-T2-4BD3-0453 de 260000';
  END IF;
END $$;

-- ---------------------------------------------------------------------------
-- A. Cerrar 4bd3932c con el cierre corregido.
--    Esperado: 120000 + 1485600 - 1431300 = 174300. Diferencia: 1029700 - 174300 = 855400.
-- ---------------------------------------------------------------------------
UPDATE pos_shifts s
SET status = 'CLOSED',
    closed_at = e.occurred_at,
    closing_expected_cash_centavos = 174300,
    closing_counted_cash_centavos = (e.payload->>'countedCashCentavos')::bigint,
    closing_difference_centavos = (e.payload->>'countedCashCentavos')::bigint - 174300,
    closed_event_id = e.event_id,
    updated_at = now() AT TIME ZONE 'UTC',
    version = s.version + 1
FROM pos_sync_events e
WHERE s.shift_id = '4bd3932c-3520-4c4c-91e7-5806e5c08098'
  AND s.status = 'OPEN'
  AND e.shift_id = s.shift_id
  AND e.event_type = 'SHIFT_CLOSED'
  AND e.aggregate_id = '311e6688-dcc2-461b-abfe-1abc3b37c2eb'
  AND e.status = 'REJECTED';

UPDATE pos_sync_events
SET status = 'ACCEPTED',
    message = 'Reprocesado manualmente 2026-10-02: cierre corregido sin retiro duplicado '
              || 'SG-T2-4BD3-0453 (original: esperado -85700, diferencia 1115400)',
    updated_at = now() AT TIME ZONE 'UTC',
    version = version + 1
WHERE shift_id = '4bd3932c-3520-4c4c-91e7-5806e5c08098'
  AND event_type = 'SHIFT_CLOSED'
  AND aggregate_id = '311e6688-dcc2-461b-abfe-1abc3b37c2eb'
  AND status = 'REJECTED';

-- ---------------------------------------------------------------------------
-- B. Anular el retiro duplicado SG-T2-4BD3-0453.
-- ---------------------------------------------------------------------------
WITH anulado AS (
  DELETE FROM pos_cash_movements
  WHERE movement_id = 'd59c454a-fc5d-4e37-b6c2-0882e0ab0242'
  RETURNING event_id
)
UPDATE pos_sync_events e
SET status = 'DUPLICATE',
    message = 'Retiro duplicado por doble toque (mismo monto que SG-T2-4BD3-0452); '
              || 'anulado manualmente 2026-10-02',
    updated_at = now() AT TIME ZONE 'UTC',
    version = e.version + 1
FROM anulado
WHERE e.event_id = anulado.event_id;

-- ---------------------------------------------------------------------------
-- C. Reconstruir b31633e3 y f1534cb1.
-- ---------------------------------------------------------------------------
CREATE TEMP TABLE recuperacion ON COMMIT DROP AS
SELECT v.shift_id,
       v.secuencia,
       v.fondo,
       gen_random_uuid() AS open_event_id,
       siguiente.occurred_at AS opened_at,
       c.headquarter_id,
       c.device_id,
       (SELECT ps.cashier_operator_id
        FROM pos_sales ps
        WHERE ps.shift_id = v.shift_id AND ps.cashier_operator_id IS NOT NULL
        GROUP BY ps.cashier_operator_id
        ORDER BY count(*) DESC
        LIMIT 1) AS cashier_operator_id,
       c.event_id AS close_event_id,
       c.occurred_at AS closed_at,
       (c.payload->>'cashExpectedCentavos')::bigint AS esperado,
       (c.payload->>'countedCashCentavos')::bigint AS contado,
       (c.payload->>'differenceCentavos')::bigint AS diferencia
FROM (VALUES ('b31633e3-8b42-4622-927b-d879d818ba9b'::uuid, 575::bigint, 133000::bigint),
             ('f1534cb1-8b85-40c9-bbc4-d9279523dc9e'::uuid, 1007::bigint, 100000::bigint))
       AS v (shift_id, secuencia, fondo)
JOIN pos_sync_events c
  ON c.shift_id = v.shift_id AND c.event_type = 'SHIFT_CLOSED' AND c.status = 'REJECTED'
JOIN pos_sync_events siguiente
  ON siguiente.device_id = c.device_id AND siguiente.device_sequence = v.secuencia + 1;

DO $$
BEGIN
  IF (SELECT count(*) FROM recuperacion) <> 2 THEN
    RAISE EXCEPTION 'La tabla de recuperación no tiene exactamente 2 turnos';
  END IF;
END $$;

-- Apertura reconstruida en la secuencia faltante.
INSERT INTO pos_sync_events
  (event_id, event_type, schema_version, device_id, headquarter_id, device_sequence,
   aggregate_id, shift_id, occurred_at, payload, status, server_received_at, message,
   created_at, updated_at)
SELECT r.open_event_id, 'SHIFT_OPENED', 1, r.device_id, r.headquarter_id, r.secuencia,
       r.shift_id, r.shift_id, r.opened_at,
       jsonb_build_object('shiftId', r.shift_id,
                          'cashierOperatorId', r.cashier_operator_id,
                          'openingCashCentavos', r.fondo,
                          'reconstructed', true),
       'ACCEPTED', now() AT TIME ZONE 'UTC',
       'Apertura reconstruida manualmente 2026-10-02 a partir del cierre; '
         || 'el evento original nunca se guardó (lote HTTP 400)',
       now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC'
FROM recuperacion r;

-- Turno ya cerrado con los datos de su cierre.
INSERT INTO pos_shifts
  (shift_id, headquarter_id, device_id, cashier_operator_id, opening_cash_centavos,
   opened_at, closed_at, status, closing_expected_cash_centavos,
   closing_counted_cash_centavos, closing_difference_centavos,
   opened_event_id, closed_event_id, created_at, updated_at)
SELECT r.shift_id, r.headquarter_id, r.device_id, r.cashier_operator_id, r.fondo,
       r.opened_at, r.closed_at, 'CLOSED', r.esperado, r.contado, r.diferencia,
       r.open_event_id, r.close_event_id, now() AT TIME ZONE 'UTC', now() AT TIME ZONE 'UTC'
FROM recuperacion r;

-- Retiros desde el payload de sus eventos rechazados.
INSERT INTO pos_cash_movements
  (movement_id, shift_id, headquarter_id, event_id, movement_type, amount_centavos,
   folio, reason, authorized_by_user_id, authorized_by_role, occurred_at)
SELECT e.aggregate_id, e.shift_id, e.headquarter_id, e.event_id, 'WITHDRAWAL',
       (e.payload->>'amountCentavos')::bigint,
       e.payload->>'folio', e.payload->>'reason',
       (e.payload->>'authorizedByUserId')::bigint, e.payload->>'authorizedByRole',
       e.occurred_at
FROM pos_sync_events e
JOIN recuperacion r ON r.shift_id = e.shift_id
WHERE e.event_type = 'CASH_WITHDRAWAL_RECORDED' AND e.status = 'REJECTED';

-- Arqueos desde el payload de sus eventos rechazados.
INSERT INTO pos_cash_counts
  (count_id, shift_id, headquarter_id, event_id, total_centavos, denominations, submitted_at)
SELECT e.aggregate_id, e.shift_id, e.headquarter_id, e.event_id,
       (e.payload->>'totalCentavos')::bigint,
       e.payload->'denominations',
       e.occurred_at
FROM pos_sync_events e
JOIN recuperacion r ON r.shift_id = e.shift_id
WHERE e.event_type = 'CASH_COUNT_SUBMITTED' AND e.status = 'REJECTED';

-- Marcar como aceptados los eventos ya aplicados.
UPDATE pos_sync_events e
SET status = 'ACCEPTED',
    message = 'Reprocesado manualmente 2026-10-02 (turno reconstruido)',
    updated_at = now() AT TIME ZONE 'UTC',
    version = e.version + 1
FROM recuperacion r
WHERE e.shift_id = r.shift_id AND e.status = 'REJECTED';

-- ---------------------------------------------------------------------------
-- D. Verificaciones posteriores.
-- ---------------------------------------------------------------------------

-- Resumen por turno: "cuadra" debe ser true en los tres.
SELECT s.shift_id,
       s.status,
       s.cashier_operator_id,
       s.opened_at - interval '6 hours' AS apertura_local,
       s.closed_at - interval '6 hours' AS cierre_local,
       s.opening_cash_centavos AS fondo,
       ventas.efectivo AS ventas_efectivo,
       coalesce(retiros.total, 0) AS retiros,
       coalesce(retiros.cantidad, 0) AS num_retiros,
       (SELECT count(*) FROM pos_cash_counts cc WHERE cc.shift_id = s.shift_id) AS num_arqueos,
       s.closing_expected_cash_centavos AS esperado,
       s.closing_counted_cash_centavos AS contado,
       s.closing_difference_centavos AS diferencia,
       s.opening_cash_centavos + ventas.efectivo - coalesce(retiros.total, 0)
         = s.closing_expected_cash_centavos AS cuadra
FROM pos_shifts s
CROSS JOIN LATERAL (
  SELECT coalesce(sum(p.amount_centavos), 0) AS efectivo
  FROM pos_sales sa
  JOIN pos_sale_payments p ON p.sale_id = sa.sale_id
  WHERE sa.shift_id = s.shift_id AND sa.status = 'CONFIRMED' AND p.method = 'CASH'
) ventas
LEFT JOIN LATERAL (
  SELECT sum(m.amount_centavos) AS total, count(*) AS cantidad
  FROM pos_cash_movements m
  WHERE m.shift_id = s.shift_id AND m.movement_type = 'WITHDRAWAL'
) retiros ON true
WHERE s.shift_id IN ('4bd3932c-3520-4c4c-91e7-5806e5c08098',
                     'b31633e3-8b42-4622-927b-d879d818ba9b',
                     'f1534cb1-8b85-40c9-bbc4-d9279523dc9e')
ORDER BY s.opened_at;

DO $$
DECLARE
  dev CONSTANT uuid := '829012b4-3a6f-4436-84e5-1ba076baed6b';
BEGIN
  IF EXISTS (SELECT 1 FROM pos_sync_events WHERE status = 'REJECTED') THEN
    RAISE EXCEPTION 'Todavía quedan eventos REJECTED';
  END IF;

  IF EXISTS (SELECT 1 FROM pos_shifts WHERE device_id = dev AND status = 'OPEN') THEN
    RAISE EXCEPTION 'T2 todavía tiene un turno OPEN';
  END IF;

  IF EXISTS (
    SELECT 1 FROM (
      SELECT device_sequence,
             lead(device_sequence) OVER (ORDER BY device_sequence) AS siguiente
      FROM pos_sync_events WHERE device_id = dev
    ) t WHERE siguiente > device_sequence + 1
  ) THEN
    RAISE EXCEPTION 'Todavía hay huecos de secuencia en T2';
  END IF;

  IF (SELECT count(*) FROM pos_cash_movements
      WHERE shift_id = 'b31633e3-8b42-4622-927b-d879d818ba9b') <> 8
     OR (SELECT count(*) FROM pos_cash_movements
         WHERE shift_id = '4bd3932c-3520-4c4c-91e7-5806e5c08098') <> 6
     OR (SELECT count(*) FROM pos_cash_counts
         WHERE shift_id = 'b31633e3-8b42-4622-927b-d879d818ba9b') <> 1
     OR (SELECT count(*) FROM pos_cash_counts
         WHERE shift_id = 'f1534cb1-8b85-40c9-bbc4-d9279523dc9e') <> 2 THEN
    RAISE EXCEPTION 'Los conteos de retiros / arqueos no son los esperados';
  END IF;

  RAISE NOTICE 'Verificaciones OK. Revisa el resumen y ejecuta COMMIT (o ROLLBACK).';
END $$;

-- Sin COMMIT a propósito.
