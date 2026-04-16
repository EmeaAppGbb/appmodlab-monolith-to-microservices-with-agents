package com.eduverse.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class EventSerializer {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private EventSerializer() {
    }

    public static String serialize(DomainEvent event) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(event);
    }

    public static <T extends DomainEvent> T deserialize(String json, Class<T> type) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, type);
    }

    public static DomainEvent deserialize(String json) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, DomainEvent.class);
    }
}
