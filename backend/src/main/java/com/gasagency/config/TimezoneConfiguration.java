package com.gasagency.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Configuration for handling Indian Standard Time (IST - UTC+5:30) consistently
 * across the application for serialization and deserialization
 */
@Configuration
public class TimezoneConfiguration {

    // Date and DateTime formats consistent with frontend (IST)
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    // Set the application timezone to IST
    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    /**
     * Configure Jackson ObjectMapper for proper datetime serialization in IST
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Set timezone to Asia/Kolkata (IST)
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        // Disable timestamps, use ISO-8601 string format
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Ignore unknown fields in JSON to handle frontend/backend field mismatches
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // Register JavaTimeModule for LocalDateTime, LocalDate handling
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // Configure LocalDate serialization/deserialization
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE_FORMAT)));

        // Configure LocalDateTime serialization/deserialization
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)));

        mapper.registerModule(javaTimeModule);

        return mapper;
    }

    /**
     * Configure Jackson2ObjectMapperBuilder for consistent handling across the
     * application
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

        // Set timezone
        builder.timeZone(TimeZone.getTimeZone("Asia/Kolkata"));

        // Disable timestamp serialization
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Set datetime formats
        builder.simpleDateFormat(DATE_TIME_FORMAT);

        return builder;
    }
}
