-- POS catalog: per-sede settings, managed sale categories, and effective catalog rows

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

CREATE TABLE pos_sale_categories (
    id              BIGSERIAL PRIMARY KEY,
    headquarter_id  BIGINT       NOT NULL REFERENCES headquarters (id),
    name            VARCHAR(64)  NOT NULL,
    display_order   INTEGER      NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX uk_pos_sale_categories_hq_name_active
    ON pos_sale_categories (headquarter_id, LOWER(name))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_pos_sale_categories_headquarter_id
    ON pos_sale_categories (headquarter_id);

CREATE INDEX idx_pos_sale_categories_active
    ON pos_sale_categories (active);

COMMENT ON TABLE pos_sale_categories IS 'Managed sale categories for the POS catalog, scoped to a headquarter.';

CREATE TABLE headquarter_items (
    id                   BIGSERIAL PRIMARY KEY,
    headquarter_id       BIGINT         NOT NULL REFERENCES headquarters (id),
    item_id              BIGINT         NOT NULL REFERENCES inventory_items (id),
    pos_sale_category_id BIGINT         NOT NULL REFERENCES pos_sale_categories (id),
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
CREATE INDEX idx_headquarter_items_pos_sale_category_id ON headquarter_items (pos_sale_category_id);
CREATE INDEX idx_headquarter_items_deleted_at ON headquarter_items (deleted_at);

COMMENT ON TABLE headquarter_items IS 'Effective POS catalog row per headquarter and global inventory item.';
COMMENT ON COLUMN headquarter_items.sale_category IS 'Deprecated compatibility value; use pos_sale_category_id.';

CREATE FUNCTION sync_headquarter_item_pos_sale_category()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    category_id BIGINT;
    category_name VARCHAR(64);
BEGIN
    category_name := NULLIF(BTRIM(NEW.sale_category), '');
    IF category_name IS NULL THEN
        category_name := 'GENERAL';
        NEW.sale_category := category_name;
    END IF;

    SELECT id INTO category_id
    FROM pos_sale_categories
    WHERE headquarter_id = NEW.headquarter_id
      AND LOWER(name) = LOWER(category_name)
      AND deleted_at IS NULL
    LIMIT 1;

    IF category_id IS NULL THEN
        INSERT INTO pos_sale_categories (headquarter_id, name, display_order)
        VALUES (NEW.headquarter_id, category_name, 0)
        RETURNING id INTO category_id;
    END IF;

    NEW.pos_sale_category_id := category_id;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_headquarter_items_sync_pos_sale_category
BEFORE INSERT OR UPDATE OF headquarter_id, sale_category
ON headquarter_items
FOR EACH ROW
EXECUTE FUNCTION sync_headquarter_item_pos_sale_category();
