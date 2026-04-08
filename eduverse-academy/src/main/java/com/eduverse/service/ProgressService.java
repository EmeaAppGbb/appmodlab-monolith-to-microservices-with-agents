package com.eduverse.service;

import com.eduverse.model.Enrollment;
import com.eduverse.model.Lesson;
import com.eduverse.model.Module;
import com.eduverse.model.Progress;
import com.eduverse.repository.EnrollmentRepository;
import com.eduverse.repository.LessonRepository;
import com.eduverse.repository.ModuleRepository;
import com.eduverse.repository.ProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Tracks student progress across lessons and enrollments.
 *
 * MONOLITH ANTI-PATTERN: This service is the most heavily depended-upon service
 * in the system. It is called directly by EnrollmentService, AssessmentService,
 * and controllers. It also reaches into the Course/Module/Lesson domain to count
 * total lessons, creating deep cross-domain data coupling. Changes to the course
 * structure model will break progress calculations.
 */
@Service
public class ProgressService {

    private static final Logger logger = LoggerFactory.getLogger(ProgressService.class);

    @Autowired
    private ProgressRepository progressRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // MONOLITH ANTI-PATTERN: Progress service directly accesses Module and Lesson
    // repositories — reaching across the course management bounded context
    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private LessonRepository lessonRepository;

    /**
     * Tracks a student's position in a video lesson. Creates or updates the
     * progress record in a single transaction that also touches the enrollment.
     */
    @Transactional
    public Progress trackLessonProgress(Long studentId, Long lessonId, Long enrollmentId,
                                        Integer positionSeconds) {
        logger.info("Tracking progress: student={}, lesson={}, enrollment={}, position={}s",
                studentId, lessonId, enrollmentId, positionSeconds);

        // Verify enrollment exists and is active — cross-domain validation
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() != Enrollment.Status.ACTIVE) {
            throw new RuntimeException("Cannot track progress for non-active enrollment: " + enrollmentId);
        }

        Optional<Progress> existing = progressRepository.findByStudentIdAndLessonId(studentId, lessonId);

        Progress progress;
        if (existing.isPresent()) {
            progress = existing.get();
            progress.setLastPositionSeconds(positionSeconds);
        } else {
            progress = new Progress();
            progress.setStudentId(studentId);
            progress.setLessonId(lessonId);
            progress.setEnrollmentId(enrollmentId);
            progress.setLastPositionSeconds(positionSeconds);
            progress.setCompleted(false);
        }

        progress = progressRepository.save(progress);

        // MONOLITH ANTI-PATTERN: side-effect — update enrollment's lastAccessed
        // in the same transaction, coupling progress tracking to enrollment state
        enrollment.setLastAccessed(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        return progress;
    }

    /**
     * Marks a lesson as complete and recalculates the overall enrollment progress.
     * Single transaction spans progress + enrollment + course structure queries.
     */
    @Transactional
    public Progress markLessonComplete(Long studentId, Long lessonId, Long enrollmentId) {
        logger.info("Marking lesson complete: student={}, lesson={}, enrollment={}",
                studentId, lessonId, enrollmentId);

        Optional<Progress> existing = progressRepository.findByStudentIdAndLessonId(studentId, lessonId);

        Progress progress;
        if (existing.isPresent()) {
            progress = existing.get();
        } else {
            progress = new Progress();
            progress.setStudentId(studentId);
            progress.setLessonId(lessonId);
            progress.setEnrollmentId(enrollmentId);
        }

        progress.setCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progress = progressRepository.save(progress);

        // Recalculate and update enrollment progress in the same transaction
        calculateEnrollmentProgress(enrollmentId);

        logger.info("Lesson {} marked complete for student {}", lessonId, studentId);
        return progress;
    }

    /**
     * Calculates overall enrollment progress by counting completed lessons against
     * total lessons in the course.
     *
     * MONOLITH ANTI-PATTERN: This method reaches deep into the course domain —
     * it queries ModuleRepository and LessonRepository to count total lessons,
     * then updates the Enrollment entity directly. Three bounded contexts
     * (progress, course, enrollment) are fused in a single method.
     */
    @Transactional
    public BigDecimal calculateEnrollmentProgress(Long enrollmentId) {
        logger.debug("Calculating progress for enrollment {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        // Count completed lessons for this enrollment
        long completedCount = progressRepository.countByEnrollmentIdAndCompletedTrue(enrollmentId);

        // MONOLITH ANTI-PATTERN: Reaching into the course domain to count total lessons
        // by traversing Course → Module → Lesson hierarchy
        List<Module> modules = moduleRepository.findByCourseIdOrderBySortOrder(enrollment.getCourseId());
        long totalLessons = 0;
        for (Module module : modules) {
            List<Lesson> lessons = lessonRepository.findByModuleIdOrderBySortOrder(module.getId());
            totalLessons += lessons.size();
        }

        BigDecimal progressPercent;
        if (totalLessons == 0) {
            progressPercent = BigDecimal.ZERO;
        } else {
            progressPercent = BigDecimal.valueOf(completedCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP);
        }

        // Directly mutate the enrollment entity from the progress service
        enrollment.setProgressPercent(progressPercent);
        enrollment.setLastAccessed(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        logger.info("Enrollment {} progress: {}/{} lessons ({}%)",
                enrollmentId, completedCount, totalLessons, progressPercent);

        return progressPercent;
    }

    public List<Progress> getStudentProgress(Long enrollmentId) {
        logger.debug("Fetching progress records for enrollment {}", enrollmentId);
        return progressRepository.findByEnrollmentId(enrollmentId);
    }

    public Optional<Progress> getLessonProgress(Long studentId, Long lessonId) {
        return progressRepository.findByStudentIdAndLessonId(studentId, lessonId);
    }
}
