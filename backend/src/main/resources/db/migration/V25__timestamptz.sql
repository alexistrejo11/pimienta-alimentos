-- UTC-naive timestamps become timestamptz without moving the instant.
-- USING col AT TIME ZONE 'UTC' does not depend on the session TimeZone.
-- ALTER DATABASE ... SET timezone is intentionally absent: PostgreSQL rejects it
-- inside Flyway's transaction, and the database name differs per environment.
-- Run that once outside Flyway. Local Docker applies it from docker/postgres/ensure-role.sh.

ALTER TABLE crm_clients
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE crm_opportunities
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE crm_projects
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE crm_project_milestones
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE tasks
    ALTER COLUMN assigned_at TYPE timestamptz USING assigned_at AT TIME ZONE 'UTC',
    ALTER COLUMN completed_at TYPE timestamptz USING completed_at AT TIME ZONE 'UTC',
    ALTER COLUMN due_date TYPE timestamptz USING due_date AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE payroll_periods
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE payroll_records
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE payroll_adjustments
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE payroll_payments
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE payroll_debts
    ALTER COLUMN settled_at TYPE timestamptz USING settled_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE employees
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE employee_attendances
    ALTER COLUMN check_in_time TYPE timestamptz USING check_in_time AT TIME ZONE 'UTC',
    ALTER COLUMN check_out_time TYPE timestamptz USING check_out_time AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE inventory_items
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE products
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE storage_locations
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE inventory_stock
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE inventory_transactions
    ALTER COLUMN approved_at TYPE timestamptz USING approved_at AT TIME ZONE 'UTC',
    ALTER COLUMN completed_at TYPE timestamptz USING completed_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE inventory_movements
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE suppliers
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE file_assets
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE headquarter_pos_settings
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_sale_categories
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE headquarter_items
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_devices
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_operators
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_enrollment_codes
    ALTER COLUMN expires_at TYPE timestamptz USING expires_at AT TIME ZONE 'UTC',
    ALTER COLUMN consumed_at TYPE timestamptz USING consumed_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE headquarters
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE account_users
    ALTER COLUMN banned_at TYPE timestamptz USING banned_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE notifications
    ALTER COLUMN sent_at TYPE timestamptz USING sent_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_change_log
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';

ALTER TABLE entity_comments
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE business_contracts
    ALTER COLUMN last_renewed_at TYPE timestamptz USING last_renewed_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_sync_events
    ALTER COLUMN occurred_at TYPE timestamptz USING occurred_at AT TIME ZONE 'UTC',
    ALTER COLUMN server_received_at TYPE timestamptz USING server_received_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_sync_incidents
    ALTER COLUMN accepted_at TYPE timestamptz USING accepted_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_sales
    ALTER COLUMN occurred_at TYPE timestamptz USING occurred_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_sale_lines
    ALTER COLUMN authorized_at TYPE timestamptz USING authorized_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_sale_payments
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_sync_tombstones
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';

ALTER TABLE operating_expenses
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE timestamptz USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE pos_health_snapshots
    ALTER COLUMN received_at TYPE timestamptz USING received_at AT TIME ZONE 'UTC';

ALTER TABLE pos_shifts
    ALTER COLUMN opened_at TYPE timestamptz USING opened_at AT TIME ZONE 'UTC',
    ALTER COLUMN closed_at TYPE timestamptz USING closed_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE pos_cash_movements
    ALTER COLUMN occurred_at TYPE timestamptz USING occurred_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE pos_cash_counts
    ALTER COLUMN submitted_at TYPE timestamptz USING submitted_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE timestamptz USING updated_at AT TIME ZONE 'UTC';
