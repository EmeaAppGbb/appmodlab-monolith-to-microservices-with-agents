package com.eduverse.notification.listener;

import com.eduverse.events.*;
import com.eduverse.notification.model.Notification;
import com.eduverse.notification.model.NotificationEvent;
import com.eduverse.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceBusEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ServiceBusEventListener listener;

    @Test
    void processMessage_studentEnrolledEvent_createsEmailAndInApp() throws Exception {
        StudentEnrolledEvent event = new StudentEnrolledEvent(1L, 100L, 10L, "Java 101", BigDecimal.valueOf(49.99));
        String json = EventSerializer.serialize(event);

        when(notificationService.processNotificationEvent(any())).thenReturn(new Notification());

        listener.processMessage(json);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService, times(2)).processNotificationEvent(captor.capture());

        List<NotificationEvent> captured = captor.getAllValues();
        // First: EMAIL
        assertEquals(Notification.Type.EMAIL, captured.get(0).getNotificationType());
        assertTrue(captured.get(0).getSubject().contains("Java 101"));
        assertEquals(100L, captured.get(0).getUserId());
        assertEquals(event.getEventId(), captured.get(0).getEventId());

        // Second: IN_APP
        assertEquals(Notification.Type.IN_APP, captured.get(1).getNotificationType());
        assertTrue(captured.get(1).getMessage().contains("Java 101"));
    }

    @Test
    void processMessage_paymentCompletedEvent_createsNotification() throws Exception {
        PaymentCompletedEvent event = new PaymentCompletedEvent(1L, 2L, BigDecimal.valueOf(99.99), "USD", "stripe_123");
        String json = EventSerializer.serialize(event);

        when(notificationService.processNotificationEvent(any())).thenReturn(new Notification());

        listener.processMessage(json);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService).processNotificationEvent(captor.capture());

        NotificationEvent captured = captor.getValue();
        assertEquals(Notification.Type.EMAIL, captured.getNotificationType());
        assertTrue(captured.getMessage().contains("99.99"));
        assertTrue(captured.getMessage().contains("USD"));
        assertTrue(captured.getMessage().contains("stripe_123"));
    }

    @Test
    void processMessage_certificateIssuedEvent_createsEmailAndInApp() throws Exception {
        CertificateIssuedEvent event = new CertificateIssuedEvent(1L, 2L, 100L, "CERT-001", "https://example.com/cert.pdf");
        String json = EventSerializer.serialize(event);

        when(notificationService.processNotificationEvent(any())).thenReturn(new Notification());

        listener.processMessage(json);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService, times(2)).processNotificationEvent(captor.capture());

        List<NotificationEvent> captured = captor.getAllValues();
        assertEquals(Notification.Type.EMAIL, captured.get(0).getNotificationType());
        assertTrue(captured.get(0).getMessage().contains("CERT-001"));
        assertEquals(100L, captured.get(0).getUserId());

        assertEquals(Notification.Type.IN_APP, captured.get(1).getNotificationType());
    }

    @Test
    void processMessage_invalidJson_throwsException() {
        assertThrows(RuntimeException.class, () -> listener.processMessage("invalid json"));
    }

    @Test
    void handleStudentEnrolled_setsCorrectEventMetadata() {
        StudentEnrolledEvent event = new StudentEnrolledEvent(1L, 100L, 10L, "Python 201", BigDecimal.ZERO);
        event.setCorrelationId("corr-789");

        when(notificationService.processNotificationEvent(any())).thenReturn(new Notification());

        listener.handleStudentEnrolled(event);

        ArgumentCaptor<NotificationEvent> captor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(notificationService, times(2)).processNotificationEvent(captor.capture());

        NotificationEvent emailEvent = captor.getAllValues().get(0);
        assertEquals("StudentEnrolled", emailEvent.getSourceEventType());
        assertEquals("corr-789", emailEvent.getCorrelationId());
        assertEquals(event.getEventId(), emailEvent.getEventId());
    }
}
