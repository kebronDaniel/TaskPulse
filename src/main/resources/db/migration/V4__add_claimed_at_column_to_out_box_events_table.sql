ALTER TABLE outbox_events
    ADD COLUMN claimed_at TIMESTAMPTZ;

ALTER TABLE outbox_events
    DROP CONSTRAINT IF EXISTS chk_outbox_events_status;

ALTER TABLE outbox_events
    ADD CONSTRAINT chk_outbox_events_status
        CHECK ( status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

CREATE INDEX idx_outbox_processing_claimed_at
    ON outbox_events (claimed_at) WHERE status = 'PROCESSING';