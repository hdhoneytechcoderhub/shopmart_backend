CREATE TABLE IF NOT EXISTS customer_notes (
    id            BIGSERIAL PRIMARY KEY,
    customer_id   BIGINT NOT NULL,
    author_id     BIGINT NOT NULL,
    note          VARCHAR(2000) NOT NULL,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customer_notes_customer ON customer_notes (customer_id);
