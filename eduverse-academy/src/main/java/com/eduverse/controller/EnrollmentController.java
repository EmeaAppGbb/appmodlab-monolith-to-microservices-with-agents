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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class EnrollmentController {

    private static final Logger logger = LoggerFactory.getLogger(EnrollmentController.class);

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private CourseService courseService;

    @PostMapping("/enroll/{courseId}")
    public String enrollStudent(@PathVariable("courseId") Long courseId,
                                @RequestParam(value = "stripeToken", required = false) String stripeToken,
                                Principal principal, RedirectAttributes redirectAttributes) {
        logger.info("Student {} enrolling in course id={}", principal.getName(), courseId);
        try {
            // Tight coupling: controller orchestrates enrollment which internally
            // calls PaymentService, ProgressService, and NotificationService
            Enrollment enrollment = enrollmentService.enrollStudent(
                    Long.parseLong(principal.getName()), courseId, stripeToken);
            redirectAttributes.addFlashAttribute("success", "Successfully enrolled in the course!");
            return "redirect:/student/course/" + courseId;
        } catch (Exception e) {
            logger.error("Error enrolling student in course id={}", courseId, e);
            redirectAttributes.addFlashAttribute("error", "Enrollment failed: " + e.getMessage());
            return "redirect:/course/view/" + courseId;
        }
    }

    @GetMapping("/student/enrollments")
    public String studentEnrollments(Principal principal, Model model) {
        logger.info("Loading enrollments for student {}", principal.getName());
        try {
            Long studentId = Long.parseLong(principal.getName());
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(studentId);
            model.addAttribute("enrollments", enrollments);

            // Tight coupling: loading course details for each enrollment
            for (Enrollment enrollment : enrollments) {
                Course course = courseService.getCourseWithModules(enrollment.getCourseId());
                model.addAttribute("course_" + enrollment.getCourseId(), course);
            }
        } catch (Exception e) {
            logger.error("Error loading student enrollments", e);
            model.addAttribute("error", "Unable to load enrollments.");
        }
        return "studentEnrollments";
    }

    @GetMapping("/student/course/{courseId}")
    public String courseLearningPage(@PathVariable("courseId") Long courseId,
                                     Principal principal, Model model) {
        logger.info("Student {} accessing learning page for course id={}", principal.getName(), courseId);
        try {
            Course course = courseService.getCourseWithModules(courseId);
            model.addAttribute("course", course);

            Long studentId = Long.parseLong(principal.getName());
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(studentId);
            Enrollment currentEnrollment = enrollments.stream()
                    .filter(e -> e.getCourseId().equals(courseId))
                    .findFirst()
                    .orElse(null);
            model.addAttribute("enrollment", currentEnrollment);
        } catch (Exception e) {
            logger.error("Error loading course learning page for course id={}", courseId, e);
            model.addAttribute("error", "Unable to load course content.");
            return "error";
        }
        return "courseLearning";
    }
}
