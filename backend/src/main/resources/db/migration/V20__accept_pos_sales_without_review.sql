-- Sales are accepted on ingest. Close the review queue so historical tickets
-- count in ACCEPTED-only reports. Accept does not change sale lines or prices.

UPDATE pos_sync_events
SET status = 'ACCEPTED',
    message = 'Accepted without review',
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'REQUIRES_REVIEW'
  AND deleted_at IS NULL;

UPDATE pos_sync_incidents
SET accepted_at = COALESCE(accepted_at, CURRENT_TIMESTAMP),
    accept_label = COALESCE(accept_label, 'Aceptada'),
    accept_note = COALESCE(accept_note, 'Cerrada: las ventas se aceptan sin revisión'),
    updated_at = CURRENT_TIMESTAMP
WHERE accepted_at IS NULL
  AND deleted_at IS NULL;
