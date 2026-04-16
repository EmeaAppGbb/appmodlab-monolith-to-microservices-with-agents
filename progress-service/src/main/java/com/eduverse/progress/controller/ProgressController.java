package com.eduverse.progress.controller;

import com.eduverse.progress.model.Progress;
import com.eduverse.progress.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<List<Progress>> getProgressByEnrollment(@PathVariable Long enrollmentId) {
        List<Progress> progress = progressService.getProgressByEnrollment(enrollmentId);
        return ResponseEntity.ok(progress);
    }

    @PostMapping("/lesson/{lessonId}/complete")
    public ResponseEntity<?> markLessonCompleted(
            @PathVariable Long lessonId,
            @RequestParam Long enrollmentId) {
        try {
            Progress progress = progressService.markLessonCompleted(enrollmentId, lessonId);
            return ResponseEntity.ok(progress);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Progress>> getProgressByStudent(@PathVariable Long studentId) {
        List<Progress> progress = progressService.getProgressByStudent(studentId);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<List<Map<String, Object>>> getDashboard(@RequestParam Long studentId) {
        List<Map<String, Object>> dashboard = progressService.getDashboard(studentId);
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/initialize")
    public ResponseEntity<List<Progress>> initializeProgress(@RequestBody InitializeProgressRequest request) {
        List<Progress> progress = progressService.initializeProgress(
                request.enrollmentId, request.studentId, request.courseId, request.lessonIds);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "progress-service"));
    }

    static class InitializeProgressRequest {
        public Long enrollmentId;
        public Long studentId;
        public Long courseId;
        public List<Long> lessonIds;
    }
}
