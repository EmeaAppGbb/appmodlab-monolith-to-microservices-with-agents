package com.eduverse.controller;

import com.eduverse.model.Certificate;
import com.eduverse.service.CertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Optional;

@Controller
public class CertificateController {

    private static final Logger logger = LoggerFactory.getLogger(CertificateController.class);

    @Autowired
    private CertificateService certificateService;

    @GetMapping("/certificate/view/{enrollmentId}")
    public String viewCertificate(@PathVariable("enrollmentId") Long enrollmentId, Model model) {
        logger.info("Viewing certificate for enrollment id={}", enrollmentId);
        try {
            Optional<Certificate> certificate = certificateService.getCertificateByEnrollmentId(enrollmentId);
            if (certificate.isPresent()) {
                model.addAttribute("certificate", certificate.get());
            } else {
                // Tight coupling: CertificateService calls EnrollmentRepository,
                // CourseRepository, and UserRepository to generate a new certificate
                Certificate generated = certificateService.generateCertificate(enrollmentId);
                model.addAttribute("certificate", generated);
            }
        } catch (Exception e) {
            logger.error("Error viewing certificate for enrollment id={}", enrollmentId, e);
            model.addAttribute("error", "Unable to load certificate: " + e.getMessage());
            return "error";
        }
        return "certificateView";
    }

    @GetMapping("/certificate/download/{id}")
    public String downloadCertificate(@PathVariable("id") Long id,
                                      HttpServletResponse response, Model model) {
        logger.info("Downloading certificate PDF for id={}", id);
        try {
            Optional<Certificate> certificate = certificateService.getCertificateByEnrollmentId(id);
            if (!certificate.isPresent()) {
                model.addAttribute("error", "Certificate not found.");
                return "error";
            }

            Certificate cert = certificate.get();
            String pdfPath = cert.getPdfUrl();

            // Tight coupling: serving files directly from local filesystem
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                model.addAttribute("error", "Certificate PDF file not found.");
                return "error";
            }

            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename=certificate_" + cert.getCertificateNumber() + ".pdf");
            response.setContentLength((int) pdfFile.length());

            try (FileInputStream fis = new FileInputStream(pdfFile);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
            return null; // Response already written
        } catch (Exception e) {
            logger.error("Error downloading certificate id={}", id, e);
            model.addAttribute("error", "Unable to download certificate.");
            return "error";
        }
    }
}
