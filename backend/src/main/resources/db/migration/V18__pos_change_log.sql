-- Durable, HQ-scoped downstream POS change feed.

CREATE TABLE pos_change_log (
    change_sequence    BIGSERIAL PRIMARY KEY,
    headquarter_id     BIGINT       NOT NULL REFERENCES headquarters (id),
    entity_type        VARCHAR(32)  NOT NULL,
    entity_id          VARCHAR(128) NOT NULL,
    operation          VARCHAR(16)  NOT NULL,
    projection_payload JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pos_change_log_entity_type
        CHECK (entity_type IN ('CATALOG_ITEM', 'INVENTORY_STOCK', 'OPERATOR', 'POLICY')),
    CONSTRAINT ck_pos_change_log_operation
        CHECK (operation IN ('UPSERT', 'DEACTIVATE'))
);

CREATE INDEX idx_pos_change_log_hq_sequence
    ON pos_change_log (headquarter_id, change_sequence);

COMMENT ON TABLE pos_change_log IS
    'Append-only durable downstream POS change feed. change_sequence is the cursor value.';
