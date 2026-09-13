-- Pimienta Alimentos — application login accounts and role assignments

CREATE TABLE account_users (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(320) NOT NULL,
    password_hash  VARCHAR(200) NOT NULL,
    first_name     VARCHAR(120) NOT NULL,
    last_name      VARCHAR(120) NOT NULL,
    gender         VARCHAR(32)  NOT NULL,
    phone          VARCHAR(32)  NOT NULL,
    date_of_birth  DATE         NOT NULL,
    account_status VARCHAR(32)  NOT NULL,
    banned_reason  VARCHAR(500),
    banned_at      TIMESTAMP,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMP,
    version        BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT uk_account_users_email UNIQUE (email),
    CONSTRAINT ck_account_users_gender
        CHECK (gender IN ('MALE', 'FEMALE', 'NON_BINARY', 'OTHER', 'PREFER_NOT_TO_SAY')),
    CONSTRAINT ck_account_users_account_status
        CHECK (account_status IN ('PENDING_APPROVAL', 'ACTIVE', 'BANNED'))
);

CREATE INDEX idx_account_users_phone ON account_users (phone);
CREATE INDEX idx_account_users_deleted_at ON account_users (deleted_at);
CREATE INDEX idx_account_users_account_status ON account_users (account_status);

COMMENT ON TABLE account_users IS 'Application login accounts (distinct from HR employee records).';

CREATE TABLE account_user_roles (
    user_id BIGINT      NOT NULL REFERENCES account_users (id) ON DELETE CASCADE,
    role    VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_account_user_roles_role
        CHECK (role IN ('USER', 'SUPPORT', 'MANAGER', 'ADMIN'))
);

CREATE INDEX idx_account_user_roles_user_id ON account_user_roles (user_id);

COMMENT ON TABLE account_user_roles IS 'Role assignments for account_users (ElementCollection).';

CREATE TABLE account_user_headquarters (
    user_id         BIGINT NOT NULL REFERENCES account_users (id) ON DELETE CASCADE,
    headquarter_id  BIGINT NOT NULL REFERENCES headquarters (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, headquarter_id)
);

CREATE INDEX idx_account_user_headquarters_hq ON account_user_headquarters (headquarter_id);

COMMENT ON TABLE account_user_headquarters IS 'Headquarters a web staff user may operate on (MANAGER: typically one sede).';
