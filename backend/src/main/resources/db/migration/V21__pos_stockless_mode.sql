-- Per-headquarter POS sales-only mode: SALE_CONFIRMED / SALE_CANCELLED do not move inventory.

ALTER TABLE headquarter_pos_settings
    ADD COLUMN stockless BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN headquarter_pos_settings.stockless IS
    'When true, POS sale and cancel events do not generate inventory movements for this headquarter.';
