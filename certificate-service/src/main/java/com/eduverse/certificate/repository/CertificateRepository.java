package com.eduverse.certificate.repository;

import com.eduverse.certificate.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByEnrollmentId(Long enrollmentId);

    List<Certificate> findByStudentId(Long studentId);

    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    boolean existsByEnrollmentId(Long enrollmentId);
}
