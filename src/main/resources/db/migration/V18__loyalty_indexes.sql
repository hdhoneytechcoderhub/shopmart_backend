-- V18: Loyalty module indexes for leaderboard, stats, and rank queries

-- Loyalty accounts: balance DESC index for leaderboard sorting
CREATE INDEX IF NOT EXISTS idx_loyalty_accounts_balance ON loyalty_accounts (balance DESC);

-- Loyalty accounts: user_id index for fast per-user lookup
CREATE INDEX IF NOT EXISTS idx_loyalty_accounts_user_id ON loyalty_accounts (user_id);

-- Loyalty transactions: type index for earn/redeem filtering
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_type ON loyalty_transactions (type);

-- Loyalty transactions: composite index for SUM queries (user + type)
CREATE INDEX IF NOT EXISTS idx_loyalty_txn_user_type ON loyalty_transactions (user_id, type);
