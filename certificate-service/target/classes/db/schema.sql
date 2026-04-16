-- Certificate Service Database Schema
-- Owns: certificates table
-- No foreign keys to other services (Database-per-Service pattern)

CREATE TABLE IF NOT EXISTS certificates (
    id                  BIGSERIAL PRIMARY KEY,
    enrollment_id       BIGINT         NOT NULL UNIQUE,
    student_id          BIGINT         NOT NULL,
    course_id           BIGINT         NOT NULL,
    certificate_number  VARCHAR(50)    UNIQUE NOT NULL,
    student_name        VARCHAR(200),
    course_title        VARCHAR(200),
    completion_date     TIMESTAMP,
    pdf_url             VARCHAR(500),
    status              VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    failure_reason      VARCHAR(500),
    issued_at           TIMESTAMP,
    created_at          TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_certificates_enrollment_id ON certificates (enrollment_id);
CREATE INDEX IF NOT EXISTS idx_certificates_student_id ON certificates (student_id);
CREATE INDEX IF NOT EXISTS idx_certificates_certificate_number ON certificates (certificate_number);
CREATE INDEX IF NOT EXISTS idx_certificates_status ON certificates (status);
