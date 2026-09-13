-- Pimienta Alimentos — payroll periods, records, adjustments, payments, and debts

CREATE TABLE payroll_periods (
    id         BIGSERIAL PRIMARY KEY,
    frequency  VARCHAR(32) NOT NULL,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    status     VARCHAR(32) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    version    BIGINT      NOT NULL DEFAULT 1,
    CONSTRAINT ck_payroll_periods_frequency
        CHECK (frequency IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'CUSTOM'))
);

CREATE INDEX idx_payroll_periods_dates ON payroll_periods (start_date, end_date);
CREATE INDEX idx_payroll_periods_status ON payroll_periods (status);
CREATE INDEX idx_payroll_periods_deleted_at ON payroll_periods (deleted_at);

COMMENT ON TABLE payroll_periods IS 'Payroll calculation windows (weekly, bi-weekly, etc.).';

CREATE TABLE payroll_records (
    id                BIGSERIAL PRIMARY KEY,
    employee_id       BIGINT         NOT NULL REFERENCES employees (id),
    period_id         BIGINT REFERENCES payroll_periods (id) ON DELETE SET NULL,
    worked_days_start DATE           NOT NULL,
    worked_days_end   DATE           NOT NULL,
    gross_amount      NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_discounts   NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_bonuses     NUMERIC(19, 4) NOT NULL DEFAULT 0,
    net_amount        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status            VARCHAR(32)    NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    version           BIGINT         NOT NULL DEFAULT 1,
    CONSTRAINT ck_payroll_records_status
        CHECK (status IN ('PENDING', 'PAID', 'PARTIAL', 'DEFERRED'))
);

CREATE INDEX idx_payroll_records_employee_id ON payroll_records (employee_id);
CREATE INDEX idx_payroll_records_period_id ON payroll_records (period_id);
CREATE INDEX idx_payroll_records_status ON payroll_records (status);
CREATE INDEX idx_payroll_records_deleted_at ON payroll_records (deleted_at);

COMMENT ON TABLE payroll_records IS 'Calculated payroll run per employee and period.';

CREATE TABLE payroll_adjustments (
    id                BIGSERIAL PRIMARY KEY,
    payroll_record_id BIGINT         NOT NULL REFERENCES payroll_records (id) ON DELETE CASCADE,
    type              VARCHAR(16)    NOT NULL,
    amount            NUMERIC(19, 4) NOT NULL DEFAULT 0,
    reason            VARCHAR(500)   NOT NULL,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    version           BIGINT         NOT NULL DEFAULT 1,
    CONSTRAINT ck_payroll_adjustments_type
        CHECK (type IN ('DISCOUNT', 'BONUS'))
);

CREATE INDEX idx_payroll_adjustments_payroll_record_id ON payroll_adjustments (payroll_record_id);
CREATE INDEX idx_payroll_adjustments_deleted_at ON payroll_adjustments (deleted_at);

COMMENT ON TABLE payroll_adjustments IS 'Bonuses and deductions applied to a payroll record.';

CREATE TABLE payroll_payments (
    id                  BIGSERIAL PRIMARY KEY,
    payroll_record_id   BIGINT REFERENCES payroll_records (id) ON DELETE SET NULL,
    employee_id         BIGINT         NOT NULL REFERENCES employees (id),
    frequency           VARCHAR(32)    NOT NULL,
    worked_days_start   DATE           NOT NULL,
    worked_days_end     DATE           NOT NULL,
    gross_amount        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    net_amount          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    destination_account VARCHAR(128)   NOT NULL,
    transaction_id      VARCHAR(128)   NOT NULL,
    status              VARCHAR(32)    NOT NULL,
    pending_amount      NUMERIC(19, 4) NOT NULL DEFAULT 0,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP,
    version             BIGINT         NOT NULL DEFAULT 1,
    CONSTRAINT ck_payroll_payments_frequency
        CHECK (frequency IN ('WEEKLY', 'BIWEEKLY', 'MONTHLY', 'CUSTOM')),
    CONSTRAINT ck_payroll_payments_status
        CHECK (status IN ('PENDING', 'PAID', 'PARTIAL', 'DEFERRED'))
);

CREATE INDEX idx_payroll_payments_payroll_record_id ON payroll_payments (payroll_record_id);
CREATE INDEX idx_payroll_payments_employee_id ON payroll_payments (employee_id);
CREATE INDEX idx_payroll_payments_status ON payroll_payments (status);
CREATE INDEX idx_payroll_payments_deleted_at ON payroll_payments (deleted_at);

COMMENT ON TABLE payroll_payments IS 'Bank disbursements linked to payroll records.';

CREATE TABLE payroll_debts (
    id                BIGSERIAL PRIMARY KEY,
    employee_id       BIGINT         NOT NULL REFERENCES employees (id),
    payroll_record_id BIGINT REFERENCES payroll_records (id) ON DELETE SET NULL,
    amount_owed       NUMERIC(19, 4) NOT NULL DEFAULT 0,
    reason            VARCHAR(500),
    settled           BOOLEAN        NOT NULL DEFAULT FALSE,
    settled_at        TIMESTAMP,
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP,
    version           BIGINT         NOT NULL DEFAULT 1
);

CREATE INDEX idx_payroll_debts_employee_id ON payroll_debts (employee_id);
CREATE INDEX idx_payroll_debts_settled ON payroll_debts (settled);
CREATE INDEX idx_payroll_debts_deleted_at ON payroll_debts (deleted_at);

COMMENT ON TABLE payroll_debts IS 'Outstanding amounts owed by employees to the company.';
