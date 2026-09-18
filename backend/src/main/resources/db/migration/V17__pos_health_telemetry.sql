-- Durable POS health snapshots reported by POST /pos/telemetry/events

CREATE TABLE pos_health_snapshots (
    id                         UUID         PRIMARY KEY,
    device_id                  UUID         NOT NULL REFERENCES pos_devices (id),
    headquarter_id             BIGINT       NOT NULL REFERENCES headquarters (id),
    sync_state                 VARCHAR(32)  NOT NULL,
    pending_events             INTEGER      NOT NULL,
    oldest_pending_age_seconds BIGINT       NOT NULL,
    app_version                VARCHAR(64)  NOT NULL,
    received_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_pos_health_snapshots_pending_events CHECK (pending_events >= 0),
    CONSTRAINT ck_pos_health_snapshots_oldest_pending_age CHECK (oldest_pending_age_seconds >= 0)
);

CREATE INDEX idx_pos_health_snapshots_device_received
    ON pos_health_snapshots (device_id, received_at DESC, id DESC);
CREATE INDEX idx_pos_health_snapshots_hq_received
    ON pos_health_snapshots (headquarter_id, received_at DESC, id DESC);

COMMENT ON TABLE pos_health_snapshots IS 'Append-only POS health reports for operational observability.';
