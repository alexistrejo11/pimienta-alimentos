-- Convocatorias F5: institutional contracts from awarded opportunities.

ALTER TABLE business_contracts DROP CONSTRAINT ck_business_contracts_category;
ALTER TABLE business_contracts
    ADD CONSTRAINT ck_business_contracts_category
        CHECK (category IS NULL OR category IN (
            'EMPLOYEE',
            'SUPPLIER',
            'CUSTOMER',
            'PARTNER',
            'OTHER',
            'INSTITUTIONAL',
            'UNDEFINED'
        ));

COMMENT ON COLUMN business_contracts.category IS
    'Counterparty kind; INSTITUTIONAL is for awarded convocatorias (opportunity module).';
