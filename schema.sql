-- *********************************************************************
-- Update Database Script
-- *********************************************************************
-- Change Log: db/changelog/db.changelog-master.xml
-- Ran at: 5/31/26, 9:54 AM
-- Against: postgres@offline:postgresql
-- Liquibase version: 5.0.1
-- *********************************************************************

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-001-enable-uuid-extension::fsa-team
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-002-create-language-table::fsa-team
CREATE TABLE language (id UUID DEFAULT uuid_generate_v4() NOT NULL, name VARCHAR(100) NOT NULL, code VARCHAR(10) NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT language_pkey PRIMARY KEY (id), UNIQUE (name), UNIQUE (code));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-003-create-country-table::fsa-team
CREATE TABLE country (id UUID DEFAULT uuid_generate_v4() NOT NULL, name VARCHAR(100) NOT NULL, code VARCHAR(10) NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT country_pkey PRIMARY KEY (id), UNIQUE (name), UNIQUE (code));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-004-create-dialect-table::fsa-team
CREATE TABLE dialect (id UUID DEFAULT uuid_generate_v4() NOT NULL, language_id UUID NOT NULL, country_id UUID, name VARCHAR(100) NOT NULL, code VARCHAR(20) NOT NULL, description TEXT, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT dialect_pkey PRIMARY KEY (id), CONSTRAINT fk_dialect_country FOREIGN KEY (country_id) REFERENCES country(id), CONSTRAINT fk_dialect_language FOREIGN KEY (language_id) REFERENCES language(id), UNIQUE (code));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-005-create-account-table::fsa-team
CREATE TABLE account (id UUID DEFAULT uuid_generate_v4() NOT NULL, username VARCHAR(50) NOT NULL, email VARCHAR(100) NOT NULL, password_hash VARCHAR(255) NOT NULL, role_code VARCHAR(20) DEFAULT 'USER' NOT NULL, native_language UUID, target_language UUID, native_dialect_id UUID, target_dialect_id UUID, is_active BOOLEAN DEFAULT TRUE NOT NULL, reset_code VARCHAR(255), reset_expires_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT account_pkey PRIMARY KEY (id), CONSTRAINT fk_account_native_lang FOREIGN KEY (native_language) REFERENCES language(id), CONSTRAINT fk_account_target_lang FOREIGN KEY (target_language) REFERENCES language(id), CONSTRAINT fk_account_target_dialect FOREIGN KEY (target_dialect_id) REFERENCES dialect(id), CONSTRAINT fk_account_native_dialect FOREIGN KEY (native_dialect_id) REFERENCES dialect(id), UNIQUE (username), UNIQUE (email));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-006-create-user-profile-table::fsa-team
CREATE TABLE user_profile (id UUID DEFAULT uuid_generate_v4() NOT NULL, account_id UUID NOT NULL, first_name VARCHAR(50) NOT NULL, last_name VARCHAR(50) NOT NULL, avatar_url VARCHAR(500), bio TEXT, total_points INTEGER DEFAULT 0 NOT NULL, total_stars INTEGER DEFAULT 0 NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT user_profile_pkey PRIMARY KEY (id), CONSTRAINT fk_user_profile_account FOREIGN KEY (account_id) REFERENCES account(id), UNIQUE (account_id));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-007-create-badge-table::fsa-team
CREATE TABLE badge (id UUID DEFAULT uuid_generate_v4() NOT NULL, name VARCHAR(100) NOT NULL, description TEXT, icon_url VARCHAR(500), min_points_required INTEGER DEFAULT 0 NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT badge_pkey PRIMARY KEY (id), UNIQUE (name));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-008-create-level-table::fsa-team
CREATE TABLE level (id UUID DEFAULT uuid_generate_v4() NOT NULL, dialect_id UUID NOT NULL, level_order INTEGER NOT NULL, name VARCHAR(100) NOT NULL, description TEXT, min_stars_required INTEGER DEFAULT 0 NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT level_pkey PRIMARY KEY (id), CONSTRAINT fk_level_dialect FOREIGN KEY (dialect_id) REFERENCES dialect(id));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-009-create-classroom-table::fsa-team
CREATE TABLE classroom (id UUID DEFAULT uuid_generate_v4() NOT NULL, teacher_id UUID NOT NULL, name VARCHAR(100) NOT NULL, code VARCHAR(20) NOT NULL, description TEXT, is_active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT classroom_pkey PRIMARY KEY (id), CONSTRAINT fk_classroom_teacher FOREIGN KEY (teacher_id) REFERENCES account(id), UNIQUE (code));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-010-create-classroom-member-table::fsa-team
CREATE TABLE classroom_member (id UUID DEFAULT uuid_generate_v4() NOT NULL, classroom_id UUID NOT NULL, account_id UUID NOT NULL, role VARCHAR(20) DEFAULT 'STUDENT' NOT NULL, joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT classroom_member_pkey PRIMARY KEY (id), CONSTRAINT fk_classroom_member_classroom FOREIGN KEY (classroom_id) REFERENCES classroom(id), CONSTRAINT fk_classroom_member_account FOREIGN KEY (account_id) REFERENCES account(id));

ALTER TABLE classroom_member ADD CONSTRAINT uk_classroom_member_classroom_account UNIQUE (classroom_id, account_id);

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-011-create-practice-session-table::fsa-team
CREATE TABLE practice_session (id UUID DEFAULT uuid_generate_v4() NOT NULL, account_id UUID NOT NULL, level_id UUID NOT NULL, session_duration INTEGER DEFAULT 0 NOT NULL, total_points_earned INTEGER DEFAULT 0 NOT NULL, total_stars_earned INTEGER DEFAULT 0 NOT NULL, is_completed BOOLEAN DEFAULT FALSE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT practice_session_pkey PRIMARY KEY (id), CONSTRAINT fk_practice_session_account FOREIGN KEY (account_id) REFERENCES account(id), CONSTRAINT fk_practice_session_level FOREIGN KEY (level_id) REFERENCES level(id));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-012-create-challenge-table::fsa-team
CREATE TABLE challenge (id UUID DEFAULT uuid_generate_v4() NOT NULL, level_id UUID NOT NULL, challenge_order INTEGER NOT NULL, word VARCHAR(100) NOT NULL, phonetic_spelling VARCHAR(500) NOT NULL, description TEXT, difficulty_level VARCHAR(20) DEFAULT 'MEDIUM' NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT challenge_pkey PRIMARY KEY (id), CONSTRAINT fk_challenge_level FOREIGN KEY (level_id) REFERENCES level(id));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-013-create-attempt-table::fsa-team
CREATE TABLE attempt (id UUID DEFAULT uuid_generate_v4() NOT NULL, practice_session_id UUID NOT NULL, challenge_id UUID NOT NULL, recorded_audio_url VARCHAR(500), is_correct BOOLEAN DEFAULT FALSE NOT NULL, similarity_score DECIMAL(5, 2) DEFAULT 0 NOT NULL, points_earned INTEGER DEFAULT 0 NOT NULL, stars_earned INTEGER DEFAULT 0 NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT attempt_pkey PRIMARY KEY (id), CONSTRAINT fk_attempt_challenge FOREIGN KEY (challenge_id) REFERENCES challenge(id), CONSTRAINT fk_attempt_practice_session FOREIGN KEY (practice_session_id) REFERENCES practice_session(id));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-014-create-attempt-phoneme-feedback-table::fsa-team
CREATE TABLE attempt_phoneme_feedback (id UUID DEFAULT uuid_generate_v4() NOT NULL, attempt_id UUID NOT NULL, phoneme VARCHAR(50) NOT NULL, expected_phoneme VARCHAR(50) NOT NULL, is_correct BOOLEAN DEFAULT FALSE NOT NULL, similarity_score DECIMAL(5, 2) DEFAULT 0 NOT NULL, feedback_message TEXT, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT attempt_phoneme_feedback_pkey PRIMARY KEY (id), CONSTRAINT fk_attempt_phoneme_feedback_attempt FOREIGN KEY (attempt_id) REFERENCES attempt(id));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-015-create-prediction-table::fsa-team
CREATE TABLE prediction (id UUID DEFAULT uuid_generate_v4() NOT NULL, attempt_id UUID NOT NULL, predicted_phoneme VARCHAR(50) NOT NULL, confidence_score DECIMAL(5, 2) DEFAULT 0 NOT NULL, model_version VARCHAR(50), created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT prediction_pkey PRIMARY KEY (id), CONSTRAINT fk_prediction_attempt FOREIGN KEY (attempt_id) REFERENCES attempt(id));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-016-create-account-badge-table::fsa-team
CREATE TABLE account_badge (account_id UUID NOT NULL, badge_id UUID NOT NULL, earned_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT account_badge_pkey PRIMARY KEY (account_id, badge_id), CONSTRAINT fk_account_badge_account FOREIGN KEY (account_id) REFERENCES account(id), CONSTRAINT fk_account_badge_badge FOREIGN KEY (badge_id) REFERENCES badge(id));

-- Changeset db/changelog/01-create-initial-schema.xml::1.0.0-017-create-indexes::fsa-team
CREATE INDEX idx_account_username ON account(username);

CREATE INDEX idx_account_email ON account(email);

CREATE INDEX idx_account_role_code ON account(role_code);

CREATE INDEX idx_user_profile_account_id ON user_profile(account_id);

CREATE INDEX idx_classroom_teacher_id ON classroom(teacher_id);

CREATE INDEX idx_classroom_code ON classroom(code);

CREATE INDEX idx_level_dialect_id ON level(dialect_id);

CREATE INDEX idx_practice_session_account_id ON practice_session(account_id);

CREATE INDEX idx_practice_session_level_id ON practice_session(level_id);

CREATE INDEX idx_challenge_level_id ON challenge(level_id);

CREATE INDEX idx_attempt_practice_session_id ON attempt(practice_session_id);

CREATE INDEX idx_attempt_challenge_id ON attempt(challenge_id);

CREATE INDEX idx_attempt_phoneme_feedback_attempt_id ON attempt_phoneme_feedback(attempt_id);

CREATE INDEX idx_prediction_attempt_id ON prediction(attempt_id);

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-001-update-user-profile::fsa-team
ALTER TABLE user_profile ADD full_name VARCHAR(255);

ALTER TABLE user_profile ADD current_streak_days INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE user_profile ADD total_experience INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE user_profile DROP COLUMN first_name;

ALTER TABLE user_profile DROP COLUMN last_name;

ALTER TABLE user_profile DROP COLUMN bio;

ALTER TABLE user_profile DROP COLUMN total_points;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-002-update-classroom::fsa-team
ALTER TABLE classroom RENAME COLUMN teacher_id TO educator_id;

ALTER TABLE classroom DROP COLUMN description;

ALTER TABLE classroom DROP COLUMN is_active;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-003-update-classroom-member::fsa-team
ALTER TABLE classroom_member RENAME COLUMN account_id TO student_id;

ALTER TABLE classroom_member DROP COLUMN role;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-004-update-practice-session::fsa-team
ALTER TABLE practice_session ADD ended_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE practice_session DROP COLUMN session_duration;

ALTER TABLE practice_session DROP COLUMN total_points_earned;

ALTER TABLE practice_session DROP COLUMN total_stars_earned;

ALTER TABLE practice_session DROP COLUMN is_completed;

ALTER TABLE practice_session DROP COLUMN created_by;

ALTER TABLE practice_session DROP COLUMN updated_by;

ALTER TABLE practice_session RENAME COLUMN created_at TO started_at;

ALTER TABLE practice_session DROP COLUMN updated_at;

ALTER TABLE practice_session DROP COLUMN level_id;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-005-update-challenge::fsa-team
ALTER TABLE challenge ADD type VARCHAR(255);

ALTER TABLE challenge ADD reference_audio_url VARCHAR(255);

ALTER TABLE challenge ADD focus_phonemes TEXT;

ALTER TABLE challenge RENAME COLUMN word TO content_text;

ALTER TABLE challenge RENAME COLUMN phonetic_spelling TO phonetic_transcription_ipa;

ALTER TABLE challenge DROP COLUMN challenge_order;

ALTER TABLE challenge DROP COLUMN description;

ALTER TABLE challenge DROP COLUMN difficulty_level;

ALTER TABLE challenge RENAME TO challenges;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-006-update-attempt::fsa-team
ALTER TABLE attempt ADD account_id UUID;

ALTER TABLE attempt ADD CONSTRAINT fk_attempt_account FOREIGN KEY (account_id) REFERENCES account (id);

ALTER TABLE attempt RENAME COLUMN practice_session_id TO session_id;

ALTER TABLE attempt RENAME COLUMN recorded_audio_url TO audio_url;

ALTER TABLE attempt RENAME COLUMN similarity_score TO score_overall;

ALTER TABLE attempt RENAME COLUMN is_correct TO is_passed;

ALTER TABLE attempt ADD latency_ms INTEGER;

ALTER TABLE attempt DROP COLUMN points_earned;

ALTER TABLE attempt DROP COLUMN stars_earned;

ALTER TABLE attempt DROP COLUMN updated_at;

ALTER TABLE attempt DROP COLUMN created_by;

ALTER TABLE attempt DROP COLUMN updated_by;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-007-update-attempt-phoneme-feedback::fsa-team
ALTER TABLE attempt_phoneme_feedback RENAME COLUMN phoneme TO phoneme_ipa;

ALTER TABLE attempt_phoneme_feedback RENAME COLUMN similarity_score TO score;

ALTER TABLE attempt_phoneme_feedback ADD sequence_order INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE attempt_phoneme_feedback ADD start_time_ms INTEGER;

ALTER TABLE attempt_phoneme_feedback ADD end_time_ms INTEGER;

ALTER TABLE attempt_phoneme_feedback DROP COLUMN expected_phoneme;

ALTER TABLE attempt_phoneme_feedback DROP COLUMN is_correct;

ALTER TABLE attempt_phoneme_feedback DROP COLUMN feedback_message;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-008-update-account::fsa-team
ALTER TABLE account ADD phone VARCHAR(20);

ALTER TABLE account ADD region VARCHAR(20);

ALTER TABLE account ADD email_verified BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE account ADD email_verify_code VARCHAR(6);

ALTER TABLE account ADD email_verify_expires_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE account ALTER COLUMN email TYPE VARCHAR(255) USING (email::VARCHAR(255));

ALTER TABLE account ALTER COLUMN reset_code TYPE VARCHAR(6) USING (reset_code::VARCHAR(6));

ALTER TABLE account DROP CONSTRAINT fk_account_native_lang;

ALTER TABLE account DROP CONSTRAINT fk_account_target_lang;

ALTER TABLE account DROP COLUMN native_language;

ALTER TABLE account DROP COLUMN target_language;

ALTER TABLE account DROP COLUMN username;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-009-update-badge::fsa-team
ALTER TABLE badge ADD code VARCHAR(50);

ALTER TABLE badge ADD criteria_json JSONB;

ALTER TABLE badge ADD CONSTRAINT uk_badge_code UNIQUE (code);

ALTER TABLE badge DROP COLUMN min_points_required;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-010-update-dialect::fsa-team
ALTER TABLE dialect DROP CONSTRAINT fk_dialect_language;

ALTER TABLE dialect DROP CONSTRAINT fk_dialect_country;

ALTER TABLE dialect DROP COLUMN language_id;

ALTER TABLE dialect DROP COLUMN country_id;

ALTER TABLE dialect DROP COLUMN code;

DROP TABLE country CASCADE;

DROP TABLE language CASCADE;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-011-update-account-badge::fsa-team
ALTER TABLE account_badge DROP COLUMN created_at;

ALTER TABLE account_badge DROP COLUMN updated_at;

ALTER TABLE account_badge DROP COLUMN created_by;

ALTER TABLE account_badge DROP COLUMN updated_by;

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-012-update-prediction::fsa-team
DROP TABLE prediction CASCADE;

CREATE TABLE predictions (id BIGINT GENERATED BY DEFAULT AS IDENTITY NOT NULL, user_id BIGINT, input_data TEXT, prediction_result TEXT, confidence_score FLOAT8, model_version VARCHAR(255), processing_time_ms BIGINT, created_at TIMESTAMP WITHOUT TIME ZONE, CONSTRAINT predictions_pkey PRIMARY KEY (id));

-- Changeset db/changelog/02-update-game-schema.xml::1.0.1-013-create-refresh-token::fsa-team
CREATE TABLE refresh_token (id UUID DEFAULT uuid_generate_v4() NOT NULL, account_id UUID NOT NULL, token VARCHAR(500) NOT NULL, expires_at TIMESTAMP WITH TIME ZONE NOT NULL, revoked BOOLEAN DEFAULT FALSE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT refresh_token_pkey PRIMARY KEY (id), UNIQUE (token));

ALTER TABLE refresh_token ADD CONSTRAINT fk_refresh_token_account FOREIGN KEY (account_id) REFERENCES account (id);

-- Changeset db/changelog/03-add-daily-analytics.xml::add-daily-analytics-table::assistant
CREATE TABLE daily_analytics (id UUID NOT NULL, record_date date NOT NULL, total_users BIGINT DEFAULT 0 NOT NULL, active_users BIGINT DEFAULT 0 NOT NULL, total_attempts BIGINT DEFAULT 0 NOT NULL, average_score DOUBLE PRECISION DEFAULT 0 NOT NULL, error_heatmaps_json TEXT, created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL, updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT daily_analytics_pkey PRIMARY KEY (id), UNIQUE (record_date));

-- Changeset db/changelog/04-add-educator-extensions.xml::20260225-01-add-level-fields::antigravity
ALTER TABLE level ADD error_tag VARCHAR(100);

ALTER TABLE level ADD ai_threshold INTEGER;

ALTER TABLE level ADD audio_url VARCHAR(500);

-- Changeset db/changelog/04-add-educator-extensions.xml::20260225-02-create-placement-rule::antigravity
-- Changeset db/changelog/04-add-educator-extensions.xml::20260225-03-create-educator-feedback::antigravity
CREATE TABLE educator_feedback (id UUID NOT NULL, attempt_id UUID NOT NULL, educator_id UUID NOT NULL, comment TEXT NOT NULL, priority VARCHAR(20) NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT educator_feedback_pkey PRIMARY KEY (id), CONSTRAINT fk_feedback_account FOREIGN KEY (educator_id) REFERENCES account(id), CONSTRAINT fk_feedback_attempt FOREIGN KEY (attempt_id) REFERENCES attempt(id));

-- Changeset db/changelog/05-seed-dialect-levels.xml::20260225-seed-dialects::antigravity
INSERT INTO dialect (id, name, description, created_at, updated_at) VALUES ('00000000-0000-0000-0001-000000000001', 'NORTH', 'Northern Vietnamese Dialect', NOW(), NOW());

INSERT INTO dialect (id, name, description, created_at, updated_at) VALUES ('00000000-0000-0000-0001-000000000002', 'CENTRAL', 'Central Vietnamese Dialect', NOW(), NOW());

INSERT INTO dialect (id, name, description, created_at, updated_at) VALUES ('00000000-0000-0000-0001-000000000003', 'SOUTH', 'Southern Vietnamese Dialect', NOW(), NOW());

-- Changeset db/changelog/05-seed-dialect-levels.xml::20260225-seed-sample-levels::antigravity
INSERT INTO level (id, dialect_id, level_order, name, description, min_stars_required, ai_threshold, created_at, updated_at) VALUES (uuid_generate_v4(), '00000000-0000-0000-0001-000000000003', '1', 'Chào hỏi cơ bản (Nam)', 'Học cách chào hỏi với giọng miền Nam', '0', '70', NOW(), NOW());

INSERT INTO level (id, dialect_id, level_order, name, description, min_stars_required, ai_threshold, created_at, updated_at) VALUES (uuid_generate_v4(), '00000000-0000-0000-0001-000000000003', '2', 'Số đếm (Nam)', 'Cách phát âm các con số giọng miền Nam', '3', '75', NOW(), NOW());

-- Changeset db/changelog/06-seed-test-accounts.xml::20260225-seed-test-accounts::antigravity
INSERT INTO account (id, email, password_hash, role_code, is_active, created_at, updated_at) VALUES ('00000000-0000-0000-0002-000000000001', 'educator@fsa.com', '$2a$10$r8VvQXf.w5G.xK6qKz7u6.Y7v6uB0v4f9w5uB0v4f9w5uB0v4f9w5', 'EDUCATOR', TRUE, NOW(), NOW());

INSERT INTO account (id, email, password_hash, role_code, is_active, created_at, updated_at) VALUES ('00000000-0000-0000-0002-000000000002', 'student@fsa.com', '$2a$10$r8VvQXf.w5G.xK6qKz7u6.Y7v6uB0v4f9w5uB0v4f9w5uB0v4f9w5', 'USER', TRUE, NOW(), NOW());

-- Changeset db/changelog/06-seed-test-accounts.xml::20260225-seed-test-classroom::antigravity
INSERT INTO classroom (id, educator_id, name, code, created_at, updated_at) VALUES ('00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0002-000000000001', 'Lớp Tiếng Việt Cơ Bản 1', 'VN101', NOW(), NOW());

INSERT INTO classroom_member (id, classroom_id, student_id, joined_at, created_at, updated_at) VALUES (uuid_generate_v4(), '00000000-0000-0000-0003-000000000001', '00000000-0000-0000-0002-000000000002', NOW(), NOW(), NOW());

-- Changeset db/changelog/07-add-content-approval-fields.xml::20260226-add-content-approval-fields::antigravity
ALTER TABLE level ADD status VARCHAR(20) DEFAULT 'APPROVED' NOT NULL;

ALTER TABLE level ADD rejection_reason TEXT;

ALTER TABLE challenges ADD status VARCHAR(20) DEFAULT 'APPROVED' NOT NULL;

ALTER TABLE challenges ADD rejection_reason TEXT;

-- Changeset db/changelog/08-database-iso-fixes-and-error-tag.xml::20260226-01-rename-plural-tables::antigravity
ALTER TABLE challenges RENAME TO challenge;

ALTER TABLE predictions RENAME TO prediction;

-- Changeset db/changelog/08-database-iso-fixes-and-error-tag.xml::20260226-02-alter-timezone-types::antigravity
ALTER TABLE daily_analytics ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING (created_at::TIMESTAMP WITH TIME ZONE);

ALTER TABLE daily_analytics ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE USING (updated_at::TIMESTAMP WITH TIME ZONE);

ALTER TABLE prediction ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE USING (created_at::TIMESTAMP WITH TIME ZONE);

ALTER TABLE prediction ADD updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE prediction ADD created_by VARCHAR(50);

ALTER TABLE prediction ADD updated_by VARCHAR(50);

-- Changeset db/changelog/08-database-iso-fixes-and-error-tag.xml::20260226-03-create-error-tag-table::antigravity
CREATE TABLE error_tag (id UUID NOT NULL, tag_code VARCHAR(50) NOT NULL, name VARCHAR(100) NOT NULL, description TEXT, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT error_tag_pkey PRIMARY KEY (id), UNIQUE (tag_code));

ALTER TABLE level ADD error_tag_id UUID;

ALTER TABLE level ADD CONSTRAINT fk_level_error_tag FOREIGN KEY (error_tag_id) REFERENCES error_tag (id);

ALTER TABLE level DROP COLUMN error_tag;

-- Changeset db/changelog/08-database-iso-fixes-and-error-tag.xml::20260226-04-seed-error-tags::antigravity
INSERT INTO error_tag (id, tag_code, name, description) VALUES (uuid_generate_v4(), 'L_N', 'Ngọng L - N', 'Lỗi phát âm nhầm lẫn giữa L và N phổ biến ở miền Bắc');

INSERT INTO error_tag (id, tag_code, name, description) VALUES (uuid_generate_v4(), 'S_X', 'Ngọng S - X', 'Lỗi phát âm chưa phân biệt âm cuốn lưỡi S và X');

INSERT INTO error_tag (id, tag_code, name, description) VALUES (uuid_generate_v4(), 'CH_TR', 'Ngọng CH - TR', 'Lỗi phát âm nhầm lẫn giữa CH và TR');

-- Changeset db/changelog/09-add-missing-relationships.xml::20260226-add-missing-relationships::antigravity
-- Changeset db/changelog/09-add-missing-relationships.xml::20260226-recreate-prediction-table::antigravity
DROP TABLE prediction CASCADE;

CREATE TABLE prediction (id UUID DEFAULT uuid_generate_v4() NOT NULL, user_id UUID NOT NULL, challenge_id UUID NOT NULL, input_data TEXT, prediction_result TEXT, confidence_score FLOAT8, model_version VARCHAR(255), processing_time_ms BIGINT, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT prediction_pkey PRIMARY KEY (id), CONSTRAINT fk_prediction_challenge FOREIGN KEY (challenge_id) REFERENCES challenge(id), CONSTRAINT fk_prediction_user FOREIGN KEY (user_id) REFERENCES account(id));

-- Changeset db/changelog/10-add-content-approval-history.xml::20260226-add-content-approval-history::antigravity
CREATE TABLE content_approval_history (id UUID DEFAULT uuid_generate_v4() NOT NULL, content_type VARCHAR(20) NOT NULL, content_id UUID NOT NULL, status VARCHAR(20) NOT NULL, comment TEXT, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT content_approval_history_pkey PRIMARY KEY (id));

CREATE INDEX idx_approval_history_content ON content_approval_history(content_type, content_id);

-- Changeset db/changelog/11-add-draft-live-content.xml::20260305-add-draft-live-columns::antigravity
ALTER TABLE level ADD parent_id UUID;

ALTER TABLE level ADD draft_id UUID;

ALTER TABLE level ADD CONSTRAINT fk_level_parent FOREIGN KEY (parent_id) REFERENCES level (id);

ALTER TABLE level ADD CONSTRAINT fk_level_draft FOREIGN KEY (draft_id) REFERENCES level (id);

ALTER TABLE challenge ADD parent_id UUID;

ALTER TABLE challenge ADD draft_id UUID;

ALTER TABLE challenge ADD CONSTRAINT fk_challenge_parent FOREIGN KEY (parent_id) REFERENCES challenge (id);

ALTER TABLE challenge ADD CONSTRAINT fk_challenge_draft FOREIGN KEY (draft_id) REFERENCES challenge (id);

-- Changeset db/changelog/12-add-content-snapshot-to-history.xml::12-add-content-snapshot-to-history::system
ALTER TABLE content_approval_history ADD content_snapshot TEXT;

COMMENT ON COLUMN content_approval_history.content_snapshot IS 'JSON string representing the content snapshot at this point in time';

-- Changeset db/changelog/13-seed-error-tag-rules.xml::20260306-seed-placement-rules::antigravity
INSERT INTO error_tag (id, tag_code, name, description) VALUES ('00000000-0000-0000-0002-000000000001', 'V_D_CONFUSION', 'Lẫn lộn V - D', 'Lỗi phát âm nhầm lẫn giữa V và D phổ biến ở miền Nam và Trung');

INSERT INTO error_tag (id, tag_code, name, description) VALUES ('00000000-0000-0000-0002-000000000002', 'TONE_INTERROGATIVE', 'Sai thanh hỏi/ngã', 'Lỗi phát âm không phân biệt rõ thanh hỏi và thanh ngã');

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-001-extend-user-profile-streak::fsa-team
ALTER TABLE user_profile ADD max_streak_days INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE user_profile ADD last_activity_date date;

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-002-extend-badge-table::fsa-team
ALTER TABLE badge ADD category VARCHAR(50) DEFAULT 'GENERAL' NOT NULL;

ALTER TABLE badge ADD rarity VARCHAR(20) DEFAULT 'BRONZE' NOT NULL;

ALTER TABLE badge ADD xp_reward INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE badge ADD is_active BOOLEAN DEFAULT TRUE NOT NULL;

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-003-create-achievement-table::fsa-team
CREATE TABLE achievement (id UUID DEFAULT uuid_generate_v4() NOT NULL, code VARCHAR(100) NOT NULL, name VARCHAR(200) NOT NULL, description TEXT, category VARCHAR(50) DEFAULT 'GENERAL' NOT NULL, icon_url VARCHAR(500), criteria_json JSONB NOT NULL, xp_reward INTEGER DEFAULT 0 NOT NULL, is_active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT achievement_pkey PRIMARY KEY (id), UNIQUE (code));

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-004-create-account-achievement-table::fsa-team
CREATE TABLE account_achievement (id UUID DEFAULT uuid_generate_v4() NOT NULL, account_id UUID NOT NULL, achievement_id UUID NOT NULL, status VARCHAR(20) DEFAULT 'LOCKED' NOT NULL, progress_value INTEGER DEFAULT 0 NOT NULL, unlocked_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT account_achievement_pkey PRIMARY KEY (id), CONSTRAINT fk_acct_achievement_achievement FOREIGN KEY (achievement_id) REFERENCES achievement(id), CONSTRAINT fk_acct_achievement_account FOREIGN KEY (account_id) REFERENCES account(id));

ALTER TABLE account_achievement ADD CONSTRAINT uk_account_achievement UNIQUE (account_id, achievement_id);

CREATE INDEX idx_account_achievement_account_id ON account_achievement(account_id);

CREATE INDEX idx_account_achievement_status ON account_achievement(status);

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-005-create-daily-challenge-table::fsa-team
CREATE TABLE daily_challenge (id UUID DEFAULT uuid_generate_v4() NOT NULL, challenge_date date NOT NULL, challenge_id UUID NOT NULL, dialect_id UUID, difficulty VARCHAR(20) DEFAULT 'MEDIUM' NOT NULL, bonus_xp INTEGER DEFAULT 0 NOT NULL, is_active BOOLEAN DEFAULT TRUE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT daily_challenge_pkey PRIMARY KEY (id), CONSTRAINT fk_daily_challenge_challenge FOREIGN KEY (challenge_id) REFERENCES challenge(id), CONSTRAINT fk_daily_challenge_dialect FOREIGN KEY (dialect_id) REFERENCES dialect(id));

ALTER TABLE daily_challenge ADD CONSTRAINT uk_daily_challenge_date_challenge UNIQUE (challenge_date, challenge_id);

CREATE INDEX idx_daily_challenge_date ON daily_challenge(challenge_date);

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-006-create-account-daily-challenge-table::fsa-team
CREATE TABLE account_daily_challenge (id UUID DEFAULT uuid_generate_v4() NOT NULL, account_id UUID NOT NULL, daily_challenge_id UUID NOT NULL, status VARCHAR(20) DEFAULT 'PENDING' NOT NULL, score_achieved DECIMAL(5, 2) DEFAULT 0, xp_earned INTEGER DEFAULT 0 NOT NULL, attempt_id UUID, completed_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT account_daily_challenge_pkey PRIMARY KEY (id), CONSTRAINT fk_adc_account FOREIGN KEY (account_id) REFERENCES account(id), CONSTRAINT fk_adc_daily_challenge FOREIGN KEY (daily_challenge_id) REFERENCES daily_challenge(id), CONSTRAINT fk_adc_attempt FOREIGN KEY (attempt_id) REFERENCES attempt(id));

ALTER TABLE account_daily_challenge ADD CONSTRAINT uk_account_daily_challenge UNIQUE (account_id, daily_challenge_id);

CREATE INDEX idx_adc_account_id ON account_daily_challenge(account_id);

CREATE INDEX idx_adc_status ON account_daily_challenge(status);

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-007-create-leaderboard-table::fsa-team
CREATE TABLE leaderboard (id UUID DEFAULT uuid_generate_v4() NOT NULL, scope VARCHAR(30) DEFAULT 'GLOBAL' NOT NULL, period_type VARCHAR(20) DEFAULT 'WEEKLY' NOT NULL, period_start date NOT NULL, period_end date NOT NULL, region_code VARCHAR(30), classroom_id UUID, dialect_id UUID, is_finalized BOOLEAN DEFAULT FALSE NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT leaderboard_pkey PRIMARY KEY (id), CONSTRAINT fk_leaderboard_classroom FOREIGN KEY (classroom_id) REFERENCES classroom(id), CONSTRAINT fk_leaderboard_dialect FOREIGN KEY (dialect_id) REFERENCES dialect(id));

CREATE INDEX idx_leaderboard_scope_period ON leaderboard(scope, period_type, period_start);

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-008-create-leaderboard-entry-table::fsa-team
CREATE TABLE leaderboard_entry (id UUID DEFAULT uuid_generate_v4() NOT NULL, leaderboard_id UUID NOT NULL, account_id UUID NOT NULL, rank_position INTEGER NOT NULL, total_xp INTEGER DEFAULT 0 NOT NULL, total_stars INTEGER DEFAULT 0 NOT NULL, challenges_completed INTEGER DEFAULT 0 NOT NULL, average_score DECIMAL(5, 2) DEFAULT 0 NOT NULL, streak_days INTEGER DEFAULT 0 NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT leaderboard_entry_pkey PRIMARY KEY (id), CONSTRAINT fk_lb_entry_account FOREIGN KEY (account_id) REFERENCES account(id), CONSTRAINT fk_lb_entry_leaderboard FOREIGN KEY (leaderboard_id) REFERENCES leaderboard(id));

ALTER TABLE leaderboard_entry ADD CONSTRAINT uk_leaderboard_entry UNIQUE (leaderboard_id, account_id);

CREATE INDEX idx_lb_entry_leaderboard_rank ON leaderboard_entry(leaderboard_id, rank_position);

CREATE INDEX idx_lb_entry_account_id ON leaderboard_entry(account_id);

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-009-create-tournament-table::fsa-team
CREATE TABLE tournament (id UUID DEFAULT uuid_generate_v4() NOT NULL, name VARCHAR(200) NOT NULL, description TEXT, type VARCHAR(30) DEFAULT 'WEEKLY' NOT NULL, status VARCHAR(20) DEFAULT 'UPCOMING' NOT NULL, dialect_id UUID, region_code VARCHAR(30), starts_at TIMESTAMP WITH TIME ZONE NOT NULL, ends_at TIMESTAMP WITH TIME ZONE NOT NULL, max_participants INTEGER, banner_url VARCHAR(500), prize_config_json JSONB, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT tournament_pkey PRIMARY KEY (id), CONSTRAINT fk_tournament_dialect FOREIGN KEY (dialect_id) REFERENCES dialect(id));

CREATE INDEX idx_tournament_status ON tournament(status);

CREATE INDEX idx_tournament_starts_at ON tournament(starts_at);

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-010-create-tournament-participant-table::fsa-team
CREATE TABLE tournament_participant (id UUID DEFAULT uuid_generate_v4() NOT NULL, tournament_id UUID NOT NULL, account_id UUID NOT NULL, rank_position INTEGER, total_xp INTEGER DEFAULT 0 NOT NULL, challenges_completed INTEGER DEFAULT 0 NOT NULL, average_score DECIMAL(5, 2) DEFAULT 0 NOT NULL, status VARCHAR(20) DEFAULT 'REGISTERED' NOT NULL, joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT tournament_participant_pkey PRIMARY KEY (id), CONSTRAINT fk_tp_account FOREIGN KEY (account_id) REFERENCES account(id), CONSTRAINT fk_tp_tournament FOREIGN KEY (tournament_id) REFERENCES tournament(id));

ALTER TABLE tournament_participant ADD CONSTRAINT uk_tournament_participant UNIQUE (tournament_id, account_id);

CREATE INDEX idx_tp_tournament_rank ON tournament_participant(tournament_id, rank_position);

CREATE INDEX idx_tp_account_id ON tournament_participant(account_id);

-- Changeset db/changelog/14-add-achievement-and-leaderboard-tables.xml::20260310-011-create-friendship-table::fsa-team
CREATE TABLE friendship (id UUID DEFAULT uuid_generate_v4() NOT NULL, requester_id UUID NOT NULL, addressee_id UUID NOT NULL, status VARCHAR(20) DEFAULT 'PENDING' NOT NULL, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, CONSTRAINT friendship_pkey PRIMARY KEY (id), CONSTRAINT fk_friendship_addressee FOREIGN KEY (addressee_id) REFERENCES account(id), CONSTRAINT fk_friendship_requester FOREIGN KEY (requester_id) REFERENCES account(id));

ALTER TABLE friendship ADD CONSTRAINT uk_friendship_pair UNIQUE (requester_id, addressee_id);

CREATE INDEX idx_friendship_requester ON friendship(requester_id);

CREATE INDEX idx_friendship_addressee ON friendship(addressee_id);

CREATE INDEX idx_friendship_status ON friendship(status);

-- Changeset db/changelog/15-seed-badges.xml::20260310-seed-badges::fsa-team
INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STREAK_3', 'Khởi Đầu Tốt', 'Duy trì chuỗi học tập 3 ngày liên tiếp', '/badges/streak_3.png', 'STREAK', 'BRONZE', 50, '{"type": "streak_days", "threshold": 3}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STREAK_7', 'Kiên Trì Một Tuần', 'Duy trì chuỗi học tập 7 ngày liên tiếp', '/badges/streak_7.png', 'STREAK', 'BRONZE', 100, '{"type": "streak_days", "threshold": 7}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STREAK_14', 'Ngọn Lửa Hai Tuần', 'Duy trì chuỗi học tập 14 ngày liên tiếp', '/badges/streak_14.png', 'STREAK', 'SILVER', 200, '{"type": "streak_days", "threshold": 14}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STREAK_30', 'Học Giả Tháng Vàng', 'Duy trì chuỗi học tập 30 ngày liên tiếp', '/badges/streak_30.png', 'STREAK', 'GOLD', 500, '{"type": "streak_days", "threshold": 30}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STREAK_60', 'Bất Khuất', 'Duy trì chuỗi học tập 60 ngày liên tiếp', '/badges/streak_60.png', 'STREAK', 'PLATINUM', 1000, '{"type": "streak_days", "threshold": 60}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STREAK_100', 'Trăm Ngày Vàng Son', 'Duy trì chuỗi học tập 100 ngày liên tiếp', '/badges/streak_100.png', 'STREAK', 'DIAMOND', 2000, '{"type": "streak_days", "threshold": 100}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STREAK_365', 'Truyền Thuyết Năm Tháng', 'Duy trì chuỗi học tập 365 ngày liên tiếp - Một huyền thoại thực sự!', '/badges/streak_365.png', 'STREAK', 'DIAMOND', 10000, '{"type": "streak_days", "threshold": 365}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_FIRST_PERFECT', 'Hoàn Hảo Đầu Tiên', 'Đạt điểm tuyệt đối 100% trong lần đầu tiên', '/badges/score_first_perfect.png', 'SCORE', 'BRONZE', 150, '{"type": "perfect_score_count", "threshold": 1}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_5_PERFECT', 'Ngũ Tuyệt', 'Đạt điểm tuyệt đối 5 lần', '/badges/score_5_perfect.png', 'SCORE', 'SILVER', 250, '{"type": "perfect_score_count", "threshold": 5}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_20_PERFECT', 'Thiên Tài Phát Âm', 'Đạt điểm tuyệt đối 20 lần', '/badges/score_20_perfect.png', 'SCORE', 'GOLD', 600, '{"type": "perfect_score_count", "threshold": 20}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_HIGH_AVG', 'Ổn Định Đỉnh Cao', 'Duy trì điểm trung bình >= 90% trong 10 thử thách liên tiếp', '/badges/score_high_avg.png', 'SCORE', 'GOLD', 500, '{"type": "avg_score_streak", "threshold": 90, "consecutive": 10}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_FIRST_CHALLENGE', 'Bước Đầu Tiên', 'Hoàn thành thử thách đầu tiên', '/badges/learn_first_challenge.png', 'LEARNING', 'BRONZE', 30, '{"type": "challenges_completed", "threshold": 1}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_10_CHALLENGES', 'Học Viên Năng Động', 'Hoàn thành 10 thử thách', '/badges/learn_10_challenges.png', 'LEARNING', 'BRONZE', 100, '{"type": "challenges_completed", "threshold": 10}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_50_CHALLENGES', 'Luyện Tập Chăm Chỉ', 'Hoàn thành 50 thử thách', '/badges/learn_50_challenges.png', 'LEARNING', 'SILVER', 300, '{"type": "challenges_completed", "threshold": 50}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_100_CHALLENGES', 'Bách Thử Bách Thắng', 'Hoàn thành 100 thử thách', '/badges/learn_100_challenges.png', 'LEARNING', 'GOLD', 700, '{"type": "challenges_completed", "threshold": 100}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_500_CHALLENGES', 'Người Chinh Phục', 'Hoàn thành 500 thử thách', '/badges/learn_500_challenges.png', 'LEARNING', 'PLATINUM', 2000, '{"type": "challenges_completed", "threshold": 500}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_LEVEL_1', 'Vượt Cấp Đầu Tiên', 'Hoàn thành level đầu tiên', '/badges/learn_level_up.png', 'LEARNING', 'BRONZE', 80, '{"type": "levels_completed", "threshold": 1}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_5_LEVELS', 'Leo Thang Xuất Sắc', 'Hoàn thành 5 level', '/badges/learn_5_levels.png', 'LEARNING', 'SILVER', 400, '{"type": "levels_completed", "threshold": 5}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_EARLY_BIRD', 'Chim Sớm', 'Hoàn thành thử thách trước 7 giờ sáng 5 lần', '/badges/learn_early_bird.png', 'LEARNING', 'SILVER', 200, '{"type": "early_bird_completions", "threshold": 5, "hour_before": 7}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_NIGHT_OWL', 'Cú Đêm', 'Hoàn thành thử thách sau 10 giờ tối 5 lần', '/badges/learn_night_owl.png', 'LEARNING', 'SILVER', 200, '{"type": "night_owl_completions", "threshold": 5, "hour_after": 22}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_SPEED_DEMON', 'Tốc Chiến Tốc Thắng', 'Hoàn thành thử thách trong vòng 5 giây và đạt điểm >= 90%', '/badges/learn_speed_demon.png', 'LEARNING', 'GOLD', 400, '{"type": "speed_completion", "max_latency_ms": 5000, "min_score": 90}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_EXPLORER_NORTH', 'Nhà Khám Phá Miền Bắc', 'Hoàn thành 20 thử thách giọng Bắc', '/badges/learn_explorer_north.png', 'LEARNING', 'SILVER', 250, '{"type": "dialect_challenges", "dialect_code": "VI-N", "threshold": 20}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_EXPLORER_SOUTH', 'Nhà Khám Phá Miền Nam', 'Hoàn thành 20 thử thách giọng Nam', '/badges/learn_explorer_south.png', 'LEARNING', 'SILVER', 250, '{"type": "dialect_challenges", "dialect_code": "VI-S", "threshold": 20}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_EXPLORER_CENTRAL', 'Nhà Khám Phá Miền Trung', 'Hoàn thành 20 thử thách giọng Trung', '/badges/learn_explorer_central.png', 'LEARNING', 'SILVER', 250, '{"type": "dialect_challenges", "dialect_code": "VI-C", "threshold": 20}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'DAILY_FIRST_COMPLETE', 'Chiến Binh Ngày Đầu', 'Hoàn thành thử thách hàng ngày lần đầu tiên', '/badges/daily_first.png', 'CHALLENGE', 'BRONZE', 50, '{"type": "daily_challenges_completed", "threshold": 1}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'DAILY_7', 'Thử Thách 7 Ngày', 'Hoàn thành 7 thử thách hàng ngày', '/badges/daily_7.png', 'CHALLENGE', 'SILVER', 200, '{"type": "daily_challenges_completed", "threshold": 7}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'DAILY_30', 'Thử Thách Tháng Thép', 'Hoàn thành 30 thử thách hàng ngày', '/badges/daily_30.png', 'CHALLENGE', 'GOLD', 600, '{"type": "daily_challenges_completed", "threshold": 30}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'DAILY_PERFECT_WEEK', 'Tuần Hoàn Hảo', 'Hoàn thành thử thách hàng ngày 7 ngày liên tiếp với điểm >= 90%', '/badges/daily_perfect_week.png', 'CHALLENGE', 'PLATINUM', 1000, '{"type": "daily_streak_perfect", "consecutive_days": 7, "min_score": 90}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SOCIAL_FIRST_FRIEND', 'Kết Nối Đầu Tiên', 'Kết bạn với người dùng đầu tiên', '/badges/social_first_friend.png', 'SOCIAL', 'BRONZE', 50, '{"type": "friends_count", "threshold": 1}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SOCIAL_5_FRIENDS', 'Cộng Đồng Nhỏ', 'Kết bạn với 5 người dùng', '/badges/social_5_friends.png', 'SOCIAL', 'BRONZE', 100, '{"type": "friends_count", "threshold": 5}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SOCIAL_20_FRIENDS', 'Đại Sứ Cộng Đồng', 'Kết bạn với 20 người dùng', '/badges/social_20_friends.png', 'SOCIAL', 'GOLD', 400, '{"type": "friends_count", "threshold": 20}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LB_FIRST_TOP10', 'Top 10', 'Lọt vào top 10 bảng xếp hạng lần đầu tiên', '/badges/lb_top10.png', 'CHALLENGE', 'SILVER', 300, '{"type": "leaderboard_top_rank", "threshold": 10}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LB_FIRST_TOP3', 'Bục Vinh Danh', 'Lọt vào top 3 bảng xếp hạng lần đầu tiên', '/badges/lb_top3.png', 'CHALLENGE', 'GOLD', 700, '{"type": "leaderboard_top_rank", "threshold": 3}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LB_CHAMPION', 'Nhà Vô Địch', 'Đứng số 1 bảng xếp hạng hàng tuần', '/badges/lb_champion.png', 'CHALLENGE', 'PLATINUM', 2000, '{"type": "leaderboard_rank_1", "period": "WEEKLY"}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'TOURNAMENT_WINNER', 'Kiếm Sĩ Giải Đấu', 'Vô địch một giải đấu', '/badges/tournament_winner.png', 'CHALLENGE', 'DIAMOND', 3000, '{"type": "tournament_wins", "threshold": 1}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'TOURNAMENT_PARTICIPANT', 'Tham Chiến', 'Tham gia giải đấu lần đầu tiên', '/badges/tournament_participant.png', 'CHALLENGE', 'BRONZE', 100, '{"type": "tournaments_joined", "threshold": 1}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SPECIAL_BETA_USER', 'Người Tiên Phong', 'Là một trong những người dùng đầu tiên của FSA 2026', '/badges/special_beta.png', 'SPECIAL', 'DIAMOND', 500, '{"type": "manual_grant", "reason": "beta_user"}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SPECIAL_PROFILE_COMPLETE', 'Hồ Sơ Hoàn Chỉnh', 'Hoàn thiện 100% hồ sơ cá nhân', '/badges/special_profile.png', 'SPECIAL', 'BRONZE', 80, '{"type": "profile_complete", "threshold": 100}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SPECIAL_COMEBACK', 'Trở Lại Mạnh Mẽ', 'Quay trở lại học sau 14 ngày vắng mặt', '/badges/special_comeback.png', 'SPECIAL', 'SILVER', 150, '{"type": "comeback_after_days", "threshold": 14}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SPECIAL_TET', 'Tết Học Tiếng Việt', 'Hoàn thành thử thách vào dịp Tết Nguyên Đán', '/badges/special_tet.png', 'SPECIAL', 'GOLD', 300, '{"type": "seasonal_event", "event": "TET"}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SPECIAL_QUIZ_MASTER', 'Bậc Thầy Kiến Thức', 'Đạt 100% trong 3 thử thách HARD liên tiếp', '/badges/special_quiz_master.png', 'SPECIAL', 'PLATINUM', 1500, '{"type": "hard_perfect_streak", "consecutive": 3}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'XP_1000', 'Học Sinh Tiềm Năng', 'Tích lũy 1,000 điểm kinh nghiệm', '/badges/xp_1000.png', 'LEARNING', 'BRONZE', 50, '{"type": "total_xp", "threshold": 1000}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'XP_5000', 'Người Học Nhiệt Huyết', 'Tích lũy 5,000 điểm kinh nghiệm', '/badges/xp_5000.png', 'LEARNING', 'SILVER', 200, '{"type": "total_xp", "threshold": 5000}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'XP_20000', 'Chuyên Gia Ngôn Ngữ', 'Tích lũy 20,000 điểm kinh nghiệm', '/badges/xp_20000.png', 'LEARNING', 'GOLD', 500, '{"type": "total_xp", "threshold": 20000}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'XP_100000', 'Đại Sư Phát Âm', 'Tích lũy 100,000 điểm kinh nghiệm', '/badges/xp_100000.png', 'LEARNING', 'DIAMOND', 2000, '{"type": "total_xp", "threshold": 100000}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STARS_10', 'Ngôi Sao Sáng', 'Thu thập 10 ngôi sao', '/badges/stars_10.png', 'SCORE', 'BRONZE', 60, '{"type": "total_stars", "threshold": 10}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STARS_50', 'Bầu Trời Ngôi Sao', 'Thu thập 50 ngôi sao', '/badges/stars_50.png', 'SCORE', 'SILVER', 250, '{"type": "total_stars", "threshold": 50}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'STARS_200', 'Dải Ngân Hà', 'Thu thập 200 ngôi sao', '/badges/stars_200.png', 'SCORE', 'PLATINUM', 1000, '{"type": "total_stars", "threshold": 200}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SESSION_MARATHON', 'Phiên Học Thần Thánh', 'Hoàn thành phiên học kéo dài > 60 phút', '/badges/session_marathon.png', 'LEARNING', 'GOLD', 350, '{"type": "session_duration_minutes", "threshold": 60}', TRUE, NOW(), NOW());

INSERT INTO badge (id, code, name, description, icon_url, category, rarity, xp_reward, criteria_json, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SESSION_10_SESSIONS', 'Thói Quen Lành Mạnh', 'Hoàn thành 10 phiên luyện tập', '/badges/session_10.png', 'LEARNING', 'SILVER', 200, '{"type": "total_sessions", "threshold": 10}', TRUE, NOW(), NOW());

-- Changeset db/changelog/16-add-quiz-tables.xml::16-add-quiz-tables::claude
CREATE TABLE quiz (id UUID NOT NULL, level_id UUID NOT NULL, title VARCHAR(255) NOT NULL, description TEXT, instructions TEXT, passing_score INTEGER NOT NULL, time_limit_minutes INTEGER, question_count INTEGER, status VARCHAR(20) NOT NULL, parent_id UUID, draft_id UUID, rejection_reason TEXT, created_at TIMESTAMP WITHOUT TIME ZONE, updated_at TIMESTAMP WITHOUT TIME ZONE, deleted_at TIMESTAMP WITHOUT TIME ZONE, created_by VARCHAR(255), updated_by VARCHAR(255), CONSTRAINT quiz_pkey PRIMARY KEY (id));

ALTER TABLE quiz ADD CONSTRAINT fk_quiz_level FOREIGN KEY (level_id) REFERENCES level (id);

ALTER TABLE quiz ADD CONSTRAINT fk_quiz_parent FOREIGN KEY (parent_id) REFERENCES quiz (id);

CREATE TABLE quiz_question (id UUID NOT NULL, quiz_id UUID NOT NULL, skill_type VARCHAR(50) NOT NULL, difficulty VARCHAR(20), question_order INTEGER NOT NULL, points INTEGER NOT NULL, content_data JSONB NOT NULL, created_at TIMESTAMP WITHOUT TIME ZONE, updated_at TIMESTAMP WITHOUT TIME ZONE, deleted_at TIMESTAMP WITHOUT TIME ZONE, created_by VARCHAR(255), updated_by VARCHAR(255), CONSTRAINT quiz_question_pkey PRIMARY KEY (id));

ALTER TABLE quiz_question ADD CONSTRAINT fk_quiz_question_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (id) ON DELETE CASCADE;

CREATE TABLE quiz_attempt (id UUID NOT NULL, quiz_id UUID NOT NULL, student_id UUID NOT NULL, score INTEGER NOT NULL, completed BOOLEAN NOT NULL, started_at TIMESTAMP WITHOUT TIME ZONE, completed_at TIMESTAMP WITHOUT TIME ZONE, answers_snapshot TEXT, quiz_type VARCHAR(20), time_taken_seconds INTEGER, created_at TIMESTAMP WITHOUT TIME ZONE, updated_at TIMESTAMP WITHOUT TIME ZONE, deleted_at TIMESTAMP WITHOUT TIME ZONE, created_by VARCHAR(255), updated_by VARCHAR(255), CONSTRAINT quiz_attempt_pkey PRIMARY KEY (id));

ALTER TABLE quiz_attempt ADD CONSTRAINT fk_quiz_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quiz (id) ON DELETE CASCADE;

ALTER TABLE quiz_attempt ADD CONSTRAINT fk_quiz_attempt_student FOREIGN KEY (student_id) REFERENCES account (id);

-- Changeset db/changelog/17-standardize-challenge-quiz-centralization.xml::17-standardize-challenge-quiz-centralization::antigravity
ALTER TABLE challenge ADD skill_type VARCHAR(50);

ALTER TABLE challenge ADD difficulty VARCHAR(20);

ALTER TABLE challenge ALTER COLUMN  type DROP NOT NULL;

ALTER TABLE quiz_question ADD challenge_id UUID;

ALTER TABLE quiz_question ADD CONSTRAINT fk_quiz_question_challenge FOREIGN KEY (challenge_id) REFERENCES challenge (id) ON DELETE SET NULL;

ALTER TABLE quiz_question DROP COLUMN content_data;

-- Changeset db/changelog/18-consolidate-user-profile.xml::18-consolidate-user-profile::antigravity
ALTER TABLE account ADD full_name VARCHAR(255);

ALTER TABLE account ADD avatar_url VARCHAR(500);

ALTER TABLE account ADD total_stars INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE account ADD current_streak_days INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE account ADD total_experience INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE account ADD max_streak_days INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE account ADD last_activity_date date;

UPDATE account a
            SET 
                full_name = p.full_name,
                avatar_url = p.avatar_url,
                total_stars = p.total_stars,
                current_streak_days = p.current_streak_days,
                total_experience = p.total_experience,
                max_streak_days = p.max_streak_days,
                last_activity_date = p.last_activity_date
            FROM user_profile p
            WHERE a.id = p.account_id;

DROP TABLE user_profile;

-- Changeset db/changelog/19-add-reward-type-to-achievement.xml::19-add-reward-type-to-achievement::antigravity
ALTER TABLE achievement ADD reward_type VARCHAR(20) DEFAULT 'ACHIEVEMENT';

-- Changeset db/changelog/20-add-content-snapshot-to-attempt.xml::20-add-content-snapshot-to-attempt::antigravity
ALTER TABLE attempt ADD content_snapshot_json JSONB;

-- Changeset db/changelog/21-add-quiz-attempt-to-attempt.xml::21-add-quiz-attempt-to-attempt::antigravity
ALTER TABLE attempt ADD quiz_attempt_id UUID;

ALTER TABLE attempt ADD CONSTRAINT fk_attempt_quiz_attempt FOREIGN KEY (quiz_attempt_id) REFERENCES quiz_attempt (id) ON DELETE CASCADE;

-- Changeset db/changelog/22-make-attempt-session-nullable.xml::22-make-attempt-session-nullable::antigravity
ALTER TABLE attempt ALTER COLUMN  session_id DROP NOT NULL;

-- Changeset db/changelog/23-merge-badges-data.xml::23-merge-badges-data::antigravity
INSERT INTO achievement (id, code, name, description, category, icon_url, criteria_json, xp_reward, is_active, reward_type, created_at, updated_at)
            SELECT 
                id, 
                REPLACE(LOWER(name), ' ', '_') as code, 
                name, 
                description, 
                'GENERAL', 
                icon_url, 
                criteria_json, 
                0, 
                true, 
                'BADGE', 
                created_at, 
                updated_at 
            FROM badge;

INSERT INTO account_achievement (id, account_id, achievement_id, progress_value, status, created_at, unlocked_at, updated_at)
            SELECT uuid_generate_v4(), account_id, badge_id, 1, 'UNLOCKED', earned_at, earned_at, earned_at FROM account_badge;

DROP TABLE account_badge;

DROP TABLE badge;

-- Changeset db/changelog/24-merge-prediction-and-phoneme-feedback.xml::24-merge-prediction-and-phoneme-feedback::antigravity
ALTER TABLE attempt ADD ai_insights_json JSONB;

ALTER TABLE attempt ADD phoneme_feedback_json JSONB;

UPDATE attempt a 
            SET ai_insights_json = (
                SELECT json_build_object(
                    'confidence_score', p.confidence_score, 
                    'prediction_result', p.prediction_result, 
                    'model_version', p.model_version
                )
                FROM prediction p 
                WHERE p.challenge_id = a.challenge_id AND p.user_id = a.account_id 
                LIMIT 1
            ) 
            WHERE EXISTS (
                SELECT 1 
                FROM prediction p 
                WHERE p.challenge_id = a.challenge_id AND p.user_id = a.account_id
            );

UPDATE attempt a 
            SET phoneme_feedback_json = (
                SELECT json_agg(json_build_object(
                    'phoneme_ipa', f.phoneme_ipa, 
                    'score', f.score, 
                    'sequence_order', f.sequence_order, 
                    'start_time_ms', f.start_time_ms, 
                    'end_time_ms', f.end_time_ms
                ) ORDER BY f.sequence_order)
                FROM attempt_phoneme_feedback f 
                WHERE f.attempt_id = a.id
            ) 
            WHERE EXISTS (
                SELECT 1 
                FROM attempt_phoneme_feedback f 
                WHERE f.attempt_id = a.id
            );

DROP TABLE prediction;

DROP TABLE attempt_phoneme_feedback;

-- Changeset db/changelog/25-create-and-migrate-content.xml::25-create-new-content-tables::antigravity
CREATE TABLE learning_unit (id UUID NOT NULL, parent_id UUID, name VARCHAR(255) NOT NULL, type VARCHAR(50) NOT NULL, metadata_json JSONB, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT learning_unit_pkey PRIMARY KEY (id));

ALTER TABLE learning_unit ADD CONSTRAINT fk_unit_parent FOREIGN KEY (parent_id) REFERENCES learning_unit (id);

CREATE TABLE content_item (id UUID NOT NULL, learning_unit_id UUID NOT NULL, title VARCHAR(255) NOT NULL, type VARCHAR(50) NOT NULL, status VARCHAR(20) NOT NULL, metadata_json JSONB, items_json JSONB, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT content_item_pkey PRIMARY KEY (id));

ALTER TABLE content_item ADD CONSTRAINT fk_content_unit FOREIGN KEY (learning_unit_id) REFERENCES learning_unit (id);

-- Changeset db/changelog/25-create-and-migrate-content.xml::25-migrate-content-data::antigravity
-- 1. Migrate Dialect -> LearningUnit
            INSERT INTO learning_unit (id, name, type, metadata_json, created_at, updated_at, created_by, updated_by)
            SELECT id, name, 'DIALECT', jsonb_build_object('description', description), created_at, updated_at, created_by, updated_by
            FROM dialect;

-- 2. Migrate Level -> LearningUnit
            INSERT INTO learning_unit (id, parent_id, name, type, metadata_json, created_at, updated_at, created_by, updated_by)
            SELECT id, dialect_id, name, 'LEVEL', jsonb_build_object(
                'level_order', level_order,
                'min_stars_required', min_stars_required,
                'error_tag_id', error_tag_id,
                'ai_threshold', ai_threshold,
                'audio_url', audio_url,
                'status', status,
                'rejection_reason', rejection_reason
            ), created_at, updated_at, created_by, updated_by
            FROM level;

-- 3. Migrate Challenge -> ContentItem
            INSERT INTO content_item (id, learning_unit_id, title, type, status, metadata_json, created_at, updated_at, created_by, updated_by)
            SELECT id, level_id, 
                   CASE 
                       WHEN length(content_text) > 255 THEN substring(content_text from 1 for 252) || '...'
                       ELSE content_text 
                   END, 
                   'PRONUNCIATION', status, jsonb_build_object(
                'content_text', content_text,
                'phonetic_transcription_ipa', phonetic_transcription_ipa,
                'reference_audio_url', reference_audio_url,
                'focus_phonemes', focus_phonemes,
                'skill_type', skill_type,
                'difficulty', difficulty,
                'rejection_reason', rejection_reason
            ), created_at, updated_at, created_by, updated_by
            FROM challenge;

-- 4. Migrate Quiz -> ContentItem
            INSERT INTO content_item (id, learning_unit_id, title, type, status, metadata_json, items_json, created_at, updated_at, created_by, updated_by)
            SELECT q.id, q.level_id, q.title, 'QUIZ', q.status, jsonb_build_object(
                'description', q.description,
                'instructions', q.instructions,
                'passing_score', q.passing_score,
                'time_limit_minutes', q.time_limit_minutes,
                'rejection_reason', q.rejection_reason
            ), (
                SELECT jsonb_agg(jsonb_build_object(
                    'skill_type', q_q.skill_type,
                    'difficulty', q_q.difficulty,
                    'question_order', q_q.question_order,
                    'points', q_q.points,
                    'challenge_id', q_q.challenge_id
                ))
                FROM quiz_question q_q
                WHERE q_q.quiz_id = q.id
            ), q.created_at, q.updated_at, q.created_by, q.updated_by
            FROM quiz q;

-- Changeset db/changelog/26-create-and-migrate-activity.xml::26-create-new-activity-tables::antigravity
CREATE TABLE study_session (id UUID NOT NULL, account_id UUID NOT NULL, session_type VARCHAR(50) NOT NULL, started_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, ended_at TIMESTAMP WITH TIME ZONE, summary_json JSONB, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT study_session_pkey PRIMARY KEY (id));

ALTER TABLE study_session ADD CONSTRAINT fk_session_account FOREIGN KEY (account_id) REFERENCES account (id);

CREATE TABLE session_detail (id UUID NOT NULL, session_id UUID NOT NULL, content_item_id UUID NOT NULL, is_passed BOOLEAN DEFAULT FALSE NOT NULL, score_overall DECIMAL(5, 2), attempt_metadata_json JSONB, created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT session_detail_pkey PRIMARY KEY (id));

ALTER TABLE session_detail ADD CONSTRAINT fk_detail_session FOREIGN KEY (session_id) REFERENCES study_session (id);

ALTER TABLE session_detail ADD CONSTRAINT fk_detail_content FOREIGN KEY (content_item_id) REFERENCES content_item (id);

-- Changeset db/changelog/26-create-and-migrate-activity.xml::26-migrate-activity-data::antigravity
-- 1. Migrate PracticeSession -> StudySession (PRACTICE)
            INSERT INTO study_session (id, account_id, session_type, started_at, ended_at, created_at, updated_at)
            SELECT id, account_id, 'PRACTICE', started_at, ended_at, started_at, started_at
            FROM practice_session;

-- 2. Migrate QuizAttempt -> StudySession (QUIZ)
            -- Reusing QuizAttempt ID as StudySession ID
            INSERT INTO study_session (id, account_id, session_type, started_at, ended_at, summary_json, created_at, updated_at, created_by, updated_by)
            SELECT id, student_id, 'QUIZ', started_at, completed_at, jsonb_build_object(
                'score', score,
                'completed', completed,
                'quiz_type', quiz_type,
                'time_taken_seconds', time_taken_seconds
            ), created_at, updated_at, created_by, updated_by
            FROM quiz_attempt;

-- 3. Migrate Attempt -> SessionDetail
            -- Handles both Practice and Quiz attempts by coalescing session_id and quiz_attempt_id
            INSERT INTO session_detail (id, session_id, content_item_id, is_passed, score_overall, attempt_metadata_json, created_at, updated_at)
            SELECT id, COALESCE(session_id, quiz_attempt_id), challenge_id, is_passed, score_overall, jsonb_build_object(
                'content_snapshot_json', content_snapshot_json,
                'ai_insights_json', ai_insights_json,
                'phoneme_feedback_json', phoneme_feedback_json,
                'audio_url', audio_url,
                'latency_ms', latency_ms
            ), created_at, created_at
            FROM attempt;

-- 4. Special handling for Quiz answers if contained in QuizAttempt
            -- If QuizAttempt has answersSnapshot, create a detail for the Quiz content_item
            INSERT INTO session_detail (id, session_id, content_item_id, is_passed, score_overall, attempt_metadata_json, created_at, updated_at, created_by, updated_by)
            SELECT gen_random_uuid(), id, quiz_id, completed, CAST(score AS DECIMAL(5,2)), jsonb_build_object(
                'answers_snapshot', answers_snapshot
            ), created_at, updated_at, created_by, updated_by
            FROM quiz_attempt
            WHERE answers_snapshot IS NOT NULL;

-- Changeset db/changelog/27-cleanup-and-rename.xml::27-rename-social-tables::antigravity
ALTER TABLE achievement RENAME TO reward_catalog;

ALTER TABLE account_achievement RENAME TO account_reward;

-- Changeset db/changelog/27-cleanup-and-rename.xml::27-drop-old-tables::antigravity
DROP TABLE attempt CASCADE;

DROP TABLE quiz_attempt CASCADE;

DROP TABLE practice_session CASCADE;

DROP TABLE quiz_question CASCADE;

DROP TABLE quiz CASCADE;

DROP TABLE challenge CASCADE;

DROP TABLE level CASCADE;

DROP TABLE dialect CASCADE;

DROP TABLE error_tag CASCADE;

-- Changeset db/changelog/28-finalize-merged-schema.xml::28-drop-old-achievement-fk::antigravity
ALTER TABLE account_reward DROP CONSTRAINT fk_acct_achievement_achievement;

-- Changeset db/changelog/28-finalize-merged-schema.xml::28-rename-achievement-id-to-reward-catalog-id::antigravity
ALTER TABLE account_reward RENAME COLUMN achievement_id TO reward_catalog_id;

-- Changeset db/changelog/28-finalize-merged-schema.xml::28-add-account-reward-fk::antigravity
ALTER TABLE account_reward ADD CONSTRAINT fk_account_reward_catalog FOREIGN KEY (reward_catalog_id) REFERENCES reward_catalog (id);

-- Changeset db/changelog/28-finalize-merged-schema.xml::28-seed-error-tags-into-learning-unit::antigravity
-- Migration-08 error tags (uuid_generate_v4() was used so IDs were random;
            -- we assign stable fixed UUIDs here for referential integrity going forward).
            INSERT INTO learning_unit (id, name, type, metadata_json, created_at, updated_at)
            VALUES
                ('00000000-0000-0000-0003-000000000001',
                 'Ngọng L - N', 'ERROR_TAG',
                 '{"tag_code":"L_N","description":"Lỗi phát âm nhầm lẫn giữa L và N phổ biến ở miền Bắc","regions":["NORTH"]}',
                 NOW(), NOW()),

                ('00000000-0000-0000-0003-000000000002',
                 'Ngọng S - X', 'ERROR_TAG',
                 '{"tag_code":"S_X","description":"Lỗi phát âm chưa phân biệt âm cuốn lưỡi S và X","regions":["NORTH","CENTRAL","SOUTH"]}',
                 NOW(), NOW()),

                ('00000000-0000-0000-0003-000000000003',
                 'Ngọng CH - TR', 'ERROR_TAG',
                 '{"tag_code":"CH_TR","description":"Lỗi phát âm nhầm lẫn giữa CH và TR","regions":["NORTH","CENTRAL","SOUTH"]}',
                 NOW(), NOW()),

                -- Migration-13 error tags (had fixed UUIDs – preserve them exactly)
                ('00000000-0000-0000-0002-000000000001',
                 'Lẫn lộn V - D', 'ERROR_TAG',
                 '{"tag_code":"V_D_CONFUSION","description":"Lỗi phát âm nhầm lẫn giữa V và D phổ biến ở miền Nam và Trung","regions":["CENTRAL","SOUTH"]}',
                 NOW(), NOW()),

                ('00000000-0000-0000-0002-000000000002',
                 'Sai thanh hỏi/ngã', 'ERROR_TAG',
                 '{"tag_code":"TONE_INTERROGATIVE","description":"Lỗi phát âm không phân biệt rõ thanh hỏi và thanh ngã","regions":["CENTRAL","SOUTH"]}',
                 NOW(), NOW())

            ON CONFLICT (id) DO NOTHING;

-- Changeset db/changelog/28-finalize-merged-schema.xml::28-cleanup-orphaned-placement-rules::antigravity
-- Changeset db/changelog/28-finalize-merged-schema.xml::28-add-placement-rule-fks::antigravity
-- Changeset db/changelog/28-finalize-merged-schema.xml::28-add-educator-feedback-fk::antigravity
ALTER TABLE educator_feedback ADD CONSTRAINT fk_educator_feedback_session_detail FOREIGN KEY (attempt_id) REFERENCES session_detail (id);

-- Changeset db/changelog/28-finalize-merged-schema.xml::28-add-performance-indexes::antigravity
CREATE INDEX idx_learning_unit_type ON learning_unit(type);

CREATE INDEX idx_learning_unit_parent_id ON learning_unit(parent_id);

CREATE INDEX idx_content_item_learning_unit_id ON content_item(learning_unit_id);

CREATE INDEX idx_content_item_type ON content_item(type);

CREATE INDEX idx_content_item_status ON content_item(status);

CREATE INDEX idx_content_item_type_status ON content_item(type, status);

CREATE INDEX idx_study_session_account_id ON study_session(account_id);

CREATE INDEX idx_study_session_session_type ON study_session(session_type);

CREATE INDEX idx_study_session_started_at ON study_session(started_at);

CREATE INDEX idx_study_session_account_type ON study_session(account_id, session_type);

CREATE INDEX idx_session_detail_session_id ON session_detail(session_id);

CREATE INDEX idx_session_detail_content_item_id ON session_detail(content_item_id);

CREATE INDEX idx_session_detail_created_at ON session_detail(created_at);

CREATE INDEX idx_reward_catalog_reward_type ON reward_catalog(reward_type);

CREATE INDEX idx_reward_catalog_is_active ON reward_catalog(is_active);

CREATE INDEX idx_account_reward_account_id ON account_reward(account_id);

CREATE INDEX idx_account_reward_status ON account_reward(status);

-- Changeset db/changelog/db.changelog-master.xml::29-rename-classroom-max-students::antigravity
ALTER TABLE classroom RENAME COLUMN max_students TO current_students;

-- Changeset db/changelog/30-add-classroom-extended-fields.xml::30-add-classroom-description::antigravity
ALTER TABLE classroom ADD description VARCHAR(1000);

-- Changeset db/changelog/30-add-classroom-extended-fields.xml::30-add-classroom-start-date::antigravity
ALTER TABLE classroom ADD start_date TIMESTAMP WITHOUT TIME ZONE;

-- Changeset db/changelog/30-add-classroom-extended-fields.xml::30-add-classroom-end-date::antigravity
ALTER TABLE classroom ADD end_date TIMESTAMP WITHOUT TIME ZONE;

-- Changeset db/changelog/30-add-classroom-extended-fields.xml::30-add-classroom-current-students::antigravity
ALTER TABLE classroom ADD current_students INTEGER;

-- Changeset db/changelog/30-add-classroom-extended-fields.xml::30-add-classroom-dialect-id::antigravity
ALTER TABLE classroom ADD dialect_id UUID;

-- Changeset db/changelog/30-add-classroom-extended-fields.xml::30-add-classroom-dialect-fk::antigravity
ALTER TABLE classroom ADD CONSTRAINT fk_classroom_dialect FOREIGN KEY (dialect_id) REFERENCES learning_unit (id);

-- Changeset db/changelog/30-add-classroom-extended-fields.xml::30-add-classroom-is-active::antigravity
ALTER TABLE classroom ADD is_active BOOLEAN DEFAULT TRUE;

-- Changeset db/changelog/db.changelog-master.xml::31-add-region-to-challenge-bank::antigravity
ALTER TABLE challenge_bank ADD region VARCHAR(20) DEFAULT 'BAC';

-- Changeset db/changelog/31-drop-classroom-entity.xml::31-drop-fk-leaderboard-classroom::antigravity
ALTER TABLE leaderboard DROP CONSTRAINT fk_leaderboard_classroom;

-- Changeset db/changelog/31-drop-classroom-entity.xml::31-drop-leaderboard-classroom-column::antigravity
ALTER TABLE leaderboard DROP COLUMN classroom_id;

-- Changeset db/changelog/31-drop-classroom-entity.xml::31-drop-classroom-member-table::antigravity
DROP TABLE classroom_member CASCADE;

-- Changeset db/changelog/31-drop-classroom-entity.xml::31-drop-classroom-table::antigravity
DROP TABLE classroom CASCADE;

-- Changeset db/changelog/32-update-reward-catalog-icons.xml::32-update-reward-catalog-icons::antigravity
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

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_first_star_1774321057369.png' WHERE code = 'SCORE_FIRST_STAR';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_50_stars_1774321082921.png' WHERE code = 'SCORE_50_STARS';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_100_stars_1774321099394.png' WHERE code = 'SCORE_100_STARS';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_300_stars_1774321125250.png' WHERE code = 'SCORE_300_STARS';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_500_stars_1774321142351.png' WHERE code = 'SCORE_500_STARS';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_perfect_first_1774321160771.png' WHERE code = 'SCORE_PERFECT_FIRST';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_perfect_5_1774321177895.png' WHERE code = 'SCORE_PERFECT_5';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_score_perfect_10_1774321194263.png' WHERE code = 'SCORE_PERFECT_10';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_social_first_friend_1774321209085.png' WHERE code = 'SOCIAL_FIRST_FRIEND';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_social_10_friends_1774321234523.png' WHERE code = 'SOCIAL_10_FRIENDS';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_social_leaderboard_top10_1774321249654.png' WHERE code = 'SOCIAL_LEADERBOARD_TOP10';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_special_early_bird_1774321265615.png' WHERE code = 'SPECIAL_EARLY_BIRD';

UPDATE reward_catalog SET icon_url = '/icons/badges/badge_special_night_owl_1774321280032.png' WHERE code = 'SPECIAL_NIGHT_OWL';

-- Changeset db/changelog/33-seed-new-reward-catalog-badges.xml::33-seed-new-reward-catalog-badges::antigravity
INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_FIRST_LEVEL', 'Bước Chân Đầu Tiên 👣', 'Hoàn thành màn chơi đầu tiên trong hành trình', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_first_level_1774320331129.png', '{"type":"levels_completed","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_5_LEVELS', 'Leo Thang Xuất Sắc 🪜', 'Hoàn thành 5 màn chơi', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_5_levels_1774320350141.png', '{"type":"levels_completed","threshold":5}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_10_LEVELS', 'Nửa Chặng Đường 🚩', 'Hoàn thành 10 màn chơi', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_10_levels_1774320365804.png', '{"type":"levels_completed","threshold":10}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_15_LEVELS', 'Về Đích Rồi! 🏁', 'Hoàn thành 15 màn chơi', 'ACHIEVEMENT', 'LEARNING', '/icons/badges/badge_learn_15_levels_1774320381238.png', '{"type":"levels_completed","threshold":15}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_NORTH_COMPLETE', 'Chinh Phục Vùng Bắc 🔵', 'Hoàn thành tất cả màn của Vùng Bắc', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_north_complete_1774320396390.png', '{"type":"region_completed","region":"NORTH"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_CENTRAL_COMPLETE', 'Chinh Phục Vùng Trung 🟡', 'Hoàn thành tất cả màn của Vùng Trung', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_central_complete_1774320415064.png', '{"type":"region_completed","region":"CENTRAL"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_SOUTH_COMPLETE', 'Chinh Phục Vùng Nam 🔴', 'Hoàn thành tất cả màn của Vùng Nam', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_south_complete_1774320437732.png', '{"type":"region_completed","region":"SOUTH"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_ALL_REGIONS', 'Hành Trình Hoàn Chỉnh 🗺️', 'Hoàn thành cả 3 vùng miền Bắc-Trung-Nam', 'ACHIEVEMENT', 'LEARNING', '/icons/badges/badge_learn_all_regions_1774320453459.png', '{"type":"all_regions_completed"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_BEGINNER_DONE', 'Nảy Mầm 🌱', 'Hoàn thành tất cả màn độ khó Sơ cấp', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_beginner_done_1774320467005.png', '{"type":"difficulty_completed","level":"BEGINNER"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_INTERMEDIATE_DONE', 'Vươn Cao 🌳', 'Hoàn thành tất cả màn độ khó Trung cấp', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_intermediate_done_1774320486543.png', '{"type":"difficulty_completed","level":"INTERMEDIATE"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_ADVANCED_DONE', 'Vượt Cao Cấp 🎓', 'Hoàn thành tất cả màn độ khó Cao cấp', 'ACHIEVEMENT', 'LEARNING', '/icons/badges/badge_learn_advanced_done_1774320526582.png', '{"type":"difficulty_completed","level":"ADVANCED"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_FIRST_3STAR', 'Ba Sao Đầu Tiên ⭐⭐⭐', 'Lần đầu đạt 3 sao trong một màn chơi', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_first_3star_1774320544136.png', '{"type":"three_star_count","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_10_3STAR', 'Mưa Sao Băng 💫', 'Đạt 3 sao trong 10 màn khác nhau', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_10_3star_1774320564153.png', '{"type":"three_star_count","threshold":10}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_ALL_3STAR', 'Hoàn Hảo Tuyệt Đối 🌌', 'Đạt 3 sao ở tất cả các màn chơi', 'ACHIEVEMENT', 'LEARNING', '/icons/badges/badge_learn_all_3star_1774320580246.png', '{"type":"all_levels_three_star"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'LEARN_RETRY_WIN', 'Không Bỏ Cuộc 🔥', 'Thua rồi vượt qua cùng một màn chơi', 'BADGE', 'LEARNING', '/icons/badges/badge_learn_retry_win_1774320596421.png', '{"type":"fail_then_pass_same_level"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_FIRST_PASS', 'Giọng Nói Đầu Tiên 🎤', 'Vượt qua màn phát âm đầu tiên', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_first_pass_1774320611814.png', '{"type":"pronunciation_level_passed","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_NL_MASTER', 'Thuần Thục N/L 🏆', 'Đạt độ chính xác N/L >= 90%', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_nl_master_1774320627713.png', '{"type":"phoneme_accuracy","pair":"N_L","threshold":90}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_SX_MASTER', 'Thuần Thục S/X 🎯', 'Đạt độ chính xác S/X >= 90%', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_sx_master_1774320644298.png', '{"type":"phoneme_accuracy","pair":"S_X","threshold":90}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_DGIR_MASTER', 'Thuần Thục D/GI/R 🎖️', 'Đạt độ chính xác D/GI/R >= 90%', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_dgir_master_1774320673724.png', '{"type":"phoneme_accuracy","pair":"D_GI_R","threshold":90}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_TRCH_MASTER', 'Thuần Thục TR/CH ⚡', 'Đạt độ chính xác TR/CH >= 90%', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_trch_master_1774320690268.png', '{"type":"phoneme_accuracy","pair":"TR_CH","threshold":90}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_ALL_PAIRS', 'Chinh Phục Mọi Phụ Âm 👑', 'Thành thạo tất cả các cặp phụ âm', 'ACHIEVEMENT', 'PRONUNCIATION', '/icons/badges/badge_phoneme_all_pairs_1774320707237.png', '{"type":"all_phoneme_pairs_mastered"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_TONE_GOOD', 'Thanh Điệu Vào Tai 🎵', 'Đạt độ chính xác thanh điệu >= 80%', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_tone_good_1774320726766.png', '{"type":"tone_accuracy","threshold":80}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_TONE_MASTER', 'Bậc Thầy Thanh Điệu 🎼', 'Thành thạo cả 6 thanh điệu tiếng Việt', 'ACHIEVEMENT', 'PRONUNCIATION', '/icons/badges/badge_phoneme_tone_master_1774320744624.png', '{"type":"tone_accuracy","threshold":95}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_PERFECT_SESSION', 'Phiên Hoàn Hảo ✨', 'Đạt điểm tuyệt đối trong một phiên phát âm', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_perfect_session_1774320759505.png', '{"type":"perfect_pronunciation_session","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_PERFECT_5', 'Ngũ Tuyệt Phát Âm 🎤x5', 'Đạt điểm tuyệt đối 5 phiên phát âm', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_perfect_5_1774320782716.png', '{"type":"perfect_pronunciation_session","threshold":5}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_ENTRY_DONE', 'Biết Mình Biết Ta 🧭', 'Hoàn thành bài kiểm tra phương ngữ đầu vào', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_entry_done_1774320798739.png', '{"type":"entry_test_completed"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'PHONEME_SPEED', 'Nói Nhanh Nói Chuẩn ⚡🎤', 'Phát âm chuẩn trong vòng 3 giây 10 lần', 'BADGE', 'PRONUNCIATION', '/icons/badges/badge_phoneme_speed_1774320815182.png', '{"type":"fast_pronunciation_count","threshold":10}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_FIRST_PLAY', 'Bắt Đầu Chơi 🎮', 'Chơi mini-game lần đầu tiên', 'BADGE', 'MINI_GAMES', '/icons/badges/badge_mini_first_play_1774320832038.png', '{"type":"minigame_played","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_10_PLAYS', 'Nghiện Game 🔥🎮', 'Chơi mini-game 10 lần', 'BADGE', 'MINI_GAMES', '/icons/badges/badge_mini_10_plays_1774320846852.png', '{"type":"minigame_played","threshold":10}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_50_PLAYS', 'Cao Thủ Mini-game 👑🎮', 'Chơi mini-game 50 lần', 'ACHIEVEMENT', 'MINI_GAMES', '/icons/badges/badge_mini_50_plays_1774320863767.png', '{"type":"minigame_played","threshold":50}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_READING_FIRST', 'Tập Đọc 📖', 'Chơi game Đọc lần đầu tiên', 'BADGE', 'MINI_GAMES', '/icons/badges/badge_mini_reading_first_1774320889622.png', '{"type":"minigame_type_played","game":"READING","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_READING_SAGA', 'Saga Đọc Hoàn Chỉnh 📚', 'Hoàn thành toàn bộ Saga Đọc', 'ACHIEVEMENT', 'MINI_GAMES', '/icons/badges/badge_mini_reading_saga_1774320907052.png', '{"type":"reading_saga_completed"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_WRITING_FIRST', 'Cầm Bút Lên ✏️', 'Chơi game Viết lần đầu tiên', 'BADGE', 'MINI_GAMES', '/icons/badges/badge_mini_writing_first_1774320925950.png', '{"type":"minigame_type_played","game":"WRITING","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_WRITING_DONE', 'Thư Pháp Số 🖌️', 'Hoàn thành các màn game Viết', 'BADGE', 'MINI_GAMES', '/icons/badges/badge_mini_writing_done_1774320945023.png', '{"type":"writing_levels_completed","threshold":5}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_MARIO_FIRST', 'Jump! 🍄', 'Chơi game Mario phát âm lần đầu tiên', 'BADGE', 'MINI_GAMES', '/icons/badges/badge_mini_mario_first_1774320962407.png', '{"type":"minigame_type_played","game":"MARIO","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_MARIO_CLEAR', 'Cờ Đến Đích 🏯', 'Hoàn thành các màn game Mario', 'BADGE', 'MINI_GAMES', '/icons/badges/badge_mini_mario_clear_1774320979828.png', '{"type":"mario_levels_completed","threshold":5}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_ALL_TYPES', 'Thiên Tài Đa Năng 🌈🎮', 'Đã chơi tất cả loại mini-game', 'ACHIEVEMENT', 'MINI_GAMES', '/icons/badges/badge_mini_all_types_1774321002432.png', '{"type":"all_minigame_types_played"}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_PERFECT_GAME', 'Điểm Tuyệt Đối 💯🎮', 'Đạt điểm tuyệt đối trong một mini-game', 'BADGE', 'MINI_GAMES', '/icons/badges/badge_mini_perfect_game_1774321018799.png', '{"type":"minigame_perfect_score","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'MINI_PERFECT_10', 'Đại Cao Thủ 💎🎮', 'Đạt điểm tuyệt đối 10 mini-game', 'ACHIEVEMENT', 'MINI_GAMES', '/icons/badges/badge_mini_perfect_10_1774321041241.png', '{"type":"minigame_perfect_score","threshold":10}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_FIRST_STAR', 'Ngôi Sao Đầu Tiên ⭐', 'Thu được ngôi sao đầu tiên', 'BADGE', 'SCORE', '/icons/badges/badge_score_first_star_1774321057369.png', '{"type":"total_stars","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_50_STARS', 'Bầu Trời Sao ✨', 'Tích lũy 50 ngôi sao', 'BADGE', 'SCORE', '/icons/badges/badge_score_50_stars_1774321082921.png', '{"type":"total_stars","threshold":50}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_100_STARS', 'Trăm Sao Tỏa Sáng 🌠', 'Tích lũy 100 ngôi sao', 'BADGE', 'SCORE', '/icons/badges/badge_score_100_stars_1774321099394.png', '{"type":"total_stars","threshold":100}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_300_STARS', 'Cơn Mưa Sao Băng 💫', 'Tích lũy 300 ngôi sao', 'BADGE', 'SCORE', '/icons/badges/badge_score_300_stars_1774321125250.png', '{"type":"total_stars","threshold":300}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_500_STARS', 'Ngân Hà Rực Rỡ 🌌', 'Tích lũy 500 ngôi sao', 'ACHIEVEMENT', 'SCORE', '/icons/badges/badge_score_500_stars_1774321142351.png', '{"type":"total_stars","threshold":500}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_PERFECT_FIRST', 'Hoàn Hảo Đầu Tiên 🥇', 'Lần đầu đạt điểm tuyệt đối', 'BADGE', 'SCORE', '/icons/badges/badge_score_perfect_first_1774321160771.png', '{"type":"perfect_score_count","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_PERFECT_5', 'Ngũ Tuyệt Điểm 🏅', 'Đạt điểm tuyệt đối 5 lần', 'BADGE', 'SCORE', '/icons/badges/badge_score_perfect_5_1774321177895.png', '{"type":"perfect_score_count","threshold":5}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SCORE_PERFECT_10', 'Đại Sư Điểm Số 🏆', 'Đạt điểm tuyệt đối 10 lần', 'ACHIEVEMENT', 'SCORE', '/icons/badges/badge_score_perfect_10_1774321194263.png', '{"type":"perfect_score_count","threshold":10}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SOCIAL_FIRST_FRIEND', 'Kết Nối Đầu Tiên 🤝', 'Kết bạn với người chơi đầu tiên', 'BADGE', 'SOCIAL', '/icons/badges/badge_social_first_friend_1774321209085.png', '{"type":"friend_count","threshold":1}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SOCIAL_10_FRIENDS', 'Cộng Đồng Sôi Nổi 👥', 'Kết bạn với 10 người chơi', 'BADGE', 'SOCIAL', '/icons/badges/badge_social_10_friends_1774321234523.png', '{"type":"friend_count","threshold":10}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SOCIAL_LEADERBOARD_TOP10', 'Hào Kiệt Top 10 🎖️', 'Vào top 10 bảng xếp hạng', 'BADGE', 'SOCIAL', '/icons/badges/badge_social_leaderboard_top10_1774321249654.png', '{"type":"leaderboard_rank","max_rank":10}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SPECIAL_EARLY_BIRD', 'Chim Sâu Buổi Sáng 🐦🌅', 'Học trong khung giờ 5–8 sáng ít nhất 10 lần', 'BADGE', 'SPECIAL', '/icons/badges/badge_special_early_bird_1774321265615.png', '{"type":"study_hour_range","from":5,"to":8,"threshold":10}', 0, TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, category, icon_url, criteria_json, xp_reward, is_active, created_at, updated_at) VALUES (uuid_generate_v4(), 'SPECIAL_NIGHT_OWL', 'Cú Đêm 🦉🌙', 'Học trong khung giờ 22–2 đêm ít nhất 10 lần', 'BADGE', 'SPECIAL', '/icons/badges/badge_special_night_owl_1774321280032.png', '{"type":"study_hour_range","from":22,"to":2,"threshold":10}', 0, TRUE, NOW(), NOW());

-- Changeset db/changelog/34-add-last-login-date.xml::34-add-last-login-date-to-account::antigravity
ALTER TABLE account ADD last_login_date date;

-- Changeset db/changelog/db.changelog-master.xml::32-add-account-learning-unit::antigravity
CREATE TABLE account_learning_unit (id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), account_id UUID NOT NULL, learning_unit_id UUID NOT NULL, stars_earned INTEGER DEFAULT 0 NOT NULL, is_completed BOOLEAN DEFAULT FALSE NOT NULL, highest_score numeric(5, 2), CONSTRAINT account_learning_unit_pkey PRIMARY KEY (id), CONSTRAINT fk_alu_account FOREIGN KEY (account_id) REFERENCES account(id), CONSTRAINT fk_alu_learning_unit FOREIGN KEY (learning_unit_id) REFERENCES learning_unit(id));

ALTER TABLE account_learning_unit ADD CONSTRAINT uniq_alu_account_unit UNIQUE (account_id, learning_unit_id);

-- Changeset db/changelog/35-add-notification-table.xml::35-add-notification-table::antigravity
CREATE TABLE notification (id UUID NOT NULL, recipient_id UUID NOT NULL, type VARCHAR(50) NOT NULL, title VARCHAR(255) NOT NULL, message VARCHAR(500) NOT NULL, reference_id UUID, is_read BOOLEAN DEFAULT FALSE NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, CONSTRAINT notification_pkey PRIMARY KEY (id));

ALTER TABLE notification ADD CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES account (id);

CREATE INDEX idx_notification_recipient ON notification(recipient_id);

CREATE INDEX idx_notification_recipient_unread ON notification(recipient_id, is_read);

-- Changeset db/changelog/36-simplify-reward-catalog.xml::36-simplify-reward-catalog::antigravity
ALTER TABLE reward_catalog DROP COLUMN criteria_json;

-- Changeset db/changelog/36-simplify-reward-catalog.xml::36-drop-reward-category::antigravity
ALTER TABLE reward_catalog DROP COLUMN category;

-- Changeset db/changelog/37-add-sort-by-to-leaderboard.xml::37-add-sort-by-to-leaderboard::antigravity
ALTER TABLE leaderboard ADD sort_by VARCHAR(30) DEFAULT 'TOTAL_XP' NOT NULL;

CREATE INDEX idx_leaderboard_scope_period_region_sort ON leaderboard(scope, period_type, region_code, sort_by);

-- Changeset db/changelog/38-data-cleanup.xml::38-cleanup-learning-unit-and-challenge-bank-v4::antigravity
DELETE FROM account_learning_unit WHERE learning_unit_id IN (SELECT id FROM learning_unit WHERE type IN ('QUIZ', 'LEVEL'));

DELETE FROM assignment WHERE learning_unit_id IN (SELECT id FROM learning_unit WHERE type IN ('QUIZ', 'LEVEL'));

DELETE FROM content_item WHERE learning_unit_id IN (SELECT id FROM learning_unit WHERE type IN ('QUIZ', 'LEVEL'));

DELETE FROM learning_unit WHERE type IN ('QUIZ', 'LEVEL');

DELETE FROM challenge_bank;

-- Changeset db/changelog/39-drop-reward-catalog-data.xml::20260402-drop-all-rewards::antigravity
-- Dropping all data from reward_catalog as requested
DELETE FROM reward_catalog;

-- Changeset db/changelog/40-seed-achievements.xml::20260402-seed-10-achievements::antigravity
-- Seeding 10 starter achievements with lightweight icons
INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'LEARN_FIRST_STEP', 'Bước chân đầu tiên 👣', 'Hoàn thành màn chơi đầu tiên trong hành trình.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/medal-icon.png', '100', TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'LEARN_DILIGENT', 'Người học chăm chỉ 📚', 'Hoàn thành 5 bài học khác nhau.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/book-icon.png', '250', TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'PHONEME_MASTER', 'Bậc thầy phát âm 🎤', 'Đạt điểm tuyệt đối 100% trong một bài kiểm tra phát âm.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/star-icon.png', '500', TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'CONQUEROR_NORTH', 'Nhà chinh phục miền Bắc 🏯', 'Hoàn thành tất cả các cấp độ giọng miền Bắc.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/map-icon.png', '1000', TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'STAR_COLLECTOR', 'Thợ săn vì sao ⭐', 'Tích lũy tổng cộng 50 sao từ các bài học.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/trophy-icon.png', '300', TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'ERROR_DESTROYER', 'Kẻ hủy diệt lỗi 🛠️', 'Sửa thành công 10 lỗi phát âm phổ biến.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/check-icon.png', '400', TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'SMART_LEARNER', 'Học giả thông thái 💡', 'Hoàn thành bài học mà không mắc bất kỳ lỗi nào.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/lightbulb-icon.png', '350', TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'SPEED_RACER', 'Thần tốc 🚀', 'Hoàn thành một Quiz trong vòng dưới 30 giây.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/rocket-icon.png', '450', TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'FRIENDLY_USER', 'Người bạn thân thiện ❤️', 'Thêm 3 người bạn mới vào danh sách.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/heart-icon.png', '200', TRUE, NOW(), NOW());

INSERT INTO reward_catalog (id, code, name, description, reward_type, icon_url, xp_reward, is_active, created_at, updated_at) VALUES (gen_random_uuid(), 'DAILY_GIFT', 'Quà tặng mỗi ngày 🎁', 'Hoàn thành chuỗi học tập trong 7 ngày liên tiếp.', 'ACHIEVEMENT', 'https://icons.iconarchive.com/icons/paomedia/small-n-flat/128/gift-icon.png', '600', TRUE, NOW(), NOW());

-- Changeset db/changelog/41-create-speaking-attempt.xml::41-create-speaking-attempt-table::antigravity
CREATE TABLE speaking_attempt (id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), account_id UUID NOT NULL, challenge_id UUID, target_text TEXT NOT NULL, asr_transcription TEXT, audio_url TEXT, gemini_score INTEGER, is_correct BOOLEAN DEFAULT FALSE NOT NULL, dialect VARCHAR(20), consent_given BOOLEAN DEFAULT TRUE NOT NULL, CONSTRAINT speaking_attempt_pkey PRIMARY KEY (id), CONSTRAINT fk_speaking_attempt_challenge FOREIGN KEY (challenge_id) REFERENCES challenge_bank(id), CONSTRAINT fk_speaking_attempt_account FOREIGN KEY (account_id) REFERENCES account(id));

CREATE INDEX idx_speaking_attempt_account ON speaking_attempt(account_id);

CREATE INDEX idx_speaking_attempt_dialect ON speaking_attempt(dialect);

CREATE INDEX idx_speaking_attempt_created_at ON speaking_attempt(created_at);

-- Changeset db/changelog/42-create-chat-message-table.xml::42-create-chat-message-table::antigravity
CREATE TABLE chat_message (id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), sender_id UUID NOT NULL, recipient_id UUID NOT NULL, content TEXT NOT NULL, timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL, status VARCHAR(20) DEFAULT 'SENT' NOT NULL, CONSTRAINT chat_message_pkey PRIMARY KEY (id));

ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_id) REFERENCES account (id);

ALTER TABLE chat_message ADD CONSTRAINT fk_chat_message_recipient FOREIGN KEY (recipient_id) REFERENCES account (id);

CREATE INDEX idx_chat_message_pair_time ON chat_message(sender_id, recipient_id, timestamp);

-- Changeset db/changelog/43-index-chat-message.xml::43-index-chat-message::cursor
CREATE INDEX idx_chat_participants ON chat_message(sender_id, recipient_id);

CREATE INDEX idx_chat_status ON chat_message(status);

-- Changeset db/changelog/44-fix-quiz-orderindex.xml::44-fix-quiz-orderindex::antigravity
DO $$
DECLARE
    r RECORD;
    prev_parent UUID := NULL;
    counter INTEGER := 0;
    parsed JSONB;
BEGIN
    FOR r IN
        SELECT id, parent_id, metadata_json
        FROM learning_unit
        WHERE type = 'QUIZ'
          AND parent_id IS NOT NULL
          AND metadata_json IS NOT NULL
        ORDER BY parent_id, id
    LOOP
        BEGIN
            parsed := r.metadata_json;
        EXCEPTION WHEN OTHERS THEN
            CONTINUE;
        END;

        IF r.parent_id IS DISTINCT FROM prev_parent THEN
            counter := 1;
            prev_parent := r.parent_id;
        ELSE
            counter := counter + 1;
        END IF;

        UPDATE learning_unit
        SET metadata_json = jsonb_set(parsed, '{orderIndex}', to_jsonb(counter))
        WHERE id = r.id;
    END LOOP;
END $$;

-- Changeset db/changelog/45-add-processing-time-to-speaking-attempt.xml::44-add-processing-time-to-speaking-attempt::antigravity
ALTER TABLE speaking_attempt ADD processing_time_ms BIGINT;

-- Changeset db/changelog/46-add-asr-latency-and-feedback.xml::45-add-asr-latency-and-feedback-to-speaking-attempt::antigravity
ALTER TABLE speaking_attempt ADD asr_processing_time_ms BIGINT;

ALTER TABLE speaking_attempt ADD gemini_feedback TEXT;

-- Changeset db/changelog/46-add-custom-learning-path-tables.xml::46-create-custom-learning-path-tables::antigravity
CREATE TABLE custom_learning_paths (id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), student_id UUID NOT NULL, educator_id UUID NOT NULL, title VARCHAR(255) NOT NULL, description TEXT, is_active BOOLEAN DEFAULT TRUE NOT NULL, CONSTRAINT custom_learning_paths_pkey PRIMARY KEY (id), CONSTRAINT fk_clp_student FOREIGN KEY (student_id) REFERENCES account(id), CONSTRAINT fk_clp_educator FOREIGN KEY (educator_id) REFERENCES account(id));

CREATE TABLE custom_path_levels (id UUID NOT NULL, custom_path_id UUID NOT NULL, level_id UUID NOT NULL, order_index INTEGER NOT NULL, CONSTRAINT custom_path_levels_pkey PRIMARY KEY (id), CONSTRAINT fk_cpl_level FOREIGN KEY (level_id) REFERENCES learning_unit(id), CONSTRAINT fk_cpl_path FOREIGN KEY (custom_path_id) REFERENCES custom_learning_paths(id));

CREATE TABLE custom_path_progress (id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), custom_path_id UUID NOT NULL, learning_unit_id UUID NOT NULL, score INTEGER DEFAULT 0 NOT NULL, is_completed BOOLEAN DEFAULT FALSE NOT NULL, CONSTRAINT custom_path_progress_pkey PRIMARY KEY (id), CONSTRAINT fk_cpp_path FOREIGN KEY (custom_path_id) REFERENCES custom_learning_paths(id), CONSTRAINT fk_cpp_unit FOREIGN KEY (learning_unit_id) REFERENCES learning_unit(id));

ALTER TABLE custom_path_progress ADD CONSTRAINT uniq_cpp_path_unit UNIQUE (custom_path_id, learning_unit_id);

-- Changeset db/changelog/47-remove-difficulty-tag.xml::47-remove-difficulty-tag::antigravity
ALTER TABLE challenge_bank DROP COLUMN difficulty_tag;

-- Changeset db/changelog/47-update-educator-feedback.xml::47-update-educator-feedback-columns::antigravity
ALTER TABLE educator_feedback ALTER COLUMN  attempt_id DROP NOT NULL;

ALTER TABLE educator_feedback ADD speaking_attempt_id UUID;

ALTER TABLE educator_feedback ADD CONSTRAINT fk_feedback_speaking_attempt FOREIGN KEY (speaking_attempt_id) REFERENCES speaking_attempt (id);

ALTER TABLE educator_feedback ADD student_id UUID NOT NULL;

ALTER TABLE educator_feedback ADD CONSTRAINT fk_feedback_student FOREIGN KEY (student_id) REFERENCES account (id);

-- Changeset db/changelog/48-remove-reward-description.xml::48-remove-reward-description::antigravity
ALTER TABLE reward_catalog DROP COLUMN description;

-- Changeset db/changelog/49-add-badge-count-to-ranking.xml::49-add-badge-count-to-ranking::antigravity
ALTER TABLE account ADD badge_count INTEGER DEFAULT 0 NOT NULL;

ALTER TABLE leaderboard_entry ADD badge_count INTEGER DEFAULT 0 NOT NULL;

UPDATE account a 
            SET badge_count = (
                SELECT COUNT(*) 
                FROM account_reward ar 
                WHERE ar.account_id = a.id AND ar.status = 'UNLOCKED'
            );

-- Changeset db/changelog/50-add-entry-test-tables.xml::20240421-50-create-entry-test-question-table::antigravity
CREATE TABLE entry_test_question (id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), target_text TEXT NOT NULL, region_category VARCHAR(50) NOT NULL, CONSTRAINT entry_test_question_pkey PRIMARY KEY (id));

-- Changeset db/changelog/50-add-entry-test-tables.xml::20240421-50-1-seed-entry-test-questions::antigravity
INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Lúa nếp nương lá nảy xanh non', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nói năng nên luyện lung tung', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nửa đêm nhai nếp nương nanh', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Dòng rạch giữa rừng rộn ràng', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Rì rào rặng rào rung rinh', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Gió giục giã giữa dòng gian nan', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trẻ chăn trâu che chung chiếc chiếu', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Chú chó chạy trong chuồng tre', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trăng tròn trên tre chông chênh', 'SOUTH_TRCH');

-- Changeset db/changelog/50-add-entry-test-tables.xml::20240421-50-2-create-entry-test-result-table::antigravity
CREATE TABLE entry_test_result (id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), account_id UUID NOT NULL, overall_score numeric(5, 2), detected_region VARCHAR(20), total_questions INTEGER, details TEXT, CONSTRAINT entry_test_result_pkey PRIMARY KEY (id), CONSTRAINT fk_etr_account FOREIGN KEY (account_id) REFERENCES account(id));

-- Changeset db/changelog/50-add-entry-test-tables.xml::20240421-50-3-add-is-unlocked-to-alu::antigravity
ALTER TABLE account_learning_unit ADD is_unlocked BOOLEAN DEFAULT FALSE NOT NULL;

-- Changeset db/changelog/50-add-entry-test-tables.xml::20240421-50-4-add-has-done-test-to-account::antigravity
ALTER TABLE account ADD has_done_entry_test BOOLEAN DEFAULT FALSE NOT NULL;

-- Changeset db/changelog/51-reset-entry-test-questions.xml::20240421-51-reset-entry-test-questions::antigravity
DELETE FROM entry_test_question;

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Lúa nếp nương.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Làm lụng nặng nề.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nói năng lưu loát.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nửa đêm ăn nếp nương nanh.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Lên núi lấy lá lốt làm thuốc.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Phụ nữ Việt Nam nên luyện nói năng.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Lúa nếp nương lá nảy xanh non, nắng nồng nàn lung linh bên lán.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Năm nay lũ lớn liên miên, làm làng lênh láng lúa nước.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Luộc hột vịt lộn, luộc lộn hột vịt lạc, ăn lộn hột vịt lộn luộc lại.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nàng Lê lên núi lấy nước nấu lẩu, leo lên lầu lấy lọ muối lại làm rơi.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trăng tròn trên tre.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Chú chó chăn trâu.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Chiếc chiếu chông chênh.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trẻ chăn trâu che chung chiếc chiếu.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Cha chở chú Chi đi chợ Chợ Lớn.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trời trong trẻo trên triền đê vắng.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trăng tròn trên tre, chú chó chui ra khỏi chuồng chạy trong sân chung.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trên trời có đám mây trắng, dưới đất có con trâu trắng đang gặm cỏ trong chuồng tre.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Chị Chín chở chú Chiến đi chơi chợ, chú Chiến cho chị Chín chiếc chiếu chàm.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trần Trọng Triết trúng tuyển trường trẻ, trở thành triết gia trẻ trung trên thị trấn.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Sương sa xào xạc.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Dòng rạch rộn ràng.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Gió giục giã giữa rừng.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Rì rào rặng rào rung rinh trong gió.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Sáng sớm xem sao sáng rực trên sân.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Dì Diệp dắt dê đi dọc dòng sông.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Sáng sớm sương xuống xào xạc, sau song sắt sen sắp nở xòe sang trọng.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Dòng rạch rộn ràng reo rắt, rừng rậm rúng rinh rộn rã rêu rao.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Giặc giã gieo giống gian nan, giữ gìn giang sơn gấm vóc gia đình.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Sáu sắc sảo xách sọt xuống xuồng, sang sông xem sổ sách sòng phẳng.', 'CENTRAL_DGIR');

-- Changeset db/changelog/52-rename-gemini-columns-to-groq.xml::52-rename-gemini-columns-to-groq::antigravity
ALTER TABLE speaking_attempt RENAME COLUMN gemini_score TO groq_score;

ALTER TABLE speaking_attempt RENAME COLUMN gemini_feedback TO groq_feedback;

-- Changeset db/changelog/53-migrate-entry-test-questions.xml::20240422-53-migrate-entry-test-questions::antigravity
DELETE FROM entry_test_question;

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Lúa nếp nương.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nói năng lưu loát.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Làm lụng nặng nề.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nắng nồng lấp lánh.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nai nhảy lên núi.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Lấy lá lốt nấu.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Lòng lẻo nên nản.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nêu nao lòng lính.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Lạ lẫm nét nhìn.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Nỗ lực lên nhấn.', 'NORTH_NL');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Sương sa xào xạc.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Dòng rạch rộn ràng.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Gió giục giữa rừng.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Sáng sớm xem sao.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Rì rào rặng rào.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Dì Diệp dắt dê.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Xa xăm sông sâu.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Rung rinh giàn giáo.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Sâu sắc xao xuyến.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Rực rỡ dưới gương.', 'CENTRAL_DGIR');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trăng tròn trên tre.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Chú chó chăn trâu.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trẻ trung chững chạc.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Chiếc chiếu chông chênh.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trời trong trẻo chưa.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Chợ Chính trúng trận.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trôi chảy chuyện chơi.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trống trải chân trời.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Chăm chỉ trồng trọt.', 'SOUTH_TRCH');

INSERT INTO entry_test_question (id, created_at, updated_at, target_text, region_category) VALUES (gen_random_uuid(), NOW(), NOW(), 'Trân trọng chào chung.', 'SOUTH_TRCH');

-- Changeset db/changelog/54-drop-quiz-data.xml::54-drop-quiz-learning-unit-data::antigravity
-- Drop data for learning units of type QUIZ and associated content items (questions)
DELETE FROM session_detail WHERE content_item_id IN (
                SELECT id FROM content_item 
                WHERE learning_unit_id IN (SELECT id FROM learning_unit WHERE type = 'QUIZ')
            );

DELETE FROM study_session WHERE session_type = 'QUIZ';

DELETE FROM account_learning_unit WHERE learning_unit_id IN (SELECT id FROM learning_unit WHERE type = 'QUIZ');

DELETE FROM custom_path_progress WHERE learning_unit_id IN (SELECT id FROM learning_unit WHERE type = 'QUIZ');

DELETE FROM custom_path_levels WHERE level_id IN (SELECT id FROM learning_unit WHERE type = 'QUIZ');

DELETE FROM content_item WHERE learning_unit_id IN (SELECT id FROM learning_unit WHERE type = 'QUIZ');

DO $$ 
            BEGIN
                IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'assignment') THEN
                    DELETE FROM assignment WHERE learning_unit_id IN (SELECT id FROM learning_unit WHERE type = 'QUIZ');
                END IF;
            END $$;

DELETE FROM learning_unit WHERE type = 'QUIZ';

-- Changeset db/changelog/55-add-personal-roadmap-tables.xml::55-add-personal-roadmap-tables::antigravity
ALTER TABLE learning_unit ADD COLUMN IF NOT EXISTS difficulty_level VARCHAR(50);

ALTER TABLE learning_unit ADD COLUMN IF NOT EXISTS error_tag VARCHAR(50);

CREATE TABLE IF NOT EXISTS entry_test_result_detail (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                entry_test_result_id UUID NOT NULL,
                question_id UUID NOT NULL,
                target_word VARCHAR(255) NOT NULL,
                status VARCHAR(50) NOT NULL,
                error_category VARCHAR(50),
                CONSTRAINT fk_entry_test_result_detail_result FOREIGN KEY (entry_test_result_id) REFERENCES entry_test_result (id),
                CONSTRAINT fk_entry_test_result_detail_question FOREIGN KEY (question_id) REFERENCES entry_test_question (id)
            );

CREATE TABLE IF NOT EXISTS personal_roadmap (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                account_id UUID NOT NULL,
                entry_test_result_id UUID NOT NULL,
                ai_analysis TEXT,
                CONSTRAINT fk_personal_roadmap_account FOREIGN KEY (account_id) REFERENCES account (id),
                CONSTRAINT fk_personal_roadmap_result FOREIGN KEY (entry_test_result_id) REFERENCES entry_test_result (id)
            );

CREATE TABLE IF NOT EXISTS personal_roadmap_item (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                personal_roadmap_id UUID NOT NULL,
                learning_unit_id UUID NOT NULL,
                error_category_target VARCHAR(50),
                order_index INT NOT NULL,
                status VARCHAR(50) NOT NULL,
                CONSTRAINT fk_personal_roadmap_item_roadmap FOREIGN KEY (personal_roadmap_id) REFERENCES personal_roadmap (id),
                CONSTRAINT fk_personal_roadmap_item_unit FOREIGN KEY (learning_unit_id) REFERENCES learning_unit (id)
            );

-- Changeset db/changelog/56-add-missing-base-entity-columns.xml::56-add-missing-base-entity-columns::antigravity
ALTER TABLE entry_test_result_detail ADD COLUMN IF NOT EXISTS created_by VARCHAR(50);

ALTER TABLE entry_test_result_detail ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50);

ALTER TABLE personal_roadmap ADD COLUMN IF NOT EXISTS created_by VARCHAR(50);

ALTER TABLE personal_roadmap ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50);

ALTER TABLE personal_roadmap_item ADD COLUMN IF NOT EXISTS created_by VARCHAR(50);

ALTER TABLE personal_roadmap_item ADD COLUMN IF NOT EXISTS updated_by VARCHAR(50);

-- Changeset db/changelog/57-refactor-roadmap.xml::57-refactor-roadmap::antigravity
-- Drop deprecated tables if they exist
            DROP TABLE IF EXISTS personal_roadmap_item CASCADE;

DROP TABLE IF EXISTS personal_roadmap CASCADE;

DROP TABLE IF EXISTS entry_test_result_detail CASCADE;

-- Alter custom_learning_paths to support AI-generated paths
            ALTER TABLE custom_learning_paths ADD COLUMN IF NOT EXISTS is_ai_generated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE custom_learning_paths ADD COLUMN IF NOT EXISTS ai_feedback TEXT;

ALTER TABLE custom_learning_paths ADD COLUMN IF NOT EXISTS target_level VARCHAR(50);

-- Make educator_id nullable since AI-generated paths won't have an educator
            ALTER TABLE custom_learning_paths ALTER COLUMN educator_id DROP NOT NULL;

-- Changeset db/changelog/58-seed-error-tag-regions.xml::58-seed-error-tag-regions::antigravity
UPDATE learning_unit 
            SET metadata_json = jsonb_set(metadata_json, '{regions}', '["NORTH"]'::jsonb)
            WHERE type = 'ERROR_TAG' AND metadata_json->>'tag_code' = 'L_N';

UPDATE learning_unit 
            SET metadata_json = jsonb_set(metadata_json, '{regions}', '["NORTH", "CENTRAL", "SOUTH"]'::jsonb)
            WHERE type = 'ERROR_TAG' AND metadata_json->>'tag_code' = 'S_X';

UPDATE learning_unit 
            SET metadata_json = jsonb_set(metadata_json, '{regions}', '["NORTH", "CENTRAL", "SOUTH"]'::jsonb)
            WHERE type = 'ERROR_TAG' AND metadata_json->>'tag_code' = 'CH_TR';

UPDATE learning_unit 
            SET metadata_json = jsonb_set(metadata_json, '{regions}', '["CENTRAL", "SOUTH"]'::jsonb)
            WHERE type = 'ERROR_TAG' AND metadata_json->>'tag_code' = 'V_D_CONFUSION';

UPDATE learning_unit 
            SET metadata_json = jsonb_set(metadata_json, '{regions}', '["NORTH", "CENTRAL", "SOUTH"]'::jsonb)
            WHERE type = 'ERROR_TAG' AND metadata_json->>'tag_code' = 'TONE_INTERROGATIVE';

-- Changeset db/changelog/59-delete-error-tags-data.xml::59-delete-error-tags-data::antigravity
-- Xóa toàn bộ dữ liệu Error Tag trong bảng learning_unit
            DELETE FROM learning_unit WHERE type = 'ERROR_TAG';

-- Changeset db/changelog/60-add-learning-unit-missing-columns.xml::60-add-learning-unit-missing-columns::antigravity
ALTER TABLE learning_unit ADD difficulty_level VARCHAR(50);

ALTER TABLE learning_unit ADD error_tag VARCHAR(50);

ALTER TABLE learning_unit ADD reward_catalog_id UUID;

ALTER TABLE learning_unit ADD CONSTRAINT fk_learning_unit_reward FOREIGN KEY (reward_catalog_id) REFERENCES reward_catalog (id);

-- Changeset db/changelog/29-update-error-tag-regions.xml::29-update-error-tag-regions::antigravity
-- Update North regions
            UPDATE learning_unit 
            SET metadata_json = '{"tag_code":"L_N","description":"Lỗi phát âm nhầm lẫn giữa L và N phổ biến ở miền Bắc","regions":["NORTH"]}'
            WHERE id = '00000000-0000-0000-0003-000000000001';

UPDATE learning_unit 
            SET metadata_json = '{"tag_code":"S_X","description":"Lỗi phát âm chưa phân biệt âm cuốn lưỡi S và X","regions":["NORTH","CENTRAL","SOUTH"]}'
            WHERE id = '00000000-0000-0000-0003-000000000002';

UPDATE learning_unit 
            SET metadata_json = '{"tag_code":"CH_TR","description":"Lỗi phát âm nhầm lẫn giữa CH và TR","regions":["NORTH","CENTRAL","SOUTH"]}'
            WHERE id = '00000000-0000-0000-0003-000000000003';

-- Update Central/South regions
            UPDATE learning_unit 
            SET metadata_json = '{"tag_code":"V_D_CONFUSION","description":"Lỗi phát âm nhầm lẫn giữa V và D phổ biến ở miền Nam và Trung","regions":["CENTRAL","SOUTH"]}'
            WHERE id = '00000000-0000-0000-0002-000000000001';

UPDATE learning_unit 
            SET metadata_json = '{"tag_code":"TONE_INTERROGATIVE","description":"Lỗi phát âm không phân biệt rõ thanh hỏi và thanh ngã","regions":["CENTRAL","SOUTH"]}'
            WHERE id = '00000000-0000-0000-0002-000000000002';

-- Changeset db/changelog/61-add-roadmap-rules-table.xml::61-add-roadmap-rules-table::antigravity
CREATE TABLE IF NOT EXISTS roadmap_rules (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(50),
                updated_by VARCHAR(50),
                min_percent DOUBLE PRECISION NOT NULL,
                max_percent DOUBLE PRECISION NOT NULL,
                difficulties VARCHAR(255) NOT NULL,
                is_active BOOLEAN NOT NULL DEFAULT TRUE
            );

-- Changeset db/changelog/61-add-roadmap-rules-table.xml::61-seed-roadmap-rules::antigravity
INSERT INTO roadmap_rules (min_percent, max_percent, difficulties, is_active)
            VALUES (0.0, 50.0, 'BEGINNER,INTERMEDIATE,ADVANCED', TRUE),
                   (50.1, 70.0, 'INTERMEDIATE,ADVANCED', TRUE),
                   (80.0, 90.0, 'ADVANCED', TRUE);

-- Changeset db/changelog/62-add-asr-fields-to-speaking-attempt.xml::62-add-asr-fields-to-speaking-attempt::antigravity
ALTER TABLE speaking_attempt ADD asr_score INTEGER;

ALTER TABLE speaking_attempt ADD word_details TEXT;

ALTER TABLE speaking_attempt ADD record_id VARCHAR(255);

-- Changeset db/changelog/63-create-system-config-table.xml::63-create-system-config-table::antigravity
CREATE TABLE IF NOT EXISTS system_config (
                id UUID PRIMARY KEY,
                config_key VARCHAR(100) UNIQUE NOT NULL,
                config_value TEXT NOT NULL,
                description VARCHAR(255),
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(50),
                updated_by VARCHAR(50)
            );

-- Seed API configs
            INSERT INTO system_config (id, config_key, config_value, description, created_by, updated_by)
            VALUES 
                ('8f5a6b0c-7b0a-41f2-9de9-52e6d622f601', 'groq.api-key', '', 'API Key cho Groq AI Cloud Service', 'SYSTEM', 'SYSTEM')
            ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (id, config_key, config_value, description, created_by, updated_by)
            VALUES 
                ('8f5a6b0c-7b0a-41f2-9de9-52e6d622f602', 'groq.model', 'llama-3.3-70b-versatile', 'Model AI chính dùng cho Groq', 'SYSTEM', 'SYSTEM')
            ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (id, config_key, config_value, description, created_by, updated_by)
            VALUES 
                ('8f5a6b0c-7b0a-41f2-9de9-52e6d622f603', 'groq.fallback-models', 'llama-3.3-70b-versatile,llama-3.1-8b-instant', 'Các model fallback dự phòng cho Groq', 'SYSTEM', 'SYSTEM')
            ON CONFLICT (config_key) DO NOTHING;

INSERT INTO system_config (id, config_key, config_value, description, created_by, updated_by)
            VALUES 
                ('8f5a6b0c-7b0a-41f2-9de9-52e6d622f604', 'groq.endpoint', 'https://api.groq.com/openai/v1/chat/completions', 'Endpoint API của Groq', 'SYSTEM', 'SYSTEM')
            ON CONFLICT (config_key) DO NOTHING;

-- Seed Prompt system instruction
            INSERT INTO system_config (id, config_key, config_value, description, created_by, updated_by)
            VALUES 
                ('8f5a6b0c-7b0a-41f2-9de9-52e6d622f605', 'prompt.pronunciation-system-instruction', 'Bạn là chuyên gia phân tích phát âm tiếng Việt. So sánh rawText với targetText và trả về nhận xét sư phạm, cụ thể, hữu ích. Hệ thống hiện tại hỗ trợ chẩn đoán và phân tích các lỗi phát âm/vùng miền sau:\n{availableErrorTags}\nHãy ưu tiên đối chiếu phát hiện lỗi xem người học có mắc phải lỗi nào trong danh sách trên hay không. Không được trả lời chung chung, không được lặp lại nguyên văn targetText, và không được dùng câu ngắn kiểu ''Phát âm chưa chính xác'' nếu chưa giải thích vì sao.', 'System Instruction chấm điểm phát âm', 'SYSTEM', 'SYSTEM')
            ON CONFLICT (config_key) DO NOTHING;

-- Seed Prompt JSON schema
            INSERT INTO system_config (id, config_key, config_value, description, created_by, updated_by)
            VALUES 
                ('8f5a6b0c-7b0a-41f2-9de9-52e6d622f606', 'prompt.pronunciation-schema-instruction', 'Trả về JSON thuần túy với các fields: accuracy (0-100), detectedError (mô tả lỗi cụ thể), feedback (ít nhất 2 câu, nêu lỗi và cách sửa), suggestion (gợi ý ngắn gọn), errorDetail (diễn giải chi tiết hơn feedback), isRegional (boolean), isCorrect (boolean), shapeKey (exact_match, near_match, pronunciation_mismatch, regional_error, missing_input).', 'Cấu trúc schema JSON đầu ra', 'SYSTEM', 'SYSTEM')
            ON CONFLICT (config_key) DO NOTHING;

-- Seed Quiz Explanation
            INSERT INTO system_config (id, config_key, config_value, description, created_by, updated_by)
            VALUES 
                ('8f5a6b0c-7b0a-41f2-9de9-52e6d622f607', 'prompt.quiz-explanation', 'Bạn là giáo viên dạy Tiếng Việt vui nhộn và tận tâm. Hãy giải thích ngắn gọn (1-3 câu) lý do vì sao đáp án là {status}. {timeoutDetail} Câu hỏi: "{question}". Người dùng chọn: "{selectedAnswer}". Đáp án đúng là: "{correctAnswer}". Kỹ năng: {skillType}. {hearingDetail} {correctDetail} Hãy giúp người dùng hiểu rõ kiến thức một cách thân thiện. Trả về JSON có field ''explanation''.', 'Template giải thích câu hỏi Quiz', 'SYSTEM', 'SYSTEM')
            ON CONFLICT (config_key) DO NOTHING;

-- Seed Entry Test Feedback
            INSERT INTO system_config (id, config_key, config_value, description, created_by, updated_by)
            VALUES 
                ('8f5a6b0c-7b0a-41f2-9de9-52e6d622f608', 'prompt.entry-test-feedback', 'Học viên mắc các lỗi sau trong phát âm: {errorDetails} Hãy viết 1 đoạn 3-4 câu nhận xét ngắn gọn, cổ vũ học viên và khuyên học viên nên ưu tiên học lỗi nào trước (dựa trên % độ chính xác thấp nhất). Trả về kết quả dưới dạng JSON có trường ''reply''.', 'Template nhận xét bài đánh giá năng lực đầu vào', 'SYSTEM', 'SYSTEM')
            ON CONFLICT (config_key) DO NOTHING;

-- Changeset db/changelog/64-add-tournament-weekly-features.xml::64-add-tournament-weekly-features::antigravity
ALTER TABLE tournament ADD questions_json JSONB;

ALTER TABLE tournament_participant ADD scores_json JSONB;

-- Changeset db/changelog/65-seed-student-entry-test.xml::65-seed-student-entry-test-completed::antigravity
UPDATE account SET email_verified = TRUE, full_name = 'Học Viên FSA (Đã Test)', has_done_entry_test = TRUE, region = 'SOUTH' WHERE email = 'student@fsa.com';

INSERT INTO entry_test_result (id, created_at, updated_at, account_id, overall_score, detected_region, total_questions, details) VALUES ('00000000-0000-0000-0004-000000000001', NOW(), NOW(), '00000000-0000-0000-0002-000000000002', 85.00, 'SOUTH', 9, '[]');

INSERT INTO account (id, email, password_hash, role_code, is_active, has_done_entry_test, region, email_verified, full_name, created_at, updated_at) VALUES ('00000000-0000-0000-0002-000000000003', 'tested_student@fsa.com', '$2a$10$r8VvQXf.w5G.xK6qKz7u6.Y7v6uB0v4f9w5uB0v4f9w5uB0v4f9w5', 'USER', TRUE, TRUE, 'SOUTH', TRUE, 'Học Viên Đã Test (tested_student@fsa.com)', NOW(), NOW());

INSERT INTO entry_test_result (id, created_at, updated_at, account_id, overall_score, detected_region, total_questions, details) VALUES ('00000000-0000-0000-0004-000000000002', NOW(), NOW(), '00000000-0000-0000-0002-000000000003', 90.00, 'SOUTH', 9, '[]');

-- Changeset db/changelog/65-seed-student-entry-test.xml::66-fix-student-passwords::antigravity
UPDATE account SET password_hash = '$2a$10$dYNuJULG6d7g3tf1hpqQAeyo5qGbjFVSMLWgRKrLMpPxkV9miJzaa' WHERE email IN ('student@fsa.com', 'tested_student@fsa.com');

-- Changeset db/changelog/66-add-lesson-plans-table.xml::66-add-lesson-plans-table::antigravity
CREATE TABLE lesson_plans (id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), educator_id UUID NOT NULL, title VARCHAR(255) NOT NULL, objective TEXT, target_students_json TEXT, achievement_goals_json TEXT, status VARCHAR(50) DEFAULT 'PUBLISHED' NOT NULL, CONSTRAINT lesson_plans_pkey PRIMARY KEY (id), CONSTRAINT fk_lp_educator FOREIGN KEY (educator_id) REFERENCES account(id));

-- Changeset db/changelog/67-create-daily-challenge-attempt-table.xml::67-create-daily-challenge-attempt-table::antigravity
CREATE TABLE daily_challenge_attempt (id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), account_id UUID NOT NULL, challenge_id UUID NOT NULL, is_correct BOOLEAN DEFAULT FALSE NOT NULL, score INTEGER NOT NULL, audio_url TEXT, feedback TEXT, dialect VARCHAR(20), CONSTRAINT daily_challenge_attempt_pkey PRIMARY KEY (id), CONSTRAINT fk_dca_challenge FOREIGN KEY (challenge_id) REFERENCES challenge_bank(id), CONSTRAINT fk_dca_account FOREIGN KEY (account_id) REFERENCES account(id));

-- Changeset db/changelog/68-create-user-feedback-table.xml::68-create-user-feedback-table::antigravity
CREATE TABLE user_feedback (id UUID NOT NULL, sender_id UUID NOT NULL, category VARCHAR(50) NOT NULL, title VARCHAR(255) NOT NULL, content TEXT NOT NULL, status VARCHAR(20) DEFAULT 'PENDING' NOT NULL, screenshot_url TEXT, admin_note TEXT, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT user_feedback_pkey PRIMARY KEY (id), CONSTRAINT fk_user_feedback_sender FOREIGN KEY (sender_id) REFERENCES account(id));

CREATE INDEX idx_user_feedback_sender_id ON user_feedback(sender_id);

CREATE INDEX idx_user_feedback_status ON user_feedback(status);

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-create-minigame-challenge-if-not-exists::antigravity
CREATE TABLE minigame_challenge (id UUID NOT NULL, game_type VARCHAR(50) NOT NULL, pair_type VARCHAR(20) NOT NULL, question_data JSONB NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL, created_by VARCHAR(50), updated_by VARCHAR(50), CONSTRAINT minigame_challenge_pkey PRIMARY KEY (id));

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-add-missing-columns-to-minigame-challenge::antigravity
ALTER TABLE minigame_challenge ADD updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE minigame_challenge ADD created_by VARCHAR(50);

ALTER TABLE minigame_challenge ADD updated_by VARCHAR(50);

UPDATE minigame_challenge SET updated_at = created_at WHERE updated_at IS NULL;

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-matching-pairs-nl::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nón","word2":"lón"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nước","word2":"lước"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nấm","word2":"lấm"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nỗi","word2":"lỗi"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nắng","word2":"lắng"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nụ","word2":"lụ"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-matching-pairs-sx::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sáng","word2":"xáng"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sắc","word2":"xắc"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sơn","word2":"xơn"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sung","word2":"xung"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sấu","word2":"xấu"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sót","word2":"xót"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-matching-pairs-dgir::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"da","word2":"gia"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dạy","word2":"giạy"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dòng","word2":"ròng"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dỗ","word2":"giỗ"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dán","word2":"rán"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dầu","word2":"giầu"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-matching-pairs-trch::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trăng","word2":"chăng"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trời","word2":"chời"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trẻ","word2":"chẻ"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trung","word2":"chung"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trà","word2":"chà"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trắc","word2":"chắc"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-word-guess-nl::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"nắng","hint":"Ánh mặt trời chiếu xuống","category":"Thời tiết"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"lạnh","hint":"Cảm giác khi mùa đông đến","category":"Thời tiết"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"nước","hint":"Chất lỏng uống hàng ngày","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"lửa","hint":"Cháy sáng, tỏa nhiệt","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"nồi","hint":"Dụng cụ nấu ăn","category":"Nhà bếp"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"lưỡi","hint":"Bộ phận trong miệng giúp nếm","category":"Cơ thể"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"nấm","hint":"Mọc ở nơi ẩm ướt, có thể ăn được","category":"Thực phẩm"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"lồng","hint":"Dùng để nhốt chim","category":"Đồ vật"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-word-guess-sx::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"sáng","hint":"Buổi đầu tiên trong ngày","category":"Thời gian"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"xanh","hint":"Màu của lá cây","category":"Màu sắc"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"sông","hint":"Dòng nước chảy dài","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"xuân","hint":"Mùa đầu tiên trong năm","category":"Thời gian"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"sách","hint":"Đọc để học kiến thức","category":"Đồ vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"xóm","hint":"Khu dân cư nhỏ","category":"Địa điểm"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"sợi","hint":"Dùng để dệt vải","category":"Đồ vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"xương","hint":"Bộ khung bên trong cơ thể","category":"Cơ thể"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-word-guess-dgir::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"dừa","hint":"Cây nhiệt đới có nước ngọt","category":"Thực vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"gió","hint":"Không khí chuyển động","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"rừng","hint":"Nơi có nhiều cây cối","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"dạy","hint":"Giáo viên làm việc này","category":"Hành động"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"giày","hint":"Đi ở chân khi ra ngoài","category":"Đồ vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"rắn","hint":"Loài bò sát không chân","category":"Động vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"dầu","hint":"Chất lỏng dùng để chiên","category":"Nhà bếp"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"giấc","hint":"... mơ — khi ngủ","category":"Sinh hoạt"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-word-guess-trch::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"trăng","hint":"Sáng trên bầu trời đêm","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"chim","hint":"Loài có cánh, biết bay","category":"Động vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"trường","hint":"Nơi học sinh đến học","category":"Địa điểm"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"chợ","hint":"Nơi mua bán hàng hóa","category":"Địa điểm"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"trẻ","hint":"Người còn nhỏ tuổi","category":"Con người"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"chạy","hint":"Di chuyển nhanh bằng chân","category":"Hành động"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"trái","hint":"Quả cây, hoặc hướng ngược phải","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"cháo","hint":"Món ăn nấu từ gạo loãng","category":"Thực phẩm"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-scenario-nl::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'N_L', '{"scenario":"Bạn đang mua nước mắm ở chợ"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'N_L', '{"scenario":"Bạn hỏi đường đến nhà sách"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'N_L', '{"scenario":"Bạn gọi món ăn có nhiều từ N và L"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-scenario-sx::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'S_X', '{"scenario":"Bạn đang hỏi mua xe đạp"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'S_X', '{"scenario":"Bạn xin phép thầy giáo"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'S_X', '{"scenario":"Bạn mô tả buổi sáng của mình"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-scenario-dgir::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'D_GI_R', '{"scenario":"Bạn giới thiệu gia đình"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'D_GI_R', '{"scenario":"Bạn hỏi đường đến rừng"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'D_GI_R', '{"scenario":"Bạn kể về giáo viên yêu thích"}', NOW(), NOW());

-- Changeset db/changelog/69-seed-minigame-challenges.xml::69-seed-scenario-trch::antigravity
INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'TR_CH', '{"scenario":"Bạn kể về trường học"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'TR_CH', '{"scenario":"Bạn mua trái cây ở chợ"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'TR_CH', '{"scenario":"Bạn mô tả trẻ em chơi đùa"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-matching-pairs-nl::antigravity
DELETE FROM minigame_challenge WHERE game_type='MATCHING_PAIRS' AND pair_type='N_L';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nón","word2":"lón"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nước","word2":"lước"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nấm","word2":"lấm"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nỗi","word2":"lỗi"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nắng","word2":"lắng"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'N_L', '{"word1":"nụ","word2":"lụ"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-matching-pairs-sx::antigravity
DELETE FROM minigame_challenge WHERE game_type='MATCHING_PAIRS' AND pair_type='S_X';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sáng","word2":"xáng"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sắc","word2":"xắc"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sơn","word2":"xơn"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sung","word2":"xung"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sấu","word2":"xấu"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'S_X', '{"word1":"sót","word2":"xót"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-matching-pairs-dgir::antigravity
DELETE FROM minigame_challenge WHERE game_type='MATCHING_PAIRS' AND pair_type='D_GI_R';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"da","word2":"gia"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dạy","word2":"giạy"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dòng","word2":"ròng"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dỗ","word2":"giỗ"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dán","word2":"rán"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'D_GI_R', '{"word1":"dầu","word2":"giầu"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-matching-pairs-trch::antigravity
DELETE FROM minigame_challenge WHERE game_type='MATCHING_PAIRS' AND pair_type='TR_CH';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trăng","word2":"chăng"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trời","word2":"chời"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trẻ","word2":"chẻ"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trung","word2":"chung"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trà","word2":"chà"}', NOW(), NOW()),
            (gen_random_uuid(), 'MATCHING_PAIRS', 'TR_CH', '{"word1":"trắc","word2":"chắc"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-word-guess-nl::antigravity
DELETE FROM minigame_challenge WHERE game_type='WORD_GUESS' AND pair_type='N_L';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"nắng","hint":"Ánh mặt trời chiếu xuống","category":"Thời tiết"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"lạnh","hint":"Cảm giác khi mùa đông đến","category":"Thời tiết"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"nước","hint":"Chất lỏng uống hàng ngày","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"lửa","hint":"Cháy sáng, tỏa nhiệt","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"nồi","hint":"Dụng cụ nấu ăn","category":"Nhà bếp"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"lưỡi","hint":"Bộ phận trong miệng giúp nếm","category":"Cơ thể"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"nấm","hint":"Mọc ở nơi ẩm ướt, có thể ăn được","category":"Thực phẩm"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'N_L', '{"word":"lồng","hint":"Dùng để nhốt chim","category":"Đồ vật"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-word-guess-sx::antigravity
DELETE FROM minigame_challenge WHERE game_type='WORD_GUESS' AND pair_type='S_X';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"sáng","hint":"Buổi đầu tiên trong ngày","category":"Thời gian"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"xanh","hint":"Màu của lá cây","category":"Màu sắc"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"sông","hint":"Dòng nước chảy dài","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"xuân","hint":"Mùa đầu tiên trong năm","category":"Thời gian"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"sách","hint":"Đọc để học kiến thức","category":"Đồ vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"xóm","hint":"Khu dân cư nhỏ","category":"Địa điểm"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"sợi","hint":"Dùng để dệt vải","category":"Đồ vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'S_X', '{"word":"xương","hint":"Bộ khung bên trong cơ thể","category":"Cơ thể"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-word-guess-dgir::antigravity
DELETE FROM minigame_challenge WHERE game_type='WORD_GUESS' AND pair_type='D_GI_R';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"dừa","hint":"Cây nhiệt đới có nước ngọt","category":"Thực vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"gió","hint":"Không khí chuyển động","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"rừng","hint":"Nơi có nhiều cây cối","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"dạy","hint":"Giáo viên làm việc này","category":"Hành động"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"giày","hint":"Đi ở chân khi ra ngoài","category":"Đồ vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"rắn","hint":"Loài bò sát không chân","category":"Động vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"dầu","hint":"Chất lỏng dùng để chiên","category":"Nhà bếp"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'D_GI_R', '{"word":"giấc","hint":"... mơ — khi ngủ","category":"Sinh hoạt"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-word-guess-trch::antigravity
DELETE FROM minigame_challenge WHERE game_type='WORD_GUESS' AND pair_type='TR_CH';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"trăng","hint":"Sáng trên bầu trời đêm","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"chim","hint":"Loài có cánh, biết bay","category":"Động vật"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"trường","hint":"Nơi học sinh đến học","category":"Địa điểm"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"chợ","hint":"Nơi mua bán hàng hóa","category":"Địa điểm"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"trẻ","hint":"Người còn nhỏ tuổi","category":"Con người"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"chạy","hint":"Di chuyển nhanh bằng chân","category":"Hành động"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"trái","hint":"Quả cây, hoặc hướng ngược phải","category":"Tự nhiên"}', NOW(), NOW()),
            (gen_random_uuid(), 'WORD_GUESS', 'TR_CH', '{"word":"cháo","hint":"Món ăn nấu từ gạo loãng","category":"Thực phẩm"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-scenario-nl::antigravity
DELETE FROM minigame_challenge WHERE game_type='CONVERSATION_SCENARIO' AND pair_type='N_L';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'N_L', '{"scenario":"Bạn đang mua nước mắm ở chợ"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'N_L', '{"scenario":"Bạn hỏi đường đến nhà sách"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'N_L', '{"scenario":"Bạn gọi món ăn có nhiều từ N và L"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-scenario-sx::antigravity
DELETE FROM minigame_challenge WHERE game_type='CONVERSATION_SCENARIO' AND pair_type='S_X';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'S_X', '{"scenario":"Bạn đang hỏi mua xe đạp"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'S_X', '{"scenario":"Bạn xin phép thầy giáo"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'S_X', '{"scenario":"Bạn mô tả buổi sáng của mình"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-scenario-dgir::antigravity
DELETE FROM minigame_challenge WHERE game_type='CONVERSATION_SCENARIO' AND pair_type='D_GI_R';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'D_GI_R', '{"scenario":"Bạn giới thiệu gia đình"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'D_GI_R', '{"scenario":"Bạn hỏi đường đến rừng"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'D_GI_R', '{"scenario":"Bạn kể về giáo viên yêu thích"}', NOW(), NOW());

-- Changeset db/changelog/70-fix-minigame-vietnamese.xml::70-fix-scenario-trch::antigravity
DELETE FROM minigame_challenge WHERE game_type='CONVERSATION_SCENARIO' AND pair_type='TR_CH';

INSERT INTO minigame_challenge (id, game_type, pair_type, question_data, created_at, updated_at) VALUES
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'TR_CH', '{"scenario":"Bạn kể về trường học"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'TR_CH', '{"scenario":"Bạn mua trái cây ở chợ"}', NOW(), NOW()),
            (gen_random_uuid(), 'CONVERSATION_SCENARIO', 'TR_CH', '{"scenario":"Bạn mô tả trẻ em chơi đùa"}', NOW(), NOW());

-- Changeset db/changelog/71-fix-achievement-icons.xml::71-fix-achievement-icon-urls::antigravity
-- Replace broken iconarchive.com URLs with working CDN icons from shields.io/simple-icons or emoji-based SVG data URIs
UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3135/3135706.png' WHERE code = 'LEARN_FIRST_STEP';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2232/2232688.png' WHERE code = 'LEARN_DILIGENT';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3588/3588294.png' WHERE code = 'PHONEME_MASTER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/684/684908.png' WHERE code = 'CONQUEROR_NORTH';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1828/1828884.png' WHERE code = 'STAR_COLLECTOR';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/190/190411.png' WHERE code = 'ERROR_DESTROYER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1995/1995574.png' WHERE code = 'SMART_LEARNER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3176/3176366.png' WHERE code = 'SPEED_RACER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/833/833472.png' WHERE code = 'FRIENDLY_USER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3468/3468377.png' WHERE code = 'DAILY_GIFT';

UPDATE reward_catalog
            SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1067/1067357.png'
            WHERE icon_url LIKE '%iconarchive%' AND code NOT IN (
                'LEARN_FIRST_STEP','LEARN_DILIGENT','PHONEME_MASTER','CONQUEROR_NORTH',
                'STAR_COLLECTOR','ERROR_DESTROYER','SMART_LEARNER','SPEED_RACER',
                'FRIENDLY_USER','DAILY_GIFT'
            );

-- Changeset db/changelog/72-fix-all-badge-icons.xml::72-fix-all-local-badge-icons::antigravity
-- Replace all broken local /icons/badges/ paths with working flaticon CDN URLs
UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3135/3135706.png' WHERE code = 'LEARN_FIRST_LEVEL' OR code = 'LEARN_FIRST_STEP';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2232/2232688.png' WHERE code = 'LEARN_5_LEVELS' OR code = 'LEARN_DILIGENT';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3176/3176366.png' WHERE code = 'LEARN_10_LEVELS';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1067/1067357.png' WHERE code = 'LEARN_15_LEVELS';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/684/684908.png'   WHERE code = 'LEARN_NORTH_COMPLETE' OR code = 'CONQUEROR_NORTH';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/684/684831.png'   WHERE code = 'LEARN_CENTRAL_COMPLETE';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/684/684852.png'   WHERE code = 'LEARN_SOUTH_COMPLETE';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3176/3176272.png' WHERE code = 'LEARN_ALL_REGIONS';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1828/1828884.png' WHERE code = 'LEARN_BEGINNER_DONE' OR code = 'STAR_COLLECTOR';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1828/1828970.png' WHERE code = 'LEARN_INTERMEDIATE_DONE';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1828/1828961.png' WHERE code = 'LEARN_ADVANCED_DONE';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1828/1828884.png' WHERE code = 'LEARN_FIRST_3STAR';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3588/3588294.png' WHERE code = 'LEARN_10_3STAR';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3588/3588614.png' WHERE code = 'LEARN_ALL_3STAR';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/190/190411.png'   WHERE code = 'LEARN_RETRY_WIN' OR code = 'ERROR_DESTROYER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3588/3588294.png' WHERE code = 'PHONEME_FIRST_PASS' OR code = 'PHONEME_MASTER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2620/2620244.png' WHERE code = 'PHONEME_NL_MASTER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2620/2620244.png' WHERE code = 'PHONEME_SX_MASTER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2620/2620244.png' WHERE code = 'PHONEME_DGIR_MASTER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2620/2620244.png' WHERE code = 'PHONEME_TRCH_MASTER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3588/3588614.png' WHERE code = 'PHONEME_ALL_PAIRS';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1995/1995574.png' WHERE code = 'PHONEME_TONE_GOOD' OR code = 'SMART_LEARNER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1995/1995574.png' WHERE code = 'PHONEME_TONE_MASTER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3588/3588294.png' WHERE code = 'PHONEME_PERFECT_SESSION';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3588/3588294.png' WHERE code = 'PHONEME_PERFECT_5';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3135/3135706.png' WHERE code = 'PHONEME_ENTRY_DONE';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3176/3176366.png' WHERE code = 'PHONEME_SPEED' OR code = 'SPEED_RACER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2991/2991148.png' WHERE code = 'MINI_FIRST_PLAY';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2991/2991148.png' WHERE code = 'MINI_10_PLAYS';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2991/2991148.png' WHERE code = 'MINI_50_PLAYS';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/2991/2991148.png' WHERE code LIKE 'MINI_%';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/833/833472.png'   WHERE code = 'FRIENDLY_USER';

UPDATE reward_catalog SET icon_url = 'https://cdn-icons-png.flaticon.com/512/3468/3468377.png' WHERE code = 'DAILY_GIFT';

UPDATE reward_catalog
            SET icon_url = 'https://cdn-icons-png.flaticon.com/512/1067/1067357.png'
            WHERE icon_url LIKE '/icons/badges/%'
               OR icon_url LIKE '%iconarchive%'
               OR icon_url IS NULL
               OR icon_url = '';

-- Changeset db/changelog/73-reset-curriculum-and-quizzes.xml::73-reset-curriculum-and-quizzes::claude
-- Wipe LEVEL + QUIZ learning_unit rows (keep DIALECT) and clean up referencing progress data.
DO $$
DECLARE
    v_unit_ids UUID[];
BEGIN
    SELECT array_agg(id) INTO v_unit_ids
    FROM learning_unit WHERE type IN ('LEVEL','QUIZ');

    IF v_unit_ids IS NULL OR array_length(v_unit_ids, 1) IS NULL THEN
        RETURN;
    END IF;

    -- 1) progress / activity / paths referencing these units
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='session_detail') THEN
        DELETE FROM session_detail
        WHERE content_item_id IN (
            SELECT id FROM content_item WHERE learning_unit_id = ANY(v_unit_ids)
        );
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='study_session') THEN
        DELETE FROM study_session WHERE session_type IN ('QUIZ','LEVEL','LESSON');
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='account_learning_unit') THEN
        DELETE FROM account_learning_unit WHERE learning_unit_id = ANY(v_unit_ids);
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='custom_path_progress') THEN
        DELETE FROM custom_path_progress WHERE learning_unit_id = ANY(v_unit_ids);
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='custom_path_levels') THEN
        DELETE FROM custom_path_levels WHERE level_id = ANY(v_unit_ids);
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='quiz_challenge_item') THEN
        DELETE FROM quiz_challenge_item WHERE quiz_id = ANY(v_unit_ids);
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='assignment') THEN
        DELETE FROM assignment WHERE learning_unit_id = ANY(v_unit_ids);
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='content_item') THEN
        DELETE FROM content_item WHERE learning_unit_id = ANY(v_unit_ids);
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='lesson_plan_item') THEN
        DELETE FROM lesson_plan_item WHERE learning_unit_id = ANY(v_unit_ids);
    END IF;

    -- 2) wipe QUIZ first (children), then LEVEL (parents)
    DELETE FROM learning_unit WHERE type = 'QUIZ';
    DELETE FROM learning_unit WHERE type = 'LEVEL';
END $$;

-- Changeset db/changelog/74-seed-curriculum-levels.xml::74-seed-curriculum-levels::claude
-- Seed 15 LEVEL learning_units (5 per dialect) under existing DIALECT roots.
DO $$
DECLARE
    v_north   UUID;
    v_central UUID;
    v_south   UUID;
BEGIN
    SELECT id INTO v_north   FROM learning_unit WHERE type='DIALECT' AND name='NORTH'   LIMIT 1;
    SELECT id INTO v_central FROM learning_unit WHERE type='DIALECT' AND name='CENTRAL' LIMIT 1;
    SELECT id INTO v_south   FROM learning_unit WHERE type='DIALECT' AND name='SOUTH'   LIMIT 1;

    IF v_north IS NULL OR v_central IS NULL OR v_south IS NULL THEN
        RAISE EXCEPTION 'Missing DIALECT learning_unit rows (NORTH/CENTRAL/SOUTH). Expected from changeset 25.';
    END IF;

    INSERT INTO learning_unit
        (id, parent_id, name, type, metadata_json, difficulty_level, error_tag, created_at, updated_at, created_by, updated_by)
    VALUES
    -- ===== NORTH (n/l) =====
    ('00000000-0000-0000-0073-110000000000', v_north,
     'Cơ bản 1 - Chào hỏi (Bắc, n/l)', 'LEVEL',
     '{"levelOrder":1,"min_stars_required":0,"ai_threshold":70,"time_per_question_seconds":120,"description":"Làm quen phân biệt n/l qua các từ chào hỏi đơn giản.","theme":"Chào hỏi","region":"NORTH","errorTag":"B_NL"}'::jsonb,
     'BEGINNER', 'B_NL', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-120000000000', v_north,
     'Cơ bản 2 - Gia đình (Bắc, n/l)', 'LEVEL',
     '{"levelOrder":2,"min_stars_required":5,"ai_threshold":72,"time_per_question_seconds":110,"description":"Phân biệt n/l trong từ vựng gia đình thường ngày.","theme":"Gia đình","region":"NORTH","errorTag":"B_NL"}'::jsonb,
     'BEGINNER', 'B_NL', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-130000000000', v_north,
     'Trung cấp 1 - Công việc (Bắc, n/l)', 'LEVEL',
     '{"levelOrder":3,"min_stars_required":10,"ai_threshold":75,"time_per_question_seconds":100,"description":"Phân biệt n/l qua tình huống công việc.","theme":"Công việc","region":"NORTH","errorTag":"B_NL"}'::jsonb,
     'INTERMEDIATE', 'B_NL', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-140000000000', v_north,
     'Trung cấp 2 - Mua sắm (Bắc, n/l)', 'LEVEL',
     '{"levelOrder":4,"min_stars_required":15,"ai_threshold":78,"time_per_question_seconds":90,"description":"Câu phức và đoạn hội thoại mua sắm với n/l.","theme":"Mua sắm","region":"NORTH","errorTag":"B_NL"}'::jsonb,
     'INTERMEDIATE', 'B_NL', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-150000000000', v_north,
     'Nâng cao - Du lịch (Bắc, n/l)', 'LEVEL',
     '{"levelOrder":5,"min_stars_required":20,"ai_threshold":80,"time_per_question_seconds":75,"description":"Đoạn văn dài và hội thoại du lịch luyện n/l nâng cao.","theme":"Du lịch","region":"NORTH","errorTag":"B_NL"}'::jsonb,
     'ADVANCED', 'B_NL', NOW(), NOW(), 'system', 'system'),

    -- ===== CENTRAL (s/x, tr/ch) =====
    ('00000000-0000-0000-0073-210000000000', v_central,
     'Cơ bản 1 - Chào hỏi (Trung, s/x, tr/ch)', 'LEVEL',
     '{"levelOrder":1,"min_stars_required":0,"ai_threshold":70,"time_per_question_seconds":120,"description":"Làm quen phân biệt s/x và tr/ch qua từ chào hỏi đơn giản.","theme":"Chào hỏi","region":"CENTRAL","errorTag":"T_SX_TRCH"}'::jsonb,
     'BEGINNER', 'T_SX_TRCH', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-220000000000', v_central,
     'Cơ bản 2 - Gia đình (Trung, s/x, tr/ch)', 'LEVEL',
     '{"levelOrder":2,"min_stars_required":5,"ai_threshold":72,"time_per_question_seconds":110,"description":"Phân biệt s/x và tr/ch trong từ vựng gia đình.","theme":"Gia đình","region":"CENTRAL","errorTag":"T_SX_TRCH"}'::jsonb,
     'BEGINNER', 'T_SX_TRCH', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-230000000000', v_central,
     'Trung cấp 1 - Công việc (Trung, s/x, tr/ch)', 'LEVEL',
     '{"levelOrder":3,"min_stars_required":10,"ai_threshold":75,"time_per_question_seconds":100,"description":"Phân biệt s/x và tr/ch qua tình huống công việc.","theme":"Công việc","region":"CENTRAL","errorTag":"T_SX_TRCH"}'::jsonb,
     'INTERMEDIATE', 'T_SX_TRCH', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-240000000000', v_central,
     'Trung cấp 2 - Mua sắm (Trung, s/x, tr/ch)', 'LEVEL',
     '{"levelOrder":4,"min_stars_required":15,"ai_threshold":78,"time_per_question_seconds":90,"description":"Câu phức và đoạn hội thoại mua sắm với s/x, tr/ch.","theme":"Mua sắm","region":"CENTRAL","errorTag":"T_SX_TRCH"}'::jsonb,
     'INTERMEDIATE', 'T_SX_TRCH', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-250000000000', v_central,
     'Nâng cao - Du lịch (Trung, s/x, tr/ch)', 'LEVEL',
     '{"levelOrder":5,"min_stars_required":20,"ai_threshold":80,"time_per_question_seconds":75,"description":"Đoạn văn dài và hội thoại du lịch luyện s/x, tr/ch nâng cao.","theme":"Du lịch","region":"CENTRAL","errorTag":"T_SX_TRCH"}'::jsonb,
     'ADVANCED', 'T_SX_TRCH', NOW(), NOW(), 'system', 'system'),

    -- ===== SOUTH (d/gi/r) =====
    ('00000000-0000-0000-0073-310000000000', v_south,
     'Cơ bản 1 - Chào hỏi (Nam, d/gi/r)', 'LEVEL',
     '{"levelOrder":1,"min_stars_required":0,"ai_threshold":70,"time_per_question_seconds":120,"description":"Làm quen phân biệt d/gi/r qua từ chào hỏi đơn giản.","theme":"Chào hỏi","region":"SOUTH","errorTag":"N_DGIR"}'::jsonb,
     'BEGINNER', 'N_DGIR', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-320000000000', v_south,
     'Cơ bản 2 - Gia đình (Nam, d/gi/r)', 'LEVEL',
     '{"levelOrder":2,"min_stars_required":5,"ai_threshold":72,"time_per_question_seconds":110,"description":"Phân biệt d/gi/r trong từ vựng gia đình.","theme":"Gia đình","region":"SOUTH","errorTag":"N_DGIR"}'::jsonb,
     'BEGINNER', 'N_DGIR', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-330000000000', v_south,
     'Trung cấp 1 - Công việc (Nam, d/gi/r)', 'LEVEL',
     '{"levelOrder":3,"min_stars_required":10,"ai_threshold":75,"time_per_question_seconds":100,"description":"Phân biệt d/gi/r qua tình huống công việc.","theme":"Công việc","region":"SOUTH","errorTag":"N_DGIR"}'::jsonb,
     'INTERMEDIATE', 'N_DGIR', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-340000000000', v_south,
     'Trung cấp 2 - Mua sắm (Nam, d/gi/r)', 'LEVEL',
     '{"levelOrder":4,"min_stars_required":15,"ai_threshold":78,"time_per_question_seconds":90,"description":"Câu phức và đoạn hội thoại mua sắm với d/gi/r.","theme":"Mua sắm","region":"SOUTH","errorTag":"N_DGIR"}'::jsonb,
     'INTERMEDIATE', 'N_DGIR', NOW(), NOW(), 'system', 'system'),
    ('00000000-0000-0000-0073-350000000000', v_south,
     'Nâng cao - Du lịch (Nam, d/gi/r)', 'LEVEL',
     '{"levelOrder":5,"min_stars_required":20,"ai_threshold":80,"time_per_question_seconds":75,"description":"Đoạn văn dài và hội thoại du lịch luyện d/gi/r nâng cao.","theme":"Du lịch","region":"SOUTH","errorTag":"N_DGIR"}'::jsonb,
     'ADVANCED', 'N_DGIR', NOW(), NOW(), 'system', 'system');
END $$;

-- Changeset db/changelog/75-seed-quizzes-north.xml::75-seed-quizzes-north::claude
-- Seed NORTH region: 75 QUIZ + 750 challenge_bank rows + quiz_challenge_item links. Idempotent.
DO $$
DECLARE
    v_old_quiz_ids UUID[];
BEGIN
    SELECT array_agg(id) INTO v_old_quiz_ids
    FROM learning_unit
    WHERE type='QUIZ'
      AND parent_id IN (
          '00000000-0000-0000-0073-110000000000'::uuid,
          '00000000-0000-0000-0073-120000000000'::uuid,
          '00000000-0000-0000-0073-130000000000'::uuid,
          '00000000-0000-0000-0073-140000000000'::uuid,
          '00000000-0000-0000-0073-150000000000'::uuid
      );

    IF v_old_quiz_ids IS NOT NULL AND array_length(v_old_quiz_ids,1) > 0 THEN
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='quiz_challenge_item') THEN
            DELETE FROM quiz_challenge_item WHERE quiz_id = ANY(v_old_quiz_ids);
        END IF;
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='account_learning_unit') THEN
            DELETE FROM account_learning_unit WHERE learning_unit_id = ANY(v_old_quiz_ids);
        END IF;
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='custom_path_progress') THEN
            DELETE FROM custom_path_progress WHERE learning_unit_id = ANY(v_old_quiz_ids);
        END IF;
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='content_item') THEN
            DELETE FROM content_item WHERE learning_unit_id = ANY(v_old_quiz_ids);
        END IF;
        DELETE FROM learning_unit WHERE id = ANY(v_old_quiz_ids);
    END IF;

    -- Clean up tables that reference challenge_bank by id, then delete the bank rows themselves.
    -- speaking_attempt has only `challenge_id` (FK to challenge_bank.id), no challenge_bank_id column.
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='speaking_attempt') THEN
        DELETE FROM speaking_attempt WHERE challenge_id IN (
            SELECT id FROM challenge_bank WHERE id::text LIKE '00000000-0000-0073-1%'
        );
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='daily_challenge_attempt') THEN
        DELETE FROM daily_challenge_attempt WHERE challenge_id IN (
            SELECT id FROM challenge_bank WHERE id::text LIKE '00000000-0000-0073-1%'
        );
    END IF;
    DELETE FROM challenge_bank WHERE id::text LIKE '00000000-0000-0073-1%';
END $$;

DO $$
DECLARE
    v_levels UUID[] := ARRAY[
        '00000000-0000-0000-0073-110000000000'::uuid,
        '00000000-0000-0000-0073-120000000000'::uuid,
        '00000000-0000-0000-0073-130000000000'::uuid,
        '00000000-0000-0000-0073-140000000000'::uuid,
        '00000000-0000-0000-0073-150000000000'::uuid
    ];
    v_chapter_themes TEXT[] := ARRAY['Chào hỏi','Gia đình','Công việc','Mua sắm','Du lịch'];
    v_seconds_per_q INT[] := ARRAY[120,110,100,90,75];

    v_pairs TEXT[][] := ARRAY[
        ARRAY['nón',  'lón',  'vật đội đầu che nắng'],
        ARRAY['nước', 'lước', 'chất lỏng để uống'],
        ARRAY['nắng', 'lắng', 'ánh sáng mặt trời'],
        ARRAY['nông', 'lông', 'thuộc nghề trồng trọt'],
        ARRAY['năm',  'lăm',  'đơn vị thời gian 12 tháng'],
        ARRAY['nỗi',  'lỗi',  'cảm xúc trong lòng'],
        ARRAY['nụ',   'lụ',   'hoa chưa nở'],
        ARRAY['nấm',  'lấm',  'một loại thực vật'],
        ARRAY['nhà',  'là',   'nơi ở của gia đình'],
        ARRAY['nói',  'lói',  'phát ra tiếng để giao tiếp'],
        ARRAY['lá',   'ná',   'phần xanh của cây'],
        ARRAY['lan',  'nan',  'tên một loài hoa'],
        ARRAY['lúa',  'núa',  'cây trồng cho hạt gạo'],
        ARRAY['leo',  'neo',  'bám lên cao'],
        ARRAY['lon',  'non',  'hộp đựng đồ uống'],
        ARRAY['làm',  'nàm',  'thực hiện công việc'],
        ARRAY['lớn',  'nớn',  'kích thước to'],
        ARRAY['lành', 'nành', 'không bị tổn thương'],
        ARRAY['lửa',  'nửa',  'ngọn cháy nóng'],
        ARRAY['lắng', 'nắng', 'tập trung nghe']
    ];

    v_phrases TEXT[] := ARRAY[
        'chiếc nón lá',     'cốc nước lọc',     'ánh nắng ban mai',  'người nông dân',    'năm mới đến',
        'nỗi nhớ quê',      'nụ hoa hồng',      'cây nấm rơm',       'ngôi nhà nhỏ',      'nói chuyện vui',
        'lá xanh tươi',     'hoa lan trắng',    'cánh đồng lúa',     'leo lên đồi',       'lon nước ngọt',
        'làm bài tập',      'lớn lên từng ngày','vết thương lành',   'ngọn lửa ấm',       'lắng nghe lời mẹ'
    ];

    v_sent_ch3 TEXT[] := ARRAY[
        'Anh nông dân làm việc trên cánh đồng từ sáng sớm.',
        'Nhóm em hôm nay nộp báo cáo đúng hạn.',
        'Năm nay công ty tuyển thêm nhiều nhân viên trẻ.',
        'Cô ấy nói chuyện với khách hàng rất lịch sự.',
        'Nội dung cuộc họp được ghi lại đầy đủ.',
        'Lương tháng này được trả vào ngày mùng năm.',
        'Lan đảm nhận phần thiết kế của dự án mới.',
        'Anh ấy là người làm việc cẩn thận và chu đáo.',
        'Lãnh đạo công ty luôn lắng nghe ý kiến nhân viên.',
        'Lớp tập huấn về kỹ năng làm việc nhóm bắt đầu vào sáng mai.'
    ];

    v_sent_ch4 TEXT[] := ARRAY[
        'Mẹ ra chợ mua một chiếc nón lá mới và một lít nước mắm.',
        'Năm nay giá lương thực lên cao nên gia đình phải chi tiêu tiết kiệm hơn.',
        'Nồi cơm điện loại lớn của hãng Nhật được nhiều người tin dùng.',
        'Nông sản miền Bắc năm nay được mùa, giá thấp hơn năm ngoái.',
        'Người nội trợ thường chọn nấm tươi vào sáng sớm để được loại ngon nhất.',
        'Lon nước ngọt được đặt trên kệ phía bên trái cửa hàng.',
        'Lúa nếp loại ngon được bán theo cân, giá không thay đổi nhiều.',
        'Lò vi sóng nhỏ gọn rất phù hợp cho gia đình ít người.',
        'Lan đến siêu thị vào buổi trưa nên không phải xếp hàng lâu.',
        'Lụa làng Vạn Phúc nổi tiếng là quà lưu niệm được khách nước ngoài ưa thích.'
    ];

    v_sent_ch5 TEXT[] := ARRAY[
        'Năm ngoái tôi đi Lào Cai, ngắm cánh đồng lúa non trải dài dưới ánh nắng vàng.',
        'Lên đỉnh Lũng Cú giữa mùa nắng, cảm giác lành lạnh khiến lòng nao nao khó tả.',
        'Làng cổ Đường Lâm vẫn giữ nét nhà tranh vách đất, nồi đồng và lu nước cổ kính.',
        'Lữ khách đến Lạng Sơn nên nán lại lâu hơn để tìm hiểu nét lễ hội của người Nùng.',
        'Nhóm bạn tôi lên Ninh Bình chèo thuyền, len lỏi qua những hang nước nhỏ và lối đá.',
        'Lên Lai Châu mùa nắng nóng, đường đèo quanh co dẫn tới những bản làng nằm lưng núi.',
        'Người dân nơi đây thường nướng nấm rừng trên lá lốt non, hương thơm lan khắp lối đi.',
        'Lữ khách nên lên kế hoạch trước, lựa thời tiết phù hợp để chuyến đi thêm trọn vẹn.',
        'Nai rừng đôi khi lao qua lối nhỏ, tài xế nên lái nhẹ và nhường lối cho động vật.',
        'Năm nay tôi lên Lạng Sơn vào lúc nắng lên, ánh nắng nhuộm vàng cả lưng núi.'
    ];

    v_quiz_id   UUID;
    v_chal_id   UUID;
    v_chapter   INT;
    v_quiz_idx  INT;
    v_q_idx     INT;
    v_seed      INT;
    v_pair      TEXT[];
    v_correct   TEXT;
    v_wrong     TEXT;
    v_meaning   TEXT;
    v_text      TEXT;
    v_skill_for_quiz TEXT;
    v_skill     TEXT;
    v_metadata  JSONB;
    v_chal_meta JSONB;
    v_pair_idx  INT;
    v_pair_count INT := array_length(v_pairs, 1);
BEGIN
    FOR v_chapter IN 1..5 LOOP
        FOR v_quiz_idx IN 1..15 LOOP
            IF v_quiz_idx <= 5 THEN
                v_skill_for_quiz := 'SPEAKING';
            ELSIF v_quiz_idx <= 9 THEN
                v_skill_for_quiz := 'LISTENING';
            ELSIF v_quiz_idx <= 12 THEN
                v_skill_for_quiz := 'WRITING';
            ELSE
                v_skill_for_quiz := 'MIX';
            END IF;

            v_quiz_id := ('00000000-0000-0073-1' ||
                          v_chapter::text ||
                          lpad(v_quiz_idx::text, 2, '0') ||
                          '-000000000000')::uuid;

            v_metadata := jsonb_build_object(
                'description', 'Bài kiểm tra ' || v_skill_for_quiz || ' - chương "' || v_chapter_themes[v_chapter] || '" - chữa ngọng n/l (Bắc).',
                'instructions',
                    CASE v_skill_for_quiz
                        WHEN 'SPEAKING'  THEN 'Đọc to và rõ phần văn bản, chú ý phân biệt âm n và l.'
                        WHEN 'LISTENING' THEN 'Nghe AI đọc và chọn từ phát âm đúng (n hay l).'
                        WHEN 'WRITING'   THEN 'Nghe và gõ lại chính xác (chú ý chữ n hoặc l).'
                        ELSE 'Câu hỏi tổng hợp 3 kỹ năng nói/nghe/viết - chữa ngọng n/l.'
                    END,
                'time_limit_seconds', v_seconds_per_q[v_chapter] * 10,
                'passing_score', 80,
                'points_per_question', 10,
                'question_count', 10,
                'comment', '',
                'skill_type', v_skill_for_quiz,
                'orderIndex', v_quiz_idx,
                'status', 'APPROVED'
            );

            INSERT INTO learning_unit
                (id, parent_id, name, type, metadata_json, difficulty_level, error_tag,
                 created_at, updated_at, created_by, updated_by)
            VALUES (
                v_quiz_id,
                v_levels[v_chapter],
                'Bắc - C' || v_chapter || ' - Quiz ' || v_quiz_idx || ' (' || v_skill_for_quiz || ')',
                'QUIZ',
                v_metadata,
                CASE WHEN v_chapter <= 2 THEN 'BEGINNER'
                     WHEN v_chapter <= 4 THEN 'INTERMEDIATE'
                     ELSE 'ADVANCED' END,
                'B_NL',
                NOW(), NOW(), 'system', 'system'
            );

            FOR v_q_idx IN 1..10 LOOP
                v_seed := ((v_chapter - 1) * 150) + ((v_quiz_idx - 1) * 10) + v_q_idx;
                v_pair_idx := ((v_seed - 1) % v_pair_count) + 1;
                v_correct := v_pairs[v_pair_idx][1];
                v_wrong   := v_pairs[v_pair_idx][2];
                v_meaning := v_pairs[v_pair_idx][3];

                IF v_chapter = 1 THEN
                    v_text := v_correct;
                ELSIF v_chapter = 2 THEN
                    v_text := v_phrases[v_pair_idx];
                ELSIF v_chapter = 3 THEN
                    v_text := v_sent_ch3[((v_q_idx - 1) % 10) + 1];
                ELSIF v_chapter = 4 THEN
                    v_text := v_sent_ch4[((v_q_idx - 1) % 10) + 1];
                ELSE
                    v_text := v_sent_ch5[((v_q_idx - 1) % 10) + 1];
                END IF;

                IF v_skill_for_quiz = 'MIX' THEN
                    v_skill := (ARRAY['SPEAKING','LISTENING','WRITING'])[((v_q_idx - 1) % 3) + 1];
                ELSE
                    v_skill := v_skill_for_quiz;
                END IF;

                IF v_skill = 'SPEAKING' THEN
                    v_chal_meta := jsonb_build_object(
                        'type','SPEAKING',
                        'transcript', v_text,
                        'targetWords', jsonb_build_array(v_correct),
                        'errorTag','B_NL',
                        'explanation','Cặp dễ nhầm n/l: phát âm đúng "' || v_correct || '" (nghĩa: ' || v_meaning || '). Nhiều người nhầm thành "' || v_wrong || '".'
                    );
                ELSIF v_skill = 'LISTENING' THEN
                    v_chal_meta := jsonb_build_object(
                        'type','LISTENING',
                        'transcript', v_text,
                        'audioWord', v_correct,
                        'correctAnswer', v_correct,
                        'options', jsonb_build_array(v_correct, v_wrong),
                        'errorTag','B_NL',
                        'explanation','AI đọc đúng từ "' || v_correct || '". Đáp án đúng là "' || v_correct || '" (nghĩa: ' || v_meaning || '), không phải "' || v_wrong || '".'
                    );
                ELSE
                    v_chal_meta := jsonb_build_object(
                        'type','WRITING',
                        'transcript', v_text,
                        'expectedAnswer', v_text,
                        'targetWords', jsonb_build_array(v_correct),
                        'errorTag','B_NL',
                        'explanation','Nghe và gõ chính xác. Lưu ý từ "' || v_correct || '" (nghĩa: ' || v_meaning || ') - viết bằng "n" hoặc "l" cho đúng.'
                    );
                END IF;

                v_chal_id := ('00000000-0000-0073-1' ||
                              v_chapter::text ||
                              lpad(v_quiz_idx::text, 2, '0') ||
                              '-0000000000' ||
                              lpad(v_q_idx::text, 2, '0'))::uuid;

                INSERT INTO challenge_bank
                    (id, content_text, skill_type, region, metadata_json, created_at, updated_at)
                VALUES (
                    v_chal_id, v_text, v_skill, 'BAC', v_chal_meta, NOW(), NOW()
                );

                INSERT INTO quiz_challenge_item
                    (id, quiz_id, challenge_bank_id, challenge_id, order_index)
                VALUES (
                    gen_random_uuid(), v_quiz_id, v_chal_id, v_chal_id, v_q_idx
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- Changeset db/changelog/76-seed-quizzes-central.xml::76-seed-quizzes-central::claude
-- Seed CENTRAL: 75 QUIZ + 750 challenge_bank rows + quiz_challenge_item links. Idempotent.
DO $$
DECLARE
    v_old_quiz_ids UUID[];
BEGIN
    SELECT array_agg(id) INTO v_old_quiz_ids
    FROM learning_unit
    WHERE type='QUIZ'
      AND parent_id IN (
          '00000000-0000-0000-0073-210000000000'::uuid,
          '00000000-0000-0000-0073-220000000000'::uuid,
          '00000000-0000-0000-0073-230000000000'::uuid,
          '00000000-0000-0000-0073-240000000000'::uuid,
          '00000000-0000-0000-0073-250000000000'::uuid
      );

    IF v_old_quiz_ids IS NOT NULL AND array_length(v_old_quiz_ids,1) > 0 THEN
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='quiz_challenge_item') THEN
            DELETE FROM quiz_challenge_item WHERE quiz_id = ANY(v_old_quiz_ids);
        END IF;
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='account_learning_unit') THEN
            DELETE FROM account_learning_unit WHERE learning_unit_id = ANY(v_old_quiz_ids);
        END IF;
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='custom_path_progress') THEN
            DELETE FROM custom_path_progress WHERE learning_unit_id = ANY(v_old_quiz_ids);
        END IF;
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='content_item') THEN
            DELETE FROM content_item WHERE learning_unit_id = ANY(v_old_quiz_ids);
        END IF;
        DELETE FROM learning_unit WHERE id = ANY(v_old_quiz_ids);
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='speaking_attempt') THEN
        DELETE FROM speaking_attempt WHERE challenge_id IN (
            SELECT id FROM challenge_bank WHERE id::text LIKE '00000000-0000-0073-2%'
        );
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='daily_challenge_attempt') THEN
        DELETE FROM daily_challenge_attempt WHERE challenge_id IN (
            SELECT id FROM challenge_bank WHERE id::text LIKE '00000000-0000-0073-2%'
        );
    END IF;
    DELETE FROM challenge_bank WHERE id::text LIKE '00000000-0000-0073-2%';
END $$;

DO $$
DECLARE
    v_levels UUID[] := ARRAY[
        '00000000-0000-0000-0073-210000000000'::uuid,
        '00000000-0000-0000-0073-220000000000'::uuid,
        '00000000-0000-0000-0073-230000000000'::uuid,
        '00000000-0000-0000-0073-240000000000'::uuid,
        '00000000-0000-0000-0073-250000000000'::uuid
    ];
    v_chapter_themes TEXT[] := ARRAY['Chào hỏi','Gia đình','Công việc','Mua sắm','Du lịch'];
    v_seconds_per_q INT[] := ARRAY[120,110,100,90,75];

    v_pairs TEXT[][] := ARRAY[
        ARRAY['sáng', 'xáng',  'thời điểm đầu ngày',          'S_X'],
        ARRAY['sông', 'xông',  'dòng nước lớn chảy',          'S_X'],
        ARRAY['sắc',  'xắc',   'màu hoặc vẻ ngoài tươi tắn',  'S_X'],
        ARRAY['sơn',  'xơn',   'phủ một lớp màu lên bề mặt',  'S_X'],
        ARRAY['sung', 'xung',  'một loại quả',                'S_X'],
        ARRAY['sấu',  'xấu',   'một loại quả chua',           'S_X'],
        ARRAY['sót',  'xót',   'thiếu hoặc bỏ qua',           'S_X'],
        ARRAY['xa',   'sa',    'cách một khoảng cách lớn',    'S_X'],
        ARRAY['xinh', 'sinh',  'có vẻ đẹp dễ thương',         'S_X'],
        ARRAY['xôi',  'sôi',   'món ăn từ gạo nếp',           'S_X'],
        ARRAY['xanh', 'sanh',  'màu của lá cây',              'S_X'],
        ARRAY['xích', 'sích',  'dây kim loại để buộc',        'S_X'],
        ARRAY['trăng','chăng', 'mặt trăng trên bầu trời',     'TR_CH'],
        ARRAY['trời', 'chời',  'không gian phía trên',        'TR_CH'],
        ARRAY['trẻ',  'chẻ',   'còn ít tuổi',                 'TR_CH'],
        ARRAY['trung','chung', 'ở giữa',                       'TR_CH'],
        ARRAY['trà',  'chà',   'loại đồ uống từ lá cây',      'TR_CH'],
        ARRAY['trắc', 'chắc',  'loại gỗ quý',                  'TR_CH'],
        ARRAY['chợ',  'trợ',   'nơi mua bán hàng hoá',         'TR_CH'],
        ARRAY['chai', 'trai',  'vật đựng chất lỏng',           'TR_CH'],
        ARRAY['chậm', 'trậm',  'tốc độ thấp',                  'TR_CH'],
        ARRAY['chuyện','truyện','sự việc đã xảy ra',          'TR_CH']
    ];

    v_phrases TEXT[] := ARRAY[
        'buổi sáng đẹp trời',  'dòng sông quê',         'màu sắc rực rỡ',         'sơn cửa nhà mới',     'quả sung chín',
        'quả sấu chua',         'bỏ sót một câu',        'đường xa quá',            'em bé xinh xắn',      'nồi xôi nóng',
        'lá cây xanh tươi',     'cuộn xích sắt',         'ánh trăng tròn',          'bầu trời trong xanh','em bé trẻ thơ',
        'miền trung quê hương','một ấm trà nóng',       'gỗ trắc quý hiếm',        'phiên chợ sớm',        'một chai nước suối',
        'đi chậm thôi',         'kể chuyện vui'
    ];

    v_sent_ch3 TEXT[] := ARRAY[
        'Sáng nay sếp triệu tập cuộc họp về sản xuất.',
        'Chuyên viên đang xử lý sự cố trong dây chuyền.',
        'Chị Trinh phụ trách sổ sách của phòng kế toán.',
        'Sản phẩm mới được xuất xưởng vào đầu tuần sau.',
        'Trưởng phòng đã trao đổi với khách hàng từ sáng.',
        'Sếp trẻ rất chu đáo trong việc chăm sóc nhân viên.',
        'Chiến lược sản xuất xanh là trọng tâm năm nay.',
        'Sai sót trong báo cáo đã được phát hiện kịp thời.',
        'Trong sản xuất, sai sót nhỏ có thể gây tổn thất lớn.',
        'Chuyên môn của chị Sáu giúp đội xử lý vấn đề rất nhanh.'
    ];

    v_sent_ch4 TEXT[] := ARRAY[
        'Chị Trang ra chợ Xuân mua một cân sấu chín và vài trái xoài tươi.',
        'Trên sạp hàng, các sản phẩm xanh sạch được xếp gọn gàng theo từng loại.',
        'Trước khi mua, chị thường xem kỹ hạn sử dụng trên chai sữa.',
        'Phiên chợ sáng ở miền Trung trao đổi nhiều sản vật của quê nhà.',
        'Trong siêu thị, sản phẩm được sắp xếp theo từng chuyên mục riêng biệt.',
        'Sạp xôi sáng đầu chợ luôn đông khách vì hương vị đặc trưng của địa phương.',
        'Chị Sáu chọn một bịch sương sâm và hai trái xoài chín để mang về.',
        'Tại chợ Trung, các sản vật được lựa chọn kỹ trước khi bày bán.',
        'Chị Xuân chọn xoài tròn, sấu chua và một ít trái sung cho bữa cơm chiều.',
        'Mỗi sáng chị Trang ra chợ sớm để xếp hàng chọn được hàng tươi nhất.'
    ];

    v_sent_ch5 TEXT[] := ARRAY[
        'Sáng sớm, chuyến xe đưa chúng tôi sang miền Trung lăn bánh trên con đường còn đẫm sương.',
        'Trong chuyến đi này, sông Hương trong xanh chảy chầm chậm qua vùng đất thơ ca xứ Huế.',
        'Chuyến tàu sáng đưa nhóm bạn lên Sa Pa, lúc đó sương sớm còn phủ trắng những đỉnh núi cao.',
        'Trên dòng sông Sài Gòn, ánh sáng buổi sớm chiếu xuống các sạp hàng san sát hai bên bờ.',
        'Chuyện về xứ Trung với sương sớm, trăng treo và sông trong là điều du khách nhớ lâu.',
        'Sáng sớm trên đảo Trường Sa, sóng xanh trong vắt trải dài tới tận chân trời xa.',
        'Trên đỉnh Sa Pa, sương phủ kín, trăng sáng và bầu trời trong xanh tạo cảm giác thanh bình.',
        'Chuyến đi sang miền Trung là dịp để chiêm ngưỡng sông trong và sương sớm phủ núi.',
        'Trên dòng sông chảy qua xứ Trung, ánh sáng chiếu rọi cả buổi sáng và buổi chiều.',
        'Sáng sớm chuyến tàu sang Trung, hành khách thường ngắm sương sa nhẹ trên dòng sông.'
    ];

    v_quiz_id   UUID;
    v_chal_id   UUID;
    v_chapter   INT;
    v_quiz_idx  INT;
    v_q_idx     INT;
    v_seed      INT;
    v_pair      TEXT[];
    v_correct   TEXT;
    v_wrong     TEXT;
    v_meaning   TEXT;
    v_subtag    TEXT;
    v_text      TEXT;
    v_skill_for_quiz TEXT;
    v_skill     TEXT;
    v_metadata  JSONB;
    v_chal_meta JSONB;
    v_pair_idx  INT;
    v_pair_count INT := array_length(v_pairs, 1);
BEGIN
    FOR v_chapter IN 1..5 LOOP
        FOR v_quiz_idx IN 1..15 LOOP
            IF v_quiz_idx <= 5 THEN
                v_skill_for_quiz := 'SPEAKING';
            ELSIF v_quiz_idx <= 9 THEN
                v_skill_for_quiz := 'LISTENING';
            ELSIF v_quiz_idx <= 12 THEN
                v_skill_for_quiz := 'WRITING';
            ELSE
                v_skill_for_quiz := 'MIX';
            END IF;

            v_quiz_id := ('00000000-0000-0073-2' ||
                          v_chapter::text ||
                          lpad(v_quiz_idx::text, 2, '0') ||
                          '-000000000000')::uuid;

            v_metadata := jsonb_build_object(
                'description', 'Bài kiểm tra ' || v_skill_for_quiz || ' - chương "' || v_chapter_themes[v_chapter] || '" - chữa ngọng s/x và tr/ch (Trung).',
                'instructions',
                    CASE v_skill_for_quiz
                        WHEN 'SPEAKING'  THEN 'Đọc to và rõ phần văn bản, chú ý phân biệt s/x và tr/ch.'
                        WHEN 'LISTENING' THEN 'Nghe AI đọc và chọn từ phát âm đúng (s/x hoặc tr/ch).'
                        WHEN 'WRITING'   THEN 'Nghe và gõ lại chính xác (chú ý s/x và tr/ch).'
                        ELSE 'Câu hỏi tổng hợp 3 kỹ năng - chữa ngọng s/x và tr/ch.'
                    END,
                'time_limit_seconds', v_seconds_per_q[v_chapter] * 10,
                'passing_score', 80,
                'points_per_question', 10,
                'question_count', 10,
                'comment', '',
                'skill_type', v_skill_for_quiz,
                'orderIndex', v_quiz_idx,
                'status', 'APPROVED'
            );

            INSERT INTO learning_unit
                (id, parent_id, name, type, metadata_json, difficulty_level, error_tag,
                 created_at, updated_at, created_by, updated_by)
            VALUES (
                v_quiz_id,
                v_levels[v_chapter],
                'Trung - C' || v_chapter || ' - Quiz ' || v_quiz_idx || ' (' || v_skill_for_quiz || ')',
                'QUIZ',
                v_metadata,
                CASE WHEN v_chapter <= 2 THEN 'BEGINNER'
                     WHEN v_chapter <= 4 THEN 'INTERMEDIATE'
                     ELSE 'ADVANCED' END,
                'T_SX_TRCH',
                NOW(), NOW(), 'system', 'system'
            );

            FOR v_q_idx IN 1..10 LOOP
                v_seed := ((v_chapter - 1) * 150) + ((v_quiz_idx - 1) * 10) + v_q_idx;
                v_pair_idx := ((v_seed - 1) % v_pair_count) + 1;
                v_correct := v_pairs[v_pair_idx][1];
                v_wrong   := v_pairs[v_pair_idx][2];
                v_meaning := v_pairs[v_pair_idx][3];
                v_subtag  := v_pairs[v_pair_idx][4];

                IF v_chapter = 1 THEN
                    v_text := v_correct;
                ELSIF v_chapter = 2 THEN
                    v_text := v_phrases[v_pair_idx];
                ELSIF v_chapter = 3 THEN
                    v_text := v_sent_ch3[((v_q_idx - 1) % 10) + 1];
                ELSIF v_chapter = 4 THEN
                    v_text := v_sent_ch4[((v_q_idx - 1) % 10) + 1];
                ELSE
                    v_text := v_sent_ch5[((v_q_idx - 1) % 10) + 1];
                END IF;

                IF v_skill_for_quiz = 'MIX' THEN
                    v_skill := (ARRAY['SPEAKING','LISTENING','WRITING'])[((v_q_idx - 1) % 3) + 1];
                ELSE
                    v_skill := v_skill_for_quiz;
                END IF;

                IF v_skill = 'SPEAKING' THEN
                    v_chal_meta := jsonb_build_object(
                        'type','SPEAKING',
                        'transcript', v_text,
                        'targetWords', jsonb_build_array(v_correct),
                        'errorTag','T_' || v_subtag,
                        'explanation','Cặp dễ nhầm ' || v_subtag || ': phát âm đúng "' || v_correct || '" (nghĩa: ' || v_meaning || '). Nhiều người nhầm thành "' || v_wrong || '".'
                    );
                ELSIF v_skill = 'LISTENING' THEN
                    v_chal_meta := jsonb_build_object(
                        'type','LISTENING',
                        'transcript', v_text,
                        'audioWord', v_correct,
                        'correctAnswer', v_correct,
                        'options', jsonb_build_array(v_correct, v_wrong),
                        'errorTag','T_' || v_subtag,
                        'explanation','AI đọc đúng từ "' || v_correct || '". Đáp án đúng là "' || v_correct || '" (nghĩa: ' || v_meaning || '), không phải "' || v_wrong || '".'
                    );
                ELSE
                    v_chal_meta := jsonb_build_object(
                        'type','WRITING',
                        'transcript', v_text,
                        'expectedAnswer', v_text,
                        'targetWords', jsonb_build_array(v_correct),
                        'errorTag','T_' || v_subtag,
                        'explanation','Nghe và gõ chính xác. Lưu ý từ "' || v_correct || '" (nghĩa: ' || v_meaning || ') - viết theo ' || v_subtag || ' cho đúng.'
                    );
                END IF;

                v_chal_id := ('00000000-0000-0073-2' ||
                              v_chapter::text ||
                              lpad(v_quiz_idx::text, 2, '0') ||
                              '-0000000000' ||
                              lpad(v_q_idx::text, 2, '0'))::uuid;

                INSERT INTO challenge_bank
                    (id, content_text, skill_type, region, metadata_json, created_at, updated_at)
                VALUES (
                    v_chal_id, v_text, v_skill, 'TRUNG', v_chal_meta, NOW(), NOW()
                );

                INSERT INTO quiz_challenge_item
                    (id, quiz_id, challenge_bank_id, challenge_id, order_index)
                VALUES (
                    gen_random_uuid(), v_quiz_id, v_chal_id, v_chal_id, v_q_idx
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- Changeset db/changelog/77-seed-quizzes-south.xml::77-seed-quizzes-south::claude
-- Seed SOUTH: 75 QUIZ + 750 challenge_bank rows + quiz_challenge_item links. Idempotent.
DO $$
DECLARE
    v_old_quiz_ids UUID[];
BEGIN
    SELECT array_agg(id) INTO v_old_quiz_ids
    FROM learning_unit
    WHERE type='QUIZ'
      AND parent_id IN (
          '00000000-0000-0000-0073-310000000000'::uuid,
          '00000000-0000-0000-0073-320000000000'::uuid,
          '00000000-0000-0000-0073-330000000000'::uuid,
          '00000000-0000-0000-0073-340000000000'::uuid,
          '00000000-0000-0000-0073-350000000000'::uuid
      );

    IF v_old_quiz_ids IS NOT NULL AND array_length(v_old_quiz_ids,1) > 0 THEN
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='quiz_challenge_item') THEN
            DELETE FROM quiz_challenge_item WHERE quiz_id = ANY(v_old_quiz_ids);
        END IF;
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='account_learning_unit') THEN
            DELETE FROM account_learning_unit WHERE learning_unit_id = ANY(v_old_quiz_ids);
        END IF;
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='custom_path_progress') THEN
            DELETE FROM custom_path_progress WHERE learning_unit_id = ANY(v_old_quiz_ids);
        END IF;
        IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='content_item') THEN
            DELETE FROM content_item WHERE learning_unit_id = ANY(v_old_quiz_ids);
        END IF;
        DELETE FROM learning_unit WHERE id = ANY(v_old_quiz_ids);
    END IF;

    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='speaking_attempt') THEN
        DELETE FROM speaking_attempt WHERE challenge_id IN (
            SELECT id FROM challenge_bank WHERE id::text LIKE '00000000-0000-0073-3%'
        );
    END IF;
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name='daily_challenge_attempt') THEN
        DELETE FROM daily_challenge_attempt WHERE challenge_id IN (
            SELECT id FROM challenge_bank WHERE id::text LIKE '00000000-0000-0073-3%'
        );
    END IF;
    DELETE FROM challenge_bank WHERE id::text LIKE '00000000-0000-0073-3%';
END $$;

DO $$
DECLARE
    v_levels UUID[] := ARRAY[
        '00000000-0000-0000-0073-310000000000'::uuid,
        '00000000-0000-0000-0073-320000000000'::uuid,
        '00000000-0000-0000-0073-330000000000'::uuid,
        '00000000-0000-0000-0073-340000000000'::uuid,
        '00000000-0000-0000-0073-350000000000'::uuid
    ];
    v_chapter_themes TEXT[] := ARRAY['Chào hỏi','Gia đình','Công việc','Mua sắm','Du lịch'];
    v_seconds_per_q INT[] := ARRAY[120,110,100,90,75];

    v_pairs TEXT[][] := ARRAY[
        ARRAY['da',     'gia',  'lớp ngoài cơ thể người'],
        ARRAY['dạy',    'rạy',  'truyền kiến thức cho người khác'],
        ARRAY['dòng',   'ròng', 'dải nước chảy liên tục'],
        ARRAY['dỗ',     'giỗ',  'làm cho hết khóc'],
        ARRAY['dán',    'rán',  'gắn lên bằng hồ hoặc keo'],
        ARRAY['dầu',    'giầu', 'chất lỏng dùng để rán'],
        ARRAY['dập',    'rập',  'làm tắt một cách nhanh chóng'],
        ARRAY['dìu',    'rìu',  'đỡ bước đi'],
        ARRAY['gia',    'da',   'người thân trong nhà'],
        ARRAY['giàu',   'dầu',  'có nhiều của cải'],
        ARRAY['giỗ',    'dỗ',   'ngày tưởng nhớ người đã mất'],
        ARRAY['giấu',   'dấu',  'che đi không cho biết'],
        ARRAY['giặt',   'dặt',  'làm sạch quần áo bằng nước'],
        ARRAY['giận',   'dận',  'cảm xúc tức tối'],
        ARRAY['rạp',    'dạp',  'nơi chiếu phim'],
        ARRAY['rán',    'dán',  'nấu bằng dầu nóng'],
        ARRAY['rộng',   'dộng', 'có diện tích lớn'],
        ARRAY['rừng',   'dừng', 'vùng đất nhiều cây cối'],
        ARRAY['rau',    'dau',  'các loại lá ăn được'],
        ARRAY['ruộng',  'duộng','cánh đồng trồng lúa']
    ];

    v_phrases TEXT[] := ARRAY[
        'làn da mịn',           'cô giáo dạy chữ',       'dòng sông quê',           'dỗ em bé khóc',         'dán hình lên tường',
        'một chai dầu ăn',      'dập tắt ngọn lửa',      'dìu bà qua đường',        'gia đình hạnh phúc',    'người giàu lòng nhân',
        'ngày giỗ ông nội',     'giấu món quà bí mật',   'giặt quần áo bẩn',        'cơn giận thoáng qua',   'rạp chiếu phim',
        'rán cá vàng ươm',      'sân vườn rộng rãi',     'rừng cây xanh',           'rau muống tươi',         'cánh đồng ruộng lúa'
    ];

    v_sent_ch3 TEXT[] := ARRAY[
        'Cô giáo dạy học sinh viết chữ thật cẩn thận.',
        'Giám đốc giao dự án mới cho nhóm phát triển.',
        'Anh ấy giặt giũ quần áo vào mỗi sáng cuối tuần.',
        'Doanh nghiệp đang giảm bớt rủi ro trong quý này.',
        'Giáo viên phải dìu dắt học trò từng bước một.',
        'Giám sát công trình rất chặt chẽ trong giai đoạn nghiệm thu.',
        'Doanh thu tháng này tăng đều nhờ chiến lược mới.',
        'Rạng sáng, giám đốc đã duyệt báo cáo của bộ phận kế toán.',
        'Cô giáo trẻ rất kiên nhẫn với học sinh lớp một.',
        'Doanh nghiệp giàu kinh nghiệm nên giải quyết vấn đề rất nhanh.'
    ];

    v_sent_ch4 TEXT[] := ARRAY[
        'Dì Dung ra chợ mua một bó rau muống và một dúm hành lá tươi.',
        'Sau đó dì rán cá, giã giò, cả gia đình quây quần bên mâm cơm.',
        'Giá rổ rau hôm nay khá rẻ nên dì mua thêm vài loại để dành.',
        'Dạo chợ buổi sáng, dì ghé nhiều sạp để chọn được hàng tươi nhất.',
        'Trong siêu thị, dì gặp đợt giảm giá lớn cho dầu ăn và gia vị.',
        'Rạp xôi và sạp giò đầu chợ luôn đông khách vào dịp cuối tuần.',
        'Cả gia đình cùng đi siêu thị, dì giành phần xếp đồ giúp em.',
        'Dì thường rảo bước qua các sạp rau trước khi quyết định mua.',
        'Dầu ăn đang được giảm giá nên dì mua hai chai để dùng dần.',
        'Gian hàng giò chả ở chợ hôm nay rất đông, người ra người vào liên tục.'
    ];

    v_sent_ch5 TEXT[] := ARRAY[
        'Dạo gần đây, gia đình tôi đi du lịch dọc dòng sông Cửu Long, ngắm những cánh đồng lúa giàu phù sa.',
        'Rạng sáng, đoàn xe rời thành phố, đi dọc theo những rặng dừa nghiêng mình theo gió.',
        'Dòng sông phản chiếu ánh nắng vàng, gió thổi rì rào qua những rặng dừa hai bên bờ.',
        'Giữa rừng tràm rộng lớn, đoàn dừng chân nghỉ tại một cánh đồng giáp giới Đồng Tháp.',
        'Dòng người đổ về rạp chiếu phim ngoài trời, gia đình tôi cũng rủ nhau đi xem cùng.',
        'Rạng đông trên dòng Cửu Long, tiếng gió và tiếng chim tạo nên một buổi sáng rất bình yên.',
        'Đi dọc dòng sông Hậu, dì giúp gia đình rộn ràng kể chuyện về những chuyến đi trước đây.',
        'Rừng đước ở Cà Mau là nơi lý tưởng để dạo bộ và cảm nhận hơi nước từ dòng sông phía xa.',
        'Cả gia đình rộn ràng dạo trên dòng kênh nhỏ, dì cầm chèo còn dượng kể chuyện vui.',
        'Dòng Cửu Long rộng mênh mông, hai bên bờ là ruộng lúa giàu màu mỡ trải dài tới chân trời.'
    ];

    v_quiz_id   UUID;
    v_chal_id   UUID;
    v_chapter   INT;
    v_quiz_idx  INT;
    v_q_idx     INT;
    v_seed      INT;
    v_pair      TEXT[];
    v_correct   TEXT;
    v_wrong     TEXT;
    v_meaning   TEXT;
    v_text      TEXT;
    v_skill_for_quiz TEXT;
    v_skill     TEXT;
    v_metadata  JSONB;
    v_chal_meta JSONB;
    v_pair_idx  INT;
    v_pair_count INT := array_length(v_pairs, 1);
BEGIN
    FOR v_chapter IN 1..5 LOOP
        FOR v_quiz_idx IN 1..15 LOOP
            IF v_quiz_idx <= 5 THEN
                v_skill_for_quiz := 'SPEAKING';
            ELSIF v_quiz_idx <= 9 THEN
                v_skill_for_quiz := 'LISTENING';
            ELSIF v_quiz_idx <= 12 THEN
                v_skill_for_quiz := 'WRITING';
            ELSE
                v_skill_for_quiz := 'MIX';
            END IF;

            v_quiz_id := ('00000000-0000-0073-3' ||
                          v_chapter::text ||
                          lpad(v_quiz_idx::text, 2, '0') ||
                          '-000000000000')::uuid;

            v_metadata := jsonb_build_object(
                'description', 'Bài kiểm tra ' || v_skill_for_quiz || ' - chương "' || v_chapter_themes[v_chapter] || '" - chữa ngọng d/gi/r (Nam).',
                'instructions',
                    CASE v_skill_for_quiz
                        WHEN 'SPEAKING'  THEN 'Đọc to và rõ phần văn bản, chú ý phân biệt d / gi / r.'
                        WHEN 'LISTENING' THEN 'Nghe AI đọc và chọn từ phát âm đúng (d / gi / r).'
                        WHEN 'WRITING'   THEN 'Nghe và gõ lại chính xác (chú ý d / gi / r).'
                        ELSE 'Câu hỏi tổng hợp 3 kỹ năng - chữa ngọng d / gi / r.'
                    END,
                'time_limit_seconds', v_seconds_per_q[v_chapter] * 10,
                'passing_score', 80,
                'points_per_question', 10,
                'question_count', 10,
                'comment', '',
                'skill_type', v_skill_for_quiz,
                'orderIndex', v_quiz_idx,
                'status', 'APPROVED'
            );

            INSERT INTO learning_unit
                (id, parent_id, name, type, metadata_json, difficulty_level, error_tag,
                 created_at, updated_at, created_by, updated_by)
            VALUES (
                v_quiz_id,
                v_levels[v_chapter],
                'Nam - C' || v_chapter || ' - Quiz ' || v_quiz_idx || ' (' || v_skill_for_quiz || ')',
                'QUIZ',
                v_metadata,
                CASE WHEN v_chapter <= 2 THEN 'BEGINNER'
                     WHEN v_chapter <= 4 THEN 'INTERMEDIATE'
                     ELSE 'ADVANCED' END,
                'N_DGIR',
                NOW(), NOW(), 'system', 'system'
            );

            FOR v_q_idx IN 1..10 LOOP
                v_seed := ((v_chapter - 1) * 150) + ((v_quiz_idx - 1) * 10) + v_q_idx;
                v_pair_idx := ((v_seed - 1) % v_pair_count) + 1;
                v_correct := v_pairs[v_pair_idx][1];
                v_wrong   := v_pairs[v_pair_idx][2];
                v_meaning := v_pairs[v_pair_idx][3];

                IF v_chapter = 1 THEN
                    v_text := v_correct;
                ELSIF v_chapter = 2 THEN
                    v_text := v_phrases[v_pair_idx];
                ELSIF v_chapter = 3 THEN
                    v_text := v_sent_ch3[((v_q_idx - 1) % 10) + 1];
                ELSIF v_chapter = 4 THEN
                    v_text := v_sent_ch4[((v_q_idx - 1) % 10) + 1];
                ELSE
                    v_text := v_sent_ch5[((v_q_idx - 1) % 10) + 1];
                END IF;

                IF v_skill_for_quiz = 'MIX' THEN
                    v_skill := (ARRAY['SPEAKING','LISTENING','WRITING'])[((v_q_idx - 1) % 3) + 1];
                ELSE
                    v_skill := v_skill_for_quiz;
                END IF;

                IF v_skill = 'SPEAKING' THEN
                    v_chal_meta := jsonb_build_object(
                        'type','SPEAKING',
                        'transcript', v_text,
                        'targetWords', jsonb_build_array(v_correct),
                        'errorTag','N_DGIR',
                        'explanation','Cặp dễ nhầm d/gi/r: phát âm đúng "' || v_correct || '" (nghĩa: ' || v_meaning || '). Nhiều người nhầm thành "' || v_wrong || '".'
                    );
                ELSIF v_skill = 'LISTENING' THEN
                    v_chal_meta := jsonb_build_object(
                        'type','LISTENING',
                        'transcript', v_text,
                        'audioWord', v_correct,
                        'correctAnswer', v_correct,
                        'options', jsonb_build_array(v_correct, v_wrong),
                        'errorTag','N_DGIR',
                        'explanation','AI đọc đúng từ "' || v_correct || '". Đáp án đúng là "' || v_correct || '" (nghĩa: ' || v_meaning || '), không phải "' || v_wrong || '".'
                    );
                ELSE
                    v_chal_meta := jsonb_build_object(
                        'type','WRITING',
                        'transcript', v_text,
                        'expectedAnswer', v_text,
                        'targetWords', jsonb_build_array(v_correct),
                        'errorTag','N_DGIR',
                        'explanation','Nghe và gõ chính xác. Lưu ý từ "' || v_correct || '" (nghĩa: ' || v_meaning || ') - viết theo d, gi hay r cho đúng.'
                    );
                END IF;

                v_chal_id := ('00000000-0000-0073-3' ||
                              v_chapter::text ||
                              lpad(v_quiz_idx::text, 2, '0') ||
                              '-0000000000' ||
                              lpad(v_q_idx::text, 2, '0'))::uuid;

                INSERT INTO challenge_bank
                    (id, content_text, skill_type, region, metadata_json, created_at, updated_at)
                VALUES (
                    v_chal_id, v_text, v_skill, 'NAM', v_chal_meta, NOW(), NOW()
                );

                INSERT INTO quiz_challenge_item
                    (id, quiz_id, challenge_bank_id, challenge_id, order_index)
                VALUES (
                    gen_random_uuid(), v_quiz_id, v_chal_id, v_chal_id, v_q_idx
                );
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

