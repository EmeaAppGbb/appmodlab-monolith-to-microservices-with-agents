package com.eduverse.service;

import com.eduverse.model.Course;
import com.eduverse.model.Enrollment;
import com.eduverse.model.Payment;
import com.eduverse.repository.CourseRepository;
import com.eduverse.repository.EnrollmentRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EnrollmentService}.
 *
 * Demonstrates the tight coupling inherent in the monolith: enrolling a student
 * requires mocking five separate dependencies (CourseRepository, EnrollmentRepository,
 * PaymentService, NotificationService, and implicitly ProgressService/CertificateService).
 * In a well-decomposed architecture, each concern would be independently testable.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ProgressService progressService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CertificateService certificateService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private Course publishedCourse;

    @Before
    public void setUp() {
        publishedCourse = new Course();
        publishedCourse.setId(1L);
        publishedCourse.setTitle("Introduction to Java");
        publishedCourse.setStatus(Course.Status.PUBLISHED);
        publishedCourse.setPrice(new BigDecimal("49.99"));
        publishedCourse.setInstructorId(10L);
    }

    /**
     * Tests the full enrollment flow for a paid course. This single test method
     * exercises four bounded contexts (course, enrollment, payment, notification)
     * — a clear sign of monolithic coupling. Each mock setup below represents a
     * cross-domain dependency that would be an API call in a microservices architecture.
     */
    @Test
    public void testEnrollStudent() {
        Long studentId = 100L;
        Long courseId = 1L;
        String stripeToken = "tok_test_123";

        // Mock: Course domain — validate course exists and is published
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(publishedCourse));

        // Mock: Enrollment domain — no duplicate enrollment
        when(enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId))
                .thenReturn(Optional.empty());

        // Mock: Enrollment domain — save returns the enrollment with an ID
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment saved = invocation.getArgument(0);
            saved.setId(500L);
            return saved;
        });

        // Mock: Payment domain — create and process payment
        Payment payment = new Payment();
        payment.setId(200L);
        payment.setEnrollmentId(500L);
        payment.setAmount(new BigDecimal("49.99"));
        payment.setStatus(Payment.Status.PENDING);

        when(paymentService.createPayment(eq(500L), eq(new BigDecimal("49.99")), eq("USD")))
                .thenReturn(payment);
        when(paymentService.processPayment(eq(200L), eq(stripeToken)))
                .thenReturn(payment);

        // Mock: Notification domain — send confirmation (demonstrates coupling)
        doNothing().when(notificationService).sendEnrollmentConfirmation(eq(studentId), anyString());

        // Execute the enrollment
        Enrollment result = enrollmentService.enrollStudent(studentId, courseId, stripeToken);

        // Verify the enrollment was created correctly
        assertNotNull(result);
        assertEquals(Long.valueOf(500L), result.getId());
        assertEquals(studentId, result.getStudentId());
        assertEquals(courseId, result.getCourseId());
        assertEquals(Enrollment.Status.ACTIVE, result.getStatus());

        // Verify cross-domain interactions — each verify below is a coupling point
        verify(courseRepository).findById(courseId);
        verify(enrollmentRepository).findByStudentIdAndCourseId(studentId, courseId);
        verify(enrollmentRepository).save(any(Enrollment.class));
        verify(paymentService).createPayment(eq(500L), eq(new BigDecimal("49.99")), eq("USD"));
        verify(paymentService).processPayment(eq(200L), eq(stripeToken));
        verify(notificationService).sendEnrollmentConfirmation(eq(studentId), eq("Introduction to Java"));
    }
}
