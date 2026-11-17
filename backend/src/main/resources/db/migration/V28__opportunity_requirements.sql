-- Convocatorias F3: checklist requirements per opportunity.

CREATE TABLE opportunity_requirements (
    id               BIGSERIAL PRIMARY KEY,
    opportunity_id   BIGINT        NOT NULL REFERENCES opportunities (id),
    title            VARCHAR(300)  NOT NULL,
    category         VARCHAR(40)   NOT NULL,
    mandatory        BOOLEAN       NOT NULL DEFAULT TRUE,
    status           VARCHAR(40)   NOT NULL,
    due_on           DATE,
    notes            VARCHAR(2000),
    sort_order       INT           NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMPTZ,
    version          BIGINT        NOT NULL DEFAULT 1,
    CONSTRAINT ck_opportunity_requirements_category CHECK (
        category IN (
            'ADMINISTRATIVE',
            'LEGAL',
            'TECHNICAL',
            'ECONOMIC',
            'OTHER',
            'UNDEFINED'
        )
    ),
    CONSTRAINT ck_opportunity_requirements_status CHECK (
        status IN (
            'PENDING',
            'IN_PROGRESS',
            'DONE',
            'BLOCKED',
            'WAIVED',
            'UNDEFINED'
        )
    )
);

CREATE INDEX idx_opportunity_requirements_opportunity_id
    ON opportunity_requirements (opportunity_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunity_requirements_deleted_at
    ON opportunity_requirements (deleted_at);

COMMENT ON TABLE opportunity_requirements IS 'Checklist items for a convocatoria; mandatory items gate markSubmitted (D14).';
