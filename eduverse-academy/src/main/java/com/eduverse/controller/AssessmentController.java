package com.eduverse.controller;

import com.eduverse.model.Assessment;
import com.eduverse.model.StudentAnswer;
import com.eduverse.service.AssessmentService;
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
public class AssessmentController {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentController.class);

    @Autowired
    private AssessmentService assessmentService;

    @GetMapping("/assessment/{id}")
    public String viewAssessment(@PathVariable("id") Long id, Model model) {
        logger.info("Loading assessment id={}", id);
        try {
            List<Assessment> assessments = assessmentService.getAssessmentByLessonId(id);
            if (assessments.isEmpty()) {
                model.addAttribute("error", "Assessment not found.");
                return "error";
            }
            model.addAttribute("assessment", assessments.get(0));
        } catch (Exception e) {
            logger.error("Error loading assessment id={}", id, e);
            model.addAttribute("error", "Unable to load assessment.");
            return "error";
        }
        return "assessmentDetail";
    }

    @PostMapping("/assessment/submit")
    public String submitAssessment(@RequestParam("assessmentId") Long assessmentId,
                                   @RequestParam("answersJson") String answersJson,
                                   Principal principal, RedirectAttributes redirectAttributes) {
        logger.info("Student {} submitting assessment id={}", principal.getName(), assessmentId);
        try {
            Long studentId = Long.parseLong(principal.getName());
            // Tight coupling: AssessmentService internally calls ProgressService
            // and EnrollmentRepository during answer submission
            StudentAnswer result = assessmentService.submitAnswer(assessmentId, studentId, answersJson);
            redirectAttributes.addFlashAttribute("success",
                    "Assessment submitted! Your score: " + result.getScore());
            return "redirect:/assessment/results/" + result.getId();
        } catch (Exception e) {
            logger.error("Error submitting assessment id={}", assessmentId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to submit assessment.");
            return "redirect:/assessment/" + assessmentId;
        }
    }

    @GetMapping("/assessment/results/{id}")
    public String viewResults(@PathVariable("id") Long id, Model model) {
        logger.info("Viewing assessment results for answer id={}", id);
        try {
            // Reusing assessmentService to look up the student answer by lesson context
            List<Assessment> assessments = assessmentService.getAssessmentByLessonId(id);
            model.addAttribute("assessments", assessments);
            model.addAttribute("answerId", id);
        } catch (Exception e) {
            logger.error("Error loading assessment results for id={}", id, e);
            model.addAttribute("error", "Unable to load results.");
            return "error";
        }
        return "assessmentResults";
    }
}
