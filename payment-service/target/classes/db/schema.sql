-- =============================================================================
-- Payment Service Database Schema
-- Owns: payments, saga_states tables
-- No foreign keys to other services (Database-per-Service pattern)
-- =============================================================================

CREATE TABLE IF NOT EXISTS payments (
    id                BIGSERIAL PRIMARY KEY,
    enrollment_id     BIGINT         NOT NULL,
    student_id        BIGINT,
    course_id         BIGINT,
    amount            NUMERIC(10, 2) NOT NULL,
    currency          VARCHAR(3)     NOT NULL DEFAULT 'USD',
    stripe_payment_id VARCHAR(100)   UNIQUE,
    idempotency_key   VARCHAR(100)   UNIQUE,
    status            VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    failure_reason    VARCHAR(500),
    paid_date         TIMESTAMP,
    refunded_date     TIMESTAMP,
    created_at        TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payments_enrollment_id ON payments(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_payments_stripe_payment_id ON payments(stripe_payment_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

CREATE TABLE IF NOT EXISTS saga_states (
    id              BIGSERIAL PRIMARY KEY,
    enrollment_id   BIGINT      NOT NULL UNIQUE,
    student_id      BIGINT,
    course_id       BIGINT,
    payment_id      BIGINT,
    state           VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    correlation_id  VARCHAR(100),
    last_event_id   VARCHAR(100),
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_saga_enrollment_id ON saga_states(enrollment_id);
CREATE INDEX IF NOT EXISTS idx_saga_correlation_id ON saga_states(correlation_id);
