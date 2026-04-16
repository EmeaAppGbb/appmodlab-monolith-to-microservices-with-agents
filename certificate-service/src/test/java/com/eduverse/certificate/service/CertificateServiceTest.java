package com.eduverse.certificate.service;

import com.eduverse.certificate.model.Certificate;
import com.eduverse.certificate.repository.CertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class CertificateServiceTest {

    @Autowired
    private CertificateRepository certificateRepository;

    private CertificateService certificateService;

    @BeforeEach
    void setUp() {
        certificateService = new CertificateService(certificateRepository);
    }

    @Test
    void generateCertificate_shouldCreateCertAndGenerateNumber() {
        Certificate certificate = certificateService.generateCertificate(
                1L, 100L, 200L, "Alice Johnson", "Java 101");

        assertNotNull(certificate.getId());
        assertEquals(1L, certificate.getEnrollmentId());
        assertEquals(100L, certificate.getStudentId());
        assertEquals(200L, certificate.getCourseId());
        assertEquals("Alice Johnson", certificate.getStudentName());
        assertEquals("Java 101", certificate.getCourseTitle());
        assertEquals(Certificate.Status.ISSUED, certificate.getStatus());
        assertNotNull(certificate.getCertificateNumber());
        assertTrue(certificate.getCertificateNumber().startsWith("CERT-"));
        assertNotNull(certificate.getPdfUrl());
        assertNotNull(certificate.getIssuedAt());
        assertNotNull(certificate.getCompletionDate());
    }

    @Test
    void generateCertificate_shouldBeIdempotent() {
        Certificate first = certificateService.generateCertificate(
                1L, 100L, 200L, "Alice Johnson", "Java 101");
        Certificate second = certificateService.generateCertificate(
                1L, 100L, 200L, "Alice Johnson", "Java 101");

        assertEquals(first.getId(), second.getId());
        assertEquals(first.getCertificateNumber(), second.getCertificateNumber());
    }

    @Test
    void getCertificateByEnrollment_shouldReturnCertificate() {
        certificateService.generateCertificate(1L, 100L, 200L, "Alice Johnson", "Java 101");

        Optional<Certificate> found = certificateService.getCertificateByEnrollment(1L);

        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getEnrollmentId());
    }

    @Test
    void getCertificatesByStudent_shouldReturnList() {
        certificateService.generateCertificate(1L, 100L, 200L, "Alice Johnson", "Java 101");
        certificateService.generateCertificate(2L, 100L, 201L, "Alice Johnson", "Spring Boot");

        List<Certificate> certificates = certificateService.getCertificatesByStudent(100L);

        assertEquals(2, certificates.size());
    }

    @Test
    void verifyCertificate_shouldFindByNumber() {
        Certificate created = certificateService.generateCertificate(
                1L, 100L, 200L, "Alice Johnson", "Java 101");

        Optional<Certificate> found = certificateService.verifyCertificate(created.getCertificateNumber());

        assertTrue(found.isPresent());
        assertEquals(created.getCertificateNumber(), found.get().getCertificateNumber());
        assertEquals(Certificate.Status.ISSUED, found.get().getStatus());
    }
}
