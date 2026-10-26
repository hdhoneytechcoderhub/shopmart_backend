CREATE TABLE IF NOT EXISTS report_schedules (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    report_type        VARCHAR(20) NOT NULL,
    frequency          VARCHAR(20) NOT NULL,
    recipient_emails   VARCHAR(1000) NOT NULL,
    enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    next_run_at        TIMESTAMP NOT NULL,
    last_run_at        TIMESTAMP,
    created_by         BIGINT,
    created_at         TIMESTAMP,
    updated_at         TIMESTAMP
);
