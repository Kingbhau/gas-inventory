package com.gasagency.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive Dashboard Summary DTO
 * Contains all metrics for today, current month, previous months, and trends
 */
public class DashboardSummaryDTO {

    // ==================== TODAY'S DATA ====================
    private BigDecimal todayTotalSales;
    private BigDecimal todayTotalExpenses;
    private BigDecimal todayNetProfit;
    private Double todayProfitMargin;
    private Integer todaySalesCount;
    private BigDecimal todayCashCollected;
    private Double todayCollectionRate;

    // Inventory Today
    private Integer todayCylindersFilled;
    private Integer todayCylindersEmpty;
    private Integer todayCylindersTotal;
    private Double todayInventoryHealth; // Percentage of filled cylinders

    // Collections Today
    private Integer todayReturnsPending;
    private BigDecimal todayAmountDue;
    private Integer todayProblematicCustomers; // Customers with > 30 days pending

    // ==================== CURRENT MONTH DATA ====================
    private BigDecimal monthlyTotalSales;
    private BigDecimal monthlyTotalExpenses;
    private BigDecimal monthlyNetProfit;
    private Double monthlyProfitMargin;
    private Integer monthlySalesCount;
    private Double monthlyCollectionRate;

    // Monthly Projections
    private BigDecimal monthlyProjectedSales; // Extrapolated for full month
    private BigDecimal monthlyProjectedProfit;
    private Integer daysInMonth;
    private Integer daysCompleted;

    // Monthly Inventory Activity
    private Integer monthlyCylindersSold;
    private Integer monthlyCylindersReturned;
    private Double monthlyInventoryTurnover; // Times per month

    // ==================== PREVIOUS MONTHS DATA ====================
    private List<MonthlyMetricsDTO> previousMonthsMetrics; // Last 6 months
    private Double monthOverMonthGrowth; // Percentage growth vs last month
    private Double yearOverYearGrowth; // Percentage growth vs same month last year

    // ==================== SUMMARY DETAILS ====================
    private String topCustomerToday;
    private String topCategoryExpenseMonthly;
    private List<CustomerDuePaymentDTO> topDebtors; // Top 5 customers with highest dues
    private List<CustomerCylinderLedgerDTO> pendingReturnsDetail; // Top pending returns

    // ==================== CHARTS DATA ====================
    private Map<String, BigDecimal> expenseCategoryBreakdown; // For pie chart
    private List<DailySalesDataDTO> monthlySalesTrend; // For line chart (daily)
    private List<VariantSalesDTO> variantSalesBreakdown; // For bar chart
    private List<InventoryHealthDTO> inventoryByWarehouse; // For bar chart

    // ==================== BUSINESS INSIGHTS ====================
    private BusinessInsightsDTO businessInsights = new BusinessInsightsDTO();
    private String salesTrend; // "UP", "DOWN", "STABLE"
    private String profitTrend;
    private String inventoryTrend;
    private String collectionTrend;
    private Double averageDailySales; // Monthly average
    private Double averageDailyExpense;
    private Integer totalActiveCustomers;
    private Integer customersWithNoDues;
    private Integer customersWithSlowPayment; // 8-30 days
    private Integer customersWithOverduePayment; // > 30 days

    // ==================== ALERTS ====================
    private List<DashboardAlertDTO> alerts; // Critical alerts for owner

    // ==================== CONSTRUCTORS ====================
    public DashboardSummaryDTO() {
    }

    // ==================== GETTERS & SETTERS ====================

    // Today's Data Getters & Setters
    public BigDecimal getTodayTotalSales() {
        return todayTotalSales;
    }

    public void setTodayTotalSales(BigDecimal todayTotalSales) {
        this.todayTotalSales = todayTotalSales;
    }

    public BigDecimal getTodayTotalExpenses() {
        return todayTotalExpenses;
    }

    public void setTodayTotalExpenses(BigDecimal todayTotalExpenses) {
        this.todayTotalExpenses = todayTotalExpenses;
    }

    public BigDecimal getTodayNetProfit() {
        return todayNetProfit;
    }

    public void setTodayNetProfit(BigDecimal todayNetProfit) {
        this.todayNetProfit = todayNetProfit;
    }

    public Double getTodayProfitMargin() {
        return todayProfitMargin;
    }

    public void setTodayProfitMargin(Double todayProfitMargin) {
        this.todayProfitMargin = todayProfitMargin;
    }

    public Integer getTodaySalesCount() {
        return todaySalesCount;
    }

    public void setTodaySalesCount(Integer todaySalesCount) {
        this.todaySalesCount = todaySalesCount;
    }

    public BigDecimal getTodayCashCollected() {
        return todayCashCollected;
    }

    public void setTodayCashCollected(BigDecimal todayCashCollected) {
        this.todayCashCollected = todayCashCollected;
    }

    public Double getTodayCollectionRate() {
        return todayCollectionRate;
    }

    public void setTodayCollectionRate(Double todayCollectionRate) {
        this.todayCollectionRate = todayCollectionRate;
    }

    public Integer getTodayCylindersFilled() {
        return todayCylindersFilled;
    }

    public void setTodayCylindersFilled(Integer todayCylindersFilled) {
        this.todayCylindersFilled = todayCylindersFilled;
    }

    public Integer getTodayCylindersEmpty() {
        return todayCylindersEmpty;
    }

    public void setTodayCylindersEmpty(Integer todayCylindersEmpty) {
        this.todayCylindersEmpty = todayCylindersEmpty;
    }

    public Integer getTodayCylindersTotal() {
        return todayCylindersTotal;
    }

    public void setTodayCylindersTotal(Integer todayCylindersTotal) {
        this.todayCylindersTotal = todayCylindersTotal;
    }

    public Double getTodayInventoryHealth() {
        return todayInventoryHealth;
    }

    public void setTodayInventoryHealth(Double todayInventoryHealth) {
        this.todayInventoryHealth = todayInventoryHealth;
    }

    public Integer getTodayReturnsPending() {
        return todayReturnsPending;
    }

    public void setTodayReturnsPending(Integer todayReturnsPending) {
        this.todayReturnsPending = todayReturnsPending;
    }

    public BigDecimal getTodayAmountDue() {
        return todayAmountDue;
    }

    public void setTodayAmountDue(BigDecimal todayAmountDue) {
        this.todayAmountDue = todayAmountDue;
    }

    public Integer getTodayProblematicCustomers() {
        return todayProblematicCustomers;
    }

    public void setTodayProblematicCustomers(Integer todayProblematicCustomers) {
        this.todayProblematicCustomers = todayProblematicCustomers;
    }

    // Monthly Data Getters & Setters
    public BigDecimal getMonthlyTotalSales() {
        return monthlyTotalSales;
    }

    public void setMonthlyTotalSales(BigDecimal monthlyTotalSales) {
        this.monthlyTotalSales = monthlyTotalSales;
    }

    public BigDecimal getMonthlyTotalExpenses() {
        return monthlyTotalExpenses;
    }

    public void setMonthlyTotalExpenses(BigDecimal monthlyTotalExpenses) {
        this.monthlyTotalExpenses = monthlyTotalExpenses;
    }

    public BigDecimal getMonthlyNetProfit() {
        return monthlyNetProfit;
    }

    public void setMonthlyNetProfit(BigDecimal monthlyNetProfit) {
        this.monthlyNetProfit = monthlyNetProfit;
    }

    public Double getMonthlyProfitMargin() {
        return monthlyProfitMargin;
    }

    public void setMonthlyProfitMargin(Double monthlyProfitMargin) {
        this.monthlyProfitMargin = monthlyProfitMargin;
    }

    public Integer getMonthlySalesCount() {
        return monthlySalesCount;
    }

    public void setMonthlySalesCount(Integer monthlySalesCount) {
        this.monthlySalesCount = monthlySalesCount;
    }

    public Double getMonthlyCollectionRate() {
        return monthlyCollectionRate;
    }

    public void setMonthlyCollectionRate(Double monthlyCollectionRate) {
        this.monthlyCollectionRate = monthlyCollectionRate;
    }

    public BigDecimal getMonthlyProjectedSales() {
        return monthlyProjectedSales;
    }

    public void setMonthlyProjectedSales(BigDecimal monthlyProjectedSales) {
        this.monthlyProjectedSales = monthlyProjectedSales;
    }

    public BigDecimal getMonthlyProjectedProfit() {
        return monthlyProjectedProfit;
    }

    public void setMonthlyProjectedProfit(BigDecimal monthlyProjectedProfit) {
        this.monthlyProjectedProfit = monthlyProjectedProfit;
    }

    public Integer getDaysInMonth() {
        return daysInMonth;
    }

    public void setDaysInMonth(Integer daysInMonth) {
        this.daysInMonth = daysInMonth;
    }

    public Integer getDaysCompleted() {
        return daysCompleted;
    }

    public void setDaysCompleted(Integer daysCompleted) {
        this.daysCompleted = daysCompleted;
    }

    public Integer getMonthlyCylindersSold() {
        return monthlyCylindersSold;
    }

    public void setMonthlyCylindersSold(Integer monthlyCylindersSold) {
        this.monthlyCylindersSold = monthlyCylindersSold;
    }

    public Integer getMonthlyCylindersReturned() {
        return monthlyCylindersReturned;
    }

    public void setMonthlyCylindersReturned(Integer monthlyCylindersReturned) {
        this.monthlyCylindersReturned = monthlyCylindersReturned;
    }

    public Double getMonthlyInventoryTurnover() {
        return monthlyInventoryTurnover;
    }

    public void setMonthlyInventoryTurnover(Double monthlyInventoryTurnover) {
        this.monthlyInventoryTurnover = monthlyInventoryTurnover;
    }

    // Previous Months Data Getters & Setters
    public List<MonthlyMetricsDTO> getPreviousMonthsMetrics() {
        return previousMonthsMetrics;
    }

    public void setPreviousMonthsMetrics(List<MonthlyMetricsDTO> previousMonthsMetrics) {
        this.previousMonthsMetrics = previousMonthsMetrics;
    }

    public Double getMonthOverMonthGrowth() {
        return monthOverMonthGrowth;
    }

    public void setMonthOverMonthGrowth(Double monthOverMonthGrowth) {
        this.monthOverMonthGrowth = monthOverMonthGrowth;
    }

    public Double getYearOverYearGrowth() {
        return yearOverYearGrowth;
    }

    public void setYearOverYearGrowth(Double yearOverYearGrowth) {
        this.yearOverYearGrowth = yearOverYearGrowth;
    }

    // Summary Details Getters & Setters
    public String getTopCustomerToday() {
        return topCustomerToday;
    }

    public void setTopCustomerToday(String topCustomerToday) {
        this.topCustomerToday = topCustomerToday;
    }

    public String getTopCategoryExpenseMonthly() {
        return topCategoryExpenseMonthly;
    }

    public void setTopCategoryExpenseMonthly(String topCategoryExpenseMonthly) {
        this.topCategoryExpenseMonthly = topCategoryExpenseMonthly;
    }

    public List<CustomerDuePaymentDTO> getTopDebtors() {
        return topDebtors;
    }

    public void setTopDebtors(List<CustomerDuePaymentDTO> topDebtors) {
        this.topDebtors = topDebtors;
    }

    public List<CustomerCylinderLedgerDTO> getPendingReturnsDetail() {
        return pendingReturnsDetail;
    }

    public void setPendingReturnsDetail(List<CustomerCylinderLedgerDTO> pendingReturnsDetail) {
        this.pendingReturnsDetail = pendingReturnsDetail;
    }

    // Charts Data Getters & Setters
    public Map<String, BigDecimal> getExpenseCategoryBreakdown() {
        return expenseCategoryBreakdown;
    }

    public void setExpenseCategoryBreakdown(Map<String, BigDecimal> expenseCategoryBreakdown) {
        this.expenseCategoryBreakdown = expenseCategoryBreakdown;
    }

    public List<DailySalesDataDTO> getMonthlySalesTrend() {
        return monthlySalesTrend;
    }

    public void setMonthlySalesTrend(List<DailySalesDataDTO> monthlySalesTrend) {
        this.monthlySalesTrend = monthlySalesTrend;
    }

    public List<VariantSalesDTO> getVariantSalesBreakdown() {
        return variantSalesBreakdown;
    }

    public void setVariantSalesBreakdown(List<VariantSalesDTO> variantSalesBreakdown) {
        this.variantSalesBreakdown = variantSalesBreakdown;
    }

    public List<InventoryHealthDTO> getInventoryByWarehouse() {
        return inventoryByWarehouse;
    }

    public void setInventoryByWarehouse(List<InventoryHealthDTO> inventoryByWarehouse) {
        this.inventoryByWarehouse = inventoryByWarehouse;
    }

    // Business Insights Getters & Setters
    public String getSalesTrend() {
        return salesTrend;
    }

    public void setSalesTrend(String salesTrend) {
        this.salesTrend = salesTrend;
    }

    public String getProfitTrend() {
        return profitTrend;
    }

    public void setProfitTrend(String profitTrend) {
        this.profitTrend = profitTrend;
    }

    public String getInventoryTrend() {
        return inventoryTrend;
    }

    public void setInventoryTrend(String inventoryTrend) {
        this.inventoryTrend = inventoryTrend;
    }

    public String getCollectionTrend() {
        return collectionTrend;
    }

    public void setCollectionTrend(String collectionTrend) {
        this.collectionTrend = collectionTrend;
    }

    public Double getAverageDailySales() {
        return averageDailySales;
    }

    public void setAverageDailySales(Double averageDailySales) {
        this.averageDailySales = averageDailySales;
    }

    public Double getAverageDailyExpense() {
        return averageDailyExpense;
    }

    public void setAverageDailyExpense(Double averageDailyExpense) {
        this.averageDailyExpense = averageDailyExpense;
    }

    public Integer getTotalActiveCustomers() {
        return totalActiveCustomers;
    }

    public void setTotalActiveCustomers(Integer totalActiveCustomers) {
        this.totalActiveCustomers = totalActiveCustomers;
    }

    public Integer getCustomersWithNoDues() {
        return customersWithNoDues;
    }

    public void setCustomersWithNoDues(Integer customersWithNoDues) {
        this.customersWithNoDues = customersWithNoDues;
    }

    public BusinessInsightsDTO getBusinessInsights() {
        return businessInsights;
    }

    public void setBusinessInsights(BusinessInsightsDTO businessInsights) {
        this.businessInsights = businessInsights;
    }

    public Integer getCustomersWithSlowPayment() {
        return customersWithSlowPayment;
    }

    public void setCustomersWithSlowPayment(Integer customersWithSlowPayment) {
        this.customersWithSlowPayment = customersWithSlowPayment;
    }

    public Integer getCustomersWithOverduePayment() {
        return customersWithOverduePayment;
    }

    public void setCustomersWithOverduePayment(Integer customersWithOverduePayment) {
        this.customersWithOverduePayment = customersWithOverduePayment;
    }

    // Alerts Getters & Setters
    public List<DashboardAlertDTO> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<DashboardAlertDTO> alerts) {
        this.alerts = alerts;
    }

    // ==================== NESTED HELPER DTOS ====================

    /**
     * Monthly Metrics for previous months
     */
    public static class MonthlyMetricsDTO {
        private String month;
        private Integer year;
        private BigDecimal totalSales;
        private BigDecimal totalExpenses;
        private BigDecimal netProfit;
        private Double profitMargin;
        private Double growthPercentage; // vs previous month

        public MonthlyMetricsDTO() {
        }

        public MonthlyMetricsDTO(String month, Integer year, BigDecimal totalSales,
                BigDecimal totalExpenses, BigDecimal netProfit, Double profitMargin, Double growthPercentage) {
            this.month = month;
            this.year = year;
            this.totalSales = totalSales;
            this.totalExpenses = totalExpenses;
            this.netProfit = netProfit;
            this.profitMargin = profitMargin;
            this.growthPercentage = growthPercentage;
        }

        // Getters & Setters
        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public Integer getYear() {
            return year;
        }

        public void setYear(Integer year) {
            this.year = year;
        }

        public BigDecimal getTotalSales() {
            return totalSales;
        }

        public void setTotalSales(BigDecimal totalSales) {
            this.totalSales = totalSales;
        }

        public BigDecimal getTotalExpenses() {
            return totalExpenses;
        }

        public void setTotalExpenses(BigDecimal totalExpenses) {
            this.totalExpenses = totalExpenses;
        }

        public BigDecimal getNetProfit() {
            return netProfit;
        }

        public void setNetProfit(BigDecimal netProfit) {
            this.netProfit = netProfit;
        }

        public Double getProfitMargin() {
            return profitMargin;
        }

        public void setProfitMargin(Double profitMargin) {
            this.profitMargin = profitMargin;
        }

        public Double getGrowthPercentage() {
            return growthPercentage;
        }

        public void setGrowthPercentage(Double growthPercentage) {
            this.growthPercentage = growthPercentage;
        }
    }

    /**
     * Daily Sales Data for line chart
     */
    public static class DailySalesDataDTO {
        private String date;
        private Integer day;
        private BigDecimal sales;
        private BigDecimal expenses;
        private BigDecimal profit;

        public DailySalesDataDTO() {
        }

        public DailySalesDataDTO(String date, Integer day, BigDecimal sales, BigDecimal expenses, BigDecimal profit) {
            this.date = date;
            this.day = day;
            this.sales = sales;
            this.expenses = expenses;
            this.profit = profit;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Integer getDay() {
            return day;
        }

        public void setDay(Integer day) {
            this.day = day;
        }

        public BigDecimal getSales() {
            return sales;
        }

        public void setSales(BigDecimal sales) {
            this.sales = sales;
        }

        public BigDecimal getExpenses() {
            return expenses;
        }

        public void setExpenses(BigDecimal expenses) {
            this.expenses = expenses;
        }

        public BigDecimal getProfit() {
            return profit;
        }

        public void setProfit(BigDecimal profit) {
            this.profit = profit;
        }
    }

    /**
     * Variant Sales for bar chart
     */
    public static class VariantSalesDTO {
        private String variantName;
        private Integer quantity;
        private BigDecimal amount;
        private Double percentage;

        public VariantSalesDTO() {
        }

        public VariantSalesDTO(String variantName, Integer quantity, BigDecimal amount, Double percentage) {
            this.variantName = variantName;
            this.quantity = quantity;
            this.amount = amount;
            this.percentage = percentage;
        }

        public String getVariantName() {
            return variantName;
        }

        public void setVariantName(String variantName) {
            this.variantName = variantName;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Double getPercentage() {
            return percentage;
        }

        public void setPercentage(Double percentage) {
            this.percentage = percentage;
        }
    }

    /**
     * Inventory Health for warehouse comparison
     */
    public static class InventoryHealthDTO {
        private String warehouseName;
        private Integer filledCount;
        private Integer emptyCount;
        private Integer totalCount;
        private Double healthPercentage;

        public InventoryHealthDTO() {
        }

        public InventoryHealthDTO(String warehouseName, Integer filledCount, Integer emptyCount, Integer totalCount,
                Double healthPercentage) {
            this.warehouseName = warehouseName;
            this.filledCount = filledCount;
            this.emptyCount = emptyCount;
            this.totalCount = totalCount;
            this.healthPercentage = healthPercentage;
        }

        public String getWarehouseName() {
            return warehouseName;
        }

        public void setWarehouseName(String warehouseName) {
            this.warehouseName = warehouseName;
        }

        public Integer getFilledCount() {
            return filledCount;
        }

        public void setFilledCount(Integer filledCount) {
            this.filledCount = filledCount;
        }

        public Integer getEmptyCount() {
            return emptyCount;
        }

        public void setEmptyCount(Integer emptyCount) {
            this.emptyCount = emptyCount;
        }

        public Integer getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }

        public Double getHealthPercentage() {
            return healthPercentage;
        }

        public void setHealthPercentage(Double healthPercentage) {
            this.healthPercentage = healthPercentage;
        }
    }

    /**
     * Dashboard Alerts
     */
    public static class DashboardAlertDTO {
        private String severity; // "critical", "warning", "info"
        private String title;
        private String message;
        private String actionLabel;
        private String actionLink;

        public DashboardAlertDTO() {
        }

        public DashboardAlertDTO(String severity, String title, String message, String actionLabel, String actionLink) {
            this.severity = severity;
            this.title = title;
            this.message = message;
            this.actionLabel = actionLabel;
            this.actionLink = actionLink;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getActionLabel() {
            return actionLabel;
        }

        public void setActionLabel(String actionLabel) {
            this.actionLabel = actionLabel;
        }

        public String getActionLink() {
            return actionLink;
        }

        public void setActionLink(String actionLink) {
            this.actionLink = actionLink;
        }
    }

    /**
     * Customer Due Payment Details
     */
    public static class CustomerDuePaymentDTO {
        private String customerName;
        private BigDecimal dueAmount;

        public CustomerDuePaymentDTO() {
        }

        public CustomerDuePaymentDTO(String customerName, BigDecimal dueAmount) {
            this.customerName = customerName;
            this.dueAmount = dueAmount;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public BigDecimal getDueAmount() {
            return dueAmount;
        }

        public void setDueAmount(BigDecimal dueAmount) {
            this.dueAmount = dueAmount;
        }
    }

    /**
     * Business Insights
     */
    public static class BusinessInsightsDTO {
        private Double monthlyGrowthPercentage;
        private Double averageCollectionRate;
        private Double averageOrderValue;
        private Double inventoryTurnoverRate;
        private String topSellingVariant;
        private Double avgProfitMargin;

        public BusinessInsightsDTO() {
        }

        public Double getMonthlyGrowthPercentage() {
            return monthlyGrowthPercentage;
        }

        public void setMonthlyGrowthPercentage(Double monthlyGrowthPercentage) {
            this.monthlyGrowthPercentage = monthlyGrowthPercentage;
        }

        public Double getAverageCollectionRate() {
            return averageCollectionRate;
        }

        public void setAverageCollectionRate(Double averageCollectionRate) {
            this.averageCollectionRate = averageCollectionRate;
        }

        public Double getAverageOrderValue() {
            return averageOrderValue;
        }

        public void setAverageOrderValue(Double averageOrderValue) {
            this.averageOrderValue = averageOrderValue;
        }

        public Double getInventoryTurnoverRate() {
            return inventoryTurnoverRate;
        }

        public void setInventoryTurnoverRate(Double inventoryTurnoverRate) {
            this.inventoryTurnoverRate = inventoryTurnoverRate;
        }

        public String getTopSellingVariant() {
            return topSellingVariant;
        }

        public void setTopSellingVariant(String topSellingVariant) {
            this.topSellingVariant = topSellingVariant;
        }

        public Double getAvgProfitMargin() {
            return avgProfitMargin;
        }

        public void setAvgProfitMargin(Double avgProfitMargin) {
            this.avgProfitMargin = avgProfitMargin;
        }
    }
}



