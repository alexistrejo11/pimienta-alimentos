-- Distinguishes globally POS-sellable items from inventory-only articles.
ALTER TABLE inventory_items
    ADD COLUMN catalog_role VARCHAR(32) NOT NULL DEFAULT 'INVENTORY_ONLY',
    ADD CONSTRAINT ck_inventory_items_catalog_role
        CHECK (catalog_role IN ('INVENTORY_ONLY', 'POS_SELLABLE'));

UPDATE inventory_items i
SET catalog_role = 'POS_SELLABLE'
WHERE EXISTS (
    SELECT 1 FROM headquarter_items h
    WHERE h.item_id = i.id AND h.deleted_at IS NULL
);

CREATE INDEX idx_inventory_items_catalog_role ON inventory_items (catalog_role);
