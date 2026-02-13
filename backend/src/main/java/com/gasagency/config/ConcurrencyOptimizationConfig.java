package com.gasagency.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.orm.jpa.JpaTransactionManager;
import jakarta.persistence.EntityManagerFactory;

/**
 * Optimized concurrency configuration for production use.
 * Ensures proper transaction management for 10-15 concurrent users.
 * 
 * HikariCP connection pooling is automatically configured via
 * application-prod.properties:
 * - Maximum pool size: 20
 * - Minimum idle: 5
 * - Connection timeout: 10 seconds
 * - Idle timeout: 5 minutes
 * - Leak detection: 60 seconds
 */
@Configuration
@EnableTransactionManagement
public class ConcurrencyOptimizationConfig {

    /**
     * Configure transaction manager with optimized settings.
     * - Global rollback on participation failure
     * - 5 minute timeout per transaction
     */
    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager txnManager = new JpaTransactionManager();
        txnManager.setEntityManagerFactory(emf);
        txnManager.setGlobalRollbackOnParticipationFailure(true);
        txnManager.setDefaultTimeout(300); // 5 minute timeout per transaction
        return txnManager;
    }
}

