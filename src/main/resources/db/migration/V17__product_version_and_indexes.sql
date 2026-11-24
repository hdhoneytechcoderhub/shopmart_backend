-- V17: Optimistic locking on products + missing indexes for hot query paths

-- Optimistic locking version column for Product entity (@Version)
ALTER TABLE products ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Cart items: FK indexes for cart-to-order conversion path
CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id ON cart_items (cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product_id ON cart_items (product_id);

-- Orders: explicit index on user_id (FK alone doesn't guarantee an index in all PG versions)
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at DESC);

-- Reviews: composite index for product review summary queries
CREATE INDEX IF NOT EXISTS idx_reviews_product_status ON reviews (product_id, status);

-- Wishlist items: FK index
CREATE INDEX IF NOT EXISTS idx_wishlist_items_user_id ON wishlist_items (user_id);

-- Payments: index for gateway ref lookups (webhook path)
CREATE INDEX IF NOT EXISTS idx_payments_gateway_ref ON payments (gateway_ref);
