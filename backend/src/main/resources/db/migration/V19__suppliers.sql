-- Supplier directory: a person who delivers one brand, linked to headquarters.

CREATE TABLE suppliers (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    phone         VARCHAR(40)  NOT NULL,
    brand         VARCHAR(120) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP,
    version       BIGINT       NOT NULL DEFAULT 1
);

CREATE INDEX idx_suppliers_deleted_at ON suppliers (deleted_at);
CREATE INDEX idx_suppliers_brand ON suppliers (brand);

CREATE TABLE supplier_headquarters (
    supplier_id     BIGINT  NOT NULL REFERENCES suppliers (id) ON DELETE CASCADE,
    headquarter_id  BIGINT  NOT NULL REFERENCES headquarters (id) ON DELETE CASCADE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (supplier_id, headquarter_id)
);

CREATE INDEX idx_supplier_headquarters_hq ON supplier_headquarters (headquarter_id);

COMMENT ON TABLE suppliers IS 'Person who supplies one brand (name, phone, brand).';
COMMENT ON TABLE supplier_headquarters IS 'Headquarters served by each supplier.';
COMMENT ON COLUMN supplier_headquarters.is_active IS 'Whether this person currently supplies that brand at the headquarter.';
