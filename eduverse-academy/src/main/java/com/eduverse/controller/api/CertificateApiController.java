package com.eduverse.controller.api;

import com.eduverse.model.Certificate;
import com.eduverse.service.CertificateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/certificates")
public class CertificateApiController {

    private static final Logger logger = LoggerFactory.getLogger(CertificateApiController.class);

    @Autowired
    private CertificateService certificateService;

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<?> getCertificateByEnrollmentId(@PathVariable("enrollmentId") Long enrollmentId) {
        try {
            Optional<Certificate> certificate = certificateService.getCertificateByEnrollmentId(enrollmentId);
            if (certificate.isPresent()) {
                return ResponseEntity.ok(certificate.get());
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Certificate not found for enrollment " + enrollmentId));
        } catch (Exception e) {
            logger.error("Error fetching certificate for enrollment {}", enrollmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/enrollment/{enrollmentId}/generate")
    public ResponseEntity<?> generateCertificate(@PathVariable("enrollmentId") Long enrollmentId) {
        try {
            Certificate certificate = certificateService.generateCertificate(enrollmentId);
            return ResponseEntity.status(HttpStatus.CREATED).body(certificate);
        } catch (Exception e) {
            logger.error("Error generating certificate for enrollment {}", enrollmentId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
