package com.gasagency.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig - Registers web-related beans and interceptors
 * Configures request/response logging and other web middleware
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RequestResponseLoggingInterceptor requestResponseLoggingInterceptor;

    /**
     * Register interceptors for request/response logging
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestResponseLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/health/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**");
    }
}

