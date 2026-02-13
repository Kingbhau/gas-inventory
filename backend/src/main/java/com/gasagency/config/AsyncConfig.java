package com.gasagency.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for Concurrent Operations
 * 
 * Enables parallel processing for independent tasks:
 * - Dashboard service calculations run in parallel
 * - Report generation doesn't block request threads
 * - Multiple service calls execute concurrently
 * 
 * Thread Pool Tuning for 10-15 concurrent users:
 * - Core threads: 20 (handle base load)
 * - Max threads: 100 (handle bursts)
 * - Queue capacity: 500 (buffer for spiky traffic)
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Main async executor for dashboard and report generation
     * 
     * Strategy: Use virtual threads + task batching for optimal concurrency
     * - Core pool: 20 threads (small, to avoid over-provisioning)
     * - Max pool: 100 threads (burst capacity)
     * - Queue: 500 tasks (buffer before rejection)
     * - Keep alive: 60s (clean up unused threads)
     */
    @Bean(name = "dashboardExecutor")
    public Executor dashboardExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Thread pool sizing
        executor.setCorePoolSize(20); // Always keep 20 threads ready
        executor.setMaxPoolSize(100); // Can spawn up to 100 threads
        executor.setQueueCapacity(500); // Buffer queue for tasks

        // Thread lifecycle
        executor.setKeepAliveSeconds(60); // Kill idle threads after 60s
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // Rejection policy: When queue is full, caller waits (CallerRunsPolicy)
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

        executor.setThreadNamePrefix("dashboard-async-");
        executor.initialize();
        return executor;
    }

    /**
     * Executor for I/O-bound operations (database queries)
     * 
     * Can be more aggressive since threads will block on I/O
     */
    @Bean(name = "ioExecutor")
    public Executor ioExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(30);
        executor.setMaxPoolSize(150);
        executor.setQueueCapacity(1000);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("io-async-");
        executor.initialize();
        return executor;
    }

    /**
     * Executor for CPU-bound operations
     * 
     * Limited to CPU count to avoid context switching overhead
     * Typically: cores * 2 for optimal performance
     */
    @Bean(name = "cpuExecutor")
    public Executor cpuExecutor() {
        int cpuCount = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(cpuCount);
        executor.setMaxPoolSize(cpuCount * 2);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(
                new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("cpu-async-");
        executor.initialize();
        return executor;
    }
}

