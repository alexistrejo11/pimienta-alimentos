-- Pimienta Alimentos — CRM clients, opportunities, projects, and milestones

CREATE TABLE crm_clients (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(500) NOT NULL,
    company_name  VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP,
    version       BIGINT       NOT NULL DEFAULT 1
);

CREATE INDEX idx_crm_clients_deleted_at ON crm_clients (deleted_at);
CREATE INDEX idx_crm_clients_name ON crm_clients (name);

COMMENT ON TABLE crm_clients IS 'CRM client accounts linked to delivery projects.';

CREATE TABLE crm_opportunities (
    id                    BIGSERIAL PRIMARY KEY,
    contact_name          VARCHAR(255),
    contact_email         VARCHAR(320),
    contact_phone         VARCHAR(64),
    company_name          VARCHAR(255),
    company_location      VARCHAR(255),
    industry              VARCHAR(255),
    title                 VARCHAR(500),
    description           VARCHAR(4000),
    estimated_value       NUMERIC(19, 4),
    probability_percent   INTEGER      NOT NULL DEFAULT 0,
    source                VARCHAR(32)  NOT NULL,
    status                VARCHAR(32)  NOT NULL,
    expected_close_date   DATE,
    actual_close_date     DATE,
    assigned_salesman_id  BIGINT,
    lost_reason           VARCHAR(2000),
    converted_project_id  BIGINT,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMP,
    version               BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_crm_opportunities_source
        CHECK (source IN (
            'INBOUND', 'OUTBOUND', 'REFERRAL', 'SOCIAL_MEDIA', 'EVENT', 'COLD_CALL', 'OTHER'
        )),
    CONSTRAINT ck_crm_opportunities_status
        CHECK (status IN (
            'NEW', 'DISCOVERY', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST', 'ABANDONED'
        ))
);

CREATE INDEX idx_crm_opportunities_status ON crm_opportunities (status);
CREATE INDEX idx_crm_opportunities_assigned_salesman_id ON crm_opportunities (assigned_salesman_id);
CREATE INDEX idx_crm_opportunities_expected_close_date ON crm_opportunities (expected_close_date);
CREATE INDEX idx_crm_opportunities_deleted_at ON crm_opportunities (deleted_at);

COMMENT ON TABLE crm_opportunities IS 'Sales pipeline opportunities before project conversion.';

CREATE TABLE crm_projects (
    id                    BIGSERIAL PRIMARY KEY,
    client_id             BIGINT REFERENCES crm_clients (id),
    origin_opportunity_id BIGINT REFERENCES crm_opportunities (id) ON DELETE SET NULL,
    project_code          VARCHAR(64)  NOT NULL,
    project_name          VARCHAR(500) NOT NULL,
    description           VARCHAR(4000),
    type                  VARCHAR(32)  NOT NULL,
    status                VARCHAR(32)  NOT NULL,
    priority              VARCHAR(32)  NOT NULL,
    project_manager_id    BIGINT,
    assigned_salesman_id  BIGINT,
    planned_start_date    DATE,
    planned_end_date      DATE,
    actual_start_date     DATE,
    actual_end_date       DATE,
    on_hold_reason        VARCHAR(2000),
    contracted_value      NUMERIC(19, 4),
    estimated_cost        NUMERIC(19, 4),
    actual_cost           NUMERIC(19, 4),
    progress_percent      INTEGER      NOT NULL DEFAULT 0,
    cancellation_reason   VARCHAR(2000),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMP,
    version               BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT uk_crm_projects_project_code UNIQUE (project_code),
    CONSTRAINT ck_crm_projects_type
        CHECK (type IN (
            'CONSULTING', 'SOFTWARE_DEVELOPMENT', 'IMPLEMENTATION', 'MAINTENANCE',
            'TRAINING', 'RESEARCH', 'OTHER'
        )),
    CONSTRAINT ck_crm_projects_status
        CHECK (status IN ('PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED', 'ARCHIVED')),
    CONSTRAINT ck_crm_projects_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_crm_projects_status ON crm_projects (status);
CREATE INDEX idx_crm_projects_client_id ON crm_projects (client_id);
CREATE INDEX idx_crm_projects_origin_opportunity_id ON crm_projects (origin_opportunity_id);
CREATE INDEX idx_crm_projects_deleted_at ON crm_projects (deleted_at);

COMMENT ON TABLE crm_projects IS 'Active or historical client projects linked to opportunities.';

CREATE TABLE crm_project_milestones (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT       NOT NULL REFERENCES crm_projects (id) ON DELETE CASCADE,
    name            VARCHAR(500) NOT NULL,
    description     VARCHAR(4000),
    status          VARCHAR(32)  NOT NULL,
    planned_date    DATE,
    actual_date     DATE,
    billing_amount  NUMERIC(19, 4),
    billed          BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_crm_project_milestones_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'DELAYED', 'CANCELLED'))
);

CREATE INDEX idx_crm_project_milestones_project_sort ON crm_project_milestones (project_id, sort_order);
CREATE INDEX idx_crm_project_milestones_deleted_at ON crm_project_milestones (deleted_at);

COMMENT ON TABLE crm_project_milestones IS 'Ordered billing/delivery milestones within a project.';
