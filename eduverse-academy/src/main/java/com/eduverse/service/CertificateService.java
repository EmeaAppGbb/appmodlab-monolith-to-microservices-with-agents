package com.eduverse.service;

import com.eduverse.model.Certificate;
import com.eduverse.model.Enrollment;
import com.eduverse.repository.CertificateRepository;
import com.eduverse.repository.CourseRepository;
import com.eduverse.repository.EnrollmentRepository;
import com.eduverse.repository.UserRepository;
import com.eduverse.model.Course;
import com.eduverse.model.User;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Generates and manages completion certificates using iText PDF.
 *
 * MONOLITH ANTI-PATTERN: This service reaches into enrollment, course, and user
 * domains to gather data for PDF generation. Certificate generation, PDF rendering,
 * file storage concerns, and notification are all fused in a single service method.
 * It also directly validates enrollment completion status — duplicating logic that
 * belongs in the enrollment domain.
 */
@Service
public class CertificateService {

    private static final Logger logger = LoggerFactory.getLogger(CertificateService.class);

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // MONOLITH ANTI-PATTERN: Certificate service reaches into course and user repos
    // to pull data for PDF generation — cross-domain data access
    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    // Hardcoded certificate storage path — should be externalized
    private static final String CERTIFICATE_STORAGE_PATH = "/var/eduverse/certificates/";

    /**
     * Generates a PDF certificate for a completed enrollment.
     *
     * MONOLITH ANTI-PATTERN: Single method combines enrollment validation, certificate
     * number generation, PDF rendering, file storage, and database persistence —
     * multiple concerns that should be separate services in a microservices architecture.
     */
    @Transactional
    public Certificate generateCertificate(Long enrollmentId) {
        logger.info("Generating certificate for enrollment {}", enrollmentId);

        // Check for existing certificate — prevents duplicates
        Optional<Certificate> existingCert = certificateRepository.findByEnrollmentId(enrollmentId);
        if (existingCert.isPresent()) {
            logger.warn("Certificate already exists for enrollment {}", enrollmentId);
            return existingCert.get();
        }

        // MONOLITH ANTI-PATTERN: Validates enrollment status — duplicating business
        // logic that should be owned by the enrollment domain
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        if (enrollment.getStatus() != Enrollment.Status.COMPLETED) {
            throw new RuntimeException("Cannot generate certificate — enrollment " +
                    enrollmentId + " is not completed (status: " + enrollment.getStatus() + ")");
        }

        // MONOLITH ANTI-PATTERN: Fetching course and user data directly from their
        // repositories rather than through a service boundary or API
        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found: " + enrollment.getCourseId()));

        User student = userRepository.findById(enrollment.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found: " + enrollment.getStudentId()));

        // Generate unique certificate number
        String certificateNumber = generateCertificateNumber(course.getId(), enrollment.getStudentId());

        // Generate PDF
        String pdfUrl = generatePdf(student, course, certificateNumber, enrollment.getCompletionDate());

        // Persist certificate record
        Certificate certificate = new Certificate();
        certificate.setEnrollmentId(enrollmentId);
        certificate.setCertificateNumber(certificateNumber);
        certificate.setIssuedDate(LocalDateTime.now());
        certificate.setTemplateId("default-v1");
        certificate.setPdfUrl(pdfUrl);

        certificate = certificateRepository.save(certificate);
        logger.info("Certificate {} generated for enrollment {}", certificateNumber, enrollmentId);

        return certificate;
    }

    public Optional<Certificate> getCertificateByEnrollmentId(Long enrollmentId) {
        return certificateRepository.findByEnrollmentId(enrollmentId);
    }

    /**
     * Verifies a certificate by its unique number.
     * Pulls enrollment and course data to build a verification response —
     * tightly coupled to multiple domains.
     */
    @Transactional(readOnly = true)
    public Certificate verifyCertificate(String certificateNumber) {
        logger.info("Verifying certificate: {}", certificateNumber);

        Certificate certificate = certificateRepository.findByCertificateNumber(certificateNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Certificate not found: " + certificateNumber));

        // MONOLITH ANTI-PATTERN: Verification touches enrollment and course repos
        // to validate the certificate is still legitimate
        Enrollment enrollment = enrollmentRepository.findById(certificate.getEnrollmentId())
                .orElseThrow(() -> new RuntimeException(
                        "Enrollment not found for certificate: " + certificateNumber));

        if (enrollment.getStatus() != Enrollment.Status.COMPLETED) {
            logger.warn("Certificate {} references non-completed enrollment {}", 
                    certificateNumber, enrollment.getId());
        }

        return certificate;
    }

    private String generateCertificateNumber(Long courseId, Long studentId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "EDU-" + timestamp + "-" + courseId + "-" + uniquePart;
    }

    /**
     * Generates a PDF certificate using iText — infrastructure concern mixed
     * directly into the service layer without separation.
     */
    private String generatePdf(User student, Course course, String certificateNumber,
                               LocalDateTime completionDate) {
        logger.debug("Generating PDF for certificate {}", certificateNumber);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 36);
            Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 16);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph header = new Paragraph("EduVerse Academy", titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            header.setSpacingAfter(20);
            document.add(header);

            Paragraph certTitle = new Paragraph("Certificate of Completion", bodyFont);
            certTitle.setAlignment(Element.ALIGN_CENTER);
            certTitle.setSpacingAfter(30);
            document.add(certTitle);

            Paragraph awarded = new Paragraph("This certificate is awarded to", bodyFont);
            awarded.setAlignment(Element.ALIGN_CENTER);
            awarded.setSpacingAfter(15);
            document.add(awarded);

            Paragraph studentName = new Paragraph(student.getFullName(), nameFont);
            studentName.setAlignment(Element.ALIGN_CENTER);
            studentName.setSpacingAfter(20);
            document.add(studentName);

            Paragraph courseInfo = new Paragraph(
                    "for successfully completing the course \"" + course.getTitle() + "\"", bodyFont);
            courseInfo.setAlignment(Element.ALIGN_CENTER);
            courseInfo.setSpacingAfter(30);
            document.add(courseInfo);

            String dateStr = completionDate != null
                    ? completionDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
            Paragraph datePara = new Paragraph("Date: " + dateStr, bodyFont);
            datePara.setAlignment(Element.ALIGN_CENTER);
            datePara.setSpacingAfter(10);
            document.add(datePara);

            Paragraph certNum = new Paragraph("Certificate No: " + certificateNumber, smallFont);
            certNum.setAlignment(Element.ALIGN_CENTER);
            document.add(certNum);

            document.close();

            // In a real monolith, this would write to the local filesystem
            String pdfPath = CERTIFICATE_STORAGE_PATH + certificateNumber + ".pdf";
            logger.info("PDF generated, size: {} bytes, path: {}", baos.size(), pdfPath);

            return pdfPath;

        } catch (Exception e) {
            logger.error("PDF generation failed for certificate {}: {}", certificateNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to generate certificate PDF: " + e.getMessage(), e);
        }
    }
}
