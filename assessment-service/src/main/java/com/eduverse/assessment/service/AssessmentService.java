package com.eduverse.assessment.service;

import com.eduverse.assessment.model.Assessment;
import com.eduverse.assessment.model.StudentAnswer;
import com.eduverse.assessment.repository.AssessmentRepository;
import com.eduverse.assessment.repository.StudentAnswerRepository;
import com.eduverse.assessment.messaging.ServiceBusEventPublisher;
import com.eduverse.events.AssessmentPassedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);

    private final AssessmentRepository assessmentRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ServiceBusEventPublisher eventPublisher;

    public AssessmentService(AssessmentRepository assessmentRepository,
                             StudentAnswerRepository studentAnswerRepository,
                             ServiceBusEventPublisher eventPublisher) {
        this.assessmentRepository = assessmentRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Assessment createAssessment(Long lessonId, Long courseId, String title,
                                       String description, Assessment.Type type,
                                       String questionsJson, Integer passingScore,
                                       Integer maxScore, Integer timeLimitMinutes) {
        Assessment assessment = new Assessment();
        assessment.setLessonId(lessonId);
        assessment.setCourseId(courseId);
        assessment.setTitle(title);
        assessment.setDescription(description);
        assessment.setType(type);
        assessment.setQuestionsJson(questionsJson);
        if (passingScore != null) {
            assessment.setPassingScore(passingScore);
        }
        if (maxScore != null) {
            assessment.setMaxScore(maxScore);
        }
        assessment.setTimeLimitMinutes(timeLimitMinutes);

        assessment = assessmentRepository.save(assessment);
        logger.info("Assessment {} created for lesson {} in course {}", assessment.getId(), lessonId, courseId);
        return assessment;
    }

    public Optional<Assessment> getAssessment(Long id) {
        return assessmentRepository.findById(id);
    }

    public Optional<Assessment> getAssessmentByLesson(Long lessonId) {
        return assessmentRepository.findByLessonId(lessonId);
    }

    public List<Assessment> getAssessmentsByCourse(Long courseId) {
        return assessmentRepository.findByCourseId(courseId);
    }

    @Transactional
    public Assessment updateAssessment(Long id, String title, String description,
                                       Assessment.Type type, String questionsJson,
                                       Integer passingScore, Integer maxScore,
                                       Integer timeLimitMinutes) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assessment not found: " + id));

        if (title != null) assessment.setTitle(title);
        if (description != null) assessment.setDescription(description);
        if (type != null) assessment.setType(type);
        if (questionsJson != null) assessment.setQuestionsJson(questionsJson);
        if (passingScore != null) assessment.setPassingScore(passingScore);
        if (maxScore != null) assessment.setMaxScore(maxScore);
        if (timeLimitMinutes != null) assessment.setTimeLimitMinutes(timeLimitMinutes);

        assessment = assessmentRepository.save(assessment);
        logger.info("Assessment {} updated", id);
        return assessment;
    }

    @Transactional
    public StudentAnswer submitAnswers(Long assessmentId, Long studentId,
                                       Long enrollmentId, String answersJson) {
        // Idempotent: if answers already submitted, return existing
        Optional<StudentAnswer> existing = studentAnswerRepository
                .findByAssessmentIdAndStudentId(assessmentId, studentId);
        if (existing.isPresent()) {
            logger.info("Answers already submitted for assessment {} by student {}, returning existing",
                    assessmentId, studentId);
            return existing.get();
        }

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found: " + assessmentId));

        // Auto-grade: simulate grading by generating a random score 60-100
        int score = ThreadLocalRandom.current().nextInt(60, 101);
        boolean passed = score >= assessment.getPassingScore();

        StudentAnswer answer = new StudentAnswer();
        answer.setAssessmentId(assessmentId);
        answer.setStudentId(studentId);
        answer.setEnrollmentId(enrollmentId);
        answer.setAnswersJson(answersJson);
        answer.setScore(score);
        answer.setPassed(passed);
        answer.setGradedAt(LocalDateTime.now());

        answer = studentAnswerRepository.save(answer);
        logger.info("Student {} submitted answers for assessment {}: score={}, passed={}",
                studentId, assessmentId, score, passed);

        // Publish event when student passes
        if (passed) {
            AssessmentPassedEvent event = new AssessmentPassedEvent(
                    assessmentId, studentId, assessment.getLessonId(),
                    enrollmentId, score, assessment.getPassingScore());
            eventPublisher.publishAssessmentPassed(event);
        }

        return answer;
    }

    public Optional<StudentAnswer> getResults(Long assessmentId, Long studentId) {
        return studentAnswerRepository.findByAssessmentIdAndStudentId(assessmentId, studentId);
    }

    public List<StudentAnswer> getStudentResults(Long studentId) {
        return studentAnswerRepository.findByStudentId(studentId);
    }
}
