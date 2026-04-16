package com.eduverse.events;

public class VideoReadyEvent extends DomainEvent {

    private Long videoId;
    private Long lessonId;
    private String transcodedUrl;
    private Integer duration;

    public VideoReadyEvent() {
    }

    public VideoReadyEvent(Long videoId, Long lessonId, String transcodedUrl, Integer duration) {
        this.videoId = videoId;
        this.lessonId = lessonId;
        this.transcodedUrl = transcodedUrl;
        this.duration = duration;
    }

    @Override
    public String getEventType() {
        return "VideoReady";
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public String getTranscodedUrl() {
        return transcodedUrl;
    }

    public void setTranscodedUrl(String transcodedUrl) {
        this.transcodedUrl = transcodedUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return "VideoReadyEvent{" +
                "videoId=" + videoId +
                ", lessonId=" + lessonId +
                ", transcodedUrl='" + transcodedUrl + '\'' +
                ", duration=" + duration +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
