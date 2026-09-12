-- POS B5: tombstones for sync deactivates (operator unassign / removals without join history)

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
