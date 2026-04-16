-- =============================================================================
-- Video Service Database Schema
-- Owns: videos table
-- No foreign keys to other services (Database-per-Service pattern)
-- =============================================================================

CREATE TABLE IF NOT EXISTS videos (
    id                BIGSERIAL PRIMARY KEY,
    lesson_id         BIGINT       NOT NULL,
    course_id         BIGINT,
    title             VARCHAR(200),
    original_url      VARCHAR(500),
    transcoded_url    VARCHAR(500),
    thumbnail_url     VARCHAR(500),
    status            VARCHAR(20)  NOT NULL DEFAULT 'UPLOADING',
    duration          INTEGER,
    file_size         BIGINT,
    content_type      VARCHAR(50),
    blob_storage_key  VARCHAR(500),
    failure_reason    VARCHAR(500),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_videos_lesson_id ON videos(lesson_id);
CREATE INDEX IF NOT EXISTS idx_videos_status ON videos(status);
CREATE INDEX IF NOT EXISTS idx_videos_course_id ON videos(course_id);
