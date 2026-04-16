package com.eduverse.enrollment.controller;

import com.eduverse.enrollment.model.Enrollment;
import com.eduverse.enrollment.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Enrollment REST API. Preserves backward compatibility with the monolith API
 * contract while adding saga-aware enrollment management.
 */
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentController.class);

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    public ResponseEntity<?> enrollStudent(@RequestBody EnrollRequest request) {
        try {
            Enrollment enrollment = enrollmentService.enrollStudent(
                    request.studentId, request.courseId, request.courseTitle, request.price);
            return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
        } catch (Exception e) {
            logger.error("Error enrolling student {} in course {}", request.studentId, request.courseId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEnrollment(@PathVariable("id") Long id) {
        try {
            return enrollmentService.getEnrollment(id)
                    .map(enrollment -> ResponseEntity.ok((Object) enrollment))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Enrollment not found: " + id)));
        } catch (Exception e) {
            logger.error("Error fetching enrollment {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/drop")
    public ResponseEntity<?> dropEnrollment(@PathVariable("id") Long id) {
        try {
            Enrollment enrollment = enrollmentService.dropEnrollment(id);
            return ResponseEntity.ok(enrollment);
        } catch (Exception e) {
            logger.error("Error dropping enrollment {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentEnrollments(@PathVariable("studentId") Long studentId) {
        try {
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(studentId);
            return ResponseEntity.ok(enrollments);
        } catch (Exception e) {
            logger.error("Error fetching enrollments for student {}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getCourseEnrollments(@PathVariable("courseId") Long courseId) {
        try {
            List<Enrollment> enrollments = enrollmentService.getCourseEnrollments(courseId);
            return ResponseEntity.ok(enrollments);
        } catch (Exception e) {
            logger.error("Error fetching enrollments for course {}", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}/dashboard")
    public ResponseEntity<?> getStudentDashboard(@PathVariable("studentId") Long studentId) {
        try {
            List<Enrollment> dashboard = enrollmentService.getStudentDashboard(studentId);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            logger.error("Error fetching dashboard for student {}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin")
    public ResponseEntity<?> getAllEnrollments(
            @RequestParam(value = "status", required = false) String status) {
        try {
            if (status != null && !status.isBlank()) {
                Enrollment.Status enrollmentStatus = Enrollment.Status.valueOf(status.toUpperCase());
                return ResponseEntity.ok(enrollmentService.getEnrollmentsByStatus(enrollmentStatus));
            }
            return ResponseEntity.ok(enrollmentService.getAllEnrollments());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status: " + status));
        } catch (Exception e) {
            logger.error("Error fetching admin enrollments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "service", "enrollment-service",
                "status", "UP"));
    }

    // --- Request DTOs ---

    static class EnrollRequest {
        public Long studentId;
        public Long courseId;
        public String courseTitle;
        public BigDecimal price;
    }
}
