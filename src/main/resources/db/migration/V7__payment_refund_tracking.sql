-- Partial-refund + idempotency support added to the payment module.
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(12,2) DEFAULT 0;
ALTER TABLE payment_transactions ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);

-- Unique only when present: Postgres treats NULL as distinct for UNIQUE, so existing rows
-- (idempotency_key = NULL) are unaffected; only future refund transactions that supply a key
-- are protected against duplicates.
CREATE UNIQUE INDEX IF NOT EXISTS uk_payment_transactions_idempotency_key
    ON payment_transactions (idempotency_key) WHERE idempotency_key IS NOT NULL;
