-- Staff users assigned to headquarters (MANAGER scoped to sede; ADMIN may have none = all sedes).

CREATE TABLE account_user_headquarters (
    user_id         BIGINT NOT NULL REFERENCES account_users (id) ON DELETE CASCADE,
    headquarter_id  BIGINT NOT NULL REFERENCES headquarters (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, headquarter_id)
);

CREATE INDEX idx_account_user_headquarters_hq ON account_user_headquarters (headquarter_id);

COMMENT ON TABLE account_user_headquarters IS 'Headquarters a web staff user may operate on (MANAGER: typically one sede).';
