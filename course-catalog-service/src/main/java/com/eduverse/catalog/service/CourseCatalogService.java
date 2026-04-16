package com.eduverse.catalog.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.eduverse.catalog.model.Course;
import com.eduverse.catalog.model.Module;
import com.eduverse.catalog.repository.CourseRepository;
import com.eduverse.catalog.repository.LessonRepository;
import com.eduverse.catalog.repository.ModuleRepository;
import com.eduverse.events.CoursePublishedEvent;
import com.eduverse.events.EventSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(CourseCatalogService.class);

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    // Nullable — absent when Service Bus connection string is not configured
    private final ServiceBusSenderClient coursePublishedSender;

    @Autowired
    public CourseCatalogService(
            CourseRepository courseRepository,
            ModuleRepository moduleRepository,
            LessonRepository lessonRepository,
            @Autowired(required = false) ServiceBusSenderClient coursePublishedSender) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.coursePublishedSender = coursePublishedSender;
    }

    // ---- Course CRUD ----

    @Transactional
    public Course createCourse(Course course) {
        logger.info("Creating course: '{}'", course.getTitle());
        if (course.getStatus() == null) {
            course.setStatus(Course.Status.DRAFT);
        }
        course = courseRepository.save(course);
        logger.info("Course {} created: '{}'", course.getId(), course.getTitle());
        return course;
    }

    @Transactional
    public Course updateCourse(Long courseId, Course updatedData) {
        logger.info("Updating course {}", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (updatedData.getTitle() != null) {
            course.setTitle(updatedData.getTitle());
        }
        if (updatedData.getDescription() != null) {
            course.setDescription(updatedData.getDescription());
        }
        if (updatedData.getCategory() != null) {
            course.setCategory(updatedData.getCategory());
        }
        if (updatedData.getPrice() != null) {
            course.setPrice(updatedData.getPrice());
        }
        if (updatedData.getDurationHours() != null) {
            course.setDurationHours(updatedData.getDurationHours());
        }
        if (updatedData.getThumbnailUrl() != null) {
            course.setThumbnailUrl(updatedData.getThumbnailUrl());
        }

        course = courseRepository.save(course);
        logger.info("Course {} updated", courseId);
        return course;
    }

    @Transactional
    public void deleteCourse(Long courseId) {
        logger.info("Deleting course {}", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        courseRepository.delete(course);
        logger.info("Course {} deleted", courseId);
    }

    /**
     * Publishes a course — sets status to PUBLISHED, records the publish date,
     * and emits a CoursePublished event to Azure Service Bus.
     */
    @Transactional
    public Course publishCourse(Long courseId) {
        logger.info("Publishing course {}", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        if (course.getStatus() == Course.Status.PUBLISHED) {
            logger.warn("Course {} is already published", courseId);
            return course;
        }

        List<Module> modules = moduleRepository.findByCourseIdOrderBySortOrder(courseId);
        if (modules.isEmpty()) {
            throw new IllegalStateException("Cannot publish course " + courseId + " — no modules defined");
        }

        course.setStatus(Course.Status.PUBLISHED);
        course.setPublishedDate(LocalDateTime.now());
        course = courseRepository.save(course);

        publishCoursePublishedEvent(course);

        logger.info("Course {} published at {}", courseId, course.getPublishedDate());
        return course;
    }

    // ---- Read operations ----

    @Transactional(readOnly = true)
    public Course getCourseWithModules(Long courseId) {
        logger.debug("Fetching course {} with modules", courseId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        // Force initialization of lazy-loaded modules and lessons
        course.getModules().forEach(m -> m.getLessons().size());
        return course;
    }

    @Transactional(readOnly = true)
    public Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
    }

    public List<Course> getAllPublishedCourses() {
        logger.debug("Fetching all published courses");
        return courseRepository.findByStatus(Course.Status.PUBLISHED);
    }

    public List<Course> getInstructorCourses(Long instructorId) {
        logger.debug("Fetching courses for instructor {}", instructorId);
        return courseRepository.findByInstructorId(instructorId);
    }

    public List<Course> getCoursesByCategory(String category) {
        logger.debug("Fetching published courses in category: {}", category);
        return courseRepository.findByCategoryAndStatus(category, Course.Status.PUBLISHED);
    }

    public List<Course> searchCourses(String query) {
        logger.debug("Searching published courses for: '{}'", query);
        return courseRepository.searchByTitleOrDescription(query, Course.Status.PUBLISHED);
    }

    // ---- Event publishing ----

    private void publishCoursePublishedEvent(Course course) {
        if (coursePublishedSender == null) {
            logger.warn("Service Bus not configured — skipping CoursePublished event for course {}", course.getId());
            return;
        }
        try {
            CoursePublishedEvent event = new CoursePublishedEvent(
                    course.getId(),
                    course.getTitle(),
                    course.getInstructorId(),
                    course.getCategory(),
                    course.getPrice());
            event.setSource("course-catalog-service");

            String json = EventSerializer.serialize(event);
            ServiceBusMessage message = new ServiceBusMessage(json);
            message.setMessageId(event.getEventId());
            coursePublishedSender.sendMessage(message);

            logger.info("Published CoursePublished event for course {}: {}", course.getId(), event.getEventId());
        } catch (Exception e) {
            logger.error("Failed to publish CoursePublished event for course {}: {}",
                    course.getId(), e.getMessage(), e);
        }
    }

    // ---- Exception ----

    public static class CourseNotFoundException extends RuntimeException {
        public CourseNotFoundException(Long courseId) {
            super("Course not found: " + courseId);
        }
    }
}
