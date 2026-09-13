-- Repair for databases that already applied V7 before catalog_role was folded into it.
ALTER TABLE inventory_items
    ADD COLUMN IF NOT EXISTS catalog_role VARCHAR(32) NOT NULL DEFAULT 'INVENTORY_ONLY';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_inventory_items_catalog_role'
          AND conrelid = 'inventory_items'::regclass
    ) THEN
        ALTER TABLE inventory_items
            ADD CONSTRAINT ck_inventory_items_catalog_role
            CHECK (catalog_role IN ('INVENTORY_ONLY', 'POS_SELLABLE'));
    END IF;
END $$;

UPDATE inventory_items i
SET catalog_role = 'POS_SELLABLE'
WHERE EXISTS (
    SELECT 1
    FROM headquarter_items h
    WHERE h.item_id = i.id
      AND h.deleted_at IS NULL
)
AND i.catalog_role = 'INVENTORY_ONLY';

CREATE INDEX IF NOT EXISTS idx_inventory_items_catalog_role
    ON inventory_items (catalog_role);
