package com.eduverse.enrollment.listener;

import com.eduverse.enrollment.service.EnrollmentService;
import com.eduverse.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events from Azure Service Bus and triggers enrollment actions.
 *
 * In production, this class uses @ServiceBusListener annotations from
 * spring-cloud-azure-starter-servicebus. For local/test environments,
 * events can be dispatched manually via the processMessage method.
 */
@Component
public class ServiceBusEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusEventListener.class);

    private final EnrollmentService enrollmentService;

    public ServiceBusEventListener(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
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
                case PaymentCompletedEvent e -> handlePaymentCompleted(e);
                case PaymentFailedEvent e -> handlePaymentFailed(e);
                case ProgressUpdatedEvent e -> handleProgressUpdated(e);
                case CertificateIssuedEvent e -> handleCertificateIssued(e);
                default -> logger.warn("Unhandled event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Failed to process Service Bus message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process event message", e);
        }
    }

    void handlePaymentCompleted(PaymentCompletedEvent event) {
        logger.info("Handling PaymentCompletedEvent: enrollmentId={}, amount={}",
                event.getEnrollmentId(), event.getAmount());
        enrollmentService.activateEnrollment(event.getEnrollmentId());
    }

    void handlePaymentFailed(PaymentFailedEvent event) {
        logger.info("Handling PaymentFailedEvent: enrollmentId={}, reason={}",
                event.getEnrollmentId(), event.getReason());
        enrollmentService.cancelEnrollment(event.getEnrollmentId(), event.getReason());
    }

    void handleProgressUpdated(ProgressUpdatedEvent event) {
        logger.info("Handling ProgressUpdatedEvent: enrollmentId={}, progress={}%",
                event.getEnrollmentId(), event.getProgressPercent());
        enrollmentService.updateProgress(event.getEnrollmentId(), event.getProgressPercent());
    }

    void handleCertificateIssued(CertificateIssuedEvent event) {
        logger.info("Handling CertificateIssuedEvent: enrollmentId={}, certificateId={}",
                event.getEnrollmentId(), event.getCertificateId());
        enrollmentService.updateCertificateReference(event.getEnrollmentId(), event.getCertificateId());
    }
}
