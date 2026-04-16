package com.eduverse.catalog.service;

import com.eduverse.catalog.model.Course;
import com.eduverse.catalog.model.Lesson;
import com.eduverse.catalog.model.Module;
import com.eduverse.catalog.repository.CourseRepository;
import com.eduverse.catalog.repository.LessonRepository;
import com.eduverse.catalog.repository.ModuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseCatalogServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private ModuleRepository moduleRepository;

    @Mock
    private LessonRepository lessonRepository;

    private CourseCatalogService service;

    @BeforeEach
    void setUp() {
        // No Service Bus sender in tests
        service = new CourseCatalogService(courseRepository, moduleRepository, lessonRepository, null);
    }

    // ---- createCourse ----

    @Test
    void createCourse_setsDefaultStatusToDraft() {
        Course course = buildCourse(null, "Test Course");
        course.setStatus(null);

        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        Course result = service.createCourse(course);

        assertNotNull(result.getId());
        assertEquals(Course.Status.DRAFT, result.getStatus());
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void createCourse_preservesExplicitStatus() {
        Course course = buildCourse(null, "Test Course");
        course.setStatus(Course.Status.PUBLISHED);

        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        Course result = service.createCourse(course);

        assertEquals(Course.Status.PUBLISHED, result.getStatus());
    }

    // ---- updateCourse ----

    @Test
    void updateCourse_updatesOnlyProvidedFields() {
        Course existing = buildCourse(1L, "Old Title");
        existing.setCategory("OldCat");
        existing.setPrice(new BigDecimal("10.00"));

        Course updateData = new Course();
        updateData.setTitle("New Title");
        updateData.setCategory("NewCat");

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = service.updateCourse(1L, updateData);

        assertEquals("New Title", result.getTitle());
        assertEquals("NewCat", result.getCategory());
        assertEquals(new BigDecimal("10.00"), result.getPrice());
    }

    @Test
    void updateCourse_notFound_throwsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseCatalogService.CourseNotFoundException.class,
                () -> service.updateCourse(999L, new Course()));
    }

    // ---- deleteCourse ----

    @Test
    void deleteCourse_existingCourse_deletes() {
        Course course = buildCourse(1L, "To Delete");
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        service.deleteCourse(1L);

        verify(courseRepository).delete(course);
    }

    @Test
    void deleteCourse_notFound_throwsException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(CourseCatalogService.CourseNotFoundException.class,
                () -> service.deleteCourse(999L));
    }

    // ---- publishCourse ----

    @Test
    void publishCourse_setsStatusAndDate() {
        Course course = buildCourse(1L, "Draft Course");
        course.setStatus(Course.Status.DRAFT);
        course.setInstructorId(2L);
        course.setCategory("Programming");
        course.setPrice(new BigDecimal("49.99"));

        Module module = new Module();
        module.setId(1L);
        module.setTitle("Module 1");
        module.setSortOrder(1);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(moduleRepository.findByCourseIdOrderBySortOrder(1L)).thenReturn(List.of(module));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course result = service.publishCourse(1L);

        assertEquals(Course.Status.PUBLISHED, result.getStatus());
        assertNotNull(result.getPublishedDate());
    }

    @Test
    void publishCourse_alreadyPublished_returnsWithoutChange() {
        Course course = buildCourse(1L, "Published Course");
        course.setStatus(Course.Status.PUBLISHED);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Course result = service.publishCourse(1L);

        assertEquals(Course.Status.PUBLISHED, result.getStatus());
        verify(courseRepository, never()).save(any());
    }

    @Test
    void publishCourse_noModules_throwsException() {
        Course course = buildCourse(1L, "Empty Course");
        course.setStatus(Course.Status.DRAFT);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(moduleRepository.findByCourseIdOrderBySortOrder(1L)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.publishCourse(1L));
    }

    // ---- Read operations ----

    @Test
    void getAllPublishedCourses_delegatesToRepository() {
        Course c1 = buildCourse(1L, "Course 1");
        Course c2 = buildCourse(2L, "Course 2");
        when(courseRepository.findByStatus(Course.Status.PUBLISHED)).thenReturn(List.of(c1, c2));

        List<Course> result = service.getAllPublishedCourses();

        assertEquals(2, result.size());
    }

    @Test
    void getInstructorCourses_delegatesToRepository() {
        Course c1 = buildCourse(1L, "Instructor Course");
        when(courseRepository.findByInstructorId(2L)).thenReturn(List.of(c1));

        List<Course> result = service.getInstructorCourses(2L);

        assertEquals(1, result.size());
    }

    @Test
    void getCoursesByCategory_delegatesToRepository() {
        Course c1 = buildCourse(1L, "Java");
        when(courseRepository.findByCategoryAndStatus("Programming", Course.Status.PUBLISHED))
                .thenReturn(List.of(c1));

        List<Course> result = service.getCoursesByCategory("Programming");

        assertEquals(1, result.size());
    }

    @Test
    void searchCourses_delegatesToRepository() {
        Course c1 = buildCourse(1L, "Java Programming");
        when(courseRepository.searchByTitleOrDescription("Java", Course.Status.PUBLISHED))
                .thenReturn(List.of(c1));

        List<Course> result = service.searchCourses("Java");

        assertEquals(1, result.size());
    }

    @Test
    void getCourseWithModules_forcesLazyLoad() {
        Course course = buildCourse(1L, "Course");
        Module module = new Module();
        module.setId(1L);
        module.setTitle("Mod");
        module.setSortOrder(1);
        Lesson lesson = new Lesson();
        lesson.setId(1L);
        lesson.setTitle("Lesson");
        lesson.setContentType(Lesson.ContentType.VIDEO);
        lesson.setSortOrder(1);
        module.getLessons().add(lesson);
        course.getModules().add(module);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Course result = service.getCourseWithModules(1L);

        assertNotNull(result);
        assertEquals(1, result.getModules().size());
        assertEquals(1, result.getModules().get(0).getLessons().size());
    }

    // ---- Helpers ----

    private Course buildCourse(Long id, String title) {
        Course course = new Course();
        course.setId(id);
        course.setTitle(title);
        course.setInstructorId(2L);
        course.setStatus(Course.Status.DRAFT);
        return course;
    }
}
