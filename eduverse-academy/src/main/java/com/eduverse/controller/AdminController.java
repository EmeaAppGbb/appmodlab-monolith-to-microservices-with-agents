package com.eduverse.controller;

import com.eduverse.model.Course;
import com.eduverse.model.Enrollment;
import com.eduverse.model.User;
import com.eduverse.repository.UserRepository;
import com.eduverse.service.CourseService;
import com.eduverse.service.EnrollmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private CourseService courseService;

    @Autowired
    private EnrollmentService enrollmentService;

    // Tight coupling: controller directly depends on a repository
    // instead of going through a dedicated UserService
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        logger.info("Loading admin dashboard with platform statistics");
        try {
            List<Course> allCourses = courseService.getAllPublishedCourses();
            List<User> allUsers = userRepository.findAll();

            model.addAttribute("totalCourses", allCourses.size());
            model.addAttribute("totalUsers", allUsers.size());

            // Tight coupling: aggregating enrollment stats by iterating
            // through all courses and querying enrollments for each
            int totalEnrollments = 0;
            int activeEnrollments = 0;
            for (Course course : allCourses) {
                List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(course.getId());
                totalEnrollments += enrollments.size();
                activeEnrollments += (int) enrollments.stream()
                        .filter(e -> e.getStatus() == Enrollment.Status.ACTIVE)
                        .count();
            }
            model.addAttribute("totalEnrollments", totalEnrollments);
            model.addAttribute("activeEnrollments", activeEnrollments);
        } catch (Exception e) {
            logger.error("Error loading admin dashboard", e);
            model.addAttribute("error", "Unable to load platform statistics.");
        }
        return "adminDashboard";
    }

    @GetMapping("/courses")
    public String manageCourses(Model model) {
        logger.info("Loading admin course management page");
        try {
            List<Course> courses = courseService.getAllPublishedCourses();
            model.addAttribute("courses", courses);
        } catch (Exception e) {
            logger.error("Error loading courses for admin", e);
            model.addAttribute("error", "Unable to load courses.");
        }
        return "adminCourses";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        logger.info("Loading admin user management page");
        try {
            List<User> users = userRepository.findAll();
            model.addAttribute("users", users);
        } catch (Exception e) {
            logger.error("Error loading users for admin", e);
            model.addAttribute("error", "Unable to load users.");
        }
        return "adminUsers";
    }
}
