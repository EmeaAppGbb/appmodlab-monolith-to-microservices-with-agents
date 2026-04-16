package com.eduverse.payment.service;

import com.eduverse.payment.gateway.StripeGateway;
import com.eduverse.payment.gateway.StripeGateway.StripePaymentResult;
import com.eduverse.payment.gateway.StripeGateway.StripeRefundResult;
import com.eduverse.payment.model.Payment;
import com.eduverse.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment business logic with Stripe integration.
 * Key improvement over monolith: Stripe calls happen OUTSIDE @Transactional
 * to prevent data inconsistency when DB commit fails after successful charge.
 */
@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final StripeGateway stripeGateway;

    public PaymentService(PaymentRepository paymentRepository, StripeGateway stripeGateway) {
        this.paymentRepository = paymentRepository;
        this.stripeGateway = stripeGateway;
    }

    @Transactional
    public Payment createPendingPayment(Long enrollmentId, Long studentId, Long courseId,
                                        BigDecimal amount, String currency) {
        String idempotencyKey = "enrollment_" + enrollmentId;

        // Idempotency: return existing payment if already created for this enrollment
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            logger.info("Payment already exists for enrollment {}, returning existing payment {}",
                    enrollmentId, existing.get().getId());
            return existing.get();
        }

        Payment payment = new Payment();
        payment.setEnrollmentId(enrollmentId);
        payment.setStudentId(studentId);
        payment.setCourseId(courseId);
        payment.setAmount(amount);
        payment.setCurrency(currency != null ? currency : "USD");
        payment.setIdempotencyKey(idempotencyKey);
        payment.setStatus(Payment.Status.PENDING);

        payment = paymentRepository.save(payment);
        logger.info("Payment {} created for enrollment {}", payment.getId(), enrollmentId);
        return payment;
    }

    /**
     * Process payment via Stripe. Stripe call is NOT inside @Transactional
     * to avoid the monolith anti-pattern of external API calls inside DB transactions.
     */
    public Payment processPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.Status.PENDING) {
            logger.warn("Payment {} is not PENDING (current: {}), skipping", paymentId, payment.getStatus());
            return payment;
        }

        // Stripe call OUTSIDE transaction — fixes the monolith anti-pattern
        StripePaymentResult result = stripeGateway.createPaymentIntent(
                payment.getAmount(), payment.getCurrency(), payment.getIdempotencyKey());

        if (result.success()) {
            return markPaymentCompleted(paymentId, result.stripePaymentId());
        } else {
            return markPaymentFailed(paymentId, result.errorMessage());
        }
    }

    @Transactional
    public Payment markPaymentCompleted(Long paymentId, String stripePaymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.Status.PENDING) {
            logger.warn("Cannot complete payment {} — not in PENDING state (current: {})",
                    paymentId, payment.getStatus());
            return payment;
        }

        payment.setStripePaymentId(stripePaymentId);
        payment.setStatus(Payment.Status.COMPLETED);
        payment.setPaidDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markPaymentFailed(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.Status.PENDING) {
            logger.warn("Cannot fail payment {} — not in PENDING state (current: {})",
                    paymentId, payment.getStatus());
            return payment;
        }

        payment.setStatus(Payment.Status.FAILED);
        payment.setFailureReason(reason);
        return paymentRepository.save(payment);
    }

    /**
     * Refund a completed payment. Stripe refund call is OUTSIDE @Transactional.
     */
    public Payment refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.Status.COMPLETED) {
            throw new RuntimeException("Cannot refund payment " + paymentId +
                    " — not in COMPLETED state (current: " + payment.getStatus() + ")");
        }

        // Stripe refund OUTSIDE transaction
        StripeRefundResult result = stripeGateway.refundPayment(payment.getStripePaymentId());

        if (result.success()) {
            return markPaymentRefunded(paymentId);
        } else {
            throw new RuntimeException("Stripe refund failed: " + result.errorMessage());
        }
    }

    @Transactional
    public Payment markPaymentRefunded(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        payment.setStatus(Payment.Status.REFUNDED);
        payment.setRefundedDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public Payment getPaymentStatus(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    public List<Payment> getPaymentsByEnrollment(Long enrollmentId) {
        return paymentRepository.findByEnrollmentId(enrollmentId);
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(Payment.Status.PENDING);
    }
}
