package com.eduverse.catalog.config;

import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.eduverse.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Azure Service Bus sender for publishing domain events.
 * Only active when a Service Bus connection string is provided,
 * allowing local development without Azure dependencies.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.azure.servicebus.connection-string")
public class ServiceBusPublisherConfig {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusPublisherConfig.class);

    @Bean
    ServiceBusSenderClient coursePublishedSender(
            @Value("${spring.cloud.azure.servicebus.connection-string}") String connectionString) {
        logger.info("Configuring Service Bus sender for topic: {}", Topics.COURSE_PUBLISHED);
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .topicName(Topics.COURSE_PUBLISHED)
                .buildClient();
    }
}
