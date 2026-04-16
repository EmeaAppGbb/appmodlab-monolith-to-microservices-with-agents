package com.eduverse.assessment;

import com.eduverse.assessment.controller.AssessmentController;
import com.eduverse.assessment.model.Assessment;
import com.eduverse.assessment.model.StudentAnswer;
import com.eduverse.assessment.service.AssessmentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssessmentController.class)
class AssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AssessmentService assessmentService;

    private Assessment createTestAssessment(Long id, Long lessonId, Long courseId) {
        Assessment assessment = new Assessment();
        assessment.setId(id);
        assessment.setLessonId(lessonId);
        assessment.setCourseId(courseId);
        assessment.setTitle("Test Assessment");
        assessment.setDescription("A test assessment");
        assessment.setType(Assessment.Type.QUIZ);
        assessment.setPassingScore(70);
        assessment.setMaxScore(100);
        assessment.setCreatedAt(LocalDateTime.now());
        assessment.setUpdatedAt(LocalDateTime.now());
        return assessment;
    }

    private StudentAnswer createTestStudentAnswer(Long id, Long assessmentId, Long studentId) {
        StudentAnswer answer = new StudentAnswer();
        answer.setId(id);
        answer.setAssessmentId(assessmentId);
        answer.setStudentId(studentId);
        answer.setScore(85);
        answer.setPassed(true);
        answer.setGradedAt(LocalDateTime.now());
        answer.setSubmittedAt(LocalDateTime.now());
        answer.setCreatedAt(LocalDateTime.now());
        return answer;
    }

    @Test
    void getAssessment_shouldReturnAssessment() throws Exception {
        Assessment assessment = createTestAssessment(1L, 10L, 1L);
        when(assessmentService.getAssessment(1L)).thenReturn(Optional.of(assessment));

        mockMvc.perform(get("/api/assessments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.lessonId").value(10))
                .andExpect(jsonPath("$.type").value("QUIZ"));
    }

    @Test
    void getAssessmentByLesson_shouldReturnAssessment() throws Exception {
        Assessment assessment = createTestAssessment(1L, 10L, 1L);
        when(assessmentService.getAssessmentByLesson(10L)).thenReturn(Optional.of(assessment));

        mockMvc.perform(get("/api/assessments/lesson/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(10));
    }

    @Test
    void createAssessment_shouldReturnCreated() throws Exception {
        Assessment assessment = createTestAssessment(1L, 10L, 1L);
        when(assessmentService.createAssessment(
                eq(10L), eq(1L), eq("Test Assessment"), any(), eq(Assessment.Type.QUIZ),
                any(), eq(70), eq(100), eq(15)))
                .thenReturn(assessment);

        mockMvc.perform(post("/api/assessments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "lessonId": 10,
                                    "courseId": 1,
                                    "title": "Test Assessment",
                                    "description": "A test assessment",
                                    "type": "QUIZ",
                                    "passingScore": 70,
                                    "maxScore": 100,
                                    "timeLimitMinutes": 15
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void submitAnswers_shouldReturnGradedAnswer() throws Exception {
        StudentAnswer answer = createTestStudentAnswer(1L, 1L, 100L);
        when(assessmentService.submitAnswers(eq(1L), eq(100L), eq(50L), any()))
                .thenReturn(answer);

        mockMvc.perform(post("/api/assessments/1/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "studentId": 100,
                                    "enrollmentId": 50,
                                    "answersJson": "[{\\"q\\":0,\\"a\\":1}]"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(85))
                .andExpect(jsonPath("$.passed").value(true));
    }

    @Test
    void getResults_shouldReturnStudentAnswer() throws Exception {
        StudentAnswer answer = createTestStudentAnswer(1L, 1L, 100L);
        when(assessmentService.getResults(1L, 100L)).thenReturn(Optional.of(answer));

        mockMvc.perform(get("/api/assessments/1/results")
                        .param("studentId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessmentId").value(1))
                .andExpect(jsonPath("$.studentId").value(100))
                .andExpect(jsonPath("$.score").value(85));
    }
}
