ALTER TABLE inventory_transactions
    DROP CONSTRAINT ck_inventory_transactions_type;

ALTER TABLE inventory_transactions
    ADD CONSTRAINT ck_inventory_transactions_type
        CHECK (type IN (
            'PURCHASE_RECEIPT', 'SALE_DISPATCH', 'INTERNAL_TRANSFER', 'PHYSICAL_COUNT',
            'ADJUSTMENT', 'RETURN_FROM_CLIENT', 'RETURN_TO_SUPPLIER', 'PRODUCTION_ISSUE',
            'SCRAP_WRITE_OFF'
        ));
