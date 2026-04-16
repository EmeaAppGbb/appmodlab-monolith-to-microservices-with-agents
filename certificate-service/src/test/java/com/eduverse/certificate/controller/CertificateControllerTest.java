package com.eduverse.certificate.controller;

import com.eduverse.certificate.model.Certificate;
import com.eduverse.certificate.service.CertificateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CertificateController.class)
class CertificateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CertificateService certificateService;

    private Certificate createTestCertificate(Long id, Long enrollmentId) {
        Certificate cert = new Certificate();
        cert.setId(id);
        cert.setEnrollmentId(enrollmentId);
        cert.setStudentId(100L);
        cert.setCourseId(200L);
        cert.setCertificateNumber("CERT-2024-100001");
        cert.setStudentName("Alice Johnson");
        cert.setCourseTitle("Java 101");
        cert.setStatus(Certificate.Status.ISSUED);
        cert.setPdfUrl("https://eduverse.blob.core.windows.net/certificates/CERT-2024-100001.pdf");
        cert.setIssuedAt(LocalDateTime.now());
        cert.setCreatedAt(LocalDateTime.now());
        cert.setUpdatedAt(LocalDateTime.now());
        return cert;
    }

    @Test
    void getCertificate_shouldReturnCertificate() throws Exception {
        Certificate cert = createTestCertificate(1L, 10L);
        when(certificateService.getCertificate(1L)).thenReturn(Optional.of(cert));

        mockMvc.perform(get("/api/certificates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.certificateNumber").value("CERT-2024-100001"))
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    void getCertificate_shouldReturn404ForMissing() throws Exception {
        when(certificateService.getCertificate(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/certificates/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void verifyCertificate_shouldReturnCertificate() throws Exception {
        Certificate cert = createTestCertificate(1L, 10L);
        when(certificateService.verifyCertificate("CERT-2024-100001")).thenReturn(Optional.of(cert));

        mockMvc.perform(get("/api/certificates/verify/CERT-2024-100001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateNumber").value("CERT-2024-100001"))
                .andExpect(jsonPath("$.studentName").value("Alice Johnson"));
    }

    @Test
    void verifyCertificate_shouldReturn404ForInvalid() throws Exception {
        when(certificateService.verifyCertificate("CERT-INVALID")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/certificates/verify/CERT-INVALID"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCertificateByEnrollment_shouldReturnCertificate() throws Exception {
        Certificate cert = createTestCertificate(1L, 10L);
        when(certificateService.getCertificateByEnrollment(10L)).thenReturn(Optional.of(cert));

        mockMvc.perform(get("/api/certificates/enrollment/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value(10));
    }

    @Test
    void getCertificatesByStudent_shouldReturnList() throws Exception {
        Certificate cert = createTestCertificate(1L, 10L);
        when(certificateService.getCertificatesByStudent(100L)).thenReturn(List.of(cert));

        mockMvc.perform(get("/api/certificates/student/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(100));
    }

    @Test
    void generateCertificate_shouldReturnCertificate() throws Exception {
        Certificate cert = createTestCertificate(1L, 10L);
        when(certificateService.generateCertificate(eq(10L), eq(100L), eq(200L),
                eq("Alice Johnson"), eq("Java 101"))).thenReturn(cert);

        mockMvc.perform(post("/api/certificates/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "enrollmentId": 10,
                                    "studentId": 100,
                                    "courseId": 200,
                                    "studentName": "Alice Johnson",
                                    "courseTitle": "Java 101"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateNumber").value("CERT-2024-100001"));
    }

    @Test
    void getDownloadUrl_shouldReturnUrl() throws Exception {
        when(certificateService.getDownloadUrl(1L))
                .thenReturn("https://eduverse.blob.core.windows.net/certificates/CERT-2024-100001.pdf");

        mockMvc.perform(get("/api/certificates/1/download"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").value(
                        "https://eduverse.blob.core.windows.net/certificates/CERT-2024-100001.pdf"));
    }

    @Test
    void getDownloadUrl_shouldReturn404ForMissing() throws Exception {
        when(certificateService.getDownloadUrl(999L))
                .thenThrow(new RuntimeException("Certificate not found: 999"));

        mockMvc.perform(get("/api/certificates/999/download"))
                .andExpect(status().isNotFound());
    }

    @Test
    void health_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/certificates/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("certificate-service"));
    }
}
