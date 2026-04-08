package com.eduverse.controller;

import com.eduverse.model.Course;
import com.eduverse.model.Enrollment;
import com.eduverse.model.Payment;
import com.eduverse.service.EnrollmentService;
import com.eduverse.service.PaymentService;
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

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EnrollmentService enrollmentService;

    @GetMapping("/payment/checkout/{courseId}")
    public String checkout(@PathVariable("courseId") Long courseId, Principal principal, Model model) {
        logger.info("Loading checkout page for course id={}, user={}", courseId, principal.getName());
        try {
            // Tight coupling: PaymentController reaches into EnrollmentService
            // to check if student is already enrolled before showing payment form
            Long studentId = Long.parseLong(principal.getName());
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(studentId);
            boolean alreadyEnrolled = enrollments.stream()
                    .anyMatch(e -> e.getCourseId().equals(courseId));

            if (alreadyEnrolled) {
                model.addAttribute("error", "You are already enrolled in this course.");
                return "redirect:/student/course/" + courseId;
            }

            model.addAttribute("courseId", courseId);
            model.addAttribute("stripePublicKey", "pk_test_eduverse_placeholder_key");
        } catch (Exception e) {
            logger.error("Error loading checkout for course id={}", courseId, e);
            model.addAttribute("error", "Unable to load payment page.");
            return "error";
        }
        return "paymentCheckout";
    }

    @PostMapping("/payment/process")
    public String processPayment(@RequestParam("courseId") Long courseId,
                                 @RequestParam("stripeToken") String stripeToken,
                                 @RequestParam("amount") BigDecimal amount,
                                 Principal principal, RedirectAttributes redirectAttributes) {
        logger.info("Processing payment for course id={}, amount={}, user={}",
                courseId, amount, principal.getName());
        try {
            Long studentId = Long.parseLong(principal.getName());

            // Tight coupling: enrollment, payment, notification, and progress
            // all happen in one synchronous transaction via EnrollmentService
            Enrollment enrollment = enrollmentService.enrollStudent(studentId, courseId, stripeToken);

            redirectAttributes.addFlashAttribute("success", "Payment processed successfully!");
            redirectAttributes.addFlashAttribute("enrollmentId", enrollment.getId());
            return "redirect:/payment/success";
        } catch (Exception e) {
            logger.error("Payment processing failed for course id={}", courseId, e);
            redirectAttributes.addFlashAttribute("error", "Payment failed: " + e.getMessage());
            return "redirect:/payment/cancel";
        }
    }

    @GetMapping("/payment/success")
    public String paymentSuccess(Model model) {
        logger.info("Displaying payment success page");
        return "paymentSuccess";
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel(Model model) {
        logger.info("Displaying payment cancellation page");
        return "paymentCancel";
    }
}
