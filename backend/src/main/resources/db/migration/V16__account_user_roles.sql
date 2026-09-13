-- Homologated account role catalog. USER is the unassigned baseline role.
ALTER TABLE account_user_roles DROP CONSTRAINT ck_account_user_roles_role;

ALTER TABLE account_user_roles
    ADD CONSTRAINT ck_account_user_roles_role
        CHECK (role IN ('ADMIN', 'MANAGER', 'SALES', 'POS_OPERATOR', 'EMPLOYEE', 'SUPPORT', 'USER'));

-- Normalize legacy accounts that predate the USER baseline role.
INSERT INTO account_user_roles (user_id, role)
SELECT u.id, 'USER'
FROM account_users u
WHERE NOT EXISTS (
    SELECT 1 FROM account_user_roles r WHERE r.user_id = u.id
);
