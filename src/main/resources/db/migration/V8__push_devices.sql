CREATE TABLE IF NOT EXISTS device_tokens (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    token         VARCHAR(500) NOT NULL,
    platform      VARCHAR(10) NOT NULL,
    last_seen_at  TIMESTAMP NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    CONSTRAINT uk_device_tokens_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_device_tokens_user ON device_tokens (user_id);

CREATE TABLE IF NOT EXISTS push_delivery_logs (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL,
    title              VARCHAR(255),
    status             VARCHAR(20) NOT NULL,
    attempted_tokens   INTEGER NOT NULL DEFAULT 0,
    delivered_tokens   INTEGER NOT NULL DEFAULT 0,
    failure_reason     VARCHAR(500),
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_push_delivery_logs_user ON push_delivery_logs (user_id, created_at DESC);
