package com.gasagency.controller;

import com.gasagency.dto.DashboardSummaryDTO;
import com.gasagency.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dashboard Controller
 * Provides comprehensive dashboard data for analytics and business intelligence
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Get comprehensive dashboard summary
     * 
     * @param year  optional year parameter (defaults to current year)
     * @param month optional month parameter 1-12 (defaults to current month)
     * @return DashboardSummaryDTO with complete dashboard data
     */
    @GetMapping("/comprehensive")
    public ResponseEntity<DashboardSummaryDTO> getComprehensiveDashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        logger.info("Fetching comprehensive dashboard summary for year: {}, month: {}", year, month);
        try {
            // Validate month if provided
            if (month != null && (month < 1 || month > 12)) {
                return ResponseEntity.badRequest().build();
            }

            DashboardSummaryDTO dashboard = dashboardService.getDashboardSummary(year, month);

            // Add cache control headers to prevent browser/proxy caching
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(dashboard);
        } catch (Exception e) {
            logger.error("Error fetching dashboard summary", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Dashboard service is healthy");
    }
}
