package com.megacorp.humanresources.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateLenientDeserializer());
        mapper.registerModule(javaTimeModule);
        return mapper;
    }

    public static class LocalDateLenientDeserializer extends JsonDeserializer<LocalDate> {
        @Override
        public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getText();
            if (value == null) return null;
            value = value.trim();
            // If value is quoted or not, try to parse
            try {
                return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                // Try parsing as unquoted (e.g., 2018-09-29)
                try {
                    return LocalDate.parse(value.replaceAll("\"", ""), DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception ignored) {
                    throw ctxt.weirdStringException(value, LocalDate.class, "Cannot deserialize to LocalDate: " + value);
                }
            }
        }
    }
}
