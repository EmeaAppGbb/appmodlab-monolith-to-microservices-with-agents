package com.eduverse.events;

import java.math.BigDecimal;

public class ProgressUpdatedEvent extends DomainEvent {

    private Long enrollmentId;
    private Long studentId;
    private BigDecimal progressPercent;
    private boolean completed;

    public ProgressUpdatedEvent() {
    }

    public ProgressUpdatedEvent(Long enrollmentId, Long studentId, BigDecimal progressPercent, boolean completed) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.progressPercent = progressPercent;
        this.completed = completed;
    }

    @Override
    public String getEventType() {
        return "ProgressUpdated";
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return "ProgressUpdatedEvent{" +
                "enrollmentId=" + enrollmentId +
                ", studentId=" + studentId +
                ", progressPercent=" + progressPercent +
                ", completed=" + completed +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
