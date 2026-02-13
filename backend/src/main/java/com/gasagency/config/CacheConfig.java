package com.gasagency.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import java.util.concurrent.TimeUnit;

/**
 * Performance Optimization Configuration: Caching
 * 
 * Implements multi-level caching strategy for high-concurrency environments:
 * - Caffeine in-memory cache for dashboard, reports, and reference data
 * - Query result caching to prevent redundant database hits
 * - Entity-level caching for frequently accessed reference data
 * 
 * Configured for 10-15 concurrent users with fast response times
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine Cache Manager with optimized settings for concurrency
     * 
     * Cache Sizes:
     * - Reference data (variants, suppliers, etc): 1000 entries
     * - Dashboard/Reports: 100 entries
     * - Customer data: 500 entries
     * 
     * Expiration Policies:
     * - Reference data: 30 minutes (cache warm data that changes infrequently)
     * - Reports: 5 minutes (balance between freshness and performance)
     * - Dashboard: 2 minutes (real-time dashboard updates)
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "referenceData",
                "dashboardCache",
                "reportCache",
                "customerCache",
                "inventoryCache",
                "priceCache",
                "alertConfigCache",
                // Business info
                "businessInfo",
                "businessInfoById",
                // Cylinder variants
                "cylinderVariantsAll",
                "cylinderVariantsActive",
                "variantById",
                // Users
                "usersActive",
                "usersAll",
                // Warehouses
                "warehousesAll",
                "warehousesActive",
                "warehouseById",
                "warehouseByName",
                // Expense categories
                "expenseCategoriesActive",
                "expenseCategoryNames",
                // Bank accounts
                "bankAccountsActive",
                // Monthly prices
                "monthlyPricesAll",
                "monthlyPricesByVariant",
                "monthlyPriceForVariantMonth",
                "monthlyPriceLatestForVariant",
                // Suppliers
                "suppliersAll",
                // Payment modes
                "paymentModesActive",
                "paymentModeNames"
        );

        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000) // Maximum total cache size
                .expireAfterWrite(10, TimeUnit.MINUTES) // Default expiration
                .recordStats()); // Enable cache statistics for monitoring

        return cacheManager;
    }

    /**
     * Reference data cache - 30 minute TTL
     * Used for: CylinderVariant, Supplier, PaymentMode, ExpenseCategory
     */
    @Bean
    public CaffeineCacheManager referenceDataCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("referenceData");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * Dashboard cache - 2 minute TTL
     * For real-time metrics and summary data
     */
    @Bean
    public CaffeineCacheManager dashboardCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("dashboardCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * Report cache - 5 minute TTL
     * For due payment reports, sales summaries, expense reports
     */
    @Bean
    public CaffeineCacheManager reportCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("reportCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * Customer cache - 15 minute TTL
     * For customer details and customer-specific summaries
     */
    @Bean
    public CaffeineCacheManager customerCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("customerCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * Inventory cache - 10 minute TTL
     * For warehouse and stock information
     */
    @Bean
    public CaffeineCacheManager inventoryCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("inventoryCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * Price cache - 60 minute TTL
     * Monthly prices change infrequently, so longer TTL is safe
     */
    @Bean
    public CaffeineCacheManager priceCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("priceCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }

    /**
     * Alert configuration cache - 30 minute TTL
     * Alert settings are updated infrequently, but should refresh periodically
     */
    @Bean
    public CaffeineCacheManager alertConfigCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("alertConfigCache");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}

