package com.eduverse.notification.listener;

import com.eduverse.events.*;
import com.eduverse.notification.model.Notification;
import com.eduverse.notification.model.NotificationEvent;
import com.eduverse.notification.model.NotificationTemplate;
import com.eduverse.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events from Azure Service Bus and triggers notifications.
 *
 * In production, this class uses @ServiceBusListener annotations from
 * spring-cloud-azure-starter-servicebus. For local/test environments,
 * events can be dispatched manually via the processMessage method.
 */
@Component
public class ServiceBusEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusEventListener.class);

    private final NotificationService notificationService;

    public ServiceBusEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Processes a raw JSON message from Azure Service Bus.
     * Deserializes using the shared EventSerializer (Jackson polymorphic) and
     * dispatches to the appropriate handler based on event type.
     */
    public void processMessage(String messageBody) {
        try {
            DomainEvent event = EventSerializer.deserialize(messageBody);
            logger.info("Received event: type={}, eventId={}", event.getEventType(), event.getEventId());

            switch (event) {
                case StudentEnrolledEvent e -> handleStudentEnrolled(e);
                case PaymentCompletedEvent e -> handlePaymentCompleted(e);
                case CertificateIssuedEvent e -> handleCertificateIssued(e);
                default -> logger.warn("Unhandled event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Failed to process Service Bus message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process event message", e);
        }
    }

    void handleStudentEnrolled(StudentEnrolledEvent event) {
        logger.info("Handling StudentEnrolledEvent: studentId={}, course='{}'",
                event.getStudentId(), event.getCourseTitle());

        NotificationEvent notification = NotificationEvent.builder()
                .eventId(event.getEventId())
                .correlationId(event.getCorrelationId())
                .sourceEventType(event.getEventType())
                .userId(event.getStudentId())
                .notificationType(Notification.Type.EMAIL)
                .subject(String.format(NotificationTemplate.STUDENT_ENROLLED.getSubjectTemplate(),
                        event.getCourseTitle()))
                .message(String.format(NotificationTemplate.STUDENT_ENROLLED.getMessageTemplate(),
                        event.getCourseTitle()))
                .build();

        notificationService.processNotificationEvent(notification);

        // Also create an in-app notification
        NotificationEvent inAppNotification = NotificationEvent.builder()
                .eventId(event.getEventId() + "-inapp")
                .correlationId(event.getCorrelationId())
                .sourceEventType(event.getEventType())
                .userId(event.getStudentId())
                .notificationType(Notification.Type.IN_APP)
                .subject("Enrollment Confirmed")
                .message("You are now enrolled in " + event.getCourseTitle())
                .build();

        notificationService.processNotificationEvent(inAppNotification);
    }

    void handlePaymentCompleted(PaymentCompletedEvent event) {
        logger.info("Handling PaymentCompletedEvent: paymentId={}, amount={}",
                event.getPaymentId(), event.getAmount());

        NotificationEvent notification = NotificationEvent.builder()
                .eventId(event.getEventId())
                .correlationId(event.getCorrelationId())
                .sourceEventType(event.getEventType())
                .userId(null) // PaymentCompletedEvent lacks studentId; resolved via enrichment in production
                .notificationType(Notification.Type.EMAIL)
                .subject(NotificationTemplate.PAYMENT_COMPLETED.getSubjectTemplate())
                .message(String.format(NotificationTemplate.PAYMENT_COMPLETED.getMessageTemplate(),
                        event.getAmount(), event.getCurrency(), event.getStripePaymentId()))
                .build();

        notificationService.processNotificationEvent(notification);
    }

    void handleCertificateIssued(CertificateIssuedEvent event) {
        logger.info("Handling CertificateIssuedEvent: studentId={}, cert={}",
                event.getStudentId(), event.getCertificateNumber());

        NotificationEvent notification = NotificationEvent.builder()
                .eventId(event.getEventId())
                .correlationId(event.getCorrelationId())
                .sourceEventType(event.getEventType())
                .userId(event.getStudentId())
                .notificationType(Notification.Type.EMAIL)
                .subject(NotificationTemplate.CERTIFICATE_ISSUED.getSubjectTemplate())
                .message(String.format(NotificationTemplate.CERTIFICATE_ISSUED.getMessageTemplate(),
                        event.getCertificateNumber()))
                .build();

        notificationService.processNotificationEvent(notification);

        // Also create an in-app notification
        NotificationEvent inAppNotification = NotificationEvent.builder()
                .eventId(event.getEventId() + "-inapp")
                .correlationId(event.getCorrelationId())
                .sourceEventType(event.getEventType())
                .userId(event.getStudentId())
                .notificationType(Notification.Type.IN_APP)
                .subject("Certificate Earned")
                .message("Certificate " + event.getCertificateNumber() + " has been issued")
                .build();

        notificationService.processNotificationEvent(inAppNotification);
    }
}
