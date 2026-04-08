package com.eduverse.service;

import com.eduverse.model.Notification;
import com.eduverse.model.User;
import com.eduverse.repository.NotificationRepository;
import com.eduverse.repository.UserRepository;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles all notification and email delivery for the platform.
 *
 * MONOLITH ANTI-PATTERN: This service is called directly by virtually every other
 * service in the system, creating a god-like dependency hub. Email delivery is
 * synchronously coupled to business transactions — if the mail server is slow,
 * enrollment, course publishing, and certificate generation all block.
 */
@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // Hardcoded SMTP config — monolith anti-pattern: config baked into service
    private static final String SMTP_HOST = "smtp.eduverse.com";
    private static final int SMTP_PORT = 587;
    private static final String SMTP_USER = "notifications@eduverse.com";
    private static final String SMTP_PASSWORD = "smtp-password-here";

    /**
     * Sends an email synchronously — blocks the calling thread and transaction.
     */
    @Transactional
    public void sendEmail(Long userId, String subject, String messageBody) {
        logger.info("Sending email to userId={}, subject='{}'", userId, subject);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Create and persist the notification record first
        Notification notification = createNotification(userId, Notification.Type.EMAIL, subject, messageBody);

        try {
            Email email = new SimpleEmail();
            email.setHostName(SMTP_HOST);
            email.setSmtpPort(SMTP_PORT);
            email.setAuthenticator(new DefaultAuthenticator(SMTP_USER, SMTP_PASSWORD));
            email.setStartTLSEnabled(true);
            email.setFrom(SMTP_USER, "EduVerse Academy");
            email.setSubject(subject);
            email.setMsg(messageBody);
            email.addTo(user.getEmail(), user.getFullName());
            email.send();

            markAsSent(notification.getId());
            logger.info("Email sent successfully to {}", user.getEmail());
        } catch (Exception e) {
            // MONOLITH ANTI-PATTERN: email failure logged but swallowed — silent data
            // inconsistency where notification record exists but was never delivered
            logger.error("Failed to send email to userId={}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Persists a notification record in the shared database.
     */
    @Transactional
    public Notification createNotification(Long userId, Notification.Type type,
                                           String subject, String message) {
        logger.debug("Creating {} notification for userId={}", type, userId);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setSubject(subject);
        notification.setMessage(message);
        notification.setSent(false);

        return notificationRepository.save(notification);
    }

    /**
     * Enrollment confirmation — directly coupled to enrollment domain knowledge.
     * This service knows the structure of enrollment data, course names, and
     * student details — violating bounded context boundaries.
     */
    @Transactional
    public void sendEnrollmentConfirmation(Long studentId, String courseName) {
        logger.info("Sending enrollment confirmation to studentId={} for course '{}'",
                studentId, courseName);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        String subject = "Welcome to " + courseName + " - EduVerse Academy";
        String message = "Dear " + student.getFullName() + ",\n\n"
                + "Congratulations! You have been successfully enrolled in \"" + courseName + "\".\n\n"
                + "You can start learning right away by visiting your dashboard.\n\n"
                + "Happy Learning!\n"
                + "The EduVerse Academy Team";

        sendEmail(studentId, subject, message);

        // Also create an in-app notification — different concern mixed in
        createNotification(studentId, Notification.Type.IN_APP, subject,
                "You are now enrolled in " + courseName);
    }

    /**
     * Certificate notification — knows about certificate domain internals.
     */
    @Transactional
    public void sendCertificateIssued(Long studentId, String courseName, String certificateNumber) {
        logger.info("Sending certificate notification to studentId={}, cert={}",
                studentId, certificateNumber);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        String subject = "Certificate Earned - " + courseName;
        String message = "Dear " + student.getFullName() + ",\n\n"
                + "Congratulations on completing \"" + courseName + "\"!\n\n"
                + "Your certificate number is: " + certificateNumber + "\n"
                + "You can download your certificate from your dashboard.\n\n"
                + "Keep up the great work!\n"
                + "The EduVerse Academy Team";

        sendEmail(studentId, subject, message);

        createNotification(studentId, Notification.Type.IN_APP, subject,
                "Certificate " + certificateNumber + " issued for " + courseName);
    }

    /**
     * Marks a notification as sent — shared mutable state across transactions.
     */
    @Transactional
    public void markAsSent(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        notification.setSent(true);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
        logger.debug("Notification {} marked as sent", notificationId);
    }

    public List<Notification> getUnsentNotifications() {
        return notificationRepository.findBySent(false);
    }

    public List<Notification> getUserNotifications(Long userId, boolean sentOnly) {
        return notificationRepository.findByUserIdAndSent(userId, sentOnly);
    }
}
