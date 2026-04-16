package com.eduverse.payment.controller;

import com.eduverse.payment.model.Payment;
import com.eduverse.payment.saga.EnrollmentPaymentSaga;
import com.eduverse.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Payment REST API. Preserves backward compatibility with the monolith API
 * contract while adding saga-aware endpoints.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final EnrollmentPaymentSaga enrollmentPaymentSaga;

    public PaymentController(PaymentService paymentService,
                             EnrollmentPaymentSaga enrollmentPaymentSaga) {
        this.paymentService = paymentService;
        this.enrollmentPaymentSaga = enrollmentPaymentSaga;
    }

    // --- Backward-compatible endpoints (matching monolith API contract) ---

    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody CreatePaymentRequest request) {
        try {
            Payment payment = paymentService.createPendingPayment(
                    request.enrollmentId, request.studentId, request.courseId,
                    request.amount, request.currency);
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (Exception e) {
            logger.error("Error creating payment for enrollment {}", request.enrollmentId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processPayment(@PathVariable("id") Long id) {
        try {
            Payment payment = paymentService.processPayment(id);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            logger.error("Error processing payment {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable("id") Long id) {
        try {
            Payment payment = paymentService.refundPayment(id);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            logger.error("Error refunding payment {}", id, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<?> getPaymentsByEnrollment(
            @PathVariable("enrollmentId") Long enrollmentId) {
        try {
            List<Payment> payments = paymentService.getPaymentsByEnrollment(enrollmentId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error fetching payments for enrollment {}", enrollmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingPayments() {
        try {
            List<Payment> payments = paymentService.getPendingPayments();
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("Error fetching pending payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable("id") Long id) {
        try {
            Payment payment = paymentService.getPaymentStatus(id);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            logger.error("Error fetching payment {}", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // --- Saga-aware endpoints (for local dev / testing without Service Bus) ---

    @PostMapping("/saga/enroll")
    public ResponseEntity<?> triggerEnrollmentSaga(@RequestBody TriggerSagaRequest request) {
        try {
            var event = new com.eduverse.events.StudentEnrolledEvent(
                    request.enrollmentId, request.studentId, request.courseId,
                    request.courseTitle, request.price);
            if (request.correlationId != null) {
                event.setCorrelationId(request.correlationId);
            }
            enrollmentPaymentSaga.handleStudentEnrolled(event);
            return ResponseEntity.ok(Map.of("message",
                    "Saga triggered for enrollment " + request.enrollmentId));
        } catch (Exception e) {
            logger.error("Error triggering saga for enrollment {}", request.enrollmentId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/saga/refund/{enrollmentId}")
    public ResponseEntity<?> triggerRefund(
            @PathVariable("enrollmentId") Long enrollmentId,
            @RequestBody(required = false) RefundRequest request) {
        try {
            String reason = request != null && request.reason != null
                    ? request.reason : "Admin-initiated refund";
            enrollmentPaymentSaga.handleRefund(enrollmentId, reason);
            return ResponseEntity.ok(Map.of("message",
                    "Refund triggered for enrollment " + enrollmentId));
        } catch (Exception e) {
            logger.error("Error triggering refund for enrollment {}", enrollmentId, e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- Request DTOs ---

    static class CreatePaymentRequest {
        public Long enrollmentId;
        public Long studentId;
        public Long courseId;
        public BigDecimal amount;
        public String currency;
    }

    static class TriggerSagaRequest {
        public Long enrollmentId;
        public Long studentId;
        public Long courseId;
        public String courseTitle;
        public BigDecimal price;
        public String correlationId;
    }

    static class RefundRequest {
        public String reason;
    }
}
