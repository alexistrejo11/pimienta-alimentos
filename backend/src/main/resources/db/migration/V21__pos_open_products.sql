ALTER TABLE headquarter_pos_settings
    ADD COLUMN allow_open_products BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE pos_sale_lines
    ADD COLUMN line_type VARCHAR(32) NOT NULL DEFAULT 'CATALOG',
    ADD COLUMN authorized_by_operator_id BIGINT REFERENCES pos_operators (id),
    ADD COLUMN authorized_at TIMESTAMP;

ALTER TABLE pos_sale_lines
    ADD CONSTRAINT ck_pos_sale_lines_line_type
        CHECK (line_type IN ('CATALOG', 'OPEN_AMOUNT', 'PENDING_CATALOG'));

CREATE INDEX idx_pos_sale_lines_line_type ON pos_sale_lines (line_type);
