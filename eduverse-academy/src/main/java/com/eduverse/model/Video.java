package com.eduverse.model;

import javax.persistence.*;

@Entity
@Table(name = "videos")
public class Video {

    public enum Status {
        UPLOADING, PROCESSING, READY, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lesson_id", nullable = false)
    private Long lessonId;

    @Column(name = "original_url", length = 500)
    private String originalUrl;

    @Column(name = "transcoded_urls", columnDefinition = "TEXT")
    private String transcodedUrls;

    @Column
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.UPLOADING;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLessonId() {
        return lessonId;
    }

    public void setLessonId(Long lessonId) {
        this.lessonId = lessonId;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getTranscodedUrls() {
        return transcodedUrls;
    }

    public void setTranscodedUrls(String transcodedUrls) {
        this.transcodedUrls = transcodedUrls;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}
