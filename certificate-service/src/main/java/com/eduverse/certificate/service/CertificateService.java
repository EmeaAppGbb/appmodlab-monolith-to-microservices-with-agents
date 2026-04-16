package com.eduverse.certificate.service;

import com.eduverse.certificate.model.Certificate;
import com.eduverse.certificate.repository.CertificateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);

    private final CertificateRepository certificateRepository;
    private final Random random = new Random();

    public CertificateService(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    /**
     * Generates a certificate for a completed enrollment.
     * Idempotent: if a certificate already exists for the enrollmentId, returns the existing one.
     */
    @Transactional
    public Certificate generateCertificate(Long enrollmentId, Long studentId, Long courseId,
                                           String studentName, String courseTitle) {
        logger.info("Generating certificate for enrollment {}, student {}", enrollmentId, studentId);

        // Idempotency: return existing certificate if already generated
        Optional<Certificate> existing = certificateRepository.findByEnrollmentId(enrollmentId);
        if (existing.isPresent()) {
            logger.info("Certificate already exists for enrollment {}, returning existing", enrollmentId);
            return existing.get();
        }

        Certificate certificate = new Certificate();
        certificate.setEnrollmentId(enrollmentId);
        certificate.setStudentId(studentId);
        certificate.setCourseId(courseId);
        certificate.setStudentName(studentName);
        certificate.setCourseTitle(courseTitle);
        certificate.setStatus(Certificate.Status.PENDING);
        certificate.setCertificateNumber(generateCertificateNumber());
        certificate.setCompletionDate(LocalDateTime.now());

        certificate = certificateRepository.save(certificate);
        logger.info("Certificate {} created with PENDING status", certificate.getId());

        // Simulate PDF generation
        certificate.setStatus(Certificate.Status.GENERATING);
        certificate = certificateRepository.save(certificate);

        String pdfUrl = String.format("https://eduverse.blob.core.windows.net/certificates/%s.pdf",
                certificate.getCertificateNumber());
        certificate.setPdfUrl(pdfUrl);
        certificate.setStatus(Certificate.Status.ISSUED);
        certificate.setIssuedAt(LocalDateTime.now());
        certificate = certificateRepository.save(certificate);

        logger.info("Certificate {} issued for enrollment {}", certificate.getCertificateNumber(), enrollmentId);
        return certificate;
    }

    @Transactional(readOnly = true)
    public Optional<Certificate> getCertificate(Long id) {
        return certificateRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Certificate> getCertificateByEnrollment(Long enrollmentId) {
        return certificateRepository.findByEnrollmentId(enrollmentId);
    }

    @Transactional(readOnly = true)
    public List<Certificate> getCertificatesByStudent(Long studentId) {
        return certificateRepository.findByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public Optional<Certificate> verifyCertificate(String certificateNumber) {
        return certificateRepository.findByCertificateNumber(certificateNumber);
    }

    @Transactional(readOnly = true)
    public String getDownloadUrl(Long id) {
        return certificateRepository.findById(id)
                .map(Certificate::getPdfUrl)
                .orElseThrow(() -> new RuntimeException("Certificate not found: " + id));
    }

    private String generateCertificateNumber() {
        int year = Year.now().getValue();
        int randomDigits = 100000 + random.nextInt(900000);
        return String.format("CERT-%d-%d", year, randomDigits);
    }
}
