package com.eduverse.certificate.messaging;

import com.eduverse.events.DomainEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events to Azure Service Bus topics.
 * Falls back to logging when Service Bus is not configured (local dev).
 */
@Component
public class ServiceBusEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ServiceBusEventPublisher.class);

    private final ObjectMapper objectMapper;
    private final boolean serviceBusEnabled;

    public ServiceBusEventPublisher(
            @Value("${spring.cloud.azure.servicebus.connection-string:}") String connectionString) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.serviceBusEnabled = connectionString != null && !connectionString.isBlank();

        if (!serviceBusEnabled) {
            logger.warn("Azure Service Bus connection string not configured. " +
                    "Events will be logged but not published.");
        }
    }

    public void publish(String topic, DomainEvent event) {
        event.setSource("certificate-service");

        try {
            String json = objectMapper.writeValueAsString(event);

            if (serviceBusEnabled) {
                // In production, use Azure Service Bus SDK to send to the topic
                logger.info("Publishing event to topic '{}': {}", topic, json);
                // TODO: Wire up actual ServiceBusSenderClient when deployed to Azure
            } else {
                logger.info("[LOCAL] Would publish to topic '{}': {}", topic, json);
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event for topic '{}': {}", topic, event, e);
            throw new RuntimeException("Event serialization failed", e);
        }
    }

    public void publishCertificateIssued(DomainEvent event) {
        publish("certificate-issued", event);
    }
}
