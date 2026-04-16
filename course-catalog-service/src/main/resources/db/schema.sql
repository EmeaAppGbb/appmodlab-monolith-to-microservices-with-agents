-- Course Catalog Service schema (Database-per-Service pattern)
-- Extracted from monolith tables: courses, modules, lessons
-- instructor_id is a soft reference to the Identity service (no FK constraint)

CREATE TABLE IF NOT EXISTS courses (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    instructor_id   BIGINT NOT NULL,
    category        VARCHAR(50),
    price           NUMERIC(10, 2),
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_date  TIMESTAMP,
    duration_hours  INTEGER,
    thumbnail_url   VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS modules (
    id          BIGSERIAL PRIMARY KEY,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    sort_order  INTEGER NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS lessons (
    id               BIGSERIAL PRIMARY KEY,
    module_id        BIGINT NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
    title            VARCHAR(200) NOT NULL,
    content_type     VARCHAR(20) NOT NULL,
    video_url        VARCHAR(500),
    duration_minutes INTEGER,
    sort_order       INTEGER NOT NULL,
    content          TEXT
);

CREATE INDEX IF NOT EXISTS idx_courses_status ON courses (status);
CREATE INDEX IF NOT EXISTS idx_courses_category ON courses (category);
CREATE INDEX IF NOT EXISTS idx_courses_instructor_id ON courses (instructor_id);
CREATE INDEX IF NOT EXISTS idx_modules_course_id ON modules (course_id);
CREATE INDEX IF NOT EXISTS idx_lessons_module_id ON lessons (module_id);
