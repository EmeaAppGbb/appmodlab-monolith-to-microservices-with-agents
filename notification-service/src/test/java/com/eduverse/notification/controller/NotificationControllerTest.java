package com.eduverse.notification.controller;

import com.eduverse.notification.model.Notification;
import com.eduverse.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getUserNotifications_returnsListForUser() throws Exception {
        Notification n = createSampleNotification();
        when(notificationService.getUserNotifications(1L)).thenReturn(List.of(n));

        mockMvc.perform(get("/api/notifications/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].subject").value("Test Subject"));
    }

    @Test
    void getNotification_existingId_returnsNotification() throws Exception {
        Notification n = createSampleNotification();
        when(notificationService.getNotification(1L)).thenReturn(Optional.of(n));

        mockMvc.perform(get("/api/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Test Subject"));
    }

    @Test
    void getNotification_missingId_returns404() throws Exception {
        when(notificationService.getNotification(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/notifications/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sendEmail_validRequest_returnsOk() throws Exception {
        Notification n = createSampleNotification();
        n.setStatus(Notification.Status.SENT);
        when(notificationService.sendAdHocEmail(eq(1L), any(), any())).thenReturn(n);

        String body = """
                {"userId": 1, "subject": "Hello", "message": "World"}
                """;

        mockMvc.perform(post("/api/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email sent successfully"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    void sendEmail_missingFields_returnsBadRequest() throws Exception {
        String body = """
                {"userId": null, "subject": "", "message": ""}
                """;

        mockMvc.perform(post("/api/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void health_returnsUp() throws Exception {
        mockMvc.perform(get("/api/notifications/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private Notification createSampleNotification() {
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(1L);
        n.setType(Notification.Type.EMAIL);
        n.setStatus(Notification.Status.PENDING);
        n.setSubject("Test Subject");
        n.setMessage("Test body");
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }
}
