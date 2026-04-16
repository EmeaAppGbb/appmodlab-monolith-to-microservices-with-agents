-- =============================================================================
-- Enrollment Service Database Schema
-- Owns: enrollments table
-- No foreign keys to other services (Database-per-Service pattern)
-- =============================================================================

CREATE TABLE IF NOT EXISTS enrollments (
    id                BIGSERIAL PRIMARY KEY,
    student_id        BIGINT         NOT NULL,
    course_id         BIGINT         NOT NULL,
    course_title      VARCHAR(200),
    status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    progress_percent  NUMERIC(5, 2)  DEFAULT 0,
    enrolled_at       TIMESTAMP,
    activated_at      TIMESTAMP,
    completed_at      TIMESTAMP,
    cancelled_at      TIMESTAMP,
    certificate_id    BIGINT,
    last_accessed_at  TIMESTAMP,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_enrollments_student_course UNIQUE (student_id, course_id)
);

CREATE INDEX IF NOT EXISTS idx_enrollments_student_id ON enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_course_id ON enrollments(course_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_status ON enrollments(status);
