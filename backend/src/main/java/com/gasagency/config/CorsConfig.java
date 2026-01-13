package com.gasagency.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:Content-Type,Authorization}")
    private String allowedHeaders;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Value("${app.cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        var corsConfig = registry.addMapping("/api/**")
                .allowedMethods(allowedMethods.split(","))
                .allowedHeaders(allowedHeaders.split(","))
                .maxAge(maxAge)
                .allowCredentials(allowCredentials);

        // Use allowedOriginPatterns if credentials are enabled to avoid wildcard
        // conflicts
        if (allowCredentials) {
            corsConfig.allowedOriginPatterns(allowedOrigins.split(","));
        } else {
            corsConfig.allowedOrigins(allowedOrigins.split(","));
        }
    }
}
