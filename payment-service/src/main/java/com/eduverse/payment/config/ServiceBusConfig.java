package com.eduverse.payment.config;

import com.eduverse.payment.messaging.StudentEnrolledEventListener;
import com.azure.spring.cloud.service.servicebus.consumer.ServiceBusRecordMessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Azure Service Bus configuration for message processing.
 * Only active when a Service Bus connection string is provided.
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.azure.servicebus.connection-string", matchIfMissing = false)
public class ServiceBusConfig {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusConfig.class);

    @Bean
    public ServiceBusRecordMessageListener studentEnrolledListener(
            StudentEnrolledEventListener listener) {
        return messageContext -> {
            String body = messageContext.getMessage().getBody().toString();
            logger.debug("Service Bus message received on student-enrolled topic");
            listener.processMessage(body);
        };
    }
}
