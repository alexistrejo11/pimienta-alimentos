-- Retire the unused CRM module. Does not touch file_assets or object storage.
-- External foreign-key names match PostgreSQL's default for inline REFERENCES.

DELETE FROM entity_comments
WHERE target_type IN ('OPPORTUNITY', 'PROJECT', 'PROJECT_MILESTONE');

ALTER TABLE entity_comments DROP CONSTRAINT ck_entity_comments_target_type;
ALTER TABLE entity_comments
    ADD CONSTRAINT ck_entity_comments_target_type
        CHECK (target_type IN (
            'TASK', 'CONTRACT', 'EMPLOYEE', 'HEADQUARTER',
            'INVENTORY_ITEM', 'INVENTORY_TRANSACTION', 'PAYROLL_RECORD'
        ));

ALTER TABLE tasks DROP CONSTRAINT tasks_project_id_fkey;
ALTER TABLE tasks DROP CONSTRAINT tasks_opportunity_id_fkey;
DROP INDEX idx_tasks_project_id;
DROP INDEX idx_tasks_opportunity_id;
ALTER TABLE tasks DROP COLUMN project_id;
ALTER TABLE tasks DROP COLUMN opportunity_id;

COMMENT ON TABLE tasks IS 'Operational tasks optionally linked to a headquarter.';

ALTER TABLE business_contracts DROP CONSTRAINT business_contracts_opportunity_id_fkey;
ALTER TABLE business_contracts DROP CONSTRAINT business_contracts_project_id_fkey;
DROP INDEX idx_business_contracts_opportunity_id;
DROP INDEX idx_business_contracts_project_id;
ALTER TABLE business_contracts DROP COLUMN opportunity_id;
ALTER TABLE business_contracts DROP COLUMN project_id;

COMMENT ON TABLE business_contracts IS 'Legal/business agreements tied to employees or counterparties.';

DROP TABLE crm_project_milestones;
DROP TABLE crm_projects;
DROP TABLE crm_opportunities;
DROP TABLE crm_clients;
