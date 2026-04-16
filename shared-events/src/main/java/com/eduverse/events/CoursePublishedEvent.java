package com.eduverse.events;

import java.math.BigDecimal;

public class CoursePublishedEvent extends DomainEvent {

    private Long courseId;
    private String courseTitle;
    private Long instructorId;
    private String category;
    private BigDecimal price;

    public CoursePublishedEvent() {
    }

    public CoursePublishedEvent(Long courseId, String courseTitle, Long instructorId, String category, BigDecimal price) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.instructorId = instructorId;
        this.category = category;
        this.price = price;
    }

    @Override
    public String getEventType() {
        return "CoursePublished";
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

    public Long getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(Long instructorId) {
        this.instructorId = instructorId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "CoursePublishedEvent{" +
                "courseId=" + courseId +
                ", courseTitle='" + courseTitle + '\'' +
                ", instructorId=" + instructorId +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
