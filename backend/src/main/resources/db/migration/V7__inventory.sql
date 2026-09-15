-- Pimienta Alimentos — inventory catalog, locations, stock, transactions, and movements

CREATE TABLE inventory_items (
    id               BIGSERIAL PRIMARY KEY,
    sku              VARCHAR(64)  NOT NULL,
    name             VARCHAR(300) NOT NULL,
    description      VARCHAR(4000),
    category         VARCHAR(32)  NOT NULL,
    unit             VARCHAR(32)  NOT NULL,
    brand            VARCHAR(120),
    barcode          VARCHAR(64),
    cost_price       NUMERIC(19, 6) NOT NULL DEFAULT 0,
    reorder_point    INTEGER      NOT NULL DEFAULT 0,
    reorder_quantity INTEGER      NOT NULL DEFAULT 0,
    status           VARCHAR(32)  NOT NULL,
    catalog_role     VARCHAR(32)  NOT NULL DEFAULT 'INVENTORY_ONLY',
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP,
    version          BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT uk_inventory_items_sku UNIQUE (sku),
    CONSTRAINT ck_inventory_items_category
        CHECK (category IN (
            'RAW_MATERIAL', 'FINISHED_GOOD', 'CONSUMABLE', 'SPARE_PART', 'PACKAGING',
            'TOOL', 'MACHINE', 'FURNITURE', 'OTHER'
        )),
    CONSTRAINT ck_inventory_items_unit
        CHECK (unit IN ('PIECE', 'KG', 'GRAM', 'LITER', 'ML', 'BOX', 'DOZEN', 'METER', 'SQUARE_METER')),
    CONSTRAINT ck_inventory_items_status
        CHECK (status IN ('ACTIVE', 'DISCONTINUED', 'OUT_OF_STOCK', 'PENDING_APPROVAL')),
    CONSTRAINT ck_inventory_items_catalog_role
        CHECK (catalog_role IN ('INVENTORY_ONLY', 'POS_SELLABLE'))
);

CREATE UNIQUE INDEX uk_inventory_items_barcode_not_null
    ON inventory_items (barcode)
    WHERE barcode IS NOT NULL;

CREATE INDEX idx_inventory_items_status ON inventory_items (status);
CREATE INDEX idx_inventory_items_deleted_at ON inventory_items (deleted_at);
CREATE INDEX idx_inventory_items_catalog_role ON inventory_items (catalog_role);

COMMENT ON TABLE inventory_items IS 'Catalog of stock-keeping units (SKU) and cost metadata.';

CREATE SEQUENCE inventory_item_sku_seq
    AS BIGINT
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1;

CREATE FUNCTION next_internal_item_sku()
RETURNS VARCHAR(64)
LANGUAGE sql
AS $$
    SELECT 'CAF-' || LPAD(nextval('inventory_item_sku_seq')::TEXT, 6, '0');
$$;

COMMENT ON FUNCTION next_internal_item_sku() IS 'Returns the next backend-generated internal SKU.';
COMMENT ON SEQUENCE inventory_item_sku_seq IS 'Sequence for backend-generated internal CAF SKUs.';

CREATE TABLE inventory_item_identifiers (
    id               BIGSERIAL PRIMARY KEY,
    item_id          BIGINT      NOT NULL REFERENCES inventory_items (id),
    value            VARCHAR(64) NOT NULL,
    value_normalized VARCHAR(64) NOT NULL,
    type             VARCHAR(32) NOT NULL,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMP,
    version          BIGINT      NOT NULL DEFAULT 1,
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

COMMENT ON TABLE inventory_item_identifiers IS 'Unique internal SKU and supplier barcode identifiers for inventory items.';

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
            'RETURN_FROM_CLIENT', 'RETURN_TO_SUPPLIER', 'PRODUCTION_ISSUE', 'SCRAP_WRITE_OFF'
        )),
    CONSTRAINT ck_inventory_transactions_status
        CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_inventory_transactions_status ON inventory_transactions (status);
CREATE INDEX idx_inventory_transactions_type ON inventory_transactions (type);
CREATE INDEX idx_inventory_transactions_deleted_at ON inventory_transactions (deleted_at);

COMMENT ON TABLE inventory_transactions IS 'Grouped inventory operations (receipts, transfers, adjustments).';

CREATE TABLE inventory_movements (
    id                      BIGSERIAL PRIMARY KEY,
    item_id                 BIGINT         NOT NULL REFERENCES inventory_items (id),
    source_location_id      BIGINT REFERENCES storage_locations (id) ON DELETE SET NULL,
    destination_location_id BIGINT REFERENCES storage_locations (id) ON DELETE SET NULL,
    transaction_id          BIGINT REFERENCES inventory_transactions (id) ON DELETE SET NULL,
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
        CHECK (direction IN ('IN', 'OUT', 'NEUTRAL'))
);

CREATE INDEX idx_inventory_movements_item_id ON inventory_movements (item_id);
CREATE INDEX idx_inventory_movements_transaction_id ON inventory_movements (transaction_id);
CREATE INDEX idx_inventory_movements_reference_number ON inventory_movements (reference_number);
CREATE INDEX idx_inventory_movements_source_location_id ON inventory_movements (source_location_id);
CREATE INDEX idx_inventory_movements_destination_location_id ON inventory_movements (destination_location_id);
CREATE INDEX idx_inventory_movements_created_at ON inventory_movements (created_at);

COMMENT ON TABLE inventory_movements IS 'Append-only ledger of quantity changes per item and location.';
