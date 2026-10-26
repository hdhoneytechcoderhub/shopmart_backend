-- Fixes a genuine table-name collision found while debugging a real Spring startup failure:
-- the "push" module's FCM device-token table and the pre-existing "mobile" module's
-- device-token table (see db/phase7.sql, which predates the push module) both targeted
-- "device_tokens" with different, incompatible column sets. V8__push_devices.sql created
-- nothing on any database where the mobile table already existed (CREATE TABLE IF NOT EXISTS
-- was a silent no-op there) — meaning the push module's entity has never actually matched a
-- real table since it was added. Rather than editing V8 (which may already be recorded in
-- flyway_schema_history, and editing an applied migration breaks checksum validation), the
-- push module now gets its own table.
CREATE TABLE IF NOT EXISTS push_device_tokens (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    token         VARCHAR(500) NOT NULL,
    platform      VARCHAR(10) NOT NULL,
    last_seen_at  TIMESTAMP NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    CONSTRAINT uk_push_device_tokens_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_push_device_tokens_user ON push_device_tokens (user_id);
