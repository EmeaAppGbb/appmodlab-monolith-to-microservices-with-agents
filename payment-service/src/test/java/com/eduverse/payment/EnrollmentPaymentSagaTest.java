package com.eduverse.payment;

import com.eduverse.events.StudentEnrolledEvent;
import com.eduverse.payment.gateway.MockStripeGateway;
import com.eduverse.payment.gateway.StripeGateway;
import com.eduverse.payment.messaging.ServiceBusEventPublisher;
import com.eduverse.payment.model.SagaState;
import com.eduverse.payment.repository.PaymentRepository;
import com.eduverse.payment.repository.SagaStateRepository;
import com.eduverse.payment.saga.EnrollmentPaymentSaga;
import com.eduverse.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class EnrollmentPaymentSagaTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SagaStateRepository sagaStateRepository;

    private EnrollmentPaymentSaga saga;
    private ServiceBusEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        StripeGateway stripeGateway = new MockStripeGateway();
        PaymentService paymentService = new PaymentService(paymentRepository, stripeGateway);
        eventPublisher = new ServiceBusEventPublisher("");
        saga = new EnrollmentPaymentSaga(paymentService, sagaStateRepository, eventPublisher);
    }

    @Test
    void handleStudentEnrolled_shouldCompleteSagaForValidPayment() {
        StudentEnrolledEvent event = new StudentEnrolledEvent(
                1L, 100L, 200L, "Java 101", new BigDecimal("50.00"));
        event.setCorrelationId("corr-123");

        saga.handleStudentEnrolled(event);

        Optional<SagaState> state = sagaStateRepository.findByEnrollmentId(1L);
        assertTrue(state.isPresent());
        assertEquals(SagaState.State.PAYMENT_COMPLETED, state.get().getState());
        assertEquals(100L, state.get().getStudentId());
        assertEquals(200L, state.get().getCourseId());
        assertNotNull(state.get().getPaymentId());
    }

    @Test
    void handleStudentEnrolled_shouldFailSagaForDeclinedPayment() {
        // Amount ending in .99 triggers mock failure
        StudentEnrolledEvent event = new StudentEnrolledEvent(
                2L, 100L, 200L, "Java 101", new BigDecimal("99.99"));

        saga.handleStudentEnrolled(event);

        Optional<SagaState> state = sagaStateRepository.findByEnrollmentId(2L);
        assertTrue(state.isPresent());
        assertEquals(SagaState.State.PAYMENT_FAILED, state.get().getState());
    }

    @Test
    void handleStudentEnrolled_shouldAutoCompleteZeroPriceEnrollment() {
        StudentEnrolledEvent event = new StudentEnrolledEvent(
                3L, 100L, 200L, "Free Course", BigDecimal.ZERO);

        saga.handleStudentEnrolled(event);

        Optional<SagaState> state = sagaStateRepository.findByEnrollmentId(3L);
        assertTrue(state.isPresent());
        assertEquals(SagaState.State.PAYMENT_COMPLETED, state.get().getState());
        assertNull(state.get().getPaymentId());
    }

    @Test
    void handleStudentEnrolled_shouldBeIdempotent() {
        StudentEnrolledEvent event = new StudentEnrolledEvent(
                4L, 100L, 200L, "Java 101", new BigDecimal("50.00"));

        saga.handleStudentEnrolled(event);
        saga.handleStudentEnrolled(event); // duplicate

        // Should still have exactly one saga state
        Optional<SagaState> state = sagaStateRepository.findByEnrollmentId(4L);
        assertTrue(state.isPresent());
        assertEquals(SagaState.State.PAYMENT_COMPLETED, state.get().getState());
    }

    @Test
    void handleRefund_shouldTransitionToRefunded() {
        StudentEnrolledEvent event = new StudentEnrolledEvent(
                5L, 100L, 200L, "Java 101", new BigDecimal("50.00"));
        saga.handleStudentEnrolled(event);

        saga.handleRefund(5L, "Student requested refund");

        Optional<SagaState> state = sagaStateRepository.findByEnrollmentId(5L);
        assertTrue(state.isPresent());
        assertEquals(SagaState.State.REFUNDED, state.get().getState());
    }

    @Test
    void handleRefund_shouldRejectIfNotCompleted() {
        StudentEnrolledEvent event = new StudentEnrolledEvent(
                6L, 100L, 200L, "Java 101", new BigDecimal("99.99")); // will fail
        saga.handleStudentEnrolled(event);

        assertThrows(RuntimeException.class, () ->
                saga.handleRefund(6L, "Refund attempt"));
    }

    @Test
    void handleStudentEnrolled_shouldHandleNullPrice() {
        StudentEnrolledEvent event = new StudentEnrolledEvent(
                7L, 100L, 200L, "Free Course", null);

        saga.handleStudentEnrolled(event);

        Optional<SagaState> state = sagaStateRepository.findByEnrollmentId(7L);
        assertTrue(state.isPresent());
        assertEquals(SagaState.State.PAYMENT_COMPLETED, state.get().getState());
    }
}
