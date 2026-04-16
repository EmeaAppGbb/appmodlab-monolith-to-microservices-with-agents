package com.eduverse.progress;

import com.eduverse.progress.controller.ProgressController;
import com.eduverse.progress.model.Progress;
import com.eduverse.progress.service.ProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProgressController.class)
class ProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProgressService progressService;

    @Autowired
    private ObjectMapper objectMapper;

    private Progress createTestProgress(Long id, Long enrollmentId, Long lessonId, boolean completed) {
        Progress p = new Progress();
        p.setId(id);
        p.setEnrollmentId(enrollmentId);
        p.setStudentId(100L);
        p.setCourseId(200L);
        p.setLessonId(lessonId);
        p.setCompleted(completed);
        p.setProgressPercent(new BigDecimal("50.00"));
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        if (completed) {
            p.setCompletedAt(LocalDateTime.now());
        }
        return p;
    }

    @Test
    void getProgressByEnrollment_shouldReturnList() throws Exception {
        Progress p1 = createTestProgress(1L, 10L, 1L, true);
        Progress p2 = createTestProgress(2L, 10L, 2L, false);
        when(progressService.getProgressByEnrollment(10L)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/progress/enrollment/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(10))
                .andExpect(jsonPath("$[0].completed").value(true))
                .andExpect(jsonPath("$[1].completed").value(false));
    }

    @Test
    void markLessonCompleted_shouldReturnUpdatedProgress() throws Exception {
        Progress p = createTestProgress(1L, 10L, 5L, true);
        p.setProgressPercent(new BigDecimal("25.00"));
        when(progressService.markLessonCompleted(10L, 5L)).thenReturn(p);

        mockMvc.perform(post("/api/progress/lesson/5/complete")
                        .param("enrollmentId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.progressPercent").value(25.00));
    }

    @Test
    void initializeProgress_shouldReturnCreatedRecords() throws Exception {
        Progress p1 = createTestProgress(1L, 10L, 1L, false);
        Progress p2 = createTestProgress(2L, 10L, 2L, false);
        when(progressService.initializeProgress(eq(10L), eq(100L), eq(200L), anyList()))
                .thenReturn(List.of(p1, p2));

        String body = """
                {
                    "enrollmentId": 10,
                    "studentId": 100,
                    "courseId": 200,
                    "lessonIds": [1, 2]
                }
                """;

        mockMvc.perform(post("/api/progress/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(10))
                .andExpect(jsonPath("$[1].lessonId").value(2));
    }

    @Test
    void getProgressByStudent_shouldReturnList() throws Exception {
        Progress p = createTestProgress(1L, 10L, 1L, true);
        when(progressService.getProgressByStudent(100L)).thenReturn(List.of(p));

        mockMvc.perform(get("/api/progress/student/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(100));
    }

    @Test
    void getDashboard_shouldReturnSummary() throws Exception {
        Map<String, Object> summary = Map.of(
                "enrollmentId", 10L,
                "courseId", 200L,
                "totalLessons", 4L,
                "completedLessons", 2L,
                "progressPercent", new BigDecimal("50.00"),
                "completed", false
        );
        when(progressService.getDashboard(100L)).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/progress/dashboard")
                        .param("studentId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(10));
    }

    @Test
    void health_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/progress/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("progress-service"));
    }
}
