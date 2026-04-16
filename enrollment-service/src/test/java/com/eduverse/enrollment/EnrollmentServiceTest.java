package com.eduverse.enrollment;

import com.eduverse.enrollment.messaging.ServiceBusEventPublisher;
import com.eduverse.enrollment.model.Enrollment;
import com.eduverse.enrollment.repository.EnrollmentRepository;
import com.eduverse.enrollment.service.EnrollmentService;
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
class EnrollmentServiceTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private EnrollmentService enrollmentService;

    @BeforeEach
    void setUp() {
        ServiceBusEventPublisher eventPublisher = new ServiceBusEventPublisher("");
        enrollmentService = new EnrollmentService(enrollmentRepository, eventPublisher);
    }

    @Test
    void enrollStudent_shouldCreateEnrollmentWithPendingStatus() {
        Enrollment enrollment = enrollmentService.enrollStudent(
                100L, 200L, "Java 101", new BigDecimal("49.99"));

        assertNotNull(enrollment.getId());
        assertEquals(100L, enrollment.getStudentId());
        assertEquals(200L, enrollment.getCourseId());
        assertEquals("Java 101", enrollment.getCourseTitle());
        assertEquals(Enrollment.Status.PENDING, enrollment.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(enrollment.getProgressPercent()));
        assertNotNull(enrollment.getEnrolledAt());
        assertNotNull(enrollment.getCreatedAt());
    }

    @Test
    void enrollStudent_shouldBeIdempotent() {
        Enrollment first = enrollmentService.enrollStudent(
                100L, 200L, "Java 101", new BigDecimal("49.99"));
        Enrollment second = enrollmentService.enrollStudent(
                100L, 200L, "Java 101", new BigDecimal("49.99"));

        assertEquals(first.getId(), second.getId());
    }

    @Test
    void activateEnrollment_shouldTransitionToActive() {
        Enrollment enrollment = enrollmentService.enrollStudent(
                100L, 200L, "Java 101", new BigDecimal("49.99"));

        Enrollment activated = enrollmentService.activateEnrollment(enrollment.getId());

        assertEquals(Enrollment.Status.ACTIVE, activated.getStatus());
        assertNotNull(activated.getActivatedAt());
    }

    @Test
    void activateEnrollment_shouldRejectInvalidState() {
        Enrollment enrollment = enrollmentService.enrollStudent(
                100L, 200L, "Java 101", new BigDecimal("49.99"));
        enrollmentService.cancelEnrollment(enrollment.getId(), "test cancel");

        assertThrows(IllegalStateException.class, () ->
                enrollmentService.activateEnrollment(enrollment.getId()));
    }

    @Test
    void cancelEnrollment_shouldWork() {
        Enrollment enrollment = enrollmentService.enrollStudent(
                100L, 200L, "Java 101", new BigDecimal("49.99"));

        Enrollment cancelled = enrollmentService.cancelEnrollment(enrollment.getId(), "Changed mind");

        assertEquals(Enrollment.Status.CANCELLED, cancelled.getStatus());
        assertNotNull(cancelled.getCancelledAt());
    }

    @Test
    void updateProgress_at100Percent_shouldTriggerCompletion() {
        Enrollment enrollment = enrollmentService.enrollStudent(
                100L, 200L, "Java 101", new BigDecimal("49.99"));
        enrollmentService.activateEnrollment(enrollment.getId());

        Enrollment completed = enrollmentService.updateProgress(
                enrollment.getId(), new BigDecimal("100.00"));

        assertEquals(Enrollment.Status.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
    }

    @Test
    void getStudentEnrollments_shouldReturnEnrollments() {
        enrollmentService.enrollStudent(100L, 200L, "Java 101", new BigDecimal("49.99"));
        enrollmentService.enrollStudent(100L, 201L, "Python 201", new BigDecimal("59.99"));

        List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(100L);

        assertEquals(2, enrollments.size());
    }
}
