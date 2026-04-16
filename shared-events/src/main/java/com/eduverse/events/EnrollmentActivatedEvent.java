package com.eduverse.events;

public class EnrollmentActivatedEvent extends DomainEvent {

    private Long enrollmentId;
    private Long studentId;
    private Long courseId;

    public EnrollmentActivatedEvent() {
    }

    public EnrollmentActivatedEvent(Long enrollmentId, Long studentId, Long courseId) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
    }

    @Override
    public String getEventType() {
        return "EnrollmentActivated";
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

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    @Override
    public String toString() {
        return "EnrollmentActivatedEvent{" +
                "enrollmentId=" + enrollmentId +
                ", studentId=" + studentId +
                ", courseId=" + courseId +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
