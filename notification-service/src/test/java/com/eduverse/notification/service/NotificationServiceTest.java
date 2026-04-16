package com.eduverse.notification.service;

import com.eduverse.notification.model.Notification;
import com.eduverse.notification.model.NotificationEvent;
import com.eduverse.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = NotificationEvent.builder()
                .eventId("evt-123")
                .correlationId("corr-456")
                .sourceEventType("StudentEnrolled")
                .userId(1L)
                .recipientEmail("student@example.com")
                .recipientName("John Doe")
                .notificationType(Notification.Type.EMAIL)
                .subject("Welcome to Java 101")
                .message("You have been enrolled.")
                .build();
    }

    @Test
    void processNotificationEvent_createsAndSendsEmail() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    n.setId(1L);
                    return n;
                });

        Notification result = notificationService.processNotificationEvent(sampleEvent);

        assertNotNull(result);
        assertEquals("evt-123", result.getEventId());
        assertEquals(1L, result.getUserId());
        assertEquals(Notification.Type.EMAIL, result.getType());
        assertEquals("StudentEnrolled", result.getSourceEventType());

        verify(mailSender).send(any(SimpleMailMessage.class));
        // save called twice: once for create, once for status update
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void processNotificationEvent_idempotent_duplicateEventId() {
        Notification existing = new Notification();
        existing.setId(99L);
        existing.setEventId("evt-123");
        existing.setStatus(Notification.Status.SENT);

        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(notificationRepository.findByEventId("evt-123"))
                .thenReturn(Optional.of(existing));

        Notification result = notificationService.processNotificationEvent(sampleEvent);

        assertEquals(99L, result.getId());
        assertEquals(Notification.Status.SENT, result.getStatus());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void processNotificationEvent_emailFailure_marksAsFailed() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    n.setId(1L);
                    return n;
                });
        doThrow(new RuntimeException("SMTP connection refused"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        Notification result = notificationService.processNotificationEvent(sampleEvent);

        assertNotNull(result);
        // save called: once for create, once for markAsFailed
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());

        Notification lastSaved = captor.getAllValues().get(1);
        assertEquals(Notification.Status.FAILED, lastSaved.getStatus());
        assertEquals("SMTP connection refused", lastSaved.getFailureReason());
    }

    @Test
    void processNotificationEvent_inApp_marksAsSentWithoutEmail() {
        NotificationEvent inAppEvent = NotificationEvent.builder()
                .eventId("evt-456")
                .userId(1L)
                .notificationType(Notification.Type.IN_APP)
                .subject("Enrolled")
                .message("You are enrolled")
                .build();

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    n.setId(2L);
                    return n;
                });

        Notification result = notificationService.processNotificationEvent(inAppEvent);

        assertNotNull(result);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void sendAdHocEmail_createsNotificationAndSendsEmail() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> {
                    Notification n = invocation.getArgument(0);
                    n.setId(3L);
                    return n;
                });

        Notification result = notificationService.sendAdHocEmail(1L, "Test Subject", "Test body");

        assertNotNull(result);
        assertEquals(Notification.Type.EMAIL, result.getType());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void getUserNotifications_returnsListForUser() {
        Notification n1 = new Notification();
        n1.setUserId(1L);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(n1));

        List<Notification> result = notificationService.getUserNotifications(1L);

        assertEquals(1, result.size());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getNotification_returnsById() {
        Notification n = new Notification();
        n.setId(1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        Optional<Notification> result = notificationService.getNotification(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }
}
