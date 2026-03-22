-- ============================================
-- Migration: Add 'region' column to challenge_bank
-- Values: 'BAC' (Bắc), 'TRUNG' (Trung), 'NAM' (Nam)
-- ============================================

ALTER TABLE challenge_bank
ADD COLUMN IF NOT EXISTS region VARCHAR(20) DEFAULT 'BAC';

-- Optional: Add comment
COMMENT ON COLUMN challenge_bank.region IS 'Miền: BAC (Bắc), TRUNG (Trung), NAM (Nam)';
