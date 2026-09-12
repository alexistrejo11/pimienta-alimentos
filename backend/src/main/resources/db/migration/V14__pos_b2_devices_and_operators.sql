-- POS B2: devices, operators, enrollment codes

-- ── pos_devices ──────────────────────────────────────────────────────────────

CREATE TABLE pos_devices (
    id                    UUID         PRIMARY KEY,
    headquarter_id        BIGINT       NOT NULL REFERENCES headquarters (id),
    visible_code          VARCHAR(32)  NOT NULL,
    device_name           VARCHAR(255) NOT NULL,
    app_version           VARCHAR(64),
    status                VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    min_app_version       VARCHAR(64)  NOT NULL DEFAULT '1.0.0',
    last_device_sequence  BIGINT,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMP,
    version               BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_pos_devices_status
        CHECK (status IN ('PENDING', 'AUTHORIZED', 'REVOKED'))
);

CREATE INDEX idx_pos_devices_headquarter_id ON pos_devices (headquarter_id);
CREATE INDEX idx_pos_devices_status ON pos_devices (status);
CREATE INDEX idx_pos_devices_deleted_at ON pos_devices (deleted_at);

CREATE UNIQUE INDEX uk_pos_devices_hq_visible_code_active
    ON pos_devices (headquarter_id, visible_code)
    WHERE deleted_at IS NULL AND status <> 'REVOKED';

COMMENT ON TABLE pos_devices IS 'POS tablets enrolled per headquarter (JWT device identity).';

-- ── pos_operators ────────────────────────────────────────────────────────────

CREATE TABLE pos_operators (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES account_users (id),
    display_name  VARCHAR(255) NOT NULL,
    pos_role      VARCHAR(32)  NOT NULL,
    pin_hash      VARCHAR(255) NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP,
    version       BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_pos_operators_role
        CHECK (pos_role IN ('CASHIER', 'MANAGER', 'SUPERADMIN'))
);

CREATE INDEX idx_pos_operators_user_id ON pos_operators (user_id);
CREATE INDEX idx_pos_operators_deleted_at ON pos_operators (deleted_at);
CREATE INDEX idx_pos_operators_active ON pos_operators (active);

COMMENT ON TABLE pos_operators IS 'POS cashiers/managers (PIN-based; optional link to web User).';

-- ── pos_operator_headquarter ─────────────────────────────────────────────────

CREATE TABLE pos_operator_headquarter (
    operator_id     BIGINT NOT NULL REFERENCES pos_operators (id),
    headquarter_id  BIGINT NOT NULL REFERENCES headquarters (id),
    PRIMARY KEY (operator_id, headquarter_id)
);

CREATE INDEX idx_pos_operator_headquarter_hq ON pos_operator_headquarter (headquarter_id);

COMMENT ON TABLE pos_operator_headquarter IS 'N:N authorization of POS operators to headquarters.';

-- ── pos_enrollment_codes ─────────────────────────────────────────────────────

CREATE TABLE pos_enrollment_codes (
    id                     BIGSERIAL PRIMARY KEY,
    code                   VARCHAR(64) NOT NULL,
    headquarter_id         BIGINT      NOT NULL REFERENCES headquarters (id),
    expires_at             TIMESTAMP   NOT NULL,
    consumed_at            TIMESTAMP,
    consumed_by_device_id  UUID REFERENCES pos_devices (id),
    created_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at             TIMESTAMP,
    version                BIGINT      NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX uk_pos_enrollment_codes_code
    ON pos_enrollment_codes (code);

CREATE INDEX idx_pos_enrollment_codes_headquarter_id ON pos_enrollment_codes (headquarter_id);
CREATE INDEX idx_pos_enrollment_codes_expires_at ON pos_enrollment_codes (expires_at);

COMMENT ON TABLE pos_enrollment_codes IS 'One-time POS device enrollment codes (TTL 10 minutes).';
