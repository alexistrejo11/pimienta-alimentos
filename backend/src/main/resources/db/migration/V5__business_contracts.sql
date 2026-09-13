-- Pimienta Alimentos — legal/business agreements

CREATE TABLE business_contracts (
    id                     BIGSERIAL PRIMARY KEY,
    name                   VARCHAR(400),
    description            VARCHAR(4000),
    category               VARCHAR(32),
    employee_id            BIGINT REFERENCES employees (id) ON DELETE SET NULL,
    opportunity_id         BIGINT REFERENCES crm_opportunities (id) ON DELETE SET NULL,
    project_id             BIGINT REFERENCES crm_projects (id) ON DELETE SET NULL,
    term_kind              VARCHAR(32),
    effective_start        DATE,
    effective_end          DATE,
    document_url           VARCHAR(2000),
    terms_and_conditions   VARCHAR(8000),
    reference_code         VARCHAR(80),
    renewal_cycle_months   INTEGER,
    agreed_value           NUMERIC(19, 4),
    currency_code          VARCHAR(3),
    extension_count        INTEGER      NOT NULL DEFAULT 0,
    last_renewed_at        TIMESTAMP,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at             TIMESTAMP,
    version                BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_business_contracts_category
        CHECK (category IS NULL OR category IN (
            'EMPLOYEE', 'SUPPLIER', 'CUSTOMER', 'PARTNER', 'OTHER', 'UNDEFINED'
        )),
    CONSTRAINT ck_business_contracts_term_kind
        CHECK (term_kind IS NULL OR term_kind IN ('FIXED_TERM', 'INDEFINITE', 'UNDEFINED'))
);

CREATE INDEX idx_business_contracts_employee_id ON business_contracts (employee_id);
CREATE INDEX idx_business_contracts_opportunity_id ON business_contracts (opportunity_id);
CREATE INDEX idx_business_contracts_project_id ON business_contracts (project_id);
CREATE INDEX idx_business_contracts_reference_code ON business_contracts (reference_code);
CREATE INDEX idx_business_contracts_deleted_at ON business_contracts (deleted_at);

COMMENT ON TABLE business_contracts IS 'Legal/business agreements tied to employees, opportunities, or projects.';
