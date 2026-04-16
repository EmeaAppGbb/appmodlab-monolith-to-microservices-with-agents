package com.eduverse.controller.api;

import com.eduverse.model.Video;
import com.eduverse.service.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/videos")
public class VideoApiController {

    private static final Logger logger = LoggerFactory.getLogger(VideoApiController.class);

    @Autowired
    private VideoService videoService;

    @PostMapping
    public ResponseEntity<?> uploadVideo(@RequestBody UploadVideoRequest request) {
        try {
            Video video = videoService.uploadVideo(request.lessonId, request.originalUrl, request.duration);
            return ResponseEntity.status(HttpStatus.CREATED).body(video);
        } catch (Exception e) {
            logger.error("Error uploading video for lesson {}", request.lessonId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<?> getVideoByLessonId(@PathVariable("lessonId") Long lessonId) {
        try {
            List<Video> videos = videoService.getVideoByLessonId(lessonId);
            return ResponseEntity.ok(videos);
        } catch (Exception e) {
            logger.error("Error fetching videos for lesson {}", lessonId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<?> streamVideo(@PathVariable("id") Long id,
                                         @RequestParam(value = "quality", defaultValue = "720p") String quality) {
        try {
            String streamUrl = videoService.streamVideo(id, quality);
            return ResponseEntity.ok(Map.of("streamUrl", streamUrl));
        } catch (Exception e) {
            logger.error("Error streaming video {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    static class UploadVideoRequest {
        public Long lessonId;
        public String originalUrl;
        public Integer duration;
    }
}
