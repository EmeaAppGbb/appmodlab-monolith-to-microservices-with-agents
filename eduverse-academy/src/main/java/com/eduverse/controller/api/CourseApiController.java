package com.eduverse.controller.api;

import com.eduverse.model.Course;
import com.eduverse.service.CourseService;
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
public class CourseApiController {

    private static final Logger logger = LoggerFactory.getLogger(CourseApiController.class);

    @Autowired
    private CourseService courseService;

    @GetMapping
    public ResponseEntity<?> getAllPublishedCourses() {
        try {
            List<Course> courses = courseService.getAllPublishedCourses();
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
            Course course = courseService.getCourseWithModules(id);
            if (course == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Course not found"));
            }
            return ResponseEntity.ok(course);
        } catch (Exception e) {
            logger.error("Error fetching course {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createCourse(@RequestBody Course course) {
        try {
            Course created = courseService.createCourse(course);
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
            Course updated = courseService.updateCourse(id, course);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Error updating course {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishCourse(@PathVariable("id") Long id) {
        try {
            Course published = courseService.publishCourse(id);
            return ResponseEntity.ok(published);
        } catch (Exception e) {
            logger.error("Error publishing course {}", id, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/instructor/{instructorId}")
    public ResponseEntity<?> getInstructorCourses(@PathVariable("instructorId") Long instructorId) {
        try {
            List<Course> courses = courseService.getInstructorCourses(instructorId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            logger.error("Error fetching courses for instructor {}", instructorId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
