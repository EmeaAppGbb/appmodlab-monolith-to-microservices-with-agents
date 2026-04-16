package com.eduverse.video.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Azure Service Bus configuration for message processing.
 * Only active when a Service Bus connection string is provided.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.azure.servicebus.connection-string", matchIfMissing = false)
public class ServiceBusConfig {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusConfig.class);
}
