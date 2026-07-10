ALTER TABLE outbox_events
    ADD COLUMN locked_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN locked_by VARCHAR(100),
    ADD COLUMN event_version INTEGER NOT NULL DEFAULT 1;

CREATE INDEX idx_outbox_events_processing_locked
    ON outbox_events (status, locked_at, updated_at);

CREATE INDEX idx_outbox_events_dead
    ON outbox_events (status, created_at, id);