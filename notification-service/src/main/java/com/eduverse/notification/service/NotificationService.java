package com.eduverse.notification.service;

import com.eduverse.notification.model.Notification;
import com.eduverse.notification.model.NotificationEvent;
import com.eduverse.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender mailSender;

    public NotificationService(NotificationRepository notificationRepository, JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.mailSender = mailSender;
    }

    /**
     * Processes a notification event with idempotency protection.
     * Uses the DB unique constraint on event_id to prevent duplicate processing
     * under concurrent delivery (Azure Service Bus is at-least-once).
     */
    @Transactional
    public Notification processNotificationEvent(NotificationEvent event) {
        logger.info("Processing notification event: eventId={}, type={}, userId={}",
                event.getEventId(), event.getSourceEventType(), event.getUserId());

        // Build the notification entity
        Notification notification = new Notification();
        notification.setEventId(event.getEventId());
        notification.setUserId(event.getUserId());
        notification.setRecipientEmail(event.getRecipientEmail());
        notification.setRecipientName(event.getRecipientName());
        notification.setType(event.getNotificationType());
        notification.setSubject(event.getSubject());
        notification.setMessage(event.getMessage());
        notification.setSourceEventType(event.getSourceEventType());
        notification.setCorrelationId(event.getCorrelationId());
        notification.setStatus(Notification.Status.PENDING);

        try {
            notification = notificationRepository.save(notification);
        } catch (DataIntegrityViolationException e) {
            // Duplicate event_id — already processed, treat as success
            logger.info("Duplicate event detected, skipping: eventId={}", event.getEventId());
            return notificationRepository.findByEventId(event.getEventId()).orElse(notification);
        }

        // Send the notification
        if (notification.getType() == Notification.Type.EMAIL) {
            sendEmail(notification);
        } else {
            // IN_APP and SMS — mark as sent (delivery via other channels)
            markAsSent(notification);
        }

        return notification;
    }

    /**
     * Sends an ad-hoc email notification (backward compatibility with monolith API).
     */
    @Transactional
    public Notification sendAdHocEmail(Long userId, String subject, String messageBody) {
        logger.info("Sending ad-hoc email to userId={}, subject='{}'", userId, subject);

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(Notification.Type.EMAIL);
        notification.setSubject(subject);
        notification.setMessage(messageBody);
        notification.setStatus(Notification.Status.PENDING);
        notification = notificationRepository.save(notification);

        sendEmail(notification);
        return notification;
    }

    void sendEmail(Notification notification) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom("notifications@eduverse.com");
            mailMessage.setTo(notification.getRecipientEmail() != null
                    ? notification.getRecipientEmail()
                    : notification.getUserId() + "@eduverse.com");
            mailMessage.setSubject(notification.getSubject());
            mailMessage.setText(notification.getMessage());

            mailSender.send(mailMessage);
            markAsSent(notification);
            logger.info("Email sent successfully for notificationId={}", notification.getId());
        } catch (Exception e) {
            markAsFailed(notification, e.getMessage());
            logger.error("Failed to send email for notificationId={}: {}",
                    notification.getId(), e.getMessage(), e);
        }
    }

    private void markAsSent(Notification notification) {
        notification.setStatus(Notification.Status.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private void markAsFailed(Notification notification, String reason) {
        notification.setStatus(Notification.Status.FAILED);
        notification.setFailureReason(reason != null && reason.length() > 500
                ? reason.substring(0, 500) : reason);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Optional<Notification> getNotification(Long id) {
        return notificationRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Notification> getFailedNotifications() {
        return notificationRepository.findByStatus(Notification.Status.FAILED);
    }
}
