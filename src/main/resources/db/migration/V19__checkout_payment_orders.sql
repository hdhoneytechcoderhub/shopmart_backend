-- Checkout / Razorpay order management additions.
-- All statements are additive and safe to run against an existing database.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_email VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(255);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS confirmation_email_sent_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS admin_email_sent_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(100);

-- Repeated create-order clicks/retries with the same key resolve to the same order.
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_client_request
    ON orders (user_id, client_request_id)
    WHERE client_request_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_razorpay_order ON orders (razorpay_order_id);
CREATE INDEX IF NOT EXISTS idx_orders_razorpay_payment ON orders (razorpay_payment_id);

-- Delivery address now captures the contact email shown on the checkout form.
ALTER TABLE addresses ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- One Razorpay payment id can never be attached to two local payment rows.
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_transaction_id
    ON payments (transaction_id)
    WHERE transaction_id IS NOT NULL;
