package com.eduverse.certificate.controller;

import com.eduverse.certificate.model.Certificate;
import com.eduverse.certificate.service.CertificateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Certificate> getCertificate(@PathVariable Long id) {
        return certificateService.getCertificate(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable Long id) {
        try {
            String url = certificateService.getDownloadUrl(id);
            return ResponseEntity.ok(Map.of("downloadUrl", url));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/verify/{certificateNumber}")
    public ResponseEntity<Certificate> verifyCertificate(@PathVariable String certificateNumber) {
        return certificateService.verifyCertificate(certificateNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/enrollment/{enrollmentId}")
    public ResponseEntity<Certificate> getCertificateByEnrollment(@PathVariable Long enrollmentId) {
        return certificateService.getCertificateByEnrollment(enrollmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Certificate>> getCertificatesByStudent(@PathVariable Long studentId) {
        List<Certificate> certificates = certificateService.getCertificatesByStudent(studentId);
        return ResponseEntity.ok(certificates);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateCertificate(@RequestBody GenerateCertificateRequest request) {
        try {
            Certificate certificate = certificateService.generateCertificate(
                    request.enrollmentId, request.studentId, request.courseId,
                    request.studentName, request.courseTitle);
            return ResponseEntity.ok(certificate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "certificate-service"));
    }

    static class GenerateCertificateRequest {
        public Long enrollmentId;
        public Long studentId;
        public Long courseId;
        public String studentName;
        public String courseTitle;
    }
}
