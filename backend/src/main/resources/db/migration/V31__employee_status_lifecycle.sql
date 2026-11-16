-- Decouple employee lifecycle from contract onboarding.
-- Map legacy statuses onto ACTIVE | INACTIVE | RESIGNED | TERMINATED | DOWN.

UPDATE employees
SET status = 'INACTIVE'
WHERE status IN (
    'DRAFT',
    'PENDING_CONTRACT',
    'SICK',
    'ON_VACATION',
    'ON_LEAVE',
    'UNDEFINED'
)
   OR status IS NULL;

UPDATE employees
SET status = 'TERMINATED'
WHERE status = 'FIRED';

ALTER TABLE employees DROP CONSTRAINT IF EXISTS ck_employees_status;

ALTER TABLE employees
    ADD CONSTRAINT ck_employees_status
        CHECK (status IS NULL OR status IN (
            'ACTIVE',
            'INACTIVE',
            'RESIGNED',
            'TERMINATED',
            'DOWN'
        ));
