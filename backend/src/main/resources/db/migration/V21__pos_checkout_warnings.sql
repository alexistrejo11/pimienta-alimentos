-- Per-sede soft warning thresholds for the POS checkout (UI only, never a validation)

ALTER TABLE headquarter_pos_settings
    ADD COLUMN high_sale_warning_centavos BIGINT NOT NULL DEFAULT 1000000,
    ADD COLUMN high_change_warning_centavos BIGINT NOT NULL DEFAULT 100000;

ALTER TABLE headquarter_pos_settings
    ADD CONSTRAINT ck_headquarter_pos_settings_high_sale_warning
        CHECK (high_sale_warning_centavos >= 0),
    ADD CONSTRAINT ck_headquarter_pos_settings_high_change_warning
        CHECK (high_change_warning_centavos >= 0);

COMMENT ON COLUMN headquarter_pos_settings.high_sale_warning_centavos IS
    'Net sale total in centavos from which the POS shows a soft confirmation; 0 disables it. Tablets default to 1000000 when absent.';

COMMENT ON COLUMN headquarter_pos_settings.high_change_warning_centavos IS
    'Cash change in centavos from which the POS asks to confirm the tendered amount; 0 disables it. Tablets default to 100000 when absent.';
