package com.eduverse.events;

import java.math.BigDecimal;

public class StudentEnrolledEvent extends DomainEvent {

    private Long enrollmentId;
    private Long studentId;
    private Long courseId;
    private String courseTitle;
    private BigDecimal price;

    public StudentEnrolledEvent() {
    }

    public StudentEnrolledEvent(Long enrollmentId, Long studentId, Long courseId, String courseTitle, BigDecimal price) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.price = price;
    }

    @Override
    public String getEventType() {
        return "StudentEnrolled";
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "StudentEnrolledEvent{" +
                "enrollmentId=" + enrollmentId +
                ", studentId=" + studentId +
                ", courseId=" + courseId +
                ", courseTitle='" + courseTitle + '\'' +
                ", price=" + price +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
