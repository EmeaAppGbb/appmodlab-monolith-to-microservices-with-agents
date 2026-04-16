package com.eduverse.catalog.controller;

import com.eduverse.catalog.model.Course;
import com.eduverse.catalog.service.CourseCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseCatalogController {

    private static final Logger logger = LoggerFactory.getLogger(CourseCatalogController.class);

    @Autowired
    private CourseCatalogService courseCatalogService;

    @GetMapping
    public ResponseEntity<?> getAllPublishedCourses() {
        try {
            List<Course> courses = courseCatalogService.getAllPublishedCourses();
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            logger.error("Error fetching published courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseWithModules(@PathVariable("id") Long id) {
        try {
            Course course = courseCatalogService.getCourseWithModules(id);
            return ResponseEntity.ok(course);
        } catch (CourseCatalogService.CourseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching course {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody Course course) {
        try {
            Course created = courseCatalogService.createCourse(course);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            logger.error("Error creating course", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCourse(@PathVariable("id") Long id, @RequestBody Course course) {
        try {
            Course updated = courseCatalogService.updateCourse(id, course);
            return ResponseEntity.ok(updated);
        } catch (CourseCatalogService.CourseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating course {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable("id") Long id) {
        try {
            courseCatalogService.deleteCourse(id);
            return ResponseEntity.noContent().build();
        } catch (CourseCatalogService.CourseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting course {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishCourse(@PathVariable("id") Long id) {
        try {
            Course published = courseCatalogService.publishCourse(id);
            return ResponseEntity.ok(published);
        } catch (CourseCatalogService.CourseNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error publishing course {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<?> getInstructorCourses(@PathVariable("instructorId") Long instructorId) {
        try {
            List<Course> courses = courseCatalogService.getInstructorCourses(instructorId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            logger.error("Error fetching courses for instructor {}", instructorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> getCoursesByCategory(@PathVariable("category") String category) {
        try {
            List<Course> courses = courseCatalogService.getCoursesByCategory(category);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            logger.error("Error fetching courses for category {}", category, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCourses(@RequestParam("q") String query) {
        try {
            List<Course> courses = courseCatalogService.searchCourses(query);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            logger.error("Error searching courses for '{}'", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "course-catalog-service"));
    }
}
