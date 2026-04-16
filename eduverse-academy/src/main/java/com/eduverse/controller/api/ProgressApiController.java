package com.eduverse.controller.api;

import com.eduverse.model.Progress;
import com.eduverse.service.ProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/progress")
public class ProgressApiController {

    private static final Logger logger = LoggerFactory.getLogger(ProgressApiController.class);

    @Autowired
    private ProgressService progressService;

    @PostMapping("/track")
    public ResponseEntity<?> trackLessonProgress(@RequestBody TrackProgressRequest request) {
        try {
            Progress progress = progressService.trackLessonProgress(
                    request.studentId, request.lessonId, request.enrollmentId, request.positionSeconds);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            logger.error("Error tracking progress for student {} lesson {}", request.studentId, request.lessonId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<?> markLessonComplete(@RequestBody MarkCompleteRequest request) {
        try {
            Progress progress = progressService.markLessonComplete(
                    request.studentId, request.lessonId, request.enrollmentId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            logger.error("Error marking lesson {} complete for student {}", request.lessonId, request.studentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<?> getStudentProgress(@PathVariable("enrollmentId") Long enrollmentId) {
        try {
            List<Progress> progressList = progressService.getStudentProgress(enrollmentId);
            return ResponseEntity.ok(progressList);
        } catch (Exception e) {
            logger.error("Error fetching progress for enrollment {}", enrollmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/enrollment/{enrollmentId}/calculate")
    public ResponseEntity<?> calculateEnrollmentProgress(@PathVariable("enrollmentId") Long enrollmentId) {
        try {
            BigDecimal percentage = progressService.calculateEnrollmentProgress(enrollmentId);
            return ResponseEntity.ok(Map.of("enrollmentId", enrollmentId, "progressPercentage", percentage));
        } catch (Exception e) {
            logger.error("Error calculating progress for enrollment {}", enrollmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}/lesson/{lessonId}")
    public ResponseEntity<?> getLessonProgress(@PathVariable("studentId") Long studentId,
                                               @PathVariable("lessonId") Long lessonId) {
        try {
            Optional<Progress> progress = progressService.getLessonProgress(studentId, lessonId);
            if (progress.isPresent()) {
                return ResponseEntity.ok(progress.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No progress found for student " + studentId + " lesson " + lessonId));
        } catch (Exception e) {
            logger.error("Error fetching progress for student {} lesson {}", studentId, lessonId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    static class TrackProgressRequest {
        public Long studentId;
        public Long lessonId;
        public Long enrollmentId;
        public Integer positionSeconds;
    }

    static class MarkCompleteRequest {
        public Long studentId;
        public Long lessonId;
        public Long enrollmentId;
    }
}
