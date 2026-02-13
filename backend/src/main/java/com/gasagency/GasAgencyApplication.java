package com.gasagency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableAspectJAutoProxy
public class GasAgencyApplication {
    public static void main(String[] args) {
        SpringApplication.run(GasAgencyApplication.class, args);
    }

}

