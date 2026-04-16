package com.eduverse.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CoursePublishedEvent.class, name = "CoursePublished"),
    @JsonSubTypes.Type(value = StudentEnrolledEvent.class, name = "StudentEnrolled"),
    @JsonSubTypes.Type(value = EnrollmentActivatedEvent.class, name = "EnrollmentActivated"),
    @JsonSubTypes.Type(value = EnrollmentCompletedEvent.class, name = "EnrollmentCompleted"),
    @JsonSubTypes.Type(value = PaymentCompletedEvent.class, name = "PaymentCompleted"),
    @JsonSubTypes.Type(value = PaymentFailedEvent.class, name = "PaymentFailed"),
    @JsonSubTypes.Type(value = PaymentRefundedEvent.class, name = "PaymentRefunded"),
    @JsonSubTypes.Type(value = CertificateIssuedEvent.class, name = "CertificateIssued"),
    @JsonSubTypes.Type(value = AssessmentPassedEvent.class, name = "AssessmentPassed"),
    @JsonSubTypes.Type(value = LessonCompletedEvent.class, name = "LessonCompleted"),
    @JsonSubTypes.Type(value = ProgressUpdatedEvent.class, name = "ProgressUpdated"),
    @JsonSubTypes.Type(value = VideoReadyEvent.class, name = "VideoReady"),
    @JsonSubTypes.Type(value = ReminderNeededEvent.class, name = "ReminderNeeded")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class DomainEvent implements Serializable {

    private String eventId;
    private Instant occurredAt;
    private String correlationId;
    private String source;
    private int version;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.version = 1;
    }

    public abstract String getEventType();

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + getEventType() + '\'' +
                ", occurredAt=" + occurredAt +
                ", correlationId='" + correlationId + '\'' +
                ", source='" + source + '\'' +
                ", version=" + version +
                '}';
    }
}
