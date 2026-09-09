-- POS B6: accept label on sync incidents (classification only; does not mutate sale payload)

ALTER TABLE pos_sync_incidents
    ADD COLUMN accept_label VARCHAR(128);

COMMENT ON COLUMN pos_sync_incidents.accept_label IS 'Staff ADMIN accept label (set on accept; nullable while open).';
