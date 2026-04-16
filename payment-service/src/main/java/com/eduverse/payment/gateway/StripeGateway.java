package com.eduverse.payment.gateway;

import java.math.BigDecimal;

/**
 * Abstraction over the Stripe payment gateway.
 * Allows easy swapping between mock and real implementations.
 */
public interface StripeGateway {

    StripePaymentResult createPaymentIntent(BigDecimal amount, String currency, String idempotencyKey);

    StripeRefundResult refundPayment(String stripePaymentId);

    record StripePaymentResult(boolean success, String stripePaymentId, String errorMessage) {}

    record StripeRefundResult(boolean success, String refundId, String errorMessage) {}
}
