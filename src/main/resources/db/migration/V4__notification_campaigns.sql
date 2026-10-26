CREATE TABLE IF NOT EXISTS notification_campaigns (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    channel           VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    target_type       VARCHAR(20) NOT NULL,
    target_role       VARCHAR(50),
    target_segment    VARCHAR(50),
    template_id       BIGINT,
    title             VARCHAR(255) NOT NULL,
    message           VARCHAR(4000) NOT NULL,
    link              VARCHAR(500),
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    scheduled_at      TIMESTAMP,
    sent_at           TIMESTAMP,
    recipient_count   INTEGER NOT NULL DEFAULT 0,
    created_by        BIGINT,
    failure_reason    VARCHAR(500),
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notification_campaigns_status ON notification_campaigns (status);
CREATE INDEX IF NOT EXISTS idx_notification_campaigns_scheduled_at ON notification_campaigns (scheduled_at);
