package com.eduverse.service;

import com.eduverse.model.Course;
import com.eduverse.model.Module;
import com.eduverse.repository.CourseRepository;
import com.eduverse.repository.ModuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages course lifecycle — creation, editing, publishing, and retrieval.
 *
 * MONOLITH ANTI-PATTERN: Publishing a course triggers a synchronous call to
 * NotificationService within the same transaction. If email delivery is slow
 * or fails, the course publish transaction is affected. The service also directly
 * manages Module entities rather than delegating to a separate content management
 * service — combining catalog and content concerns.
 */
@Service
public class CourseService {

    private static final Logger logger = LoggerFactory.getLogger(CourseService.class);

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    // MONOLITH ANTI-PATTERN: Direct dependency on notification service — course
    // publishing is coupled to email delivery in a single transaction
    @Autowired
    private NotificationService notificationService;

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
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        // MONOLITH ANTI-PATTERN: Published courses can still be mutated — no
        // proper state machine enforcement; relies on callers to check status
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

    /**
     * Publishes a course — sets status to PUBLISHED, records the publish date,
     * and sends a notification to the instructor.
     *
     * MONOLITH ANTI-PATTERN: Course publishing, status management, and notification
     * delivery all happen in one @Transactional method. The notification side-effect
     * is hidden from the caller, and a notification failure could roll back the publish.
     */
    @Transactional
    public Course publishCourse(Long courseId) {
        logger.info("Publishing course {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        if (course.getStatus() == Course.Status.PUBLISHED) {
            logger.warn("Course {} is already published", courseId);
            return course;
        }

        // Validate course has content before publishing
        List<Module> modules = moduleRepository.findByCourseIdOrderBySortOrder(courseId);
        if (modules.isEmpty()) {
            throw new RuntimeException("Cannot publish course " + courseId + " — no modules defined");
        }

        course.setStatus(Course.Status.PUBLISHED);
        course.setPublishedDate(LocalDateTime.now());
        course = courseRepository.save(course);

        // MONOLITH ANTI-PATTERN: Synchronous notification within publish transaction
        // — if email server is down, publishing fails or the error is silently swallowed
        try {
            notificationService.sendEmail(
                    course.getInstructorId(),
                    "Course Published - " + course.getTitle(),
                    "Your course \"" + course.getTitle() + "\" has been published and is now " +
                            "available to students on EduVerse Academy.\n\n" +
                            "Published at: " + course.getPublishedDate()
            );
        } catch (Exception e) {
            // Swallow notification failure — inconsistent behavior
            logger.error("Failed to send publish notification for course {}: {}",
                    courseId, e.getMessage());
        }

        logger.info("Course {} published at {}", courseId, course.getPublishedDate());
        return course;
    }

    /**
     * Fetches a course with its modules eagerly loaded.
     * Triggers Hibernate lazy-loading within the transaction boundary.
     */
    @Transactional(readOnly = true)
    public Course getCourseWithModules(Long courseId) {
        logger.debug("Fetching course {} with modules", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        // Force initialization of lazy-loaded modules within transaction
        course.getModules().size();

        return course;
    }

    public List<Course> getAllPublishedCourses() {
        logger.debug("Fetching all published courses");
        return courseRepository.findByStatus(Course.Status.PUBLISHED);
    }

    public List<Course> getInstructorCourses(Long instructorId) {
        logger.debug("Fetching courses for instructor {}", instructorId);
        return courseRepository.findByInstructorId(instructorId);
    }
}
