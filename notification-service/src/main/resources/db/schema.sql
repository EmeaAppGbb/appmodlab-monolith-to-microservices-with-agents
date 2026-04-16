-- Notification Service schema (Database-per-Service pattern)
-- Migrated from monolith 'notifications' table with enhancements for
-- event-driven architecture: idempotency, status tracking, and correlation.

CREATE TABLE IF NOT EXISTS notifications (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(255) UNIQUE,
    user_id         BIGINT NOT NULL,
    recipient_email VARCHAR(255),
    recipient_name  VARCHAR(255),
    type            VARCHAR(10) NOT NULL,
    status          VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    subject         VARCHAR(200),
    message         TEXT,
    source_event_type VARCHAR(50),
    correlation_id  VARCHAR(50),
    sent_at         TIMESTAMP,
    failure_reason  VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications (status);
CREATE INDEX IF NOT EXISTS idx_notifications_event_id ON notifications (event_id);
