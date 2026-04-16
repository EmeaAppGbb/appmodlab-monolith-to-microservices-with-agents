package com.eduverse.video.controller;

import com.eduverse.video.model.Video;
import com.eduverse.video.service.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Video REST API. Preserves backward compatibility with the monolith API
 * contract while providing dedicated video management endpoints.
 */
@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(@RequestBody UploadVideoRequest request) {
        try {
            Video video = videoService.upload(
                    request.lessonId, request.courseId, request.title,
                    request.contentType, request.fileSize);
            return ResponseEntity.status(HttpStatus.CREATED).body(video);
        } catch (Exception e) {
            logger.error("Error uploading video for lesson {}", request.lessonId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVideo(@PathVariable("id") Long id) {
        try {
            Video video = videoService.getVideoStatus(id);
            return ResponseEntity.ok(video);
        } catch (Exception e) {
            logger.error("Error fetching video {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getVideoStatus(@PathVariable("id") Long id) {
        try {
            Video video = videoService.getVideoStatus(id);
            return ResponseEntity.ok(Map.of(
                    "id", video.getId(),
                    "status", video.getStatus(),
                    "title", video.getTitle() != null ? video.getTitle() : ""
            ));
        } catch (Exception e) {
            logger.error("Error fetching video status {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<?> getStreamUrl(@PathVariable("id") Long id) {
        try {
            String streamUrl = videoService.getStreamUrl(id);
            return ResponseEntity.ok(Map.of("streamUrl", streamUrl));
        } catch (Exception e) {
            logger.error("Error getting stream URL for video {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<?> getVideoByLesson(@PathVariable("lessonId") Long lessonId) {
        try {
            Optional<Video> video = videoService.getVideosByLesson(lessonId);
            if (video.isPresent()) {
                return ResponseEntity.ok(video.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No video found for lesson " + lessonId));
            }
        } catch (Exception e) {
            logger.error("Error fetching video for lesson {}", lessonId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getVideosByCourse(@PathVariable("courseId") Long courseId) {
        try {
            List<Video> videos = videoService.getVideosByCourse(courseId);
            return ResponseEntity.ok(videos);
        } catch (Exception e) {
            logger.error("Error fetching videos for course {}", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processVideo(@PathVariable("id") Long id) {
        try {
            Video video = videoService.processVideo(id);
            return ResponseEntity.ok(video);
        } catch (Exception e) {
            logger.error("Error processing video {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "service", "video-service",
                "status", "UP"
        ));
    }

    // --- Request DTO ---

    static class UploadVideoRequest {
        public Long lessonId;
        public Long courseId;
        public String title;
        public String contentType;
        public Long fileSize;
    }
}
