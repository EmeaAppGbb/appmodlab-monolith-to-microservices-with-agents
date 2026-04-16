package com.eduverse.controller.api;

import com.eduverse.model.Assessment;
import com.eduverse.model.StudentAnswer;
import com.eduverse.service.AssessmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentApiController {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentApiController.class);

    @Autowired
    private AssessmentService assessmentService;

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<?> getAssessmentByLessonId(@PathVariable("lessonId") Long lessonId) {
        try {
            List<Assessment> assessments = assessmentService.getAssessmentByLessonId(lessonId);
            return ResponseEntity.ok(assessments);
        } catch (Exception e) {
            logger.error("Error fetching assessments for lesson {}", lessonId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<?> submitAnswer(@PathVariable("id") Long id, @RequestBody SubmitAnswerRequest request) {
        try {
            StudentAnswer answer = assessmentService.submitAnswer(id, request.studentId, request.answersJson);
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            logger.error("Error submitting answer for assessment {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    static class SubmitAnswerRequest {
        public Long studentId;
        public String answersJson;
    }
}
