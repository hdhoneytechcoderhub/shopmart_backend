-- Product & Category Enhancements (Gujarati/English feature list item, tracked since the
-- earliest backend audit as a genuinely missing field set).

ALTER TABLE products ADD COLUMN IF NOT EXISTS meta_keywords VARCHAR(500);
ALTER TABLE products ADD COLUMN IF NOT EXISTS low_stock_threshold INTEGER;

ALTER TABLE categories ADD COLUMN IF NOT EXISTS parent_id BIGINT;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_categories_parent'
    ) THEN
        ALTER TABLE categories
            ADD CONSTRAINT fk_categories_parent
            FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories (parent_id);
