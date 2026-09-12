-- POS catalog foundation: managed sale categories and canonical identifiers.

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

ALTER TABLE headquarter_items
    ADD COLUMN IF NOT EXISTS pos_sale_category_id BIGINT;

-- Preserve the current catalog exactly while moving its category to a managed row.
INSERT INTO pos_sale_categories (headquarter_id, name, display_order)
SELECT DISTINCT ON (headquarter_id, LOWER(BTRIM(sale_category)))
       headquarter_id, BTRIM(sale_category), 0
FROM headquarter_items
WHERE deleted_at IS NULL
  AND sale_category IS NOT NULL
  AND BTRIM(sale_category) <> ''
ORDER BY headquarter_id, LOWER(BTRIM(sale_category)), id;

UPDATE pos_sale_categories c
SET display_order = ranked.position
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY headquarter_id ORDER BY LOWER(name), id) - 1 AS position
    FROM pos_sale_categories
    WHERE deleted_at IS NULL
) ranked
WHERE c.id = ranked.id;

UPDATE headquarter_items h
SET pos_sale_category_id = c.id
FROM pos_sale_categories c
WHERE c.headquarter_id = h.headquarter_id
  AND LOWER(c.name) = LOWER(BTRIM(h.sale_category))
  AND c.deleted_at IS NULL;

-- Existing rows are required to have a category; GENERAL handles malformed legacy data.
INSERT INTO pos_sale_categories (headquarter_id, name, display_order)
SELECT DISTINCT h.headquarter_id, 'GENERAL', 0
FROM headquarter_items h
WHERE h.deleted_at IS NULL
  AND h.pos_sale_category_id IS NULL;

UPDATE headquarter_items h
SET pos_sale_category_id = c.id
FROM pos_sale_categories c
WHERE c.headquarter_id = h.headquarter_id
  AND c.name = 'GENERAL'
  AND c.deleted_at IS NULL
  AND h.pos_sale_category_id IS NULL;

-- Keep the pre-Fase-2 API working while it still writes sale_category text.
CREATE OR REPLACE FUNCTION sync_headquarter_item_pos_sale_category()
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

ALTER TABLE headquarter_items
    ALTER COLUMN pos_sale_category_id SET NOT NULL,
    ADD CONSTRAINT fk_headquarter_items_pos_sale_category
        FOREIGN KEY (pos_sale_category_id) REFERENCES pos_sale_categories (id);

CREATE INDEX idx_headquarter_items_pos_sale_category_id
    ON headquarter_items (pos_sale_category_id);

-- Keep the text column during the API migration. It is synchronized by the new write path
-- until the compatibility column is removed in a later migration.
COMMENT ON COLUMN headquarter_items.sale_category IS 'Deprecated compatibility value; use pos_sale_category_id.';

CREATE SEQUENCE inventory_item_sku_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1;

SELECT setval(
    'inventory_item_sku_seq',
    COALESCE((
        SELECT MAX(CAST(SUBSTRING(UPPER(sku) FROM '^CAF-([0-9]+)$') AS BIGINT))
        FROM inventory_items
        WHERE UPPER(sku) ~ '^CAF-[0-9]+$'
    ), 1),
    EXISTS (
        SELECT 1
        FROM inventory_items
        WHERE UPPER(sku) ~ '^CAF-[0-9]+$'
    )
);

CREATE OR REPLACE FUNCTION next_internal_item_sku()
RETURNS VARCHAR(64)
LANGUAGE sql
AS $$
    SELECT 'CAF-' || LPAD(nextval('inventory_item_sku_seq')::TEXT, 6, '0');
$$;

COMMENT ON FUNCTION next_internal_item_sku() IS 'Returns the next backend-generated internal SKU.';

COMMENT ON SEQUENCE inventory_item_sku_seq IS 'Sequence for backend-generated internal CAF SKUs.';

CREATE TABLE inventory_item_identifiers (
    id              BIGSERIAL PRIMARY KEY,
    item_id         BIGINT      NOT NULL REFERENCES inventory_items (id),
    value           VARCHAR(64) NOT NULL,
    value_normalized VARCHAR(64) NOT NULL,
    type            VARCHAR(32) NOT NULL,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    version         BIGINT      NOT NULL DEFAULT 1,
    CONSTRAINT ck_inventory_item_identifiers_type
        CHECK (type IN ('SKU', 'SUPPLIER_BARCODE'))
);

CREATE UNIQUE INDEX uk_inventory_item_identifiers_value_active
    ON inventory_item_identifiers (value_normalized)
    WHERE deleted_at IS NULL AND active = TRUE;

CREATE UNIQUE INDEX uk_inventory_item_identifiers_item_type_active
    ON inventory_item_identifiers (item_id, type)
    WHERE deleted_at IS NULL AND active = TRUE;

CREATE INDEX idx_inventory_item_identifiers_item_id
    ON inventory_item_identifiers (item_id);

CREATE INDEX idx_inventory_item_identifiers_value_normalized
    ON inventory_item_identifiers (value_normalized);

-- Import the existing SKU namespace. Legacy barcodes equal to the SKU are deliberately
-- represented once; the old barcode column remains available during API migration.
INSERT INTO inventory_item_identifiers (item_id, value, value_normalized, type)
SELECT id, sku, UPPER(BTRIM(sku)), 'SKU'
FROM inventory_items
WHERE deleted_at IS NULL
  AND BTRIM(sku) <> '';

INSERT INTO inventory_item_identifiers (item_id, value, value_normalized, type)
SELECT i.id, i.barcode, UPPER(BTRIM(i.barcode)), 'SUPPLIER_BARCODE'
FROM inventory_items i
WHERE i.deleted_at IS NULL
  AND i.barcode IS NOT NULL
  AND BTRIM(i.barcode) <> ''
  AND UPPER(BTRIM(i.barcode)) <> UPPER(BTRIM(i.sku));

COMMENT ON TABLE inventory_item_identifiers IS 'Unique internal SKU and supplier barcode identifiers for inventory items.';
