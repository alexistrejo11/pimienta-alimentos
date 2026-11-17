-- Convocatorias F1: organizations + opportunities (lifecycle). Requirements/proposals later.

CREATE TABLE organizations (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(200)  NOT NULL,
    kind           VARCHAR(40)   NOT NULL,
    notes          VARCHAR(2000),
    contact_name   VARCHAR(160),
    contact_email  VARCHAR(160),
    contact_phone  VARCHAR(40),
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ,
    version        BIGINT        NOT NULL DEFAULT 1,
    CONSTRAINT ck_organizations_kind CHECK (
        kind IN ('PUBLIC_INSTITUTION', 'PRIVATE_COMPANY', 'OTHER', 'UNDEFINED')
    )
);

CREATE INDEX idx_organizations_deleted_at ON organizations (deleted_at);
CREATE INDEX idx_organizations_name ON organizations (name);

COMMENT ON TABLE organizations IS 'Institutions / counterparties for convocatorias (opportunity module).';

CREATE TABLE opportunities (
    id                    BIGSERIAL PRIMARY KEY,
    organization_id       BIGINT        NOT NULL REFERENCES organizations (id),
    title                 VARCHAR(240)  NOT NULL,
    status                VARCHAR(40)   NOT NULL,
    source_url            VARCHAR(500),
    external_reference    VARCHAR(120),
    published_on          DATE,
    submission_deadline   DATE,
    decision_on           DATE,
    notes                 VARCHAR(4000),
    contract_id           BIGINT        REFERENCES business_contracts (id),
    submitted_at          TIMESTAMPTZ,
    closed_at             TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMPTZ,
    version               BIGINT        NOT NULL DEFAULT 1,
    CONSTRAINT ck_opportunities_status CHECK (
        status IN (
            'DRAFT',
            'UNDER_REVIEW',
            'PREPARING',
            'SUBMITTED',
            'WON',
            'LOST',
            'ABANDONED',
            'CANCELLED',
            'UNDEFINED'
        )
    )
);

CREATE INDEX idx_opportunities_deleted_at ON opportunities (deleted_at);
CREATE INDEX idx_opportunities_organization_id ON opportunities (organization_id);
CREATE INDEX idx_opportunities_status ON opportunities (status);
CREATE INDEX idx_opportunities_submission_deadline ON opportunities (submission_deadline);

COMMENT ON TABLE opportunities IS 'Convocatorias / institutional opportunities with fixed lifecycle statuses.';
COMMENT ON COLUMN opportunities.contract_id IS 'Set when won (F5 bridge); nullable in F1.';
