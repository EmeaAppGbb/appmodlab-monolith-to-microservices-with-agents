package com.eduverse.payment.messaging;

import com.eduverse.events.StudentEnrolledEvent;
import com.eduverse.payment.saga.EnrollmentPaymentSaga;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Azure Service Bus listener for StudentEnrolledEvent.
 * Triggers the enrollment-payment saga when a student enrolls in a course.
 *
 * Activated only when Service Bus connection string is configured.
 * In local development, events are triggered via the REST API instead.
 */
@Component
@ConditionalOnProperty(name = "spring.cloud.azure.servicebus.connection-string", matchIfMissing = false)
public class StudentEnrolledEventListener {

    private static final Logger logger = LoggerFactory.getLogger(StudentEnrolledEventListener.class);

    private final EnrollmentPaymentSaga saga;
    private final ObjectMapper objectMapper;

    public StudentEnrolledEventListener(EnrollmentPaymentSaga saga) {
        this.saga = saga;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Process incoming StudentEnrolledEvent from the "student-enrolled" topic.
     * This method is called by the Azure Service Bus processor.
     */
    public void processMessage(String messageBody) {
        logger.info("Received StudentEnrolledEvent message: {}", messageBody);

        try {
            StudentEnrolledEvent event = objectMapper.readValue(messageBody, StudentEnrolledEvent.class);
            logger.info("Processing enrollment event: enrollmentId={}, studentId={}, courseId={}, price={}",
                    event.getEnrollmentId(), event.getStudentId(), event.getCourseId(), event.getPrice());

            saga.handleStudentEnrolled(event);

            logger.info("Successfully processed StudentEnrolledEvent for enrollment {}",
                    event.getEnrollmentId());
        } catch (Exception e) {
            logger.error("Failed to process StudentEnrolledEvent: {}", e.getMessage(), e);
            throw new RuntimeException("Event processing failed", e);
        }
    }
}
