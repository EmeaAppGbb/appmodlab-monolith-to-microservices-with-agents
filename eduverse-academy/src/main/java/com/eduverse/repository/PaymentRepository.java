package com.eduverse.repository;

import com.eduverse.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByEnrollmentId(Long enrollmentId);

    List<Payment> findByStatus(Payment.Status status);

    Optional<Payment> findByStripePaymentId(String stripePaymentId);
}
