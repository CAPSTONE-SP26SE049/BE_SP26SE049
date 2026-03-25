-- ============================================================
-- V4__update_reward_catalog_icons.sql
-- Cập nhật iconUrl cho 52 huy hiệu/thành tích
-- Chạy sau V3__seed_reward_catalog.sql
-- ============================================================

-- ==================== LEARNING (15) ====================
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_first_level_1774320331129.png' WHERE code = 'LEARN_FIRST_LEVEL';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_5_levels_1774320350141.png' WHERE code = 'LEARN_5_LEVELS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_10_levels_1774320365804.png' WHERE code = 'LEARN_10_LEVELS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_15_levels_1774320381238.png' WHERE code = 'LEARN_15_LEVELS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_north_complete_1774320396390.png' WHERE code = 'LEARN_NORTH_COMPLETE';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_central_complete_1774320415064.png' WHERE code = 'LEARN_CENTRAL_COMPLETE';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_south_complete_1774320437732.png' WHERE code = 'LEARN_SOUTH_COMPLETE';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_all_regions_1774320453459.png' WHERE code = 'LEARN_ALL_REGIONS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_beginner_done_1774320467005.png' WHERE code = 'LEARN_BEGINNER_DONE';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_intermediate_done_1774320486543.png' WHERE code = 'LEARN_INTERMEDIATE_DONE';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_advanced_done_1774320526582.png' WHERE code = 'LEARN_ADVANCED_DONE';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_first_3star_1774320544136.png' WHERE code = 'LEARN_FIRST_3STAR';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_10_3star_1774320564153.png' WHERE code = 'LEARN_10_3STAR';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_all_3star_1774320580246.png' WHERE code = 'LEARN_ALL_3STAR';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_learn_retry_win_1774320596421.png' WHERE code = 'LEARN_RETRY_WIN';

-- ==================== PRONUNCIATION (12) ====================
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_first_pass_1774320611814.png' WHERE code = 'PHONEME_FIRST_PASS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_nl_master_1774320627713.png' WHERE code = 'PHONEME_NL_MASTER';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_sx_master_1774320644298.png' WHERE code = 'PHONEME_SX_MASTER';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_dgir_master_1774320673724.png' WHERE code = 'PHONEME_DGIR_MASTER';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_trch_master_1774320690268.png' WHERE code = 'PHONEME_TRCH_MASTER';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_all_pairs_1774320707237.png' WHERE code = 'PHONEME_ALL_PAIRS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_tone_good_1774320726766.png' WHERE code = 'PHONEME_TONE_GOOD';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_tone_master_1774320744624.png' WHERE code = 'PHONEME_TONE_MASTER';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_perfect_session_1774320759505.png' WHERE code = 'PHONEME_PERFECT_SESSION';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_perfect_5_1774320782716.png' WHERE code = 'PHONEME_PERFECT_5';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_entry_done_1774320798739.png' WHERE code = 'PHONEME_ENTRY_DONE';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_phoneme_speed_1774320815182.png' WHERE code = 'PHONEME_SPEED';

-- ==================== MINI_GAMES (12) ====================
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_first_play_1774320832038.png' WHERE code = 'MINI_FIRST_PLAY';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_10_plays_1774320846852.png' WHERE code = 'MINI_10_PLAYS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_50_plays_1774320863767.png' WHERE code = 'MINI_50_PLAYS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_reading_first_1774320889622.png' WHERE code = 'MINI_READING_FIRST';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_reading_saga_1774320907052.png' WHERE code = 'MINI_READING_SAGA';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_writing_first_1774320925950.png' WHERE code = 'MINI_WRITING_FIRST';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_writing_done_1774320945023.png' WHERE code = 'MINI_WRITING_DONE';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_mario_first_1774320962407.png' WHERE code = 'MINI_MARIO_FIRST';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_mario_clear_1774320979828.png' WHERE code = 'MINI_MARIO_CLEAR';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_all_types_1774321002432.png' WHERE code = 'MINI_ALL_TYPES';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_perfect_game_1774321018799.png' WHERE code = 'MINI_PERFECT_GAME';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_mini_perfect_10_1774321041241.png' WHERE code = 'MINI_PERFECT_10';

-- ==================== SCORE (8) ====================
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_first_star_1774321057369.png' WHERE code = 'SCORE_FIRST_STAR';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_50_stars_1774321082921.png' WHERE code = 'SCORE_50_STARS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_100_stars_1774321099394.png' WHERE code = 'SCORE_100_STARS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_300_stars_1774321125250.png' WHERE code = 'SCORE_300_STARS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_500_stars_1774321142351.png' WHERE code = 'SCORE_500_STARS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_perfect_first_1774321160771.png' WHERE code = 'SCORE_PERFECT_FIRST';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_perfect_5_1774321177895.png' WHERE code = 'SCORE_PERFECT_5';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_perfect_10_1774321194263.png' WHERE code = 'SCORE_PERFECT_10';

-- ==================== SOCIAL (3) ====================
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_social_first_friend_1774321209085.png' WHERE code = 'SOCIAL_FIRST_FRIEND';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_social_10_friends_1774321234523.png' WHERE code = 'SOCIAL_10_FRIENDS';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_social_leaderboard_top10_1774321249654.png' WHERE code = 'SOCIAL_LEADERBOARD_TOP10';

-- ==================== SPECIAL (2) ====================
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_special_early_bird_1774321265615.png' WHERE code = 'SPECIAL_EARLY_BIRD';
UPDATE reward_catalog SET icon_url = '/icons/badges/badge_special_night_owl_1774321280032.png' WHERE code = 'SPECIAL_NIGHT_OWL';
