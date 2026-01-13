package com.gasagency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class GasAgencyApplication {
    public static void main(String[] args) {
        SpringApplication.run(GasAgencyApplication.class, args);
    }

}
