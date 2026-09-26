-- Narrow gender to MALE / FEMALE / OTHER; date_of_birth no longer required for new accounts.

UPDATE account_users
SET gender = 'OTHER'
WHERE gender IN ('NON_BINARY', 'PREFER_NOT_TO_SAY');

ALTER TABLE account_users
    DROP CONSTRAINT ck_account_users_gender;

ALTER TABLE account_users
    ADD CONSTRAINT ck_account_users_gender
        CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'));

ALTER TABLE account_users
    ALTER COLUMN date_of_birth DROP NOT NULL;
