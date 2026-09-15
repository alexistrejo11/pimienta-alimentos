-- Historical records must never be detached or removed by deleting their parents.
ALTER TABLE inventory_movements
    DROP CONSTRAINT IF EXISTS inventory_movements_source_location_id_fkey,
    DROP CONSTRAINT IF EXISTS inventory_movements_destination_location_id_fkey,
    DROP CONSTRAINT IF EXISTS inventory_movements_transaction_id_fkey;

ALTER TABLE inventory_movements
    ADD CONSTRAINT fk_inventory_movements_source_location
        FOREIGN KEY (source_location_id) REFERENCES storage_locations (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_inventory_movements_destination_location
        FOREIGN KEY (destination_location_id) REFERENCES storage_locations (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_inventory_movements_transaction
        FOREIGN KEY (transaction_id) REFERENCES inventory_transactions (id) ON DELETE RESTRICT;

-- The application treats movements as an append-only ledger.
CREATE OR REPLACE FUNCTION prevent_inventory_movement_delete()
RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'inventory_movements are append-only';
END;
$$;

DROP TRIGGER IF EXISTS trg_prevent_inventory_movement_delete ON inventory_movements;
CREATE TRIGGER trg_prevent_inventory_movement_delete
    BEFORE DELETE ON inventory_movements
    FOR EACH ROW EXECUTE FUNCTION prevent_inventory_movement_delete();
