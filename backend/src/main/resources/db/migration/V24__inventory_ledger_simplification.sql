-- Inventory ledger simplification. No production stock rows exist; old type values are removed.

DELETE FROM inventory_movements;
DELETE FROM inventory_transactions;

DROP TABLE IF EXISTS inventory_count_responses;
DROP TABLE IF EXISTS inventory_count_sessions;

ALTER TABLE headquarter_pos_settings DROP COLUMN IF EXISTS default_negative_stock_limit;
ALTER TABLE headquarter_items DROP COLUMN IF EXISTS negative_stock_limit;
ALTER TABLE pos_sale_lines DROP COLUMN IF EXISTS sold_with_negative_stock;
ALTER TABLE inventory_stock DROP COLUMN IF EXISTS reserved_quantity;
ALTER TABLE inventory_stock DROP COLUMN IF EXISTS in_transit_quantity;

ALTER TABLE inventory_transactions DROP CONSTRAINT IF EXISTS ck_inventory_transactions_type;
ALTER TABLE inventory_transactions
    ADD CONSTRAINT ck_inventory_transactions_type
        CHECK (type IN ('IN', 'OUT', 'ADJUSTMENT'));

ALTER TABLE inventory_transactions DROP CONSTRAINT IF EXISTS ck_inventory_transactions_status;
ALTER TABLE inventory_transactions
    ADD CONSTRAINT ck_inventory_transactions_status
        CHECK (status IN ('COMPLETED'));

ALTER TABLE inventory_transactions DROP CONSTRAINT IF EXISTS chk_inventory_transactions_exit_reason;
ALTER TABLE inventory_transactions
    ADD CONSTRAINT ck_inventory_transactions_exit_reason
        CHECK (
            (type = 'OUT' AND exit_reason IN ('WASTE', 'WITHDRAWAL'))
            OR (type <> 'OUT' AND exit_reason IS NULL)
        );

ALTER TABLE inventory_movements DROP CONSTRAINT IF EXISTS ck_inventory_movements_type;
ALTER TABLE inventory_movements
    ADD CONSTRAINT ck_inventory_movements_type
        CHECK (type IN ('IN', 'OUT', 'ADJUSTMENT_PLUS', 'ADJUSTMENT_MINUS'));

ALTER TABLE inventory_movements DROP CONSTRAINT IF EXISTS ck_inventory_movements_direction;
ALTER TABLE inventory_movements
    ADD CONSTRAINT ck_inventory_movements_direction
        CHECK (direction IN ('IN', 'OUT'));

ALTER TABLE inventory_movements ADD COLUMN IF NOT EXISTS exit_reason VARCHAR(32);

ALTER TABLE inventory_movements DROP CONSTRAINT IF EXISTS ck_inventory_movements_exit_reason;
ALTER TABLE inventory_movements
    ADD CONSTRAINT ck_inventory_movements_exit_reason
        CHECK (
            (type = 'OUT' AND exit_reason IN ('SALE', 'WASTE', 'WITHDRAWAL'))
            OR (type = 'IN' AND (exit_reason IS NULL OR exit_reason = 'RETURN_FROM_CLIENT'))
            OR (type IN ('ADJUSTMENT_PLUS', 'ADJUSTMENT_MINUS') AND exit_reason IS NULL)
        );

COMMENT ON COLUMN inventory_transactions.exit_reason IS
    'Staff OUT reason: WASTE or WITHDRAWAL. Null for IN and ADJUSTMENT.';
COMMENT ON COLUMN inventory_movements.exit_reason IS
    'OUT: SALE, WASTE, WITHDRAWAL. IN may be RETURN_FROM_CLIENT for a POS sale reversal.';
