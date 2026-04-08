package com.eduverse.repository;

import com.eduverse.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByEnrollmentId(Long enrollmentId);

    Optional<Certificate> findByCertificateNumber(String certificateNumber);
}
