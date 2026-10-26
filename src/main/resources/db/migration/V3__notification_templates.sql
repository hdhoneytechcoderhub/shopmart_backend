CREATE TABLE IF NOT EXISTS notification_templates (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(100) NOT NULL,
    channel       VARCHAR(20)  NOT NULL,
    subject       VARCHAR(255),
    body          VARCHAR(4000) NOT NULL,
    description   VARCHAR(1000),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    CONSTRAINT uk_notification_template_code UNIQUE (code)
);
