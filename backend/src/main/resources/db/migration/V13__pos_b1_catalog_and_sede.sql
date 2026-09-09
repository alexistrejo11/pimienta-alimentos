-- POS B1: per-sede catalog, pos-settings, LocationType.POS, unique barcode

-- ── storage_locations: POS type + headquarter_id ─────────────────────────────

ALTER TABLE storage_locations
    ADD COLUMN headquarter_id BIGINT REFERENCES headquarters (id);

CREATE INDEX idx_storage_locations_headquarter_id ON storage_locations (headquarter_id);

ALTER TABLE storage_locations
    DROP CONSTRAINT ck_storage_locations_type;

ALTER TABLE storage_locations
    ADD CONSTRAINT ck_storage_locations_type
        CHECK (type IN ('WAREHOUSE', 'ZONE', 'AISLE', 'SHELF', 'BIN', 'POS'));

ALTER TABLE storage_locations
    ADD CONSTRAINT ck_storage_locations_pos_headquarter
        CHECK (type <> 'POS' OR headquarter_id IS NOT NULL);

CREATE UNIQUE INDEX uk_storage_locations_pos_headquarter_active
    ON storage_locations (headquarter_id)
    WHERE type = 'POS' AND deleted_at IS NULL;

CREATE UNIQUE INDEX uk_storage_locations_pos_code_active
    ON storage_locations (code)
    WHERE type = 'POS' AND deleted_at IS NULL;

-- ── headquarter POS operational settings (1:1) ───────────────────────────────

CREATE TABLE headquarter_pos_settings (
    id                           BIGSERIAL PRIMARY KEY,
    headquarter_id               BIGINT       NOT NULL REFERENCES headquarters (id),
    currency                     VARCHAR(8)   NOT NULL DEFAULT 'MXN',
    catalog_stale_warn_hours     INTEGER      NOT NULL DEFAULT 24,
    catalog_stale_block_hours    INTEGER      NOT NULL DEFAULT 72,
    open_amount_categories       JSONB        NOT NULL DEFAULT '[]'::jsonb,
    default_negative_stock_limit INTEGER,
    created_at                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at                   TIMESTAMP,
    version                      BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT uk_headquarter_pos_settings_hq UNIQUE (headquarter_id)
);

CREATE INDEX idx_headquarter_pos_settings_deleted_at
    ON headquarter_pos_settings (deleted_at);

COMMENT ON TABLE headquarter_pos_settings IS 'POS operational config per headquarter (currency, catalog stale thresholds, open-amount categories).';

-- ── headquarter_items: effective POS catalog per sede ────────────────────────

CREATE TABLE headquarter_items (
    id                   BIGSERIAL PRIMARY KEY,
    headquarter_id       BIGINT         NOT NULL REFERENCES headquarters (id),
    item_id              BIGINT         NOT NULL REFERENCES inventory_items (id),
    sale_category        VARCHAR(64)    NOT NULL,
    sale_price           NUMERIC(19, 6) NOT NULL DEFAULT 0,
    available            BOOLEAN        NOT NULL DEFAULT TRUE,
    stock_policy         VARCHAR(32)    NOT NULL DEFAULT 'CONTROLLED',
    negative_stock_limit INTEGER,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at           TIMESTAMP,
    version              BIGINT         NOT NULL DEFAULT 1,
    CONSTRAINT ck_headquarter_items_stock_policy
        CHECK (stock_policy IN ('CONTROLLED', 'NOT_CONTROLLED'))
);

CREATE UNIQUE INDEX uk_headquarter_items_hq_item_active
    ON headquarter_items (headquarter_id, item_id)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_headquarter_items_headquarter_id ON headquarter_items (headquarter_id);
CREATE INDEX idx_headquarter_items_item_id ON headquarter_items (item_id);
CREATE INDEX idx_headquarter_items_deleted_at ON headquarter_items (deleted_at);

COMMENT ON TABLE headquarter_items IS 'Effective POS catalog row per headquarter and global inventory item.';

-- ── barcode: unique global when not null (includes soft-deleted) ─────────────

-- V12 dummy seed has no barcodes; clear any accidental duplicates defensively.
UPDATE inventory_items i
SET barcode = NULL
WHERE barcode IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM inventory_items d
      WHERE d.barcode = i.barcode
        AND d.id < i.id
  );

DROP INDEX IF EXISTS idx_inventory_items_barcode;

CREATE UNIQUE INDEX uk_inventory_items_barcode_not_null
    ON inventory_items (barcode)
    WHERE barcode IS NOT NULL;
