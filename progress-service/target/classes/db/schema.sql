-- =============================================================================
-- Progress Service Database Schema
-- Owns: student_progress table
-- No foreign keys to other services (Database-per-Service pattern)
-- =============================================================================

CREATE TABLE IF NOT EXISTS student_progress (
    id                BIGSERIAL PRIMARY KEY,
    enrollment_id     BIGINT         NOT NULL,
    student_id        BIGINT         NOT NULL,
    course_id         BIGINT         NOT NULL,
    lesson_id         BIGINT         NOT NULL,
    completed         BOOLEAN        NOT NULL DEFAULT FALSE,
    completed_at      TIMESTAMP,
    progress_percent  NUMERIC(5, 2)  DEFAULT 0.00,
    last_accessed_at  TIMESTAMP,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_enrollment_lesson UNIQUE (enrollment_id, lesson_id)
);

CREATE INDEX IF NOT EXISTS idx_progress_enrollment_id ON student_progress(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_progress_student_id ON student_progress(student_id);
CREATE INDEX IF NOT EXISTS idx_progress_course_id ON student_progress(course_id);
CREATE INDEX IF NOT EXISTS idx_progress_lesson_id ON student_progress(lesson_id);
