package com.eduverse.notification.model;

/**
 * Maps domain events to notification templates.
 * Each template defines the subject and message body pattern for a specific event type.
 */
public enum NotificationTemplate {

    STUDENT_ENROLLED(
        "Welcome to %s - EduVerse Academy",
        """
        Congratulations! You have been successfully enrolled in "%s".

        You can start learning right away by visiting your dashboard.

        Happy Learning!
        The EduVerse Academy Team"""
    ),

    PAYMENT_COMPLETED(
        "Payment Confirmed - EduVerse Academy",
        """
        Your payment of %s %s has been successfully processed.

        Payment reference: %s
        
        Thank you for your purchase!
        The EduVerse Academy Team"""
    ),

    CERTIFICATE_ISSUED(
        "Certificate Earned - EduVerse Academy",
        """
        Congratulations on earning your certificate!

        Certificate number: %s
        You can download your certificate from your dashboard.

        Keep up the great work!
        The EduVerse Academy Team"""
    );

    private final String subjectTemplate;
    private final String messageTemplate;

    NotificationTemplate(String subjectTemplate, String messageTemplate) {
        this.subjectTemplate = subjectTemplate;
        this.messageTemplate = messageTemplate;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }
}
