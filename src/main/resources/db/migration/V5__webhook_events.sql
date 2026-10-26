CREATE TABLE IF NOT EXISTS webhook_events (
    id            BIGSERIAL PRIMARY KEY,
    provider      VARCHAR(30) NOT NULL,
    event_id      VARCHAR(128) NOT NULL,
    received_at   TIMESTAMP NOT NULL,
    CONSTRAINT uk_webhook_provider_event UNIQUE (provider, event_id)
);
