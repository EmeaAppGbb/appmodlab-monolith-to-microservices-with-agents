package com.eduverse.assessment;

import com.eduverse.assessment.messaging.ServiceBusEventPublisher;
import com.eduverse.assessment.model.Assessment;
import com.eduverse.assessment.model.StudentAnswer;
import com.eduverse.assessment.repository.AssessmentRepository;
import com.eduverse.assessment.repository.StudentAnswerRepository;
import com.eduverse.assessment.service.AssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AssessmentServiceTest {

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    private AssessmentService assessmentService;

    @BeforeEach
    void setUp() {
        ServiceBusEventPublisher eventPublisher = new ServiceBusEventPublisher("");
        assessmentService = new AssessmentService(assessmentRepository, studentAnswerRepository, eventPublisher);
    }

    @Test
    void createAssessment_shouldCreateAndReturn() {
        Assessment assessment = assessmentService.createAssessment(
                10L, 1L, "Test Quiz", "A test quiz",
                Assessment.Type.QUIZ, "[{\"q\":\"What is 1+1?\"}]",
                70, 100, 15);

        assertNotNull(assessment.getId());
        assertEquals(10L, assessment.getLessonId());
        assertEquals(1L, assessment.getCourseId());
        assertEquals("Test Quiz", assessment.getTitle());
        assertEquals(Assessment.Type.QUIZ, assessment.getType());
        assertEquals(70, assessment.getPassingScore());
        assertEquals(100, assessment.getMaxScore());
        assertEquals(15, assessment.getTimeLimitMinutes());
        assertNotNull(assessment.getCreatedAt());
    }

    @Test
    void getAssessmentByLesson_shouldReturnAssessment() {
        assessmentService.createAssessment(
                20L, 2L, "Lesson Quiz", "Quiz for lesson 20",
                Assessment.Type.QUIZ, "[]", 70, 100, null);

        Optional<Assessment> found = assessmentService.getAssessmentByLesson(20L);

        assertTrue(found.isPresent());
        assertEquals("Lesson Quiz", found.get().getTitle());
        assertEquals(20L, found.get().getLessonId());
    }

    @Test
    void submitAnswers_shouldGradeAndReturnScore() {
        Assessment assessment = assessmentService.createAssessment(
                30L, 3L, "Graded Quiz", "Auto-graded quiz",
                Assessment.Type.QUIZ, "[]", 70, 100, 10);

        StudentAnswer answer = assessmentService.submitAnswers(
                assessment.getId(), 100L, 50L, "[{\"q\":0,\"a\":1}]");

        assertNotNull(answer.getId());
        assertEquals(assessment.getId(), answer.getAssessmentId());
        assertEquals(100L, answer.getStudentId());
        assertEquals(50L, answer.getEnrollmentId());
        assertNotNull(answer.getScore());
        assertTrue(answer.getScore() >= 60 && answer.getScore() <= 100);
        assertNotNull(answer.getPassed());
        assertNotNull(answer.getGradedAt());
    }

    @Test
    void submitAnswers_shouldBeIdempotent() {
        Assessment assessment = assessmentService.createAssessment(
                40L, 4L, "Idempotent Quiz", "Test idempotency",
                Assessment.Type.EXAM, "[]", 70, 100, null);

        StudentAnswer first = assessmentService.submitAnswers(
                assessment.getId(), 200L, 60L, "[{\"q\":0,\"a\":0}]");
        StudentAnswer second = assessmentService.submitAnswers(
                assessment.getId(), 200L, 60L, "[{\"q\":0,\"a\":1}]");

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getScore(), second.getScore());
    }

    @Test
    void getResults_shouldReturnStudentAnswer() {
        Assessment assessment = assessmentService.createAssessment(
                50L, 5L, "Results Quiz", "Test results",
                Assessment.Type.QUIZ, "[]", 70, 100, null);

        assessmentService.submitAnswers(assessment.getId(), 300L, 70L, "[]");

        Optional<StudentAnswer> results = assessmentService.getResults(assessment.getId(), 300L);

        assertTrue(results.isPresent());
        assertEquals(300L, results.get().getStudentId());
        assertEquals(assessment.getId(), results.get().getAssessmentId());
    }
}
