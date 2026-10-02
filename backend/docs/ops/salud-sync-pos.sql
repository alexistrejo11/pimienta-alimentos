-- Revisión de salud del sync POS. Correr al terminar cada jornada.
-- Ambas consultas deben regresar cero filas; si regresan algo, revisar antes del siguiente día.
-- Las fechas están en UTC (hora local = UTC-6).

-- 1. Eventos rechazados por el servidor.
SELECT e.server_received_at,
       d.visible_code,
       e.event_type,
       e.device_sequence,
       e.event_id,
       e.shift_id,
       e.message
FROM pos_sync_events e
JOIN pos_devices d ON d.id = e.device_id
WHERE e.status = 'REJECTED'
ORDER BY e.server_received_at DESC;

-- 2. Huecos en la secuencia de cada tableta (eventos que nunca llegaron al servidor).
SELECT d.visible_code,
       s.device_id,
       s.device_sequence + 1      AS falta_desde,
       s.siguiente - 1            AS falta_hasta,
       s.siguiente - s.device_sequence - 1 AS eventos_faltantes
FROM (
    SELECT device_id,
           device_sequence,
           LEAD(device_sequence) OVER (PARTITION BY device_id ORDER BY device_sequence) AS siguiente
    FROM pos_sync_events
) s
JOIN pos_devices d ON d.id = s.device_id
WHERE s.siguiente > s.device_sequence + 1
ORDER BY d.visible_code, s.device_sequence;
