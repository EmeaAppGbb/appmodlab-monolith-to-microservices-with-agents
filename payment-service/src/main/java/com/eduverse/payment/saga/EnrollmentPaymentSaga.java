package com.eduverse.payment.saga;

import com.eduverse.events.PaymentCompletedEvent;
import com.eduverse.events.PaymentFailedEvent;
import com.eduverse.events.PaymentRefundedEvent;
import com.eduverse.events.StudentEnrolledEvent;
import com.eduverse.payment.messaging.ServiceBusEventPublisher;
import com.eduverse.payment.model.Payment;
import com.eduverse.payment.model.SagaState;
import com.eduverse.payment.repository.SagaStateRepository;
import com.eduverse.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Orchestrates the payment leg of the enrollment-payment saga.
 *
 * Saga flow:
 *   1. Receive StudentEnrolledEvent → create saga (INITIATED)
 *   2. Create pending payment → advance to PAYMENT_PENDING
 *   3. Process payment via Stripe (outside transaction)
 *   4a. Success → PAYMENT_COMPLETED, publish PaymentCompletedEvent
 *   4b. Failure → PAYMENT_FAILED, publish PaymentFailedEvent
 *   5. On refund request → REFUNDED, publish PaymentRefundedEvent
 *
 * Zero-price enrollments are auto-completed without Stripe interaction.
 */
@Component
public class EnrollmentPaymentSaga {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentPaymentSaga.class);

    private final PaymentService paymentService;
    private final SagaStateRepository sagaStateRepository;
    private final ServiceBusEventPublisher eventPublisher;

    public EnrollmentPaymentSaga(PaymentService paymentService,
                                  SagaStateRepository sagaStateRepository,
                                  ServiceBusEventPublisher eventPublisher) {
        this.paymentService = paymentService;
        this.sagaStateRepository = sagaStateRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Entry point: triggered by StudentEnrolledEvent from Service Bus.
     */
    public void handleStudentEnrolled(StudentEnrolledEvent event) {
        logger.info("Saga: Handling StudentEnrolledEvent for enrollment {}", event.getEnrollmentId());

        // Idempotency: skip if saga already exists for this enrollment
        Optional<SagaState> existing = sagaStateRepository.findByEnrollmentId(event.getEnrollmentId());
        if (existing.isPresent()) {
            logger.info("Saga already exists for enrollment {} in state {}, skipping",
                    event.getEnrollmentId(), existing.get().getState());
            return;
        }

        // Zero-price enrollment: auto-complete without payment
        if (event.getPrice() == null || event.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            handleZeroPriceEnrollment(event);
            return;
        }

        SagaState saga = initiateSaga(event);
        processPaymentForSaga(saga, event);
    }

    @Transactional
    protected SagaState initiateSaga(StudentEnrolledEvent event) {
        SagaState saga = new SagaState();
        saga.setEnrollmentId(event.getEnrollmentId());
        saga.setStudentId(event.getStudentId());
        saga.setCourseId(event.getCourseId());
        saga.setCorrelationId(event.getCorrelationId());
        saga.setLastEventId(event.getEventId());
        saga.setState(SagaState.State.INITIATED);
        return sagaStateRepository.save(saga);
    }

    private void processPaymentForSaga(SagaState saga, StudentEnrolledEvent event) {
        try {
            // Create pending payment
            Payment payment = paymentService.createPendingPayment(
                    event.getEnrollmentId(),
                    event.getStudentId(),
                    event.getCourseId(),
                    event.getPrice(),
                    "USD"
            );

            advanceSagaState(saga.getId(), SagaState.State.PAYMENT_PENDING, payment.getId());

            // Process via Stripe (outside transaction — fixes monolith anti-pattern)
            Payment processed = paymentService.processPayment(payment.getId());

            if (processed.getStatus() == Payment.Status.COMPLETED) {
                advanceSagaState(saga.getId(), SagaState.State.PAYMENT_COMPLETED, payment.getId());
                publishPaymentCompleted(processed, event.getStudentId());
            } else {
                advanceSagaState(saga.getId(), SagaState.State.PAYMENT_FAILED, payment.getId());
                publishPaymentFailed(processed);
            }

        } catch (Exception e) {
            logger.error("Saga: Payment processing failed for enrollment {}: {}",
                    event.getEnrollmentId(), e.getMessage(), e);
            advanceSagaState(saga.getId(), SagaState.State.PAYMENT_FAILED, saga.getPaymentId());

            PaymentFailedEvent failedEvent = new PaymentFailedEvent(
                    saga.getPaymentId(), event.getEnrollmentId(), e.getMessage());
            failedEvent.setCorrelationId(event.getCorrelationId());
            eventPublisher.publishPaymentFailed(failedEvent);
        }
    }

    private void handleZeroPriceEnrollment(StudentEnrolledEvent event) {
        logger.info("Saga: Zero-price enrollment {}, auto-completing", event.getEnrollmentId());

        SagaState saga = new SagaState();
        saga.setEnrollmentId(event.getEnrollmentId());
        saga.setStudentId(event.getStudentId());
        saga.setCourseId(event.getCourseId());
        saga.setCorrelationId(event.getCorrelationId());
        saga.setLastEventId(event.getEventId());
        saga.setState(SagaState.State.PAYMENT_COMPLETED);
        sagaStateRepository.save(saga);

        // Publish completed event with zero amount
        PaymentCompletedEvent completedEvent = new PaymentCompletedEvent(
                null, event.getEnrollmentId(), BigDecimal.ZERO, "USD", null);
        completedEvent.setCorrelationId(event.getCorrelationId());
        eventPublisher.publishPaymentCompleted(completedEvent);
    }

    /**
     * Handle refund request for a completed payment.
     */
    public void handleRefund(Long enrollmentId, String reason) {
        SagaState saga = sagaStateRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new RuntimeException(
                        "No saga found for enrollment: " + enrollmentId));

        if (saga.getState() != SagaState.State.PAYMENT_COMPLETED) {
            throw new RuntimeException("Cannot refund enrollment " + enrollmentId +
                    " — saga state is " + saga.getState());
        }

        Payment refunded = paymentService.refundPayment(saga.getPaymentId());
        advanceSagaState(saga.getId(), SagaState.State.REFUNDED, saga.getPaymentId());

        PaymentRefundedEvent refundedEvent = new PaymentRefundedEvent(
                refunded.getId(), enrollmentId, refunded.getAmount(), reason);
        refundedEvent.setCorrelationId(saga.getCorrelationId());
        eventPublisher.publishPaymentRefunded(refundedEvent);
    }

    @Transactional
    protected void advanceSagaState(Long sagaId, SagaState.State newState, Long paymentId) {
        SagaState saga = sagaStateRepository.findById(sagaId)
                .orElseThrow(() -> new RuntimeException("Saga not found: " + sagaId));

        if (!isValidTransition(saga.getState(), newState)) {
            logger.warn("Invalid saga transition from {} to {} for saga {}",
                    saga.getState(), newState, sagaId);
            return;
        }

        logger.info("Saga {}: {} → {}", sagaId, saga.getState(), newState);
        saga.setState(newState);
        if (paymentId != null) {
            saga.setPaymentId(paymentId);
        }
        sagaStateRepository.save(saga);
    }

    private boolean isValidTransition(SagaState.State from, SagaState.State to) {
        return switch (from) {
            case INITIATED -> to == SagaState.State.PAYMENT_PENDING;
            case PAYMENT_PENDING -> to == SagaState.State.PAYMENT_COMPLETED
                    || to == SagaState.State.PAYMENT_FAILED;
            case PAYMENT_COMPLETED -> to == SagaState.State.REFUNDED;
            case PAYMENT_FAILED, REFUNDED -> false;
        };
    }

    private void publishPaymentCompleted(Payment payment, Long studentId) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                payment.getId(),
                payment.getEnrollmentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStripePaymentId()
        );
        event.setCorrelationId(
                sagaStateRepository.findByEnrollmentId(payment.getEnrollmentId())
                        .map(SagaState::getCorrelationId).orElse(null));
        eventPublisher.publishPaymentCompleted(event);
    }

    private void publishPaymentFailed(Payment payment) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                payment.getId(),
                payment.getEnrollmentId(),
                payment.getFailureReason()
        );
        event.setCorrelationId(
                sagaStateRepository.findByEnrollmentId(payment.getEnrollmentId())
                        .map(SagaState::getCorrelationId).orElse(null));
        eventPublisher.publishPaymentFailed(event);
    }
}
