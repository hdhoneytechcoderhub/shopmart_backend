-- V6.5: create the payment tables that V7 assumes already exist.
--
-- Why this file exists:
-- V7__payment_refund_tracking.sql starts with `ALTER TABLE payments ...` and
-- `ALTER TABLE payment_transactions ...`. Those two tables were only ever created by
-- Hibernate's old ddl-auto=update; no migration created them. A database that Flyway
-- baselined at version 1 from a partial schema reached version 6 without either table,
-- so V7 failed with:
--     SQL State: 42P01  ERROR: relation "payments" does not exist
--
-- V7 is already recorded in flyway_schema_history on affected databases, so it must not be
-- edited (that would break checksum validation). This migration is versioned between V6 and
-- V7 so it is applied first, is purely additive, and leaves V7 and every later checksum
-- untouched.
--
-- The definitions match the Payment and PaymentTransaction entities. V7 still adds
-- refunded_amount and idempotency_key, and V19/V20 still add their own index and status
-- constraint, so those migrations keep their original role.

CREATE TABLE IF NOT EXISTS payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT        NOT NULL,
    method          VARCHAR(255)  NOT NULL,
    amount          NUMERIC(12,2) NOT NULL,
    status          VARCHAR(255)  NOT NULL DEFAULT 'CREATED',
    provider        VARCHAR(255),
    gateway_ref     VARCHAR(255),
    transaction_id  VARCHAR(255),
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_payments_order ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_gateway_ref ON payments (gateway_ref);

CREATE TABLE IF NOT EXISTS payment_transactions (
    id                 BIGSERIAL PRIMARY KEY,
    order_id           BIGINT        NOT NULL,
    payment_id         BIGINT,
    gateway_order_id   VARCHAR(255),
    transaction_id     VARCHAR(255),
    amount             NUMERIC(38,2),
    status             VARCHAR(20)   NOT NULL,
    provider           VARCHAR(255),
    note               VARCHAR(500),
    created_at         TIMESTAMPTZ   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_txn_order ON payment_transactions (order_id);
CREATE INDEX IF NOT EXISTS idx_txn_payment ON payment_transactions (payment_id);

-- Razorpay/refund code paths write these columns through the Payment/PaymentTransaction
-- entities. V7 adds them on the normal path; these IF NOT EXISTS guards make this migration
-- safe on a database where one of the tables already exists in a partially-created shape.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS provider VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS gateway_ref VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS payment_id BIGINT;
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS gateway_order_id VARCHAR(255);
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS transaction_id VARCHAR(255);
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS provider VARCHAR(255);
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS note VARCHAR(500);
