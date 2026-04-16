package com.eduverse.enrollment.service;

import com.eduverse.enrollment.messaging.ServiceBusEventPublisher;
import com.eduverse.enrollment.model.Enrollment;
import com.eduverse.enrollment.repository.EnrollmentRepository;
import com.eduverse.events.EnrollmentActivatedEvent;
import com.eduverse.events.EnrollmentCompletedEvent;
import com.eduverse.events.StudentEnrolledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EnrollmentService {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final ServiceBusEventPublisher eventPublisher;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             ServiceBusEventPublisher eventPublisher) {
        this.enrollmentRepository = enrollmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Enrollment enrollStudent(Long studentId, Long courseId, String courseTitle, BigDecimal price) {
        // Idempotent: if enrollment exists for student+course, return existing
        Optional<Enrollment> existing = enrollmentRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existing.isPresent()) {
            logger.info("Enrollment already exists for student {} and course {}, returning existing enrollment {}",
                    studentId, courseId, existing.get().getId());
            return existing.get();
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setCourseTitle(courseTitle);
        enrollment.setStatus(Enrollment.Status.PENDING);
        enrollment.setProgressPercent(BigDecimal.ZERO);

        enrollment = enrollmentRepository.save(enrollment);
        logger.info("Enrollment {} created for student {} in course {}", enrollment.getId(), studentId, courseId);

        // Publish StudentEnrolledEvent
        StudentEnrolledEvent event = new StudentEnrolledEvent(
                enrollment.getId(), studentId, courseId, courseTitle, price);
        eventPublisher.publishStudentEnrolled(event);

        return enrollment;
    }

    @Transactional
    public Enrollment activateEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        // State guard: only transitions from PENDING or AWAITING_PAYMENT
        if (enrollment.getStatus() != Enrollment.Status.PENDING
                && enrollment.getStatus() != Enrollment.Status.AWAITING_PAYMENT) {
            throw new IllegalStateException("Cannot activate enrollment " + enrollmentId +
                    " — invalid state: " + enrollment.getStatus());
        }

        enrollment.setStatus(Enrollment.Status.ACTIVE);
        enrollment.setActivatedAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);
        logger.info("Enrollment {} activated", enrollmentId);

        // Publish EnrollmentActivatedEvent
        EnrollmentActivatedEvent event = new EnrollmentActivatedEvent(
                enrollment.getId(), enrollment.getStudentId(), enrollment.getCourseId());
        eventPublisher.publishEnrollmentActivated(event);

        return enrollment;
    }

    @Transactional
    public Enrollment updateProgress(Long enrollmentId, BigDecimal progressPercent) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        enrollment.setProgressPercent(progressPercent);
        enrollment.setLastAccessedAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);
        logger.info("Enrollment {} progress updated to {}%", enrollmentId, progressPercent);

        // If 100%, trigger completion
        if (progressPercent.compareTo(new BigDecimal("100")) >= 0) {
            return completeEnrollment(enrollmentId);
        }

        return enrollment;
    }

    @Transactional
    public Enrollment completeEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() != Enrollment.Status.ACTIVE) {
            throw new IllegalStateException("Cannot complete enrollment " + enrollmentId +
                    " — invalid state: " + enrollment.getStatus());
        }

        enrollment.setStatus(Enrollment.Status.COMPLETED);
        enrollment.setCompletedAt(LocalDateTime.now());
        enrollment.setProgressPercent(new BigDecimal("100.00"));
        enrollment = enrollmentRepository.save(enrollment);
        logger.info("Enrollment {} completed", enrollmentId);

        // Publish EnrollmentCompletedEvent
        EnrollmentCompletedEvent event = new EnrollmentCompletedEvent(
                enrollment.getId(), enrollment.getStudentId(),
                enrollment.getCourseId(), enrollment.getCourseTitle());
        eventPublisher.publishEnrollmentCompleted(event);

        return enrollment;
    }

    @Transactional
    public Enrollment cancelEnrollment(Long enrollmentId, String reason) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() != Enrollment.Status.PENDING
                && enrollment.getStatus() != Enrollment.Status.AWAITING_PAYMENT
                && enrollment.getStatus() != Enrollment.Status.ACTIVE) {
            throw new IllegalStateException("Cannot cancel enrollment " + enrollmentId +
                    " — invalid state: " + enrollment.getStatus());
        }

        enrollment.setStatus(Enrollment.Status.CANCELLED);
        enrollment.setCancelledAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);
        logger.info("Enrollment {} cancelled: {}", enrollmentId, reason);

        return enrollment;
    }

    @Transactional
    public Enrollment expireEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() != Enrollment.Status.PENDING
                && enrollment.getStatus() != Enrollment.Status.AWAITING_PAYMENT) {
            throw new IllegalStateException("Cannot expire enrollment " + enrollmentId +
                    " — invalid state: " + enrollment.getStatus());
        }

        enrollment.setStatus(Enrollment.Status.EXPIRED);
        enrollment = enrollmentRepository.save(enrollment);
        logger.info("Enrollment {} expired", enrollmentId);

        return enrollment;
    }

    @Transactional
    public Enrollment dropEnrollment(Long enrollmentId) {
        return cancelEnrollment(enrollmentId, "Student dropped enrollment");
    }

    @Transactional
    public Enrollment updateCertificateReference(Long enrollmentId, Long certificateId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        enrollment.setCertificateId(certificateId);
        enrollment = enrollmentRepository.save(enrollment);
        logger.info("Enrollment {} certificate reference updated to {}", enrollmentId, certificateId);

        return enrollment;
    }

    public Optional<Enrollment> getEnrollment(Long id) {
        return enrollmentRepository.findById(id);
    }

    public List<Enrollment> getStudentEnrollments(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    public List<Enrollment> getCourseEnrollments(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    public List<Enrollment> getStudentDashboard(Long studentId) {
        List<Enrollment> active = enrollmentRepository.findByStudentIdAndStatus(
                studentId, Enrollment.Status.ACTIVE);
        List<Enrollment> completed = enrollmentRepository.findByStudentIdAndStatus(
                studentId, Enrollment.Status.COMPLETED);
        List<Enrollment> dashboard = new ArrayList<>(active);
        dashboard.addAll(completed);
        return dashboard;
    }

    public List<Enrollment> getEnrollmentsByStatus(Enrollment.Status status) {
        return enrollmentRepository.findByStatus(status);
    }

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }
}
