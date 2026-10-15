-- Operating expenses (gastos operativos). Receipt object keys live on the row as jsonb.
-- Payroll is a separate module and is not stored here.

CREATE TABLE operating_expenses (
    id                   BIGSERIAL PRIMARY KEY,
    headquarter_id       BIGINT         NOT NULL REFERENCES headquarters (id),
    amount               NUMERIC(14, 2) NOT NULL,
    category             VARCHAR(40)    NOT NULL,
    concept              VARCHAR(200)   NOT NULL,
    notes                VARCHAR(500),
    occurred_on          DATE           NOT NULL,
    recorded_by_user_id  BIGINT         NOT NULL REFERENCES account_users (id),
    attachment_keys      JSONB          NOT NULL DEFAULT '[]'::jsonb,
    created_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at           TIMESTAMP,
    version              BIGINT         NOT NULL DEFAULT 1,
    CONSTRAINT ck_operating_expenses_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_operating_expenses_category CHECK (
        category IN (
            'SUPPLIERS_MERCHANDISE',
            'RENT',
            'UTILITIES',
            'MAINTENANCE',
            'SUPPLIES_PETTY_CASH',
            'OTHER'
        )
    ),
    CONSTRAINT ck_operating_expenses_attachment_count CHECK (
        jsonb_typeof(attachment_keys) = 'array' AND jsonb_array_length(attachment_keys) <= 5
    )
);

CREATE INDEX idx_operating_expenses_hq_occurred_on ON operating_expenses (headquarter_id, occurred_on)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_operating_expenses_deleted_at ON operating_expenses (deleted_at);

COMMENT ON TABLE operating_expenses IS 'Operating expenses recorded against one headquarter. Excludes payroll.';
COMMENT ON COLUMN operating_expenses.category IS 'Operating cost category. Not used for payroll.';
COMMENT ON COLUMN operating_expenses.attachment_keys IS 'Private S3 object keys for receipt images, stored as a JSON array of at most 5 strings.';
COMMENT ON COLUMN operating_expenses.occurred_on IS 'Business day of the operating expense (America/Mexico_City).';
