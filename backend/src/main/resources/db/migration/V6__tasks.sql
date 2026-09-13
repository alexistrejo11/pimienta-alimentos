-- Pimienta Alimentos — operational tasks

CREATE TABLE tasks (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(300),
    description     VARCHAR(4000),
    status          VARCHAR(32),
    priority        VARCHAR(32),
    assigned_to_id  BIGINT,
    assigned_by_id  BIGINT,
    created_by_id   BIGINT,
    assigned_at     TIMESTAMP,
    completed_at    TIMESTAMP,
    due_date        TIMESTAMP,
    headquarter_id  BIGINT REFERENCES headquarters (id) ON DELETE SET NULL,
    project_id      BIGINT REFERENCES crm_projects (id) ON DELETE SET NULL,
    opportunity_id  BIGINT REFERENCES crm_opportunities (id) ON DELETE SET NULL,
    checklist       JSONB,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_tasks_status
        CHECK (status IS NULL OR status IN (
            'PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'DELAYED', 'ON_HOLD', 'FAILED', 'UNDEFINED'
        )),
    CONSTRAINT ck_tasks_priority
        CHECK (priority IS NULL OR priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT', 'UNDEFINED'))
);

CREATE INDEX idx_tasks_assigned_to_id ON tasks (assigned_to_id);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_project_id ON tasks (project_id);
CREATE INDEX idx_tasks_opportunity_id ON tasks (opportunity_id);
CREATE INDEX idx_tasks_headquarter_id ON tasks (headquarter_id);
CREATE INDEX idx_tasks_due_date ON tasks (due_date);
CREATE INDEX idx_tasks_deleted_at ON tasks (deleted_at);

COMMENT ON TABLE tasks IS 'Operational tasks optionally linked to projects, opportunities, or headquarters.';
