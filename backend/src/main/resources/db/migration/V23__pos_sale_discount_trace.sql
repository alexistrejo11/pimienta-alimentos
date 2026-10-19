-- Discount audit columns on accepted sales, plus a unit-cost snapshot on each line.

ALTER TABLE pos_sales
    ADD COLUMN discount_mode VARCHAR(32),
    ADD COLUMN discount_reason VARCHAR(512),
    ADD COLUMN discount_authorized_by_operator_id BIGINT;

ALTER TABLE pos_sales
    ADD CONSTRAINT ck_pos_sales_discount_mode
        CHECK (discount_mode IS NULL OR discount_mode IN ('FULL_COURTESY', 'AT_COST', 'TARGET_AMOUNT'));

ALTER TABLE pos_sales
    ADD CONSTRAINT fk_pos_sales_discount_authorizer
        FOREIGN KEY (discount_authorized_by_operator_id) REFERENCES pos_operators (id);

ALTER TABLE pos_sale_lines
    ADD COLUMN unit_cost_centavos BIGINT;

COMMENT ON COLUMN pos_sales.discount_mode IS 'FULL_COURTESY, AT_COST, or TARGET_AMOUNT when the ticket has a discount.';
COMMENT ON COLUMN pos_sales.discount_reason IS 'Mandatory concept captured on the tablet for a discounted sale.';
COMMENT ON COLUMN pos_sale_lines.unit_cost_centavos IS 'Catalog unit cost snapshot at sale time, in centavos.';

-- Older SALE_CONFIRMED events stored the concept only inside the raw payload.
UPDATE pos_sales AS s
SET
    discount_mode = CASE
        WHEN s.discount_centavos = s.gross_centavos THEN 'FULL_COURTESY'
        ELSE 'TARGET_AMOUNT'
    END,
    discount_reason = NULLIF(LEFT(BTRIM(e.payload #>> '{discount,reason}'), 512), ''),
    discount_authorized_by_operator_id = (
        SELECT o.id
        FROM pos_operators o
        WHERE (e.payload #>> '{discount,authorizedByUserId}') ~ '^[0-9]+$'
          AND o.id = (e.payload #>> '{discount,authorizedByUserId}')::bigint
    )
FROM pos_sync_events e
WHERE e.event_id = s.event_id
  AND s.discount_centavos > 0;
