package com.eduverse.events;

public class AssessmentPassedEvent extends DomainEvent {

    private Long assessmentId;
    private Long studentId;
    private Long lessonId;
    private Long enrollmentId;
    private Integer score;
    private Integer passingScore;

    public AssessmentPassedEvent() {
    }

    public AssessmentPassedEvent(Long assessmentId, Long studentId, Long lessonId, Long enrollmentId, Integer score, Integer passingScore) {
        this.assessmentId = assessmentId;
        this.studentId = studentId;
        this.lessonId = lessonId;
        this.enrollmentId = enrollmentId;
        this.score = score;
        this.passingScore = passingScore;
    }

    @Override
    public String getEventType() {
        return "AssessmentPassed";
    }

    public Long getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Long assessmentId) {
        this.assessmentId = assessmentId;
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

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(Integer passingScore) {
        this.passingScore = passingScore;
    }

    @Override
    public String toString() {
        return "AssessmentPassedEvent{" +
                "assessmentId=" + assessmentId +
                ", studentId=" + studentId +
                ", lessonId=" + lessonId +
                ", enrollmentId=" + enrollmentId +
                ", score=" + score +
                ", passingScore=" + passingScore +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
