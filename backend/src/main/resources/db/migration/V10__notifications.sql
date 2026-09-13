-- Pimienta Alimentos — outbound notification audit log

CREATE TABLE notifications (
    id                      UUID PRIMARY KEY,
    channel                 VARCHAR(16)  NOT NULL,
    type                    VARCHAR(64)  NOT NULL,
    status                  VARCHAR(16)  NOT NULL,
    recipient_email         VARCHAR(320),
    recipient_phone         VARCHAR(32),
    recipient_display_name  VARCHAR(240),
    recipient_user_id       BIGINT,
    subject                 VARCHAR(500),
    body                    TEXT,
    template_id             VARCHAR(120),
    template_variables      JSONB,
    correlation_id          VARCHAR(200),
    related_user_id         BIGINT,
    locale                  VARCHAR(16)  NOT NULL DEFAULT 'en',
    attempt_count           INT          NOT NULL DEFAULT 0,
    last_error              VARCHAR(1000),
    sent_at                 TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP,
    version                 BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_notifications_channel
        CHECK (channel IN ('EMAIL', 'SMS', 'LOG')),
    CONSTRAINT ck_notifications_type
        CHECK (type IN ('ACCOUNT_PENDING_APPROVAL', 'UNDEFINED')),
    CONSTRAINT ck_notifications_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_notifications_created_at ON notifications (created_at);
CREATE INDEX idx_notifications_channel ON notifications (channel);
CREATE INDEX idx_notifications_status ON notifications (status);
CREATE INDEX idx_notifications_type ON notifications (type);
CREATE INDEX idx_notifications_correlation_id ON notifications (correlation_id);
CREATE INDEX idx_notifications_recipient_user_id ON notifications (recipient_user_id);
CREATE INDEX idx_notifications_related_user_id ON notifications (related_user_id);
CREATE INDEX idx_notifications_deleted_at ON notifications (deleted_at);

COMMENT ON TABLE notifications IS 'Audit log of outbound notifications (all channels).';
