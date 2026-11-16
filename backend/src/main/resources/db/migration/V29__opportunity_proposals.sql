-- Convocatorias F4: proposal packages (technical / economic / combined).

CREATE TABLE opportunity_proposals (
    id               BIGSERIAL PRIMARY KEY,
    opportunity_id   BIGINT        NOT NULL REFERENCES opportunities (id),
    kind             VARCHAR(40)   NOT NULL,
    notes            VARCHAR(4000),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at       TIMESTAMPTZ,
    version          BIGINT        NOT NULL DEFAULT 1,
    CONSTRAINT ck_opportunity_proposals_kind CHECK (
        kind IN ('TECHNICAL', 'ECONOMIC', 'COMBINED', 'UNDEFINED')
    )
);

CREATE INDEX idx_opportunity_proposals_opportunity_id
    ON opportunity_proposals (opportunity_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunity_proposals_deleted_at
    ON opportunity_proposals (deleted_at);

COMMENT ON TABLE opportunity_proposals IS 'Proposal packages attached to a convocatoria; files use entityType=proposal.';
