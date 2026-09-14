CREATE UNIQUE INDEX ux_pos_shifts_active_device
    ON pos_shifts (device_id)
    WHERE status = 'OPEN';

CREATE UNIQUE INDEX ux_pos_shifts_active_operator
    ON pos_shifts (cashier_operator_id)
    WHERE status = 'OPEN' AND cashier_operator_id IS NOT NULL;
