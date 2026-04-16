package com.eduverse.controller.api;

import com.eduverse.model.Enrollment;
import com.eduverse.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentApiController {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentApiController.class);

    @Autowired
    private EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<?> enrollStudent(@RequestBody EnrollmentRequest request) {
        try {
            Enrollment enrollment = enrollmentService.enrollStudent(
                    request.studentId, request.courseId, request.stripeToken);
            return ResponseEntity.status(HttpStatus.CREATED).body(enrollment);
        } catch (Exception e) {
            logger.error("Error enrolling student {} in course {}", request.studentId, request.courseId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
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

    @PostMapping("/{id}/progress")
    public ResponseEntity<?> updateProgress(@PathVariable("id") Long id, @RequestBody ProgressUpdateRequest request) {
        try {
            enrollmentService.updateProgress(request.studentId, request.lessonId, id);
            return ResponseEntity.ok(Map.of("message", "Progress updated"));
        } catch (Exception e) {
            logger.error("Error updating progress for enrollment {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeEnrollment(@PathVariable("id") Long id) {
        try {
            Enrollment enrollment = enrollmentService.completeEnrollment(id);
            return ResponseEntity.ok(enrollment);
        } catch (Exception e) {
            logger.error("Error completing enrollment {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    static class EnrollmentRequest {
        public Long studentId;
        public Long courseId;
        public String stripeToken;
    }

    static class ProgressUpdateRequest {
        public Long studentId;
        public Long lessonId;
    }
}
