-- Pimienta Alimentos — polymorphic comments on business entities

CREATE TABLE entity_comments (
    id          BIGSERIAL PRIMARY KEY,
    target_type VARCHAR(32)  NOT NULL,
    target_id   BIGINT       NOT NULL,
    author_id   BIGINT       NOT NULL REFERENCES account_users (id),
    body        VARCHAR(4000) NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP,
    version     BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_entity_comments_target_type
        CHECK (target_type IN (
            'TASK', 'OPPORTUNITY', 'PROJECT', 'PROJECT_MILESTONE', 'CONTRACT', 'EMPLOYEE',
            'HEADQUARTER', 'INVENTORY_ITEM', 'INVENTORY_TRANSACTION', 'PAYROLL_RECORD'
        ))
);

CREATE INDEX idx_entity_comments_target ON entity_comments (target_type, target_id);
CREATE INDEX idx_entity_comments_author_id ON entity_comments (author_id);
CREATE INDEX idx_entity_comments_deleted_at ON entity_comments (deleted_at);

COMMENT ON TABLE entity_comments IS 'User-authored notes attached to any supported business entity.';
