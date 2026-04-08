package com.eduverse.controller;

import com.eduverse.model.Course;
import com.eduverse.model.Enrollment;
import com.eduverse.service.CourseService;
import com.eduverse.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/instructor")
public class InstructorController {

    private static final Logger logger = LoggerFactory.getLogger(InstructorController.class);

    @Autowired
    private CourseService courseService;

    @Autowired
    private EnrollmentService enrollmentService;

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        logger.info("Loading instructor dashboard for {}", principal.getName());
        try {
            Long instructorId = Long.parseLong(principal.getName());
            List<Course> courses = courseService.getInstructorCourses(instructorId);
            model.addAttribute("courses", courses);

            // Tight coupling: loading enrollment counts for each course
            // by directly querying EnrollmentService from the instructor context
            Map<Long, Integer> enrollmentCounts = new HashMap<>();
            for (Course course : courses) {
                List<Enrollment> courseEnrollments = enrollmentService.getStudentEnrollments(course.getId());
                enrollmentCounts.put(course.getId(), courseEnrollments.size());
            }
            model.addAttribute("enrollmentCounts", enrollmentCounts);
            model.addAttribute("totalCourses", courses.size());
        } catch (Exception e) {
            logger.error("Error loading instructor dashboard", e);
            model.addAttribute("error", "Unable to load dashboard.");
        }
        return "instructorDashboard";
    }

    @GetMapping("/analytics/{courseId}")
    public String courseAnalytics(@PathVariable("courseId") Long courseId,
                                 Principal principal, Model model) {
        logger.info("Loading analytics for course id={}, instructor={}", courseId, principal.getName());
        try {
            Course course = courseService.getCourseWithModules(courseId);
            model.addAttribute("course", course);

            // Tight coupling: cross-domain queries to build analytics
            // from enrollment data within the instructor's controller
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(courseId);
            model.addAttribute("enrollments", enrollments);
            model.addAttribute("totalEnrollments", enrollments.size());

            long activeCount = enrollments.stream()
                    .filter(e -> e.getStatus() == Enrollment.Status.ACTIVE)
                    .count();
            long completedCount = enrollments.stream()
                    .filter(e -> e.getStatus() == Enrollment.Status.COMPLETED)
                    .count();
            long droppedCount = enrollments.stream()
                    .filter(e -> e.getStatus() == Enrollment.Status.DROPPED)
                    .count();

            model.addAttribute("activeEnrollments", activeCount);
            model.addAttribute("completedEnrollments", completedCount);
            model.addAttribute("droppedEnrollments", droppedCount);
        } catch (Exception e) {
            logger.error("Error loading analytics for course id={}", courseId, e);
            model.addAttribute("error", "Unable to load course analytics.");
            return "error";
        }
        return "courseAnalytics";
    }
}
