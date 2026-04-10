-- V6: Remove XP (totalExperience) from system
-- XP is no longer used in the system. This migration:
-- 1. Resets all totalExperience values to 0 (preserve column structure for backward compat)
-- 2. Zeroes out totalXp in leaderboard_entry (column: total_xp)
-- Note: Column is kept to avoid breaking existing JPA entities, but data is cleared.

-- Clear XP on all accounts
UPDATE account
SET total_experience = 0
WHERE total_experience IS NOT NULL AND total_experience != 0;

-- Clear XP on leaderboard entries
UPDATE leaderboard_entry
SET total_xp = 0
WHERE total_xp IS NOT NULL AND total_xp != 0;

-- Default sort for leaderboard records: change TOTAL_XP -> TOTAL_STARS
UPDATE leaderboard
SET sort_by = 'TOTAL_STARS'
WHERE sort_by = 'TOTAL_XP';
