package com.eduverse.service;

import com.eduverse.model.Certificate;
import com.eduverse.model.Course;
import com.eduverse.model.Enrollment;
import com.eduverse.model.Payment;
import com.eduverse.repository.CourseRepository;
import com.eduverse.repository.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates the student enrollment lifecycle — the most tightly coupled service
 * in the entire monolith.
 *
 * MONOLITH ANTI-PATTERN: This service is the poster child for tight coupling.
 * A single enrollStudent() call creates an enrollment, processes a payment,
 * initializes progress tracking, and sends a notification — all in ONE database
 * transaction spanning four bounded contexts. It @Autowires five other services,
 * creating a dependency web that makes independent deployment, testing, and
 * scaling impossible. Any change to payment, progress, certificate, or notification
 * logic requires retesting the entire enrollment flow.
 */
@Service
public class EnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // MONOLITH ANTI-PATTERN: Direct dependency on course repository — enrollment
    // service reaches into the course domain to validate and fetch course data
    @Autowired
    private CourseRepository courseRepository;

    // MONOLITH ANTI-PATTERN: Four service-to-service dependencies — enrollment is
    // the hub of a dependency star topology
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProgressService progressService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CertificateService certificateService;

    /**
     * Enrolls a student in a course — the most monolithic method in the system.
     *
     * MONOLITH ANTI-PATTERN: A SINGLE @Transactional method that:
     * 1. Validates course existence and status (Course domain)
     * 2. Checks for duplicate enrollment (Enrollment domain)
     * 3. Creates the enrollment record (Enrollment domain)
     * 4. Creates and processes a payment (Payment domain)
     * 5. Sends a confirmation notification (Notification domain)
     *
     * If the email server is slow, the entire enrollment + payment transaction
     * is held open. If the payment fails, the enrollment is rolled back even
     * though no money was charged. Five bounded contexts in one transaction.
     */
    @Transactional
    public Enrollment enrollStudent(Long studentId, Long courseId, String stripeToken) {
        logger.info("Enrolling student {} in course {}", studentId, courseId);

        // Step 1: Validate course — reaching into course domain
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        if (course.getStatus() != Course.Status.PUBLISHED) {
            throw new RuntimeException("Cannot enroll in unpublished course: " + courseId);
        }

        // Step 2: Check for duplicate enrollment
        enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .ifPresent(existing -> {
                    throw new RuntimeException("Student " + studentId +
                            " is already enrolled in course " + courseId);
                });

        // Step 3: Create enrollment record
        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setStatus(Enrollment.Status.ACTIVE);
        enrollment.setProgressPercent(BigDecimal.ZERO);

        enrollment = enrollmentRepository.save(enrollment);
        logger.info("Enrollment {} created for student {} in course {}", 
                enrollment.getId(), studentId, courseId);

        // Step 4: Create and process payment — cross-domain transaction
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            logger.info("Processing payment of {} for enrollment {}", course.getPrice(), enrollment.getId());

            Payment payment = paymentService.createPayment(
                    enrollment.getId(), course.getPrice(), "USD");

            try {
                paymentService.processPayment(payment.getId(), stripeToken);
                logger.info("Payment processed for enrollment {}", enrollment.getId());
            } catch (Exception e) {
                // MONOLITH ANTI-PATTERN: Payment failure rolls back the ENTIRE enrollment
                // transaction — the enrollment record is lost even though it's a separate concern
                logger.error("Payment failed for enrollment {}, rolling back: {}",
                        enrollment.getId(), e.getMessage());
                throw new RuntimeException("Enrollment failed — payment error: " + e.getMessage(), e);
            }
        } else {
            logger.info("Free course — no payment required for enrollment {}", enrollment.getId());
        }

        // Step 5: Send notification — synchronous email in the enrollment transaction
        try {
            notificationService.sendEnrollmentConfirmation(studentId, course.getTitle());
        } catch (Exception e) {
            // MONOLITH ANTI-PATTERN: Notification failure is swallowed — the student
            // is enrolled but never gets confirmation, and there's no retry mechanism
            logger.error("Failed to send enrollment confirmation for student {}: {}",
                    studentId, e.getMessage());
        }

        logger.info("Student {} successfully enrolled in course '{}' (enrollment {})",
                studentId, course.getTitle(), enrollment.getId());

        return enrollment;
    }

    public List<Enrollment> getStudentEnrollments(Long studentId) {
        logger.debug("Fetching enrollments for student {}", studentId);
        return enrollmentRepository.findByStudentId(studentId);
    }

    /**
     * Updates progress by delegating to ProgressService — pass-through coupling.
     *
     * MONOLITH ANTI-PATTERN: This method exists only to proxy to ProgressService,
     * adding an unnecessary layer of indirection while still maintaining tight coupling.
     * Controllers call EnrollmentService.updateProgress() which calls
     * ProgressService.markLessonComplete() — two services for one action.
     */
    @Transactional
    public void updateProgress(Long studentId, Long lessonId, Long enrollmentId) {
        logger.info("Updating progress via enrollment service: student={}, lesson={}, enrollment={}",
                studentId, lessonId, enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() != Enrollment.Status.ACTIVE) {
            throw new RuntimeException("Cannot update progress — enrollment " +
                    enrollmentId + " is not active");
        }

        // MONOLITH ANTI-PATTERN: Delegating directly to ProgressService — tight coupling
        progressService.markLessonComplete(studentId, lessonId, enrollmentId);

        // Update last accessed timestamp — enrollment service managing progress state
        enrollment.setLastAccessed(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    /**
     * Completes an enrollment and generates a certificate — crosses enrollment,
     * progress, certificate, and notification domains in one transaction.
     *
     * MONOLITH ANTI-PATTERN: Enrollment completion triggers certificate generation
     * (which generates a PDF using iText) and notification sending — all synchronously
     * inside a single transaction. PDF generation failure rolls back the completion.
     */
    @Transactional
    public Enrollment completeEnrollment(Long enrollmentId) {
        logger.info("Completing enrollment {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() != Enrollment.Status.ACTIVE) {
            throw new RuntimeException("Cannot complete enrollment " + enrollmentId +
                    " — current status: " + enrollment.getStatus());
        }

        // Verify progress is at 100% — cross-domain validation
        BigDecimal progress = progressService.calculateEnrollmentProgress(enrollmentId);
        if (progress.compareTo(BigDecimal.valueOf(100)) < 0) {
            throw new RuntimeException("Cannot complete enrollment " + enrollmentId +
                    " — progress is only " + progress + "%");
        }

        enrollment.setStatus(Enrollment.Status.COMPLETED);
        enrollment.setCompletionDate(LocalDateTime.now());
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        logger.info("Enrollment {} marked as COMPLETED", enrollmentId);

        // Generate certificate — cross-domain call to CertificateService
        Certificate certificate = certificateService.generateCertificate(enrollmentId);
        logger.info("Certificate {} generated for enrollment {}", 
                certificate.getCertificateNumber(), enrollmentId);

        // Fetch course name for notification — reaching into course domain again
        Course course = courseRepository.findById(savedEnrollment.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found: " + savedEnrollment.getCourseId()));

        // Send certificate notification — synchronous email in completion transaction
        try {
            notificationService.sendCertificateIssued(
                    savedEnrollment.getStudentId(),
                    course.getTitle(),
                    certificate.getCertificateNumber()
            );
        } catch (Exception e) {
            logger.error("Failed to send certificate notification for enrollment {}: {}",
                    enrollmentId, e.getMessage());
        }

        return savedEnrollment;
    }
}
