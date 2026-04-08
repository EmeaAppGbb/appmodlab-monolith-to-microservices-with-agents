package com.eduverse.service;

import com.eduverse.model.Course;
import com.eduverse.model.Module;
import com.eduverse.repository.CourseRepository;
import com.eduverse.repository.ModuleRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CourseService}.
 *
 * Uses Mockito to isolate the service from its dependencies (repositories,
 * NotificationService). These tests demonstrate the existing test coverage
 * for the monolith before decomposition.
 */
@RunWith(MockitoJUnitRunner.class)
public class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CourseService courseService;

    private Course sampleCourse;

    @Before
    public void setUp() {
        sampleCourse = new Course();
        sampleCourse.setId(1L);
        sampleCourse.setTitle("Introduction to Java");
        sampleCourse.setDescription("Learn Java fundamentals");
        sampleCourse.setInstructorId(10L);
        sampleCourse.setCategory("Programming");
        sampleCourse.setPrice(new BigDecimal("49.99"));
    }

    @Test
    public void testCreateCourse() {
        Course newCourse = new Course();
        newCourse.setTitle("Spring Boot Essentials");
        newCourse.setInstructorId(10L);

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        Course result = courseService.createCourse(newCourse);

        assertNotNull(result);
        assertEquals(Long.valueOf(2L), result.getId());
        assertEquals("Spring Boot Essentials", result.getTitle());
        assertEquals(Course.Status.DRAFT, result.getStatus());

        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    public void testPublishCourse() {
        sampleCourse.setStatus(Course.Status.DRAFT);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(sampleCourse));

        // Course must have at least one module to be published
        Module module = new Module();
        module.setId(100L);
        when(moduleRepository.findByCourseIdOrderBySortOrder(1L))
                .thenReturn(Collections.singletonList(module));

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // NotificationService is called during publish — demonstrates the coupling
        doNothing().when(notificationService).sendEmail(anyLong(), anyString(), anyString());

        Course result = courseService.publishCourse(1L);

        assertEquals(Course.Status.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedDate());

        verify(courseRepository).save(sampleCourse);
        verify(notificationService).sendEmail(eq(10L), contains("Course Published"), anyString());
    }

    @Test
    public void testGetAllPublishedCourses() {
        Course course1 = new Course();
        course1.setId(1L);
        course1.setTitle("Java 101");
        course1.setStatus(Course.Status.PUBLISHED);

        Course course2 = new Course();
        course2.setId(2L);
        course2.setTitle("Python 101");
        course2.setStatus(Course.Status.PUBLISHED);

        when(courseRepository.findByStatus(Course.Status.PUBLISHED))
                .thenReturn(Arrays.asList(course1, course2));

        List<Course> result = courseService.getAllPublishedCourses();

        assertEquals(2, result.size());
        assertEquals("Java 101", result.get(0).getTitle());
        assertEquals("Python 101", result.get(1).getTitle());

        verify(courseRepository).findByStatus(Course.Status.PUBLISHED);
    }
}
