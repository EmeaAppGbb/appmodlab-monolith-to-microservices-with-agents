package com.eduverse.controller;

import com.eduverse.model.Course;
import com.eduverse.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private CourseService courseService;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        logger.info("Loading home page with featured courses");
        try {
            List<Course> featuredCourses = courseService.getAllPublishedCourses();
            model.addAttribute("featuredCourses", featuredCourses);
            model.addAttribute("courseCount", featuredCourses.size());
        } catch (Exception e) {
            logger.error("Error loading featured courses", e);
            model.addAttribute("error", "Unable to load courses at this time.");
        }
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        logger.info("Displaying login page");
        return "login";
    }
}
