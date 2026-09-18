CREATE TABLE pos_shifts (
    shift_id UUID PRIMARY KEY,
    headquarter_id BIGINT NOT NULL REFERENCES headquarters (id),
    device_id UUID NOT NULL REFERENCES pos_devices (id),
    cashier_operator_id BIGINT REFERENCES pos_operators (id),
    opening_cash_centavos BIGINT NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    status VARCHAR(16) NOT NULL,
    closing_expected_cash_centavos BIGINT,
    closing_counted_cash_centavos BIGINT,
    closing_difference_centavos BIGINT,
    opened_event_id UUID NOT NULL UNIQUE REFERENCES pos_sync_events (event_id),
    closed_event_id UUID UNIQUE REFERENCES pos_sync_events (event_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT ck_pos_shifts_status CHECK (status IN ('OPEN', 'CLOSED'))
);
CREATE INDEX idx_pos_shifts_hq_opened_at ON pos_shifts (headquarter_id, opened_at);
CREATE UNIQUE INDEX ux_pos_shifts_active_device
    ON pos_shifts (device_id)
    WHERE status = 'OPEN';
CREATE UNIQUE INDEX ux_pos_shifts_active_operator
    ON pos_shifts (cashier_operator_id)
    WHERE status = 'OPEN' AND cashier_operator_id IS NOT NULL;

CREATE TABLE pos_cash_movements (
    movement_id UUID PRIMARY KEY,
    shift_id UUID NOT NULL REFERENCES pos_shifts (shift_id),
    headquarter_id BIGINT NOT NULL REFERENCES headquarters (id),
    event_id UUID NOT NULL UNIQUE REFERENCES pos_sync_events (event_id),
    movement_type VARCHAR(16) NOT NULL,
    amount_centavos BIGINT NOT NULL,
    folio VARCHAR(64),
    reason VARCHAR(512),
    authorized_by_user_id BIGINT,
    authorized_by_role VARCHAR(32),
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT ck_pos_cash_movements_type CHECK (movement_type IN ('DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT ck_pos_cash_movements_amount CHECK (amount_centavos > 0)
);
CREATE INDEX idx_pos_cash_movements_shift ON pos_cash_movements (shift_id, occurred_at);

CREATE TABLE pos_cash_counts (
    count_id UUID PRIMARY KEY,
    shift_id UUID NOT NULL REFERENCES pos_shifts (shift_id),
    headquarter_id BIGINT NOT NULL REFERENCES headquarters (id),
    event_id UUID NOT NULL UNIQUE REFERENCES pos_sync_events (event_id),
    total_centavos BIGINT NOT NULL,
    denominations JSONB NOT NULL DEFAULT '{}'::jsonb,
    submitted_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT ck_pos_cash_counts_total CHECK (total_centavos >= 0)
);
CREATE INDEX idx_pos_cash_counts_shift ON pos_cash_counts (shift_id, submitted_at);
