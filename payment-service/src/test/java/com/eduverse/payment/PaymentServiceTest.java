package com.eduverse.payment;

import com.eduverse.payment.gateway.MockStripeGateway;
import com.eduverse.payment.gateway.StripeGateway;
import com.eduverse.payment.model.Payment;
import com.eduverse.payment.repository.PaymentRepository;
import com.eduverse.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;
    private StripeGateway stripeGateway;

    @BeforeEach
    void setUp() {
        stripeGateway = new MockStripeGateway();
        paymentService = new PaymentService(paymentRepository, stripeGateway);
    }

    @Test
    void createPendingPayment_shouldCreatePayment() {
        Payment payment = paymentService.createPendingPayment(
                1L, 100L, 200L, new BigDecimal("49.99"), "USD");

        assertNotNull(payment.getId());
        assertEquals(1L, payment.getEnrollmentId());
        assertEquals(100L, payment.getStudentId());
        assertEquals(200L, payment.getCourseId());
        assertEquals(new BigDecimal("49.99"), payment.getAmount());
        assertEquals("USD", payment.getCurrency());
        assertEquals(Payment.Status.PENDING, payment.getStatus());
        assertEquals("enrollment_1", payment.getIdempotencyKey());
    }

    @Test
    void createPendingPayment_shouldBeIdempotent() {
        Payment first = paymentService.createPendingPayment(
                1L, 100L, 200L, new BigDecimal("49.99"), "USD");
        Payment second = paymentService.createPendingPayment(
                1L, 100L, 200L, new BigDecimal("49.99"), "USD");

        assertEquals(first.getId(), second.getId());
    }

    @Test
    void processPayment_shouldCompleteSuccessfully() {
        Payment pending = paymentService.createPendingPayment(
                1L, 100L, 200L, new BigDecimal("50.00"), "USD");

        Payment processed = paymentService.processPayment(pending.getId());

        assertEquals(Payment.Status.COMPLETED, processed.getStatus());
        assertNotNull(processed.getStripePaymentId());
        assertTrue(processed.getStripePaymentId().startsWith("pi_mock_"));
        assertNotNull(processed.getPaidDate());
    }

    @Test
    void processPayment_shouldFailForDeclinedCard() {
        // Amounts ending in .99 trigger mock failure
        Payment pending = paymentService.createPendingPayment(
                2L, 100L, 200L, new BigDecimal("99.99"), "USD");

        Payment processed = paymentService.processPayment(pending.getId());

        assertEquals(Payment.Status.FAILED, processed.getStatus());
        assertNull(processed.getStripePaymentId());
        assertNotNull(processed.getFailureReason());
    }

    @Test
    void processPayment_shouldSkipNonPendingPayment() {
        Payment pending = paymentService.createPendingPayment(
                3L, 100L, 200L, new BigDecimal("50.00"), "USD");
        paymentService.processPayment(pending.getId());

        // Try processing again — should return existing completed state
        Payment reprocessed = paymentService.processPayment(pending.getId());
        assertEquals(Payment.Status.COMPLETED, reprocessed.getStatus());
    }

    @Test
    void refundPayment_shouldRefundCompletedPayment() {
        Payment pending = paymentService.createPendingPayment(
                4L, 100L, 200L, new BigDecimal("50.00"), "USD");
        paymentService.processPayment(pending.getId());

        Payment refunded = paymentService.refundPayment(pending.getId());

        assertEquals(Payment.Status.REFUNDED, refunded.getStatus());
        assertNotNull(refunded.getRefundedDate());
    }

    @Test
    void refundPayment_shouldRejectNonCompletedPayment() {
        Payment pending = paymentService.createPendingPayment(
                5L, 100L, 200L, new BigDecimal("50.00"), "USD");

        assertThrows(RuntimeException.class, () ->
                paymentService.refundPayment(pending.getId()));
    }

    @Test
    void getPaymentsByEnrollment_shouldReturnPayments() {
        paymentService.createPendingPayment(6L, 100L, 200L, new BigDecimal("50.00"), "USD");

        List<Payment> payments = paymentService.getPaymentsByEnrollment(6L);

        assertEquals(1, payments.size());
        assertEquals(6L, payments.get(0).getEnrollmentId());
    }

    @Test
    void getPendingPayments_shouldReturnOnlyPending() {
        paymentService.createPendingPayment(7L, 100L, 200L, new BigDecimal("50.00"), "USD");
        Payment toComplete = paymentService.createPendingPayment(
                8L, 101L, 201L, new BigDecimal("75.00"), "USD");
        paymentService.processPayment(toComplete.getId());

        List<Payment> pending = paymentService.getPendingPayments();

        assertEquals(1, pending.size());
        assertEquals(7L, pending.get(0).getEnrollmentId());
    }
}
