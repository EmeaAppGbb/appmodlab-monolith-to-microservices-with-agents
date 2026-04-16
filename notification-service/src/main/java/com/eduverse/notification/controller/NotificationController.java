package com.eduverse.notification.controller;

import com.eduverse.notification.model.Notification;
import com.eduverse.notification.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getUserNotifications(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getNotification(@PathVariable Long id) {
        return notificationService.getNotification(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Backward-compatible endpoint matching the monolith's POST /api/notifications/email.
     */
    @PostMapping("/email")
    public ResponseEntity<?> sendEmail(@Valid @RequestBody SendEmailRequest request) {
        try {
            Notification notification = notificationService.sendAdHocEmail(
                    request.userId, request.subject, request.message);
            return ResponseEntity.ok(Map.of(
                    "message", "Email sent successfully",
                    "notificationId", notification.getId(),
                    "status", notification.getStatus().name()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "notification-service"));
    }

    static class SendEmailRequest {
        @NotNull
        public Long userId;
        @NotBlank
        public String subject;
        @NotBlank
        public String message;
    }
}
