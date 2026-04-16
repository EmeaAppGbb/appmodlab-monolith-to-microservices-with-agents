package com.eduverse.events;

import java.math.BigDecimal;

public class PaymentCompletedEvent extends DomainEvent {

    private Long paymentId;
    private Long enrollmentId;
    private BigDecimal amount;
    private String currency;
    private String stripePaymentId;

    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(Long paymentId, Long enrollmentId, BigDecimal amount, String currency, String stripePaymentId) {
        this.paymentId = paymentId;
        this.enrollmentId = enrollmentId;
        this.amount = amount;
        this.currency = currency;
        this.stripePaymentId = stripePaymentId;
    }

    @Override
    public String getEventType() {
        return "PaymentCompleted";
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStripePaymentId() {
        return stripePaymentId;
    }

    public void setStripePaymentId(String stripePaymentId) {
        this.stripePaymentId = stripePaymentId;
    }

    @Override
    public String toString() {
        return "PaymentCompletedEvent{" +
                "paymentId=" + paymentId +
                ", enrollmentId=" + enrollmentId +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", stripePaymentId='" + stripePaymentId + '\'' +
                ", eventId='" + getEventId() + '\'' +
                ", occurredAt=" + getOccurredAt() +
                '}';
    }
}
