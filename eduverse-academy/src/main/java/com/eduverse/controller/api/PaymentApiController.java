package com.eduverse.controller.api;

import com.eduverse.model.Payment;
import com.eduverse.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentApiController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentApiController.class);

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> createPayment(@RequestBody CreatePaymentRequest request) {
        try {
            Payment payment = paymentService.createPayment(
                    request.enrollmentId, request.amount, request.currency);
            return ResponseEntity.status(HttpStatus.CREATED).body(payment);
        } catch (Exception e) {
            logger.error("Error creating payment for enrollment {}", request.enrollmentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<?> processPayment(@PathVariable("id") Long id, @RequestBody ProcessPaymentRequest request) {
        try {
            Payment payment = paymentService.processPayment(id, request.stripeToken);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            logger.error("Error processing payment {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable("id") Long id) {
        try {
            Payment payment = paymentService.refundPayment(id);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            logger.error("Error refunding payment {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<?> getPaymentByEnrollmentId(@PathVariable("enrollmentId") Long enrollmentId) {
        try {
            List<Payment> payments = paymentService.getPaymentByEnrollmentId(enrollmentId);
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

    static class CreatePaymentRequest {
        public Long enrollmentId;
        public BigDecimal amount;
        public String currency;
    }

    static class ProcessPaymentRequest {
        public String stripeToken;
    }
}
