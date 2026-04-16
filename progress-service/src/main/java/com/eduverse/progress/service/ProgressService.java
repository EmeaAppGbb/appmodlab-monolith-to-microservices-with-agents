package com.eduverse.progress.service;

import com.eduverse.progress.model.Progress;
import com.eduverse.progress.repository.ProgressRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgressService {

    private static final Logger logger = LoggerFactory.getLogger(ProgressService.class);

    private final ProgressRepository progressRepository;

    public ProgressService(ProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    /**
     * Initializes progress records for each lesson in an enrollment.
     * Idempotent — skips lessons that already have a progress record.
     */
    @Transactional
    public List<Progress> initializeProgress(Long enrollmentId, Long studentId, Long courseId,
                                             List<Long> lessonIds) {
        logger.info("Initializing progress for enrollmentId={}, studentId={}, courseId={}, lessons={}",
                enrollmentId, studentId, courseId, lessonIds.size());

        List<Progress> results = new ArrayList<>();

        for (Long lessonId : lessonIds) {
            Optional<Progress> existing = progressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId);
            if (existing.isPresent()) {
                logger.debug("Progress already exists for enrollmentId={}, lessonId={} — skipping",
                        enrollmentId, lessonId);
                results.add(existing.get());
                continue;
            }

            Progress progress = new Progress();
            progress.setEnrollmentId(enrollmentId);
            progress.setStudentId(studentId);
            progress.setCourseId(courseId);
            progress.setLessonId(lessonId);
            progress.setCompleted(false);
            progress.setProgressPercent(BigDecimal.ZERO);

            progress = progressRepository.save(progress);
            results.add(progress);
            logger.debug("Created progress record id={} for enrollmentId={}, lessonId={}",
                    progress.getId(), enrollmentId, lessonId);
        }

        return results;
    }

    /**
     * Marks a lesson as completed and recalculates progress percentage
     * for all records in this enrollment.
     */
    @Transactional
    public Progress markLessonCompleted(Long enrollmentId, Long lessonId) {
        logger.info("Marking lesson completed: enrollmentId={}, lessonId={}", enrollmentId, lessonId);

        Progress progress = progressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                .orElseThrow(() -> new RuntimeException(
                        "Progress not found for enrollmentId=" + enrollmentId + ", lessonId=" + lessonId));

        if (Boolean.TRUE.equals(progress.getCompleted())) {
            logger.info("Lesson already completed: enrollmentId={}, lessonId={}", enrollmentId, lessonId);
            return progress;
        }

        progress.setCompleted(true);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastAccessedAt(LocalDateTime.now());
        progressRepository.save(progress);

        // Recalculate progress percentage for all records in this enrollment
        BigDecimal percent = recalculateProgressPercent(enrollmentId);

        List<Progress> allProgress = progressRepository.findByEnrollmentId(enrollmentId);
        for (Progress p : allProgress) {
            p.setProgressPercent(percent);
            progressRepository.save(p);
        }

        // Return the updated record
        return progressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                .orElse(progress);
    }

    /**
     * Calculates the current progress percentage for an enrollment.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateProgress(Long enrollmentId) {
        long totalCount = progressRepository.countByEnrollmentId(enrollmentId);
        if (totalCount == 0) {
            return BigDecimal.ZERO;
        }
        long completedCount = progressRepository.countByEnrollmentIdAndCompleted(enrollmentId, true);
        return BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<Progress> getProgressByEnrollment(Long enrollmentId) {
        return progressRepository.findByEnrollmentId(enrollmentId);
    }

    @Transactional(readOnly = true)
    public List<Progress> getProgressByStudent(Long studentId) {
        return progressRepository.findByStudentId(studentId);
    }

    /**
     * Returns a dashboard summary of progress per enrollment for a student.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDashboard(Long studentId) {
        List<Progress> allProgress = progressRepository.findByStudentId(studentId);

        Map<Long, List<Progress>> byEnrollment = allProgress.stream()
                .collect(Collectors.groupingBy(Progress::getEnrollmentId));

        List<Map<String, Object>> dashboard = new ArrayList<>();
        for (Map.Entry<Long, List<Progress>> entry : byEnrollment.entrySet()) {
            Long enrollmentId = entry.getKey();
            List<Progress> records = entry.getValue();

            long totalLessons = records.size();
            long completedLessons = records.stream().filter(p -> Boolean.TRUE.equals(p.getCompleted())).count();
            BigDecimal percent = totalLessons > 0
                    ? BigDecimal.valueOf(completedLessons)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            Long courseId = records.get(0).getCourseId();

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("enrollmentId", enrollmentId);
            summary.put("courseId", courseId);
            summary.put("totalLessons", totalLessons);
            summary.put("completedLessons", completedLessons);
            summary.put("progressPercent", percent);
            summary.put("completed", completedLessons == totalLessons);
            dashboard.add(summary);
        }

        return dashboard;
    }

    @Transactional(readOnly = true)
    public boolean isLessonCompleted(Long enrollmentId, Long lessonId) {
        return progressRepository.findByEnrollmentIdAndLessonId(enrollmentId, lessonId)
                .map(p -> Boolean.TRUE.equals(p.getCompleted()))
                .orElse(false);
    }

    private BigDecimal recalculateProgressPercent(Long enrollmentId) {
        long totalCount = progressRepository.countByEnrollmentId(enrollmentId);
        if (totalCount == 0) {
            return BigDecimal.ZERO;
        }
        long completedCount = progressRepository.countByEnrollmentIdAndCompleted(enrollmentId, true);
        return BigDecimal.valueOf(completedCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
    }
}
