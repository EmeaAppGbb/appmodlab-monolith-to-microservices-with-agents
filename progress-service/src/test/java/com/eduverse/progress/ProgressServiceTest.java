package com.eduverse.progress;

import com.eduverse.progress.model.Progress;
import com.eduverse.progress.repository.ProgressRepository;
import com.eduverse.progress.service.ProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProgressServiceTest {

    @Autowired
    private ProgressRepository progressRepository;

    private ProgressService progressService;

    @BeforeEach
    void setUp() {
        progressService = new ProgressService(progressRepository);
    }

    @Test
    void initializeProgress_shouldCreateRecords() {
        List<Long> lessonIds = List.of(1L, 2L, 3L);

        List<Progress> results = progressService.initializeProgress(10L, 100L, 200L, lessonIds);

        assertEquals(3, results.size());
        for (Progress p : results) {
            assertEquals(10L, p.getEnrollmentId());
            assertEquals(100L, p.getStudentId());
            assertEquals(200L, p.getCourseId());
            assertFalse(p.getCompleted());
            assertNotNull(p.getId());
        }
    }

    @Test
    void initializeProgress_shouldBeIdempotent() {
        List<Long> lessonIds = List.of(1L, 2L, 3L);

        List<Progress> first = progressService.initializeProgress(10L, 100L, 200L, lessonIds);
        List<Progress> second = progressService.initializeProgress(10L, 100L, 200L, lessonIds);

        assertEquals(first.size(), second.size());
        for (int i = 0; i < first.size(); i++) {
            assertEquals(first.get(i).getId(), second.get(i).getId());
        }

        // Verify no duplicates in DB
        long count = progressRepository.countByEnrollmentId(10L);
        assertEquals(3, count);
    }

    @Test
    void markLessonCompleted_shouldUpdateStatusAndRecalculatePercent() {
        progressService.initializeProgress(10L, 100L, 200L, List.of(1L, 2L, 3L, 4L));

        Progress completed = progressService.markLessonCompleted(10L, 1L);

        assertTrue(completed.getCompleted());
        assertNotNull(completed.getCompletedAt());
        assertEquals(0, new BigDecimal("25.00").compareTo(completed.getProgressPercent()));

        // All records in enrollment should have updated percent
        List<Progress> all = progressRepository.findByEnrollmentId(10L);
        for (Progress p : all) {
            assertEquals(0, new BigDecimal("25.00").compareTo(p.getProgressPercent()));
        }
    }

    @Test
    void calculateProgress_shouldReturnCorrectPercentage() {
        progressService.initializeProgress(10L, 100L, 200L, List.of(1L, 2L, 3L, 4L));
        progressService.markLessonCompleted(10L, 1L);
        progressService.markLessonCompleted(10L, 2L);

        BigDecimal percent = progressService.calculateProgress(10L);

        assertEquals(0, new BigDecimal("50.00").compareTo(percent));
    }

    @Test
    void getProgressByEnrollment_shouldReturnAllRecords() {
        progressService.initializeProgress(10L, 100L, 200L, List.of(1L, 2L, 3L));

        List<Progress> results = progressService.getProgressByEnrollment(10L);

        assertEquals(3, results.size());
    }

    @Test
    void markLessonCompleted_alreadyCompleted_shouldReturnExisting() {
        progressService.initializeProgress(10L, 100L, 200L, List.of(1L, 2L));
        progressService.markLessonCompleted(10L, 1L);

        Progress result = progressService.markLessonCompleted(10L, 1L);

        assertTrue(result.getCompleted());
    }

    @Test
    void isLessonCompleted_shouldReturnCorrectStatus() {
        progressService.initializeProgress(10L, 100L, 200L, List.of(1L, 2L));

        assertFalse(progressService.isLessonCompleted(10L, 1L));

        progressService.markLessonCompleted(10L, 1L);

        assertTrue(progressService.isLessonCompleted(10L, 1L));
    }

    @Test
    void getDashboard_shouldReturnSummaryPerEnrollment() {
        progressService.initializeProgress(10L, 100L, 200L, List.of(1L, 2L, 3L, 4L));
        progressService.markLessonCompleted(10L, 1L);
        progressService.markLessonCompleted(10L, 2L);

        progressService.initializeProgress(20L, 100L, 300L, List.of(5L, 6L));
        progressService.markLessonCompleted(20L, 5L);
        progressService.markLessonCompleted(20L, 6L);

        var dashboard = progressService.getDashboard(100L);

        assertEquals(2, dashboard.size());
    }
}
