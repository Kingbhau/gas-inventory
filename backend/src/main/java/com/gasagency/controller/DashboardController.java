package com.gasagency.controller;

import com.gasagency.dto.response.DashboardSummaryDTO;
import com.gasagency.service.DashboardService;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
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
    public ResponseEntity<ApiResponse<DashboardSummaryDTO>> getComprehensiveDashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        logger.info("Fetching comprehensive dashboard summary for year: {}, month: {}", year, month);
        try {
            // Validate month if provided
            if (month != null && (month < 1 || month > 12)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseUtil.error("Invalid month parameter", "INVALID_ARGUMENT"));
            }

            DashboardSummaryDTO dashboard = dashboardService.getDashboardSummary(year, month);

            // Add cache control headers to prevent browser/proxy caching
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(ApiResponseUtil.success("Dashboard summary retrieved successfully", dashboard));
        } catch (Exception e) {
            logger.error("Error fetching dashboard summary", e);
            return ResponseEntity.status(500)
                    .body(ApiResponseUtil.error("Failed to fetch dashboard summary", "DASHBOARD_FETCH_FAILED"));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponseUtil.success("Dashboard service is healthy"));
    }
}

