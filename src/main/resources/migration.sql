-- ============================================================
-- CipherLink Database Migration — Upgraded Schema
-- Run these in order on your Supabase/PostgreSQL instance.
-- ============================================================

-- 1. Add new columns to messages table
ALTER TABLE messages ADD COLUMN IF NOT EXISTS reply_to_message_id UUID REFERENCES messages(id) ON DELETE SET NULL;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS reply_preview TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS edited_payload TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_edited BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS pinned_by_user_id UUID;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS media_url TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS media_type VARCHAR(20);
ALTER TABLE messages ADD COLUMN IF NOT EXISTS voice_duration_seconds INT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS location_lat DOUBLE PRECISION;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS location_lng DOUBLE PRECISION;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS location_label VARCHAR(255);
ALTER TABLE messages ADD COLUMN IF NOT EXISTS link_preview_json TEXT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS reactions_json TEXT NOT NULL DEFAULT '{}';

-- 2. Add new columns to group_messages table
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS reply_to_message_id UUID;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS reply_preview TEXT;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS edited_payload TEXT;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS edited_at TIMESTAMP;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS is_edited BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS media_url TEXT;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS media_type VARCHAR(20);
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS voice_duration_seconds INT;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS link_preview_json TEXT;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS reactions_json TEXT NOT NULL DEFAULT '{}';

-- 3. Add columns to user_profiles
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS is_online BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS last_seen TIMESTAMP;
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS profile_picture_storage_path VARCHAR(500);

-- 4. Add storage_path to media_messages
ALTER TABLE media_messages ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500);

-- 5. User privacy settings
CREATE TABLE IF NOT EXISTS user_privacy_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    online_status_visibility VARCHAR(10) NOT NULL DEFAULT 'CONTACTS',
    last_seen_visibility VARCHAR(10) NOT NULL DEFAULT 'CONTACTS',
    read_receipts_visibility VARCHAR(10) NOT NULL DEFAULT 'CONTACTS',
    story_visibility VARCHAR(10) NOT NULL DEFAULT 'CONTACTS',
    profile_picture_visibility VARCHAR(10) NOT NULL DEFAULT 'CONTACTS',
    about_visibility VARCHAR(10) NOT NULL DEFAULT 'CONTACTS'
);

-- 6. Call records
CREATE TABLE IF NOT EXISTS call_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    caller_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    call_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'MISSED',
    room_id VARCHAR(100),
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    answered_at TIMESTAMP,
    ended_at TIMESTAMP,
    duration_seconds INT
);
CREATE INDEX IF NOT EXISTS idx_call_records_caller ON call_records(caller_id);
CREATE INDEX IF NOT EXISTS idx_call_records_receiver ON call_records(receiver_id);
CREATE INDEX IF NOT EXISTS idx_call_records_started_at ON call_records(started_at DESC);

-- 7. Live locations
CREATE TABLE IF NOT EXISTS live_locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    group_id UUID,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    label VARCHAR(255),
    shared_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_live_locations_user ON live_locations(user_id);
CREATE INDEX IF NOT EXISTS idx_live_locations_conv ON live_locations(conversation_id, is_active);

-- 8. Indexes for performance
CREATE INDEX IF NOT EXISTS idx_messages_conv_deleted ON messages(conversation_id, is_deleted, sent_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_pinned ON messages(conversation_id, is_pinned) WHERE is_pinned = TRUE;
CREATE INDEX IF NOT EXISTS idx_group_messages_group ON group_messages(group_id, is_deleted, sent_at DESC);

-- 9. Device FCM tokens table (if not exists)
CREATE TABLE IF NOT EXISTS device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id UUID NOT NULL,
    fcm_token TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, device_id)
);
CREATE INDEX IF NOT EXISTS idx_device_tokens_user ON device_tokens(user_id);
