package com.eduverse.payment.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock Stripe gateway for development and testing.
 * Simulates payment processing with deterministic behavior:
 * - Amounts ending in .99 simulate failures
 * - All other amounts succeed
 */
@Component
public class MockStripeGateway implements StripeGateway {

    private static final Logger logger = LoggerFactory.getLogger(MockStripeGateway.class);

    @Override
    public StripePaymentResult createPaymentIntent(BigDecimal amount, String currency, String idempotencyKey) {
        logger.info("Mock Stripe: Creating payment intent for {} {}, idempotencyKey={}",
                amount, currency, idempotencyKey);

        // Simulate failure for amounts ending in .99
        if (amount.remainder(BigDecimal.ONE).compareTo(new BigDecimal("0.99")) == 0) {
            logger.warn("Mock Stripe: Simulating payment failure for amount {}", amount);
            return new StripePaymentResult(false, null, "Card declined (mock)");
        }

        String stripeId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        logger.info("Mock Stripe: Payment intent created: {}", stripeId);
        return new StripePaymentResult(true, stripeId, null);
    }

    @Override
    public StripeRefundResult refundPayment(String stripePaymentId) {
        logger.info("Mock Stripe: Processing refund for {}", stripePaymentId);

        String refundId = "re_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        logger.info("Mock Stripe: Refund created: {}", refundId);
        return new StripeRefundResult(true, refundId, null);
    }
}
