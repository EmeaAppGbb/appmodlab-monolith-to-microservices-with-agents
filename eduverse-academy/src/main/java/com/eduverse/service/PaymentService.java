package com.eduverse.service;

import com.eduverse.model.Enrollment;
import com.eduverse.model.Payment;
import com.eduverse.repository.EnrollmentRepository;
import com.eduverse.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.model.Refund;
import com.stripe.param.ChargeCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles payment processing with Stripe integration and enrollment status updates.
 *
 * MONOLITH ANTI-PATTERN: Payment processing is synchronously coupled to enrollment
 * status management. The service directly mutates Enrollment entities on payment
 * completion — a single transaction spans financial and academic domains.
 * Stripe API calls happen inside database transactions, risking inconsistency
 * if the commit fails after a successful charge.
 */
@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    // MONOLITH ANTI-PATTERN: Payment service directly depends on enrollment repository
    // to update enrollment status after payment — cross-domain write
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // Hardcoded Stripe API key — should be externalized configuration
    private static final String STRIPE_API_KEY = "sk_test_eduverse_placeholder_key";

    /**
     * Creates a pending payment record tied to an enrollment.
     */
    @Transactional
    public Payment createPayment(Long enrollmentId, BigDecimal amount, String currency) {
        logger.info("Creating payment: enrollment={}, amount={} {}", enrollmentId, amount, currency);

        // Verify enrollment exists — cross-domain validation
        enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        Payment payment = new Payment();
        payment.setEnrollmentId(enrollmentId);
        payment.setAmount(amount);
        payment.setCurrency(currency != null ? currency : "USD");
        payment.setStatus(Payment.Status.PENDING);

        payment = paymentRepository.save(payment);
        logger.info("Payment {} created for enrollment {}", payment.getId(), enrollmentId);

        return payment;
    }

    /**
     * Processes payment via Stripe and updates enrollment status in one transaction.
     *
     * MONOLITH ANTI-PATTERN: External API call (Stripe) inside a database transaction.
     * If Stripe succeeds but the DB commit fails, the customer is charged but the
     * enrollment is never activated — data inconsistency across systems.
     */
    @Transactional
    public Payment processPayment(Long paymentId, String stripeToken) {
        logger.info("Processing payment {} with Stripe", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new RuntimeException("Payment " + paymentId + " is not in PENDING state");
        }

        try {
            // Stripe API call inside a transaction — anti-pattern
            Stripe.apiKey = STRIPE_API_KEY;

            ChargeCreateParams params = ChargeCreateParams.builder()
                    .setAmount(payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency(payment.getCurrency().toLowerCase())
                    .setSource(stripeToken)
                    .setDescription("EduVerse Academy - Enrollment #" + payment.getEnrollmentId())
                    .build();

            Charge charge = Charge.create(params);

            payment.setStripePaymentId(charge.getId());
            payment.setStatus(Payment.Status.COMPLETED);
            payment.setPaidDate(LocalDateTime.now());
            paymentRepository.save(payment);

            // MONOLITH ANTI-PATTERN: Directly update enrollment status from payment service
            Enrollment enrollment = enrollmentRepository.findById(payment.getEnrollmentId())
                    .orElseThrow(() -> new RuntimeException(
                            "Enrollment not found: " + payment.getEnrollmentId()));
            enrollment.setStatus(Enrollment.Status.ACTIVE);
            enrollmentRepository.save(enrollment);

            logger.info("Payment {} completed, Stripe charge: {}", paymentId, charge.getId());

        } catch (Exception e) {
            logger.error("Payment processing failed for payment {}: {}", paymentId, e.getMessage(), e);
            payment.setStatus(Payment.Status.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }

        return payment;
    }

    /**
     * Processes a refund via Stripe and drops enrollment status.
     * Another cross-domain transaction — financial refund + academic status change.
     */
    @Transactional
    public Payment refundPayment(Long paymentId) {
        logger.info("Processing refund for payment {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.Status.COMPLETED) {
            throw new RuntimeException("Cannot refund payment " + paymentId + " — not in COMPLETED state");
        }

        try {
            Stripe.apiKey = STRIPE_API_KEY;

            RefundCreateParams params = RefundCreateParams.builder()
                    .setCharge(payment.getStripePaymentId())
                    .build();

            Refund.create(params);

            payment.setStatus(Payment.Status.REFUNDED);
            payment.setRefundedDate(LocalDateTime.now());
            paymentRepository.save(payment);

            // MONOLITH ANTI-PATTERN: Refund changes enrollment status to DROPPED
            Enrollment enrollment = enrollmentRepository.findById(payment.getEnrollmentId())
                    .orElseThrow(() -> new RuntimeException(
                            "Enrollment not found: " + payment.getEnrollmentId()));
            enrollment.setStatus(Enrollment.Status.DROPPED);
            enrollmentRepository.save(enrollment);

            logger.info("Payment {} refunded, enrollment {} dropped", paymentId, enrollment.getId());

        } catch (Exception e) {
            logger.error("Refund failed for payment {}: {}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Refund processing failed: " + e.getMessage(), e);
        }

        return payment;
    }

    public List<Payment> getPaymentByEnrollmentId(Long enrollmentId) {
        return paymentRepository.findByEnrollmentId(enrollmentId);
    }

    public List<Payment> getPendingPayments() {
        return paymentRepository.findByStatus(Payment.Status.PENDING);
    }
}
