ALTER TABLE outbox_events
    ADD COLUMN partition_key VARCHAR(255) NOT NULL;