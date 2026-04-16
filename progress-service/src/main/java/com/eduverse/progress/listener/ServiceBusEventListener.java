package com.eduverse.progress.listener;

import com.eduverse.events.*;
import com.eduverse.progress.model.Progress;
import com.eduverse.progress.messaging.ServiceBusEventPublisher;
import com.eduverse.progress.service.ProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Consumes domain events from Azure Service Bus and triggers progress tracking.
 *
 * In production, this class is wired via ServiceBusConfig.
 * For local/test environments, events can be dispatched manually via the processMessage method.
 */
@Component
public class ServiceBusEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusEventListener.class);

    private final ProgressService progressService;
    private final ServiceBusEventPublisher eventPublisher;

    public ServiceBusEventListener(ProgressService progressService,
                                   ServiceBusEventPublisher eventPublisher) {
        this.progressService = progressService;
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
                case EnrollmentActivatedEvent e -> handleEnrollmentActivated(e);
                case AssessmentPassedEvent e -> handleAssessmentPassed(e);
                default -> logger.warn("Unhandled event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            logger.error("Failed to process Service Bus message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process event message", e);
        }
    }

    void handleEnrollmentActivated(EnrollmentActivatedEvent event) {
        logger.info("Handling EnrollmentActivatedEvent: enrollmentId={}, studentId={}, courseId={}",
                event.getEnrollmentId(), event.getStudentId(), event.getCourseId());

        // In production, lessonIds would be fetched from the course-catalog-service.
        // For now, use an empty list — the initialize endpoint can be called directly.
        List<Long> lessonIds = Collections.emptyList();

        progressService.initializeProgress(
                event.getEnrollmentId(), event.getStudentId(), event.getCourseId(), lessonIds);
    }

    void handleAssessmentPassed(AssessmentPassedEvent event) {
        logger.info("Handling AssessmentPassedEvent: enrollmentId={}, lessonId={}, score={}",
                event.getEnrollmentId(), event.getLessonId(), event.getScore());

        Progress progress = progressService.markLessonCompleted(
                event.getEnrollmentId(), event.getLessonId());

        // Publish lesson-completed event
        LessonCompletedEvent lessonCompletedEvent = new LessonCompletedEvent(
                event.getStudentId(), event.getLessonId(), event.getEnrollmentId());
        lessonCompletedEvent.setCorrelationId(event.getCorrelationId());
        eventPublisher.publishLessonCompleted(lessonCompletedEvent);

        // Publish progress-updated event
        BigDecimal percent = progressService.calculateProgress(event.getEnrollmentId());
        ProgressUpdatedEvent progressUpdatedEvent = new ProgressUpdatedEvent(
                event.getEnrollmentId(), event.getStudentId(), percent,
                percent.compareTo(new BigDecimal("100.00")) == 0);
        progressUpdatedEvent.setCorrelationId(event.getCorrelationId());
        eventPublisher.publishProgressUpdated(progressUpdatedEvent);
    }
}
