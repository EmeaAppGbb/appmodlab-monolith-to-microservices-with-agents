package com.eduverse.controller;

import com.eduverse.model.Course;
import com.eduverse.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
public class CourseController {

    private static final Logger logger = LoggerFactory.getLogger(CourseController.class);

    @Autowired
    private CourseService courseService;

    @GetMapping("/catalog")
    public String catalog(Model model) {
        logger.info("Loading course catalog");
        try {
            List<Course> courses = courseService.getAllPublishedCourses();
            model.addAttribute("courses", courses);
        } catch (Exception e) {
            logger.error("Error loading course catalog", e);
            model.addAttribute("error", "Unable to load course catalog.");
        }
        return "catalog";
    }

    @GetMapping("/course/view/{id}")
    public String viewCourse(@PathVariable("id") Long id, Model model) {
        logger.info("Viewing course details for id={}", id);
        try {
            Course course = courseService.getCourseWithModules(id);
            model.addAttribute("course", course);
        } catch (Exception e) {
            logger.error("Error loading course id={}", id, e);
            model.addAttribute("error", "Course not found.");
            return "error";
        }
        return "courseDetail";
    }

    @PostMapping("/course/create")
    public String createCourse(@ModelAttribute Course course, Principal principal,
                               RedirectAttributes redirectAttributes) {
        logger.info("Instructor {} creating new course: {}", principal.getName(), course.getTitle());
        try {
            Course created = courseService.createCourse(course);
            redirectAttributes.addFlashAttribute("success", "Course created successfully.");
            return "redirect:/course/edit/" + created.getId();
        } catch (Exception e) {
            logger.error("Error creating course", e);
            redirectAttributes.addFlashAttribute("error", "Failed to create course.");
            return "redirect:/instructor/dashboard";
        }
    }

    @PostMapping("/course/publish/{id}")
    public String publishCourse(@PathVariable("id") Long id, Principal principal,
                                RedirectAttributes redirectAttributes) {
        logger.info("Instructor {} publishing course id={}", principal.getName(), id);
        try {
            courseService.publishCourse(id);
            redirectAttributes.addFlashAttribute("success", "Course published successfully.");
        } catch (Exception e) {
            logger.error("Error publishing course id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "Failed to publish course.");
        }
        return "redirect:/instructor/dashboard";
    }

    @GetMapping("/course/edit/{id}")
    public String editCourse(@PathVariable("id") Long id, Model model, Principal principal) {
        logger.info("Instructor {} editing course id={}", principal.getName(), id);
        try {
            Course course = courseService.getCourseWithModules(id);
            model.addAttribute("course", course);
        } catch (Exception e) {
            logger.error("Error loading course for editing, id={}", id, e);
            model.addAttribute("error", "Course not found.");
            return "error";
        }
        return "courseEdit";
    }
}
