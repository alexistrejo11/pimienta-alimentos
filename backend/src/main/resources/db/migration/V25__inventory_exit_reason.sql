ALTER TABLE inventory_transactions
    ADD COLUMN exit_reason VARCHAR(32);

UPDATE inventory_transactions
SET exit_reason = 'SCRAP'
WHERE type = 'SCRAP_WRITE_OFF' AND exit_reason IS NULL;

ALTER TABLE inventory_transactions
    ADD CONSTRAINT chk_inventory_transactions_exit_reason
        CHECK (
            exit_reason IS NULL
            OR exit_reason IN (
                'SCRAP',
                'DAMAGED',
                'EXPIRED',
                'INTERNAL_USE',
                'INVENTORY_ADJUSTMENT'
            )
        );

COMMENT ON COLUMN inventory_transactions.exit_reason IS
    'Structured exit reason for SCRAP_WRITE_OFF; null for other transaction types.';
