package com.eduverse.payment;

import com.eduverse.payment.controller.PaymentController;
import com.eduverse.payment.model.Payment;
import com.eduverse.payment.saga.EnrollmentPaymentSaga;
import com.eduverse.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private EnrollmentPaymentSaga enrollmentPaymentSaga;

    private Payment createTestPayment(Long id, Long enrollmentId, Payment.Status status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setEnrollmentId(enrollmentId);
        payment.setStudentId(100L);
        payment.setCourseId(200L);
        payment.setAmount(new BigDecimal("49.99"));
        payment.setCurrency("USD");
        payment.setStatus(status);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        return payment;
    }

    @Test
    void getPaymentStatus_shouldReturnPayment() throws Exception {
        Payment payment = createTestPayment(1L, 10L, Payment.Status.COMPLETED);
        when(paymentService.getPaymentStatus(1L)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.enrollmentId").value(10))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getPaymentStatus_shouldReturn404ForMissing() throws Exception {
        when(paymentService.getPaymentStatus(999L))
                .thenThrow(new RuntimeException("Payment not found: 999"));

        mockMvc.perform(get("/api/payments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaymentsByEnrollment_shouldReturnList() throws Exception {
        Payment payment = createTestPayment(1L, 10L, Payment.Status.PENDING);
        when(paymentService.getPaymentsByEnrollment(10L)).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments/enrollment/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(10));
    }

    @Test
    void processPayment_shouldReturnProcessedPayment() throws Exception {
        Payment completed = createTestPayment(1L, 10L, Payment.Status.COMPLETED);
        completed.setStripePaymentId("pi_mock_123");
        when(paymentService.processPayment(1L)).thenReturn(completed);

        mockMvc.perform(post("/api/payments/1/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.stripePaymentId").value("pi_mock_123"));
    }

    @Test
    void refundPayment_shouldReturnRefundedPayment() throws Exception {
        Payment refunded = createTestPayment(1L, 10L, Payment.Status.REFUNDED);
        when(paymentService.refundPayment(1L)).thenReturn(refunded);

        mockMvc.perform(post("/api/payments/1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void createPayment_shouldReturnCreatedPayment() throws Exception {
        Payment payment = createTestPayment(1L, 10L, Payment.Status.PENDING);
        when(paymentService.createPendingPayment(eq(10L), eq(100L), eq(200L),
                any(BigDecimal.class), eq("USD"))).thenReturn(payment);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "enrollmentId": 10,
                                    "studentId": 100,
                                    "courseId": 200,
                                    "amount": 49.99,
                                    "currency": "USD"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").value(10));
    }

    @Test
    void getPendingPayments_shouldReturnPendingList() throws Exception {
        Payment payment = createTestPayment(1L, 10L, Payment.Status.PENDING);
        when(paymentService.getPendingPayments()).thenReturn(List.of(payment));

        mockMvc.perform(get("/api/payments/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void triggerSaga_shouldReturnSuccess() throws Exception {
        mockMvc.perform(post("/api/payments/saga/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "enrollmentId": 10,
                                    "studentId": 100,
                                    "courseId": 200,
                                    "courseTitle": "Java 101",
                                    "price": 49.99
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Saga triggered for enrollment 10"));
    }
}
