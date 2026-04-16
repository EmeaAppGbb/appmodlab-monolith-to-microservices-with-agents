package com.eduverse.assessment.controller;

import com.eduverse.assessment.model.Assessment;
import com.eduverse.assessment.model.StudentAnswer;
import com.eduverse.assessment.service.AssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentController.class);

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAssessment(@PathVariable("id") Long id) {
        try {
            return assessmentService.getAssessment(id)
                    .map(a -> ResponseEntity.ok((Object) a))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Assessment not found: " + id)));
        } catch (Exception e) {
            logger.error("Error fetching assessment {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<?> getAssessmentByLesson(@PathVariable("lessonId") Long lessonId) {
        try {
            return assessmentService.getAssessmentByLesson(lessonId)
                    .map(a -> ResponseEntity.ok((Object) a))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No assessment found for lesson: " + lessonId)));
        } catch (Exception e) {
            logger.error("Error fetching assessment for lesson {}", lessonId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<?> getAssessmentsByCourse(@PathVariable("courseId") Long courseId) {
        try {
            List<Assessment> assessments = assessmentService.getAssessmentsByCourse(courseId);
            return ResponseEntity.ok(assessments);
        } catch (Exception e) {
            logger.error("Error fetching assessments for course {}", courseId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createAssessment(@RequestBody CreateAssessmentRequest request) {
        try {
            Assessment assessment = assessmentService.createAssessment(
                    request.lessonId, request.courseId, request.title,
                    request.description, request.type, request.questionsJson,
                    request.passingScore, request.maxScore, request.timeLimitMinutes);
            return ResponseEntity.status(HttpStatus.CREATED).body(assessment);
        } catch (Exception e) {
            logger.error("Error creating assessment", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAssessment(@PathVariable("id") Long id,
                                              @RequestBody UpdateAssessmentRequest request) {
        try {
            Assessment assessment = assessmentService.updateAssessment(
                    id, request.title, request.description, request.type,
                    request.questionsJson, request.passingScore, request.maxScore,
                    request.timeLimitMinutes);
            return ResponseEntity.ok(assessment);
        } catch (Exception e) {
            logger.error("Error updating assessment {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitAnswers(@PathVariable("id") Long id,
                                           @RequestBody SubmitAnswersRequest request) {
        try {
            StudentAnswer answer = assessmentService.submitAnswers(
                    id, request.studentId, request.enrollmentId, request.answersJson);
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            logger.error("Error submitting answers for assessment {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<?> getResults(@PathVariable("id") Long id,
                                        @RequestParam("studentId") Long studentId) {
        try {
            return assessmentService.getResults(id, studentId)
                    .map(a -> ResponseEntity.ok((Object) a))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "No results found for assessment " + id +
                                    " and student " + studentId)));
        } catch (Exception e) {
            logger.error("Error fetching results for assessment {} student {}", id, studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}/results")
    public ResponseEntity<?> getStudentResults(@PathVariable("studentId") Long studentId) {
        try {
            List<StudentAnswer> results = assessmentService.getStudentResults(studentId);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error fetching results for student {}", studentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "assessment-service"));
    }

    // --- Request DTOs ---

    static class CreateAssessmentRequest {
        public Long lessonId;
        public Long courseId;
        public String title;
        public String description;
        public Assessment.Type type;
        public String questionsJson;
        public Integer passingScore;
        public Integer maxScore;
        public Integer timeLimitMinutes;
    }

    static class UpdateAssessmentRequest {
        public String title;
        public String description;
        public Assessment.Type type;
        public String questionsJson;
        public Integer passingScore;
        public Integer maxScore;
        public Integer timeLimitMinutes;
    }

    static class SubmitAnswersRequest {
        public Long studentId;
        public Long enrollmentId;
        public String answersJson;
    }
}
