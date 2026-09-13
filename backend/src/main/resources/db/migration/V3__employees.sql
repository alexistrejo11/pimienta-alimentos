-- Pimienta Alimentos — HR employee master data and daily attendance

CREATE TABLE employees (
    id                              BIGSERIAL PRIMARY KEY,
    first_name                      VARCHAR(100),
    last_name                       VARCHAR(100),
    photo_url                       VARCHAR(255),
    email                           VARCHAR(320),
    phone                           VARCHAR(40),
    address                         VARCHAR(500),
    birth_date                      DATE,
    nationality                     VARCHAR(80),
    curp                            VARCHAR(18),
    rfc                             VARCHAR(13),
    nss                             VARCHAR(11),
    clabe                           VARCHAR(18),
    employee_number                 VARCHAR(32),
    position                        VARCHAR(120),
    department                      VARCHAR(120),
    contract_type                   VARCHAR(32),
    work_shift                      VARCHAR(32),
    hire_date                       DATE,
    termination_date                DATE,
    salary_per_week                 NUMERIC(19, 6),
    bonuses                         NUMERIC(19, 6),
    food_vouchers                   NUMERIC(19, 6),
    integration_factor              NUMERIC(19, 6),
    imss_worker_type                VARCHAR(32),
    imss_salary_type                VARCHAR(32),
    christmas_bonus_days            INTEGER      NOT NULL DEFAULT 0,
    vacation_days                   INTEGER      NOT NULL DEFAULT 0,
    vacation_premium_percent        NUMERIC(19, 6),
    status                          VARCHAR(32),
    work_schedule                   JSONB,
    created_at                      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at                      TIMESTAMP,
    version                         BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_employees_status
        CHECK (status IS NULL OR status IN (
            'DRAFT', 'PENDING_CONTRACT', 'ACTIVE', 'SICK', 'ON_VACATION', 'ON_LEAVE',
            'TERMINATED', 'FIRED', 'RESIGNED', 'UNDEFINED'
        )),
    CONSTRAINT ck_employees_contract_type
        CHECK (contract_type IS NULL OR contract_type IN (
            'INDEFINITE', 'FIXED_TERM', 'PROJECT_BASED', 'TEMPORARY', 'FREELANCE', 'UNDEFINED'
        )),
    CONSTRAINT ck_employees_work_shift
        CHECK (work_shift IS NULL OR work_shift IN (
            'MORNING', 'AFTERNOON', 'NIGHT', 'MIXED', 'REMOTE', 'UNDEFINED'
        )),
    CONSTRAINT ck_employees_imss_worker_type
        CHECK (imss_worker_type IS NULL OR imss_worker_type IN (
            'PERMANENT_URBAN', 'EVENTUAL_URBAN', 'PERMANENT_RURAL', 'EVENTUAL_RURAL'
        )),
    CONSTRAINT ck_employees_imss_salary_type
        CHECK (imss_salary_type IS NULL OR imss_salary_type IN ('FIXED', 'VARIABLE', 'MIXED'))
);

CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_deleted_at ON employees (deleted_at);
CREATE INDEX idx_employees_personal_email ON employees (email);
CREATE INDEX idx_employees_employee_number ON employees (employee_number);
CREATE INDEX idx_employees_employment_department ON employees (department);

COMMENT ON TABLE employees IS 'HR employee master data with embedded personal, employment, and payroll fields.';

CREATE TABLE employee_attendances (
    id                           BIGSERIAL PRIMARY KEY,
    employee_id                  BIGINT       NOT NULL REFERENCES employees (id),
    headquarter_id               BIGINT       NOT NULL REFERENCES headquarters (id),
    work_date                    DATE         NOT NULL,
    check_in_time                TIMESTAMP,
    check_out_time               TIMESTAMP,
    status                       VARCHAR(48)  NOT NULL,
    check_in_evidence_photo_url  VARCHAR(2000),
    check_out_evidence_photo_url VARCHAR(2000),
    created_at                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at                   TIMESTAMP,
    version                      BIGINT       NOT NULL DEFAULT 1,
    CONSTRAINT ck_employee_attendances_status
        CHECK (status IN (
            'UNDEFINED', 'CHECKED_IN', 'CHECKED_OUT',
            'AUTO_CLOSED_EXCEEDED_MAX_SHIFT_HOURS', 'AUTO_CLOSED_ASSUMED_CONTRACT_DAY'
        ))
);

CREATE INDEX idx_employee_attendances_employee_work_date ON employee_attendances (employee_id, work_date);
CREATE INDEX idx_employee_attendances_headquarter_work_date ON employee_attendances (headquarter_id, work_date);
CREATE INDEX idx_employee_attendances_work_date ON employee_attendances (work_date);
CREATE INDEX idx_employee_attendances_deleted_at ON employee_attendances (deleted_at);

COMMENT ON TABLE employee_attendances IS 'Daily check-in/out records per employee and headquarter.';
