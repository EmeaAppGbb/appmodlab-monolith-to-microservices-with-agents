package com.eduverse.controller.api;

import com.eduverse.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationApiController.class);

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/email")
    public ResponseEntity<?> sendEmail(@RequestBody SendEmailRequest request) {
        try {
            notificationService.sendEmail(request.userId, request.subject, request.message);
            return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
        } catch (Exception e) {
            logger.error("Error sending email to user {}", request.userId, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    static class SendEmailRequest {
        public Long userId;
        public String subject;
        public String message;
    }
}
