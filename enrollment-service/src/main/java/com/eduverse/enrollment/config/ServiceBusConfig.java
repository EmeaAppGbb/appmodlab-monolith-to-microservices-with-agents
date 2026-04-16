package com.eduverse.enrollment.config;

import com.azure.spring.cloud.service.servicebus.consumer.ServiceBusErrorHandler;
import com.azure.spring.cloud.service.servicebus.consumer.ServiceBusRecordMessageListener;
import com.eduverse.enrollment.listener.ServiceBusEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Azure Service Bus message processors for domain event consumption.
 *
 * This configuration is only active when the Service Bus connection string is provided,
 * allowing the service to run locally without Azure dependencies during development.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.azure.servicebus.connection-string")
public class ServiceBusConfig {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusConfig.class);

    @Bean
    ServiceBusRecordMessageListener serviceBusRecordMessageListener(ServiceBusEventListener eventListener) {
        return messageContext -> {
            String body = messageContext.getMessage().getBody().toString();
            logger.debug("Received Service Bus message: {}", body);
            eventListener.processMessage(body);
        };
    }

    @Bean
    ServiceBusErrorHandler serviceBusErrorHandler() {
        return errorContext -> {
            logger.error("Service Bus processing error: {}",
                    errorContext.getException().getMessage(), errorContext.getException());
        };
    }
}
