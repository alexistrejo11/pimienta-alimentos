-- A device sequence identifies one causal position on one tablet.
CREATE UNIQUE INDEX uk_pos_sync_events_device_sequence
    ON pos_sync_events (device_id, device_sequence);
