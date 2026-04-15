package com.eduverse.service;

import com.eduverse.model.Assessment;
import com.eduverse.model.Progress;
import com.eduverse.model.StudentAnswer;
import com.eduverse.repository.AssessmentRepository;
import com.eduverse.repository.EnrollmentRepository;
import com.eduverse.repository.StudentAnswerRepository;
import com.eduverse.model.Enrollment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Manages assessments, student answer submissions, and grading.
 *
 * MONOLITH ANTI-PATTERN: This service directly invokes ProgressService to update
 * lesson completion when a student passes an assessment. Grading logic, progress
 * tracking, and enrollment lookups are all fused into a single transactional flow.
 * The auto-grading and manual grading paths both reach across domain boundaries
 * to update progress — any change to progress tracking requires changes here too.
 */
@Service
public class AssessmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentService.class);

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    // MONOLITH ANTI-PATTERN: Direct dependency on progress service — assessment
    // grading is tightly coupled to progress tracking
    @Autowired
    private ProgressService progressService;

    // MONOLITH ANTI-PATTERN: Assessment service directly accesses enrollment
    // repository to look up enrollment IDs for progress updates
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Transactional
    public Assessment createAssessment(Assessment assessment) {
        logger.info("Creating assessment '{}' for lesson {}", assessment.getTitle(), assessment.getLessonId());

        assessment = assessmentRepository.save(assessment);
        logger.info("Assessment {} created: '{}'", assessment.getId(), assessment.getTitle());

        return assessment;
    }

    public List<Assessment> getAssessmentByLessonId(Long lessonId) {
        logger.debug("Fetching assessments for lesson {}", lessonId);
        return assessmentRepository.findByLessonId(lessonId);
    }

    /**
     * Submits a student's answer, auto-grades it for quizzes, and updates progress.
     *
     * MONOLITH ANTI-PATTERN: This single method performs answer persistence, auto-grading,
     * enrollment lookup, and progress tracking — four distinct concerns in one transaction.
     * A failure in progress tracking rolls back the answer submission.
     */
    @Transactional
    public StudentAnswer submitAnswer(Long assessmentId, Long studentId, String answersJson) {
        logger.info("Student {} submitting answer for assessment {}", studentId, assessmentId);

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found: " + assessmentId));

        StudentAnswer answer = new StudentAnswer();
        answer.setAssessmentId(assessmentId);
        answer.setStudentId(studentId);
        answer.setAnswersJson(answersJson);

        // Auto-grade quizzes — scoring logic embedded in the service layer
        if (assessment.getType() == Assessment.Type.QUIZ) {
            int score = autoGradeQuiz(assessment, answersJson);
            answer.setScore(score);

            if (assessment.getPassingScore() != null && score >= assessment.getPassingScore()) {
                answer.setFeedback("Passed! Score: " + score + "/" + assessment.getPassingScore());
                logger.info("Student {} passed quiz {} with score {}", studentId, assessmentId, score);

                // MONOLITH ANTI-PATTERN: On quiz pass, directly call ProgressService to
                // mark the lesson as complete — cross-domain side-effect inside grading
                updateProgressOnPass(studentId, assessment.getLessonId());
            } else {
                answer.setFeedback("Score: " + score + ". Passing score: " + assessment.getPassingScore());
                logger.info("Student {} did not pass quiz {} (score: {}, required: {})",
                        studentId, assessmentId, score, assessment.getPassingScore());
            }
        }

        answer = studentAnswerRepository.save(answer);
        logger.info("Answer {} saved for assessment {}", answer.getId(), assessmentId);

        return answer;
    }

    /**
     * Manual grading by an instructor — also triggers progress update on pass.
     */
    @Transactional
    public StudentAnswer gradeManually(Long answerId, Integer score, String feedback, Long gradedBy) {
        logger.info("Instructor {} grading answer {} with score {}", gradedBy, answerId, score);

        StudentAnswer answer = studentAnswerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Student answer not found: " + answerId));

        answer.setScore(score);
        answer.setFeedback(feedback);
        answer.setGradedBy(gradedBy);

        StudentAnswer savedAnswer = studentAnswerRepository.save(answer);

        // Check if the student passed and update progress
        Assessment assessment = assessmentRepository.findById(savedAnswer.getAssessmentId())
                .orElseThrow(() -> new RuntimeException("Assessment not found: " + savedAnswer.getAssessmentId()));

        if (assessment.getPassingScore() != null && score >= assessment.getPassingScore()) {
            logger.info("Student {} passed assessment {} via manual grading",
                    savedAnswer.getStudentId(), assessment.getId());

            // MONOLITH ANTI-PATTERN: Same cross-domain progress update in manual grading path
            updateProgressOnPass(savedAnswer.getStudentId(), assessment.getLessonId());
        }

        return savedAnswer;
    }

    /**
     * Auto-grades a quiz by doing simple JSON answer comparison.
     * Scoring logic is hardcoded — no extensibility or pluggable grading strategies.
     */
    private int autoGradeQuiz(Assessment assessment, String answersJson) {
        logger.debug("Auto-grading quiz {}", assessment.getId());

        // Simplified auto-grading: count matching answers from JSON
        // In a real system, this would parse questionsJson and answersJson to compare
        // For this monolith, we simulate scoring based on answer content length
        if (answersJson == null || answersJson.trim().isEmpty()) {
            return 0;
        }

        // Simulate grading: hash-based pseudo-random score for demonstration
        int baseScore = Math.abs(answersJson.hashCode() % 40) + 60; // Score between 60-100
        logger.debug("Auto-grade result for quiz {}: {}", assessment.getId(), baseScore);

        return baseScore;
    }

    /**
     * MONOLITH ANTI-PATTERN: Private helper that crosses domain boundaries —
     * looks up enrollment from enrollment repository, then calls ProgressService
     * to mark lesson completion. Assessment domain has deep knowledge of both
     * enrollment and progress domain internals.
     */
    private void updateProgressOnPass(Long studentId, Long lessonId) {
        try {
            // Find the student's active enrollment that covers this lesson
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
            Optional<Enrollment> activeEnrollment = enrollments.stream()
                    .filter(e -> e.getStatus() == Enrollment.Status.ACTIVE)
                    .findFirst();

            if (activeEnrollment.isPresent()) {
                progressService.markLessonComplete(studentId, lessonId, activeEnrollment.get().getId());
                logger.info("Progress updated: student {} completed lesson {} in enrollment {}",
                        studentId, lessonId, activeEnrollment.get().getId());
            } else {
                logger.warn("No active enrollment found for student {} to update progress", studentId);
            }
        } catch (Exception e) {
            // MONOLITH ANTI-PATTERN: Swallowing exceptions from cross-domain calls
            // can lead to silent data inconsistency
            logger.error("Failed to update progress for student {} on lesson {}: {}",
                    studentId, lessonId, e.getMessage());
        }
    }
}
