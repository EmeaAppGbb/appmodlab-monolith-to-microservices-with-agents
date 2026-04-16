package com.eduverse.events;

public class PaymentFailedEvent extends DomainEvent {

    private Long paymentId;
    private Long enrollmentId;
    private String reason;

    public PaymentFailedEvent() {
    }

    public PaymentFailedEvent(Long paymentId, Long enrollmentId, String reason) {
        this.paymentId = paymentId;
        this.enrollmentId = enrollmentId;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "PaymentFailed";
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "PaymentFailedEvent{" +
                "paymentId=" + paymentId +
                ", enrollmentId=" + enrollmentId +
                ", reason='" + reason + '\'' +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
