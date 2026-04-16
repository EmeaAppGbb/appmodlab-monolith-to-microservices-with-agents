package com.eduverse.events;

import java.math.BigDecimal;

public class PaymentRefundedEvent extends DomainEvent {

    private Long paymentId;
    private Long enrollmentId;
    private BigDecimal amount;
    private String reason;

    public PaymentRefundedEvent() {
    }

    public PaymentRefundedEvent(Long paymentId, Long enrollmentId, BigDecimal amount, String reason) {
        this.paymentId = paymentId;
        this.enrollmentId = enrollmentId;
        this.amount = amount;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "PaymentRefunded";
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "PaymentRefundedEvent{" +
                "paymentId=" + paymentId +
                ", enrollmentId=" + enrollmentId +
                ", amount=" + amount +
                ", reason='" + reason + '\'' +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
