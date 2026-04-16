package com.eduverse.payment.repository;

import com.eduverse.payment.model.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaStateRepository extends JpaRepository<SagaState, Long> {

    Optional<SagaState> findByEnrollmentId(Long enrollmentId);

    Optional<SagaState> findByCorrelationId(String correlationId);

    List<SagaState> findByState(SagaState.State state);
}
