package com.eduverse.certificate.listener;

import com.eduverse.certificate.model.Certificate;
import com.eduverse.certificate.messaging.ServiceBusEventPublisher;
import com.eduverse.certificate.service.CertificateService;
import com.eduverse.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumes domain events from Azure Service Bus and triggers certificate generation.
 *
 * In production, this class is wired via ServiceBusConfig. For local/test environments,
 * events can be dispatched manually via the processMessage method.
 */
@Component
public class ServiceBusEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusEventListener.class);

    private final CertificateService certificateService;
    private final ServiceBusEventPublisher eventPublisher;

    public ServiceBusEventListener(CertificateService certificateService,
                                   ServiceBusEventPublisher eventPublisher) {
        this.certificateService = certificateService;
        this.eventPublisher = eventPublisher;
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
                case EnrollmentCompletedEvent e -> handleEnrollmentCompleted(e);
                default -> logger.warn("Unhandled event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Failed to process Service Bus message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process event message", e);
        }
    }

    void handleEnrollmentCompleted(EnrollmentCompletedEvent event) {
        logger.info("Handling EnrollmentCompletedEvent: enrollmentId={}, studentId={}, course='{}'",
                event.getEnrollmentId(), event.getStudentId(), event.getCourseTitle());

        Certificate certificate = certificateService.generateCertificate(
                event.getEnrollmentId(),
                event.getStudentId(),
                event.getCourseId(),
                null,
                event.getCourseTitle()
        );

        // Publish CertificateIssuedEvent
        CertificateIssuedEvent issuedEvent = new CertificateIssuedEvent(
                certificate.getId(),
                certificate.getEnrollmentId(),
                certificate.getStudentId(),
                certificate.getCertificateNumber(),
                certificate.getPdfUrl()
        );
        issuedEvent.setCorrelationId(event.getCorrelationId());
        eventPublisher.publishCertificateIssued(issuedEvent);
    }
}
