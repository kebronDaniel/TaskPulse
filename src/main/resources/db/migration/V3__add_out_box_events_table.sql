CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,

    CONSTRAINT chk_outbox_events_status
        CHECK ( status IN ('PENDING', 'PUBLISHED', 'FAILED')),

    CONSTRAINT chk_outbox_events_attempt_count
        CHECK ( attempt_count >=0 )
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (occurred_at) WHERE status = 'PENDING';