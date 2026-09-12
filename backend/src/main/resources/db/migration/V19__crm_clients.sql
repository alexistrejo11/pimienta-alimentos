-- CRM clients (referenced by crm_projects.client_id)

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

ALTER TABLE crm_projects
    ADD CONSTRAINT fk_crm_projects_client
        FOREIGN KEY (client_id) REFERENCES crm_clients (id);

INSERT INTO crm_clients (id, name, company_name, created_at, updated_at, version) VALUES
    (1, 'Hotel Chain MX', 'Hotel Chain MX', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (2, 'Distribuidora Norte', 'Distribuidora Norte SA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1),
    (3, 'Gourmet Retail', 'Gourmet Retail Group', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1);

SELECT setval(pg_get_serial_sequence('crm_clients', 'id'), (SELECT MAX(id) FROM crm_clients));
