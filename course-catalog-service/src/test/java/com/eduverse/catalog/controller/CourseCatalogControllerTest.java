package com.eduverse.catalog.controller;

import com.eduverse.catalog.model.Course;
import com.eduverse.catalog.service.CourseCatalogService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseCatalogController.class)
class CourseCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseCatalogService courseCatalogService;

    @Autowired
    private ObjectMapper objectMapper;

    // ---- GET /api/courses ----

    @Test
    void getAllPublishedCourses_returnsOkWithList() throws Exception {
        when(courseCatalogService.getAllPublishedCourses())
                .thenReturn(List.of(sampleCourse()));

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java 101"));
    }

    // ---- GET /api/courses/{id} ----

    @Test
    void getCourse_existingId_returnsOk() throws Exception {
        when(courseCatalogService.getCourseWithModules(1L)).thenReturn(sampleCourse());

        mockMvc.perform(get("/api/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java 101"));
    }

    @Test
    void getCourse_missingId_returns404() throws Exception {
        when(courseCatalogService.getCourseWithModules(999L))
                .thenThrow(new CourseCatalogService.CourseNotFoundException(999L));

        mockMvc.perform(get("/api/courses/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Course not found: 999"));
    }

    // ---- POST /api/courses ----

    @Test
    void createCourse_returnsCreated() throws Exception {
        Course input = new Course();
        input.setTitle("New Course");
        input.setInstructorId(2L);

        Course saved = sampleCourse();
        saved.setTitle("New Course");
        when(courseCatalogService.createCourse(any(Course.class))).thenReturn(saved);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Course"));
    }

    // ---- PUT /api/courses/{id} ----

    @Test
    void updateCourse_returnsOk() throws Exception {
        Course update = new Course();
        update.setTitle("Updated Title");

        Course result = sampleCourse();
        result.setTitle("Updated Title");
        when(courseCatalogService.updateCourse(eq(1L), any(Course.class))).thenReturn(result);

        mockMvc.perform(put("/api/courses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void updateCourse_notFound_returns404() throws Exception {
        when(courseCatalogService.updateCourse(eq(999L), any(Course.class)))
                .thenThrow(new CourseCatalogService.CourseNotFoundException(999L));

        mockMvc.perform(put("/api/courses/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Course())))
                .andExpect(status().isNotFound());
    }

    // ---- DELETE /api/courses/{id} ----

    @Test
    void deleteCourse_returnsNoContent() throws Exception {
        doNothing().when(courseCatalogService).deleteCourse(1L);

        mockMvc.perform(delete("/api/courses/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteCourse_notFound_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new CourseCatalogService.CourseNotFoundException(999L))
                .when(courseCatalogService).deleteCourse(999L);

        mockMvc.perform(delete("/api/courses/999"))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/courses/{id}/publish ----

    @Test
    void publishCourse_returnsOk() throws Exception {
        Course published = sampleCourse();
        published.setStatus(Course.Status.PUBLISHED);
        published.setPublishedDate(LocalDateTime.now());
        when(courseCatalogService.publishCourse(1L)).thenReturn(published);

        mockMvc.perform(post("/api/courses/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publishCourse_noModules_returnsBadRequest() throws Exception {
        when(courseCatalogService.publishCourse(1L))
                .thenThrow(new IllegalStateException("Cannot publish — no modules"));

        mockMvc.perform(post("/api/courses/1/publish"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot publish — no modules"));
    }

    // ---- GET /api/courses/instructor/{id} ----

    @Test
    void getInstructorCourses_returnsOk() throws Exception {
        when(courseCatalogService.getInstructorCourses(2L))
                .thenReturn(List.of(sampleCourse()));

        mockMvc.perform(get("/api/courses/instructor/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].instructorId").value(2));
    }

    // ---- GET /api/courses/category/{category} ----

    @Test
    void getCoursesByCategory_returnsOk() throws Exception {
        when(courseCatalogService.getCoursesByCategory("Programming"))
                .thenReturn(List.of(sampleCourse()));

        mockMvc.perform(get("/api/courses/category/Programming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Programming"));
    }

    // ---- GET /api/courses/search?q= ----

    @Test
    void searchCourses_returnsOk() throws Exception {
        when(courseCatalogService.searchCourses("Java"))
                .thenReturn(List.of(sampleCourse()));

        mockMvc.perform(get("/api/courses/search").param("q", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java 101"));
    }

    // ---- GET /api/courses/health ----

    @Test
    void health_returnsUp() throws Exception {
        mockMvc.perform(get("/api/courses/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ---- Helper ----

    private Course sampleCourse() {
        Course c = new Course();
        c.setId(1L);
        c.setTitle("Java 101");
        c.setDescription("Learn Java");
        c.setInstructorId(2L);
        c.setCategory("Programming");
        c.setPrice(new BigDecimal("49.99"));
        c.setStatus(Course.Status.DRAFT);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }
}
