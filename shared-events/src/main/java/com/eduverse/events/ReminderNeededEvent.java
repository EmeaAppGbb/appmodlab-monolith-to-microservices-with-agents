package com.eduverse.events;

public class ReminderNeededEvent extends DomainEvent {

    private Long studentId;
    private Long enrollmentId;
    private Long courseId;
    private String courseTitle;
    private String reminderType;

    public ReminderNeededEvent() {
    }

    public ReminderNeededEvent(Long studentId, Long enrollmentId, Long courseId, String courseTitle, String reminderType) {
        this.studentId = studentId;
        this.enrollmentId = enrollmentId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.reminderType = reminderType;
    }

    @Override
    public String getEventType() {
        return "ReminderNeeded";
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
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

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    @Override
    public String toString() {
        return "ReminderNeededEvent{" +
                "studentId=" + studentId +
                ", enrollmentId=" + enrollmentId +
                ", courseId=" + courseId +
                ", courseTitle='" + courseTitle + '\'' +
                ", reminderType='" + reminderType + '\'' +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
