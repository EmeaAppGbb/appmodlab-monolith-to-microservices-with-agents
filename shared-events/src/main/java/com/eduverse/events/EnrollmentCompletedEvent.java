package com.eduverse.events;

public class EnrollmentCompletedEvent extends DomainEvent {

    private Long enrollmentId;
    private Long studentId;
    private Long courseId;
    private String courseTitle;

    public EnrollmentCompletedEvent() {
    }

    public EnrollmentCompletedEvent(Long enrollmentId, Long studentId, Long courseId, String courseTitle) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
    }

    @Override
    public String getEventType() {
        return "EnrollmentCompleted";
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

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    @Override
    public String toString() {
        return "EnrollmentCompletedEvent{" +
                "enrollmentId=" + enrollmentId +
                ", studentId=" + studentId +
                ", courseId=" + courseId +
                ", courseTitle='" + courseTitle + '\'' +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
