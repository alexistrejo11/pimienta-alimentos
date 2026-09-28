-- Pimienta Alimentos — warehouse items, sale products, locations, stock, and movements

CREATE TABLE inventory_items (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(300) NOT NULL,
    description      VARCHAR(4000),
    category         VARCHAR(32)  NOT NULL,
    unit             VARCHAR(32)  NOT NULL,
    brand            VARCHAR(120),
    cost_price       NUMERIC(19, 6) NOT NULL DEFAULT 0,
    reorder_point    INTEGER      NOT NULL DEFAULT 0,
    reorder_quantity INTEGER      NOT NULL DEFAULT 0,
    status           VARCHAR(32)  NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP,
    version          BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_inventory_items_category
        CHECK (category IN (
            'RAW_MATERIAL', 'FINISHED_GOOD', 'CONSUMABLE', 'SPARE_PART', 'PACKAGING',
            'TOOL', 'MACHINE', 'FURNITURE', 'OTHER'
        )),
    CONSTRAINT ck_inventory_items_unit
        CHECK (unit IN ('PIECE', 'KG', 'GRAM', 'LITER', 'ML', 'BOX', 'DOZEN', 'METER', 'SQUARE_METER')),
    CONSTRAINT ck_inventory_items_status
        CHECK (status IN ('ACTIVE', 'DISCONTINUED', 'OUT_OF_STOCK', 'PENDING_APPROVAL'))
);

CREATE INDEX idx_inventory_items_status ON inventory_items (status);
CREATE INDEX idx_inventory_items_deleted_at ON inventory_items (deleted_at);

COMMENT ON TABLE inventory_items IS 'Warehouse things that can be counted: supplies, furniture, and stock behind a sale product.';

CREATE SEQUENCE product_sku_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1;

CREATE FUNCTION next_internal_item_sku()
RETURNS VARCHAR(64)
LANGUAGE sql
AS $$
    SELECT 'CAF-' || LPAD(nextval('product_sku_seq')::TEXT, 6, '0');
$$;

COMMENT ON FUNCTION next_internal_item_sku() IS 'Returns the next sale-product SKU. Assigned by trg_products_assign_sku.';
COMMENT ON SEQUENCE product_sku_seq IS 'Sequence for sale-product CAF SKUs.';

CREATE TABLE products (
    id                BIGSERIAL PRIMARY KEY,
    sku               VARCHAR(64)  NOT NULL,
    name              VARCHAR(300) NOT NULL,
    description       VARCHAR(4000),
    unit              VARCHAR(32)  NOT NULL,
    barcode           VARCHAR(64),
    status            VARCHAR(32)  NOT NULL,
    inventory_item_id BIGINT REFERENCES inventory_items (id),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    version           BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_unit
        CHECK (unit IN ('PIECE', 'KG', 'GRAM', 'LITER', 'ML', 'BOX', 'DOZEN', 'METER', 'SQUARE_METER')),
    CONSTRAINT ck_products_status
        CHECK (status IN ('ACTIVE', 'DISCONTINUED'))
);

CREATE UNIQUE INDEX uk_products_barcode_active
    ON products (barcode)
    WHERE barcode IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX uk_products_inventory_item_active
    ON products (inventory_item_id)
    WHERE inventory_item_id IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_deleted_at ON products (deleted_at);
CREATE INDEX idx_products_inventory_item_id ON products (inventory_item_id);

COMMENT ON TABLE products IS 'Sale catalog. SKU is required and filled by trigger. inventory_item_id is set only when the product is stocked.';
COMMENT ON COLUMN products.inventory_item_id IS 'Stock identity for a controlled product. Null for dishes prepared to order.';

CREATE FUNCTION assign_product_sku()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.sku IS NULL OR BTRIM(NEW.sku) = '' THEN
        NEW.sku := next_internal_item_sku();
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_products_assign_sku
    BEFORE INSERT ON products
    FOR EACH ROW
    EXECUTE FUNCTION assign_product_sku();

CREATE TABLE storage_locations (
    id                BIGSERIAL PRIMARY KEY,
    code              VARCHAR(64)  NOT NULL,
    name              VARCHAR(200) NOT NULL,
    description       VARCHAR(2000),
    type              VARCHAR(32)  NOT NULL,
    parent_id         BIGINT REFERENCES storage_locations (id) ON DELETE SET NULL,
    headquarter_id    BIGINT REFERENCES headquarters (id),
    max_capacity      INTEGER      NOT NULL DEFAULT 0,
    occupied_capacity INTEGER      NOT NULL DEFAULT 0,
    status            VARCHAR(32)  NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    version           BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_storage_locations_type
        CHECK (type IN ('WAREHOUSE', 'ZONE', 'AISLE', 'SHELF', 'BIN', 'POS')),
    CONSTRAINT ck_storage_locations_status
        CHECK (status IN ('ACTIVE', 'FULL', 'BLOCKED', 'INACTIVE')),
    CONSTRAINT ck_storage_locations_pos_headquarter
        CHECK (type <> 'POS' OR headquarter_id IS NOT NULL)
);

CREATE INDEX idx_storage_locations_code ON storage_locations (code);
CREATE INDEX idx_storage_locations_parent_id ON storage_locations (parent_id);
CREATE INDEX idx_storage_locations_headquarter_id ON storage_locations (headquarter_id);
CREATE INDEX idx_storage_locations_deleted_at ON storage_locations (deleted_at);

CREATE UNIQUE INDEX uk_storage_locations_pos_headquarter_active
    ON storage_locations (headquarter_id)
    WHERE type = 'POS' AND deleted_at IS NULL;

CREATE UNIQUE INDEX uk_storage_locations_pos_code_active
    ON storage_locations (code)
    WHERE type = 'POS' AND deleted_at IS NULL;

COMMENT ON TABLE storage_locations IS 'Warehouses, zones, bins, and per-headquarter POS stock locations.';

CREATE TABLE inventory_stock (
    id                  BIGSERIAL PRIMARY KEY,
    item_id             BIGINT       NOT NULL REFERENCES inventory_items (id),
    location_id         BIGINT       NOT NULL REFERENCES storage_locations (id),
    available_quantity  INTEGER      NOT NULL DEFAULT 0,
    reserved_quantity   INTEGER      NOT NULL DEFAULT 0,
    in_transit_quantity INTEGER      NOT NULL DEFAULT 0,
    status              VARCHAR(32)  NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    version             BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_inventory_stock_status
        CHECK (status IN ('NORMAL', 'LOW_STOCK', 'OUT_OF_STOCK', 'OVERSTOCKED'))
);

CREATE INDEX idx_inventory_stock_item_location ON inventory_stock (item_id, location_id);
CREATE INDEX idx_inventory_stock_item_id ON inventory_stock (item_id);
CREATE INDEX idx_inventory_stock_location_id ON inventory_stock (location_id);
CREATE INDEX idx_inventory_stock_deleted_at ON inventory_stock (deleted_at);

CREATE UNIQUE INDEX uk_inventory_stock_item_location_active
    ON inventory_stock (item_id, location_id)
    WHERE deleted_at IS NULL;

COMMENT ON TABLE inventory_stock IS 'Quantity on hand per item and storage location.';

CREATE TABLE inventory_transactions (
    id                  BIGSERIAL PRIMARY KEY,
    transaction_number  VARCHAR(64)  NOT NULL,
    type                VARCHAR(40)  NOT NULL,
    status              VARCHAR(32)  NOT NULL,
    external_reference  VARCHAR(120),
    notes               VARCHAR(4000),
    exit_reason         VARCHAR(32),
    initiated_by_id     BIGINT,
    approved_by_id      BIGINT,
    approved_at         TIMESTAMP,
    completed_at        TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    version             BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT uk_inventory_transactions_transaction_number UNIQUE (transaction_number),
    CONSTRAINT ck_inventory_transactions_type
        CHECK (type IN (
            'PURCHASE_RECEIPT', 'SALE_DISPATCH', 'INTERNAL_TRANSFER', 'PHYSICAL_COUNT',
            'ADJUSTMENT', 'RETURN_FROM_CLIENT', 'RETURN_TO_SUPPLIER', 'PRODUCTION_ISSUE',
            'SCRAP_WRITE_OFF'
        )),
    CONSTRAINT ck_inventory_transactions_status
        CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_inventory_transactions_exit_reason
        CHECK (
            exit_reason IS NULL
            OR exit_reason IN (
                'SCRAP',
                'DAMAGED',
                'EXPIRED',
                'INTERNAL_USE',
                'INVENTORY_ADJUSTMENT'
            )
        )
);

CREATE INDEX idx_inventory_transactions_status ON inventory_transactions (status);
CREATE INDEX idx_inventory_transactions_type ON inventory_transactions (type);
CREATE INDEX idx_inventory_transactions_deleted_at ON inventory_transactions (deleted_at);

COMMENT ON TABLE inventory_transactions IS 'Grouped inventory operations (receipts, transfers, adjustments).';
COMMENT ON COLUMN inventory_transactions.exit_reason IS
    'Structured exit reason for SCRAP_WRITE_OFF; null for other transaction types.';

CREATE TABLE inventory_movements (
    id                      BIGSERIAL PRIMARY KEY,
    item_id                 BIGINT         NOT NULL REFERENCES inventory_items (id),
    source_location_id      BIGINT,
    destination_location_id BIGINT,
    transaction_id          BIGINT,
    quantity                INTEGER        NOT NULL,
    unit_cost               NUMERIC(19, 6) NOT NULL DEFAULT 0,
    type                    VARCHAR(32)    NOT NULL,
    direction               VARCHAR(32)    NOT NULL,
    description             VARCHAR(2000),
    reference_number        VARCHAR(120),
    performed_by_id         BIGINT,
    stock_after_movement    INTEGER        NOT NULL DEFAULT 0,
    created_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 BIGINT         NOT NULL DEFAULT 1,
    CONSTRAINT ck_inventory_movements_type
        CHECK (type IN (
            'PURCHASE', 'RETURN_FROM_CLIENT', 'INITIAL_STOCK', 'SALE', 'RETURN_TO_SUPPLIER',
            'USAGE', 'SCRAP', 'TRANSFER', 'ADJUSTMENT_PLUS', 'ADJUSTMENT_MINUS'
        )),
    CONSTRAINT ck_inventory_movements_direction
        CHECK (direction IN ('IN', 'OUT', 'NEUTRAL')),
    CONSTRAINT fk_inventory_movements_source_location
        FOREIGN KEY (source_location_id) REFERENCES storage_locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movements_destination_location
        FOREIGN KEY (destination_location_id) REFERENCES storage_locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_inventory_movements_transaction
        FOREIGN KEY (transaction_id) REFERENCES inventory_transactions (id) ON DELETE RESTRICT
);

CREATE INDEX idx_inventory_movements_item_id ON inventory_movements (item_id);
CREATE INDEX idx_inventory_movements_transaction_id ON inventory_movements (transaction_id);
CREATE INDEX idx_inventory_movements_reference_number ON inventory_movements (reference_number);
CREATE INDEX idx_inventory_movements_source_location_id ON inventory_movements (source_location_id);
CREATE INDEX idx_inventory_movements_destination_location_id ON inventory_movements (destination_location_id);
CREATE INDEX idx_inventory_movements_created_at ON inventory_movements (created_at);

COMMENT ON TABLE inventory_movements IS 'Append-only ledger of quantity changes per item and location.';

-- Historical records must never be detached or removed by deleting their parents.
CREATE OR REPLACE FUNCTION prevent_inventory_movement_delete()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'inventory_movements are append-only';
END;
$$;

CREATE TRIGGER trg_prevent_inventory_movement_delete
    BEFORE DELETE ON inventory_movements
    FOR EACH ROW EXECUTE FUNCTION prevent_inventory_movement_delete();
