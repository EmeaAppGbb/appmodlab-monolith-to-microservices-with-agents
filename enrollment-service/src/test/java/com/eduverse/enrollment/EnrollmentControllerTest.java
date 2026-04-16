package com.eduverse.enrollment;

import com.eduverse.enrollment.controller.EnrollmentController;
import com.eduverse.enrollment.model.Enrollment;
import com.eduverse.enrollment.service.EnrollmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnrollmentService enrollmentService;

    private Enrollment createTestEnrollment(Long id, Long studentId, Long courseId,
                                            Enrollment.Status status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(id);
        enrollment.setStudentId(studentId);
        enrollment.setCourseId(courseId);
        enrollment.setCourseTitle("Java 101");
        enrollment.setStatus(status);
        enrollment.setProgressPercent(BigDecimal.ZERO);
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setCreatedAt(LocalDateTime.now());
        enrollment.setUpdatedAt(LocalDateTime.now());
        return enrollment;
    }

    @Test
    void createEnrollment_shouldReturnCreated() throws Exception {
        Enrollment enrollment = createTestEnrollment(1L, 100L, 200L, Enrollment.Status.PENDING);
        when(enrollmentService.enrollStudent(eq(100L), eq(200L), eq("Java 101"),
                any(BigDecimal.class))).thenReturn(enrollment);

        mockMvc.perform(post("/api/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "studentId": 100,
                                    "courseId": 200,
                                    "courseTitle": "Java 101",
                                    "price": 49.99
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.studentId").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getEnrollment_shouldReturnEnrollment() throws Exception {
        Enrollment enrollment = createTestEnrollment(1L, 100L, 200L, Enrollment.Status.ACTIVE);
        when(enrollmentService.getEnrollment(1L)).thenReturn(Optional.of(enrollment));

        mockMvc.perform(get("/api/enrollments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.studentId").value(100))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void dropEnrollment_shouldReturnCancelled() throws Exception {
        Enrollment enrollment = createTestEnrollment(1L, 100L, 200L, Enrollment.Status.CANCELLED);
        when(enrollmentService.dropEnrollment(1L)).thenReturn(enrollment);

        mockMvc.perform(post("/api/enrollments/1/drop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void getStudentEnrollments_shouldReturnList() throws Exception {
        Enrollment enrollment = createTestEnrollment(1L, 100L, 200L, Enrollment.Status.ACTIVE);
        when(enrollmentService.getStudentEnrollments(100L)).thenReturn(List.of(enrollment));

        mockMvc.perform(get("/api/enrollments/student/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(100));
    }

    @Test
    void getStudentDashboard_shouldReturnDashboard() throws Exception {
        Enrollment active = createTestEnrollment(1L, 100L, 200L, Enrollment.Status.ACTIVE);
        Enrollment completed = createTestEnrollment(2L, 100L, 201L, Enrollment.Status.COMPLETED);
        when(enrollmentService.getStudentDashboard(100L)).thenReturn(List.of(active, completed));

        mockMvc.perform(get("/api/enrollments/student/100/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"));
    }
}
