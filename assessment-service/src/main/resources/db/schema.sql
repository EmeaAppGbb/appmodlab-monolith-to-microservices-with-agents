-- =============================================================================
-- Assessment Service Database Schema
-- Owns: assessments, student_answers tables
-- No foreign keys to other services (Database-per-Service pattern)
-- =============================================================================

CREATE TABLE IF NOT EXISTS assessments (
    id                  BIGSERIAL PRIMARY KEY,
    lesson_id           BIGINT       NOT NULL,
    course_id           BIGINT       NOT NULL,
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    type                VARCHAR(20)  NOT NULL,
    questions_json      TEXT,
    passing_score       INTEGER      NOT NULL DEFAULT 70,
    max_score           INTEGER      NOT NULL DEFAULT 100,
    time_limit_minutes  INTEGER,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_assessments_lesson_id ON assessments(lesson_id);
CREATE INDEX IF NOT EXISTS idx_assessments_course_id ON assessments(course_id);
CREATE INDEX IF NOT EXISTS idx_assessments_type ON assessments(type);

CREATE TABLE IF NOT EXISTS student_answers (
    id              BIGSERIAL PRIMARY KEY,
    assessment_id   BIGINT    NOT NULL REFERENCES assessments(id),
    student_id      BIGINT    NOT NULL,
    enrollment_id   BIGINT,
    answers_json    TEXT,
    score           INTEGER,
    passed          BOOLEAN,
    graded_at       TIMESTAMP,
    submitted_at    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (assessment_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_student_answers_assessment_id ON student_answers(assessment_id);
CREATE INDEX IF NOT EXISTS idx_student_answers_student_id ON student_answers(student_id);
