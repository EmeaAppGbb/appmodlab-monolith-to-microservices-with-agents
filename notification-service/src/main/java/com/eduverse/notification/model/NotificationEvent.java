package com.eduverse.notification.model;

/**
 * Internal DTO representing a notification request derived from a domain event.
 * Decouples the notification processing logic from the specific domain event structure.
 */
public class NotificationEvent {

    private String eventId;
    private String correlationId;
    private String sourceEventType;
    private Long userId;
    private String recipientEmail;
    private String recipientName;
    private Notification.Type notificationType;
    private String subject;
    private String message;

    public NotificationEvent() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public String getSourceEventType() { return sourceEventType; }
    public void setSourceEventType(String sourceEventType) { this.sourceEventType = sourceEventType; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public Notification.Type getNotificationType() { return notificationType; }
    public void setNotificationType(Notification.Type notificationType) { this.notificationType = notificationType; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static class Builder {
        private final NotificationEvent event = new NotificationEvent();

        public Builder eventId(String eventId) { event.eventId = eventId; return this; }
        public Builder correlationId(String correlationId) { event.correlationId = correlationId; return this; }
        public Builder sourceEventType(String sourceEventType) { event.sourceEventType = sourceEventType; return this; }
        public Builder userId(Long userId) { event.userId = userId; return this; }
        public Builder recipientEmail(String recipientEmail) { event.recipientEmail = recipientEmail; return this; }
        public Builder recipientName(String recipientName) { event.recipientName = recipientName; return this; }
        public Builder notificationType(Notification.Type notificationType) { event.notificationType = notificationType; return this; }
        public Builder subject(String subject) { event.subject = subject; return this; }
        public Builder message(String message) { event.message = message; return this; }

        public NotificationEvent build() {
            return event;
        }
    }
}
