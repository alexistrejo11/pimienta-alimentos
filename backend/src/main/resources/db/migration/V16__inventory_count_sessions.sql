CREATE TABLE inventory_count_sessions (
    id BIGSERIAL PRIMARY KEY,
    location_id BIGINT NOT NULL REFERENCES storage_locations(id),
    count_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by_id BIGINT NOT NULL,
    submitted_by_id BIGINT,
    approved_by_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    approved_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    CONSTRAINT ck_inventory_count_type CHECK (count_type IN ('FULL', 'PARTIAL')),
    CONSTRAINT ck_inventory_count_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'CANCELLED'))
);
CREATE TABLE inventory_count_responses (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES inventory_count_sessions(id),
    item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    expected_quantity INTEGER NOT NULL,
    counted_quantity INTEGER,
    variance INTEGER,
    counted_at TIMESTAMP,
    CONSTRAINT uk_inventory_count_response_item UNIQUE (session_id, item_id),
    CONSTRAINT ck_inventory_count_response_quantities CHECK (expected_quantity >= 0 AND (counted_quantity IS NULL OR counted_quantity >= 0))
);
CREATE INDEX idx_inventory_count_sessions_location ON inventory_count_sessions(location_id);
CREATE INDEX idx_inventory_count_sessions_status ON inventory_count_sessions(status);
CREATE UNIQUE INDEX uk_inventory_count_active_location ON inventory_count_sessions(location_id) WHERE status IN ('DRAFT', 'SUBMITTED');
