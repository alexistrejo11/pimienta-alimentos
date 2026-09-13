-- POS sync event ledger, incidents, immutable sales snapshots, and tombstones

CREATE TABLE pos_sync_events (
    event_id            UUID         PRIMARY KEY,
    event_type          VARCHAR(64)  NOT NULL,
    schema_version      INTEGER      NOT NULL DEFAULT 1,
    device_id           UUID         NOT NULL REFERENCES pos_devices (id),
    headquarter_id      BIGINT       NOT NULL REFERENCES headquarters (id),
    device_sequence     BIGINT       NOT NULL,
    aggregate_id        UUID,
    shift_id            UUID,
    occurred_at         TIMESTAMP    NOT NULL,
    payload             JSONB        NOT NULL DEFAULT '{}'::jsonb,
    status              VARCHAR(32)  NOT NULL,
    server_received_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    incident_id         UUID,
    message             VARCHAR(512),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    version             BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_pos_sync_events_status
        CHECK (status IN ('ACCEPTED', 'DUPLICATE', 'REQUIRES_REVIEW', 'REJECTED'))
);

CREATE INDEX idx_pos_sync_events_device_sequence
    ON pos_sync_events (device_id, device_sequence);
CREATE INDEX idx_pos_sync_events_headquarter_id ON pos_sync_events (headquarter_id);
CREATE INDEX idx_pos_sync_events_status ON pos_sync_events (status);
CREATE INDEX idx_pos_sync_events_deleted_at ON pos_sync_events (deleted_at);

COMMENT ON TABLE pos_sync_events IS 'Idempotent POS device sync event ledger (unique event_id).';

CREATE TABLE pos_sync_incidents (
    id              UUID         PRIMARY KEY,
    event_id        UUID         NOT NULL REFERENCES pos_sync_events (event_id),
    headquarter_id  BIGINT       NOT NULL REFERENCES headquarters (id),
    reason_code     VARCHAR(64)  NOT NULL,
    detail          VARCHAR(1024),
    accepted_at     TIMESTAMP,
    accepted_by     BIGINT,
    accept_note     VARCHAR(1024),
    accept_label    VARCHAR(128),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 1
);

CREATE INDEX idx_pos_sync_incidents_event_id ON pos_sync_incidents (event_id);
CREATE INDEX idx_pos_sync_incidents_headquarter_id ON pos_sync_incidents (headquarter_id);
CREATE INDEX idx_pos_sync_incidents_accepted_at ON pos_sync_incidents (accepted_at);
CREATE INDEX idx_pos_sync_incidents_deleted_at ON pos_sync_incidents (deleted_at);

COMMENT ON TABLE pos_sync_incidents IS 'POS sync review incidents (accept does not mutate sale payload).';
COMMENT ON COLUMN pos_sync_incidents.accept_label IS 'Staff ADMIN accept label (set on accept; nullable while open).';

ALTER TABLE pos_sync_events
    ADD CONSTRAINT fk_pos_sync_events_incident
        FOREIGN KEY (incident_id) REFERENCES pos_sync_incidents (id);

CREATE TABLE pos_sales (
    sale_id             UUID         PRIMARY KEY,
    event_id            UUID         NOT NULL UNIQUE REFERENCES pos_sync_events (event_id),
    headquarter_id      BIGINT       NOT NULL REFERENCES headquarters (id),
    device_id           UUID         NOT NULL REFERENCES pos_devices (id),
    shift_id            UUID,
    cashier_operator_id BIGINT       REFERENCES pos_operators (id),
    folio               VARCHAR(64)  NOT NULL,
    gross_centavos      BIGINT       NOT NULL DEFAULT 0,
    discount_centavos   BIGINT       NOT NULL DEFAULT 0,
    total_centavos      BIGINT       NOT NULL DEFAULT 0,
    status              VARCHAR(32)  NOT NULL DEFAULT 'CONFIRMED',
    occurred_at         TIMESTAMP    NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    version             BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_pos_sales_status
        CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);

CREATE INDEX idx_pos_sales_headquarter_id ON pos_sales (headquarter_id);
CREATE INDEX idx_pos_sales_device_id ON pos_sales (device_id);
CREATE INDEX idx_pos_sales_occurred_at ON pos_sales (occurred_at);
CREATE INDEX idx_pos_sales_deleted_at ON pos_sales (deleted_at);

COMMENT ON TABLE pos_sales IS 'Immutable POS sale facts (unique sale_id; never overwritten).';

CREATE TABLE pos_sale_lines (
    id                          BIGSERIAL PRIMARY KEY,
    sale_id                     UUID         NOT NULL REFERENCES pos_sales (sale_id),
    line_id                     UUID         NOT NULL,
    product_id                  BIGINT       REFERENCES inventory_items (id),
    product_name                VARCHAR(255) NOT NULL,
    sale_category               VARCHAR(64),
    quantity                    INTEGER      NOT NULL,
    unit                        VARCHAR(32)  NOT NULL DEFAULT 'PIECE',
    unit_price_centavos         BIGINT       NOT NULL DEFAULT 0,
    subtotal_centavos           BIGINT       NOT NULL DEFAULT 0,
    stock_policy                VARCHAR(32)  NOT NULL DEFAULT 'NOT_CONTROLLED',
    sold_with_negative_stock    BOOLEAN      NOT NULL DEFAULT FALSE,
    sold_while_unavailable      BOOLEAN      NOT NULL DEFAULT FALSE,
    raw_barcode                 VARCHAR(128),
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at                  TIMESTAMP,
    version                     BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_pos_sale_lines_stock_policy
        CHECK (stock_policy IN ('CONTROLLED', 'NOT_CONTROLLED')),
    CONSTRAINT uk_pos_sale_lines_sale_line UNIQUE (sale_id, line_id)
);

CREATE INDEX idx_pos_sale_lines_sale_id ON pos_sale_lines (sale_id);
CREATE INDEX idx_pos_sale_lines_product_id ON pos_sale_lines (product_id);

COMMENT ON TABLE pos_sale_lines IS 'Snapshot lines for a POS sale (names/prices frozen at sync time).';

CREATE TABLE pos_sale_payments (
    id                  BIGSERIAL PRIMARY KEY,
    sale_id             UUID         NOT NULL REFERENCES pos_sales (sale_id),
    payment_id          UUID         NOT NULL,
    method              VARCHAR(32)  NOT NULL,
    amount_centavos     BIGINT       NOT NULL DEFAULT 0,
    tendered_centavos   BIGINT,
    change_centavos     BIGINT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    version             BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_pos_sale_payments_method
        CHECK (method IN ('CASH', 'EXTERNAL_CARD_MP', 'CORTESIA')),
    CONSTRAINT uk_pos_sale_payments_sale_payment UNIQUE (sale_id, payment_id)
);

CREATE INDEX idx_pos_sale_payments_sale_id ON pos_sale_payments (sale_id);

COMMENT ON TABLE pos_sale_payments IS 'Snapshot payments for a POS sale.';

CREATE TABLE pos_sync_tombstones (
    id              BIGSERIAL PRIMARY KEY,
    headquarter_id  BIGINT       NOT NULL REFERENCES headquarters (id),
    entity          VARCHAR(32)  NOT NULL,
    entity_id       VARCHAR(64)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pos_sync_tombstones_entity CHECK (entity IN ('product', 'operator'))
);

CREATE INDEX idx_pos_sync_tombstones_hq_created
    ON pos_sync_tombstones (headquarter_id, created_at);

COMMENT ON TABLE pos_sync_tombstones IS
    'POS sync deactivate markers (e.g. operator unassign) for GET /pos/sync/changes.';
