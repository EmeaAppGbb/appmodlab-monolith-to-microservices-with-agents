package com.eduverse.events;

public class LessonCompletedEvent extends DomainEvent {

    private Long studentId;
    private Long lessonId;
    private Long enrollmentId;

    public LessonCompletedEvent() {
    }

    public LessonCompletedEvent(Long studentId, Long lessonId, Long enrollmentId) {
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.enrollmentId = enrollmentId;
    }

    @Override
    public String getEventType() {
        return "LessonCompleted";
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    @Override
    public String toString() {
        return "LessonCompletedEvent{" +
                "studentId=" + studentId +
                ", lessonId=" + lessonId +
                ", enrollmentId=" + enrollmentId +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
