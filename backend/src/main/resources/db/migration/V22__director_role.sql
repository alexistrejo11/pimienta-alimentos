-- POS_OPERATOR accounts become floor staff. DIRECTOR is the sede-scoped operations lead.
UPDATE account_user_roles
SET role = 'EMPLOYEE'
WHERE role = 'POS_OPERATOR';

ALTER TABLE account_user_roles DROP CONSTRAINT ck_account_user_roles_role;

ALTER TABLE account_user_roles
    ADD CONSTRAINT ck_account_user_roles_role
        CHECK (role IN ('ADMIN', 'DIRECTOR', 'MANAGER', 'SALES', 'EMPLOYEE', 'SUPPORT', 'USER'));
