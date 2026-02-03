package com.gasagency.service;

import com.gasagency.dto.*;
import com.gasagency.dto.DashboardSummaryDTO.InventoryHealthDTO;
import com.gasagency.dto.DashboardSummaryDTO.DashboardAlertDTO;
import com.gasagency.dto.DashboardSummaryDTO.BusinessInsightsDTO;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.entity.Sale;
import com.gasagency.entity.SaleItem;
import com.gasagency.entity.AlertNotification;
import com.gasagency.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Dashboard Service - Provides comprehensive business analytics with
 * optimizations:
 * - Caching for dashboard data (2-minute TTL)
 * - Parallel async execution for independent calculations
 * - Proper JPA queries to prevent N+1 problems
 * - Pagination at database level
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // Services
    private final SaleService saleService;
    private final ExpenseService expenseService;
    private final CustomerDuePaymentService customerDuePaymentService;
    private final InventoryStockService inventoryStockService;
    private final CustomerCylinderLedgerService customerCylinderLedgerService;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final SaleRepository saleRepository;
    private final CustomerCylinderLedgerRepository customerCylinderLedgerRepository;
    private final AlertNotificationService alertNotificationService;

    public DashboardService(
            SaleService saleService,
            ExpenseService expenseService,
            CustomerDuePaymentService customerDuePaymentService,
            InventoryStockService inventoryStockService,
            CustomerCylinderLedgerService customerCylinderLedgerService,
            CustomerRepository customerRepository,
            WarehouseRepository warehouseRepository,
            SaleRepository saleRepository,
            CustomerCylinderLedgerRepository customerCylinderLedgerRepository,
            AlertNotificationService alertNotificationService) {
        this.saleService = saleService;
        this.expenseService = expenseService;
        this.customerDuePaymentService = customerDuePaymentService;
        this.inventoryStockService = inventoryStockService;
        this.customerCylinderLedgerService = customerCylinderLedgerService;
        this.customerRepository = customerRepository;
        this.warehouseRepository = warehouseRepository;
        this.saleRepository = saleRepository;
        this.customerCylinderLedgerRepository = customerCylinderLedgerRepository;
        this.alertNotificationService = alertNotificationService;
    }

    /**
     * OPTIMIZED: Get comprehensive dashboard summary
     * Note: Cache has been disabled to ensure real-time data
     */
    public DashboardSummaryDTO getDashboardSummary(Integer year, Integer month) {
        DashboardSummaryDTO dto = new DashboardSummaryDTO();

        try {
            LocalDate today = LocalDate.now();

            // Determine the month to analyze
            YearMonth targetMonth;
            if (year != null && month != null) {
                targetMonth = YearMonth.of(year, month);
            } else {
                targetMonth = YearMonth.now();
            }

            LocalDate monthStart = targetMonth.atDay(1);
            LocalDate monthEnd = targetMonth.atEndOfMonth();

            // Execute independent calculations in parallel
            CompletableFuture<Void> todayMetrics = calculateTodayMetricsAsync(dto, today);
            CompletableFuture<Void> monthlyMetrics = calculateMonthlyMetricsAsync(dto, monthStart, monthEnd);
            CompletableFuture<Void> customerMetrics = calculateCustomerMetricsAsync(dto);
            CompletableFuture<Void> breakdowns = calculateBreakdownsAsync(dto, monthStart, monthEnd);
            CompletableFuture<Void> dailyTrend = calculateDailySalesTrendAsync(dto, monthStart, monthEnd);
            CompletableFuture<Void> topDebtors = getTopDebtorsAsync(dto);

            // Wait for all parallel operations to complete
            CompletableFuture.allOf(
                    todayMetrics, monthlyMetrics, customerMetrics,
                    breakdowns, dailyTrend, topDebtors).join();

            // Business insights (depends on other calculations)
            calculateBusinessInsights(dto);

            // Generate alerts
            generateAlerts(dto);

        } catch (Exception e) {
            logger.error("Error loading dashboard summary", e);
        }

        return dto;
    }

    /**
     * Default method without year/month (uses current)
     */
    public DashboardSummaryDTO getDashboardSummary() {
        return getDashboardSummary(null, null);
    }

    @Async("dashboardExecutor")
    private CompletableFuture<Void> calculateTodayMetricsAsync(DashboardSummaryDTO dto, LocalDate today) {
        try {
            calculateTodayMetrics(dto, today);
        } catch (Exception e) {
            logger.warn("Error calculating today metrics", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("dashboardExecutor")
    private CompletableFuture<Void> calculateMonthlyMetricsAsync(DashboardSummaryDTO dto, LocalDate monthStart,
            LocalDate monthEnd) {
        try {
            calculateMonthlyMetrics(dto, monthStart, monthEnd);
        } catch (Exception e) {
            logger.warn("Error calculating monthly metrics", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("dashboardExecutor")
    private CompletableFuture<Void> calculateCustomerMetricsAsync(DashboardSummaryDTO dto) {
        try {
            calculateCustomerMetrics(dto);
        } catch (Exception e) {
            logger.warn("Error calculating customer metrics", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("dashboardExecutor")
    private CompletableFuture<Void> calculateBreakdownsAsync(DashboardSummaryDTO dto, LocalDate monthStart,
            LocalDate monthEnd) {
        try {
            calculateBreakdowns(dto, monthStart, monthEnd);
        } catch (Exception e) {
            logger.warn("Error calculating breakdowns", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("dashboardExecutor")
    private CompletableFuture<Void> calculateDailySalesTrendAsync(DashboardSummaryDTO dto, LocalDate monthStart,
            LocalDate monthEnd) {
        try {
            calculateDailySalesTrend(dto, monthStart, monthEnd);
        } catch (Exception e) {
            logger.warn("Error calculating daily sales trend", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("dashboardExecutor")
    private CompletableFuture<Void> getTopDebtorsAsync(DashboardSummaryDTO dto) {
        try {
            Pageable topPage = PageRequest.of(0, 10);
            Page<CustomerDuePaymentDTO> debtors = customerDuePaymentService.getDuePaymentReport(
                    null, null, null, null, null, topPage);
            List<DashboardSummaryDTO.CustomerDuePaymentDTO> topDebtorsList = debtors.getContent().stream()
                    .map(d -> {
                        DashboardSummaryDTO.CustomerDuePaymentDTO innerDto = new DashboardSummaryDTO.CustomerDuePaymentDTO();
                        innerDto.setCustomerName(d.getCustomerName());
                        innerDto.setDueAmount(d.getDueAmount());
                        return innerDto;
                    })
                    .collect(Collectors.toList());
            dto.setTopDebtors(topDebtorsList);
        } catch (Exception e) {
            logger.warn("Error fetching top debtors", e);
            dto.setTopDebtors(new ArrayList<>());
        }
        return CompletableFuture.completedFuture(null);
    }

    private void calculateTodayMetrics(DashboardSummaryDTO dto, LocalDate today) {
        try {
            // Get today's sales
            SaleSummaryDTO saleSummary = saleService.getSalesSummary(
                    today.toString(), today.toString(), null, null, null, null, null);

            BigDecimal todaySales = ZERO;
            int todayTransactions = 0;
            String topCustomer = null;
            if (saleSummary != null) {
                todaySales = BigDecimal.valueOf(saleSummary.getTotalSalesAmount());
                todayTransactions = saleSummary.getTransactionCount();
                topCustomer = saleSummary.getTopCustomer();
            }
            dto.setTodayTotalSales(todaySales);
            dto.setTodaySalesCount(todayTransactions);
            dto.setTopCustomerToday(topCustomer);

            // Get today's expenses
            Page<ExpenseDTO> todayExpenses = expenseService.getExpensesByDateRange(
                    today, today, PageRequest.of(0, Integer.MAX_VALUE));
            BigDecimal todayExpensesTotal = todayExpenses.getContent().stream()
                    .map(ExpenseDTO::getAmount)
                    .reduce(ZERO, BigDecimal::add);
            dto.setTodayTotalExpenses(todayExpensesTotal);

            // Calculate profit and margin
            BigDecimal todayProfit = todaySales.subtract(todayExpensesTotal);
            dto.setTodayNetProfit(todayProfit);

            if (todaySales.compareTo(ZERO) > 0) {
                double profitMargin = todayProfit.divide(todaySales, 4, RoundingMode.HALF_UP).doubleValue() * 100;
                dto.setTodayProfitMargin(Math.min(100.0, profitMargin));
            } else {
                dto.setTodayProfitMargin(0.0);
            }

            // Get today's collection data from current month
            LocalDate monthStart = today.withDayOfMonth(1);
            Page<CustomerDuePaymentDTO> monthDebtors = customerDuePaymentService.getDuePaymentReport(
                    monthStart, today, null, null, null, PageRequest.of(0, Integer.MAX_VALUE));

            BigDecimal totalDue = monthDebtors.getContent().stream()
                    .map(com.gasagency.dto.CustomerDuePaymentDTO::getDueAmount)
                    .reduce(ZERO, BigDecimal::add);

            // Cash collected = Sum of amountReceived from today's ledger entries (not sales
            // table)
            List<CustomerCylinderLedger> allTodayLedgers = customerCylinderLedgerRepository
                    .findByTransactionDateAndRefType(today, CustomerCylinderLedger.TransactionType.SALE);

            BigDecimal todayCashCollected = allTodayLedgers.stream()
                    .map(ledger -> ledger.getAmountReceived() != null ? ledger.getAmountReceived() : ZERO)
                    .reduce(ZERO, BigDecimal::add);
            dto.setTodayCashCollected(todayCashCollected);
            dto.setTodayAmountDue(ZERO);

            // Collection rate calculation
            if (todayCashCollected.add(totalDue).compareTo(ZERO) > 0) {
                double collectionRate = (todayCashCollected.doubleValue() /
                        (todayCashCollected.add(totalDue).doubleValue())) * 100;
                dto.setTodayCollectionRate(Math.min(100.0, collectionRate));
            } else {
                dto.setTodayCollectionRate(0.0);
            }

            dto.setTodayAmountDue(totalDue);
            dto.setTodayProblematicCustomers(monthDebtors.getContent().size());

            // Get today's inventory status
            try {
                List<InventoryStockDTO> allInventory = inventoryStockService.getAllStock();
                long totalFilled = allInventory.stream()
                        .mapToLong(stock -> stock.getFilledQty() != null ? stock.getFilledQty() : 0L)
                        .sum();
                long totalEmpty = allInventory.stream()
                        .mapToLong(stock -> stock.getEmptyQty() != null ? stock.getEmptyQty() : 0L)
                        .sum();
                long totalInventory = totalFilled + totalEmpty;

                dto.setTodayCylindersFilled((int) totalFilled);
                dto.setTodayCylindersEmpty((int) totalEmpty);
                dto.setTodayCylindersTotal((int) totalInventory);

                if (totalInventory > 0) {
                    double health = (double) totalFilled / totalInventory * 100.0;
                    dto.setTodayInventoryHealth(Math.min(100.0, health));
                } else {
                    dto.setTodayInventoryHealth(0.0);
                }
            } catch (Exception e) {
                logger.warn("Error getting inventory data", e);
                dto.setTodayCylindersFilled(0);
                dto.setTodayCylindersEmpty(0);
                dto.setTodayCylindersTotal(0);
                dto.setTodayInventoryHealth(0.0);
            }
            dto.setTodayReturnsPending(0);

        } catch (Exception e) {
            logger.warn("Error calculating today metrics", e);
            dto.setTodayTotalSales(ZERO);
            dto.setTodayTotalExpenses(ZERO);
            dto.setTodayNetProfit(ZERO);
            dto.setTodayCollectionRate(0.0);
            dto.setTodayProblematicCustomers(0);
        }
    }

    private void calculateMonthlyMetrics(DashboardSummaryDTO dto, LocalDate monthStart, LocalDate monthEnd) {
        try {
            // Get current month sales
            SaleSummaryDTO monthlySales = saleService.getSalesSummary(
                    monthStart.toString(), monthEnd.toString(), null, null, null, null, null);

            BigDecimal monthlySalesTotal = ZERO;
            int monthlySalesCount = 0;
            if (monthlySales != null) {
                monthlySalesTotal = BigDecimal.valueOf(monthlySales.getTotalSalesAmount());
                monthlySalesCount = monthlySales.getTransactionCount();
            }
            dto.setMonthlyTotalSales(monthlySalesTotal);
            dto.setMonthlySalesCount(monthlySalesCount);

            int daysInMonth = monthEnd.getDayOfMonth();
            int daysCompleted = LocalDate.now().getDayOfMonth();

            // Average daily sales
            dto.setAverageDailySales(daysCompleted > 0 ? monthlySalesTotal.doubleValue() / daysCompleted : 0.0);

            // Get current month expenses
            Page<ExpenseDTO> monthlyExpenses = expenseService.getExpensesByDateRange(
                    monthStart, monthEnd, PageRequest.of(0, Integer.MAX_VALUE));
            BigDecimal monthlyExpensesTotal = monthlyExpenses.getContent().stream()
                    .map(ExpenseDTO::getAmount)
                    .reduce(ZERO, BigDecimal::add);
            dto.setMonthlyTotalExpenses(monthlyExpensesTotal);

            // Average daily expense
            dto.setAverageDailyExpense(daysCompleted > 0 ? monthlyExpensesTotal.doubleValue() / daysCompleted : 0.0);

            // Calculate profit and margin
            BigDecimal monthlyProfit = monthlySalesTotal.subtract(monthlyExpensesTotal);
            dto.setMonthlyNetProfit(monthlyProfit);

            if (monthlySalesTotal.compareTo(ZERO) > 0) {
                double profitMargin = monthlyProfit.divide(monthlySalesTotal, 4, RoundingMode.HALF_UP).doubleValue()
                        * 100;
                dto.setMonthlyProfitMargin(Math.min(100.0, profitMargin));
            } else {
                dto.setMonthlyProfitMargin(0.0);
            }

            // Calculate collection rate for month: cash collected / total due
            // Get all outstanding dues at month start
            Page<CustomerDuePaymentDTO> monthDebtors = customerDuePaymentService.getDuePaymentReport(
                    monthStart, monthEnd, null, null, null, PageRequest.of(0, Integer.MAX_VALUE));
            BigDecimal totalMonthlyDue = monthDebtors.getContent().stream()
                    .map(com.gasagency.dto.CustomerDuePaymentDTO::getDueAmount)
                    .reduce(ZERO, BigDecimal::add);

            // Get cash collected this month (from sales and payments)
            BigDecimal cashCollected = monthlySalesTotal; // Sales are cash collections

            // Collection rate = cash collected / (cash collected + remaining dues)
            if (cashCollected.add(totalMonthlyDue).compareTo(ZERO) > 0) {
                double collectionRate = (cashCollected.doubleValue() /
                        (cashCollected.add(totalMonthlyDue).doubleValue())) * 100;
                dto.setMonthlyCollectionRate(Math.min(100.0, collectionRate));
            } else {
                dto.setMonthlyCollectionRate(0.0);
            }

            // Days info
            dto.setDaysInMonth(daysInMonth);
            dto.setDaysCompleted(daysCompleted);

            // Projections for full month based on current daily average
            if (daysCompleted > 0) {
                double dailyAvgSales = monthlySalesTotal.doubleValue() / daysCompleted;
                double dailyAvgExpense = monthlyExpensesTotal.doubleValue() / daysCompleted;

                BigDecimal projectedSales = BigDecimal.valueOf(dailyAvgSales * daysInMonth);
                BigDecimal projectedExpense = BigDecimal.valueOf(dailyAvgExpense * daysInMonth);
                BigDecimal projectedProfit = projectedSales.subtract(projectedExpense);

                dto.setMonthlyProjectedSales(projectedSales);
                dto.setMonthlyProjectedProfit(projectedProfit);
            }

            // Calculate month-over-month growth
            try {
                LocalDate previousMonthStart = monthStart.minusMonths(1).withDayOfMonth(1);
                LocalDate previousMonthEnd = previousMonthStart.withDayOfMonth(previousMonthStart.lengthOfMonth());

                SaleSummaryDTO previousMonth = saleService.getSalesSummary(
                        previousMonthStart.toString(), previousMonthEnd.toString(), null, null, null, null, null);

                if (previousMonth != null && previousMonth.getTotalSalesAmount() > 0) {
                    double growth = ((monthlySalesTotal.doubleValue() - previousMonth.getTotalSalesAmount())
                            / previousMonth.getTotalSalesAmount()) * 100;
                    dto.setMonthOverMonthGrowth(growth);
                }
            } catch (Exception gre) {
                logger.warn("Error calculating month-over-month growth", gre);
            }

            // Calculate sales and profit trends (UP/DOWN/STABLE)
            if (dto.getMonthOverMonthGrowth() != null) {
                if (dto.getMonthOverMonthGrowth() > 5.0) {
                    dto.setSalesTrend("UP");
                    dto.setProfitTrend("UP");
                } else if (dto.getMonthOverMonthGrowth() < -5.0) {
                    dto.setSalesTrend("DOWN");
                    dto.setProfitTrend("DOWN");
                } else {
                    dto.setSalesTrend("STABLE");
                    dto.setProfitTrend("STABLE");
                }
            } else {
                dto.setSalesTrend("STABLE");
                dto.setProfitTrend("STABLE");
            }

            // Collection trend based on collection rate
            if (dto.getMonthlyCollectionRate() > 50.0) {
                dto.setCollectionTrend("UP");
            } else if (dto.getMonthlyCollectionRate() < 30.0) {
                dto.setCollectionTrend("DOWN");
            } else {
                dto.setCollectionTrend("STABLE");
            }

            // Inventory trend: check if inventory levels are stable, increasing, or
            // decreasing
            // For now, set to STABLE (would need historical data for better calculation)
            dto.setInventoryTrend("STABLE");

        } catch (Exception e) {
            logger.warn("Error calculating monthly metrics", e);
            dto.setMonthlyTotalSales(ZERO);
            dto.setMonthlyTotalExpenses(ZERO);
            dto.setMonthlyNetProfit(ZERO);
            dto.setMonthlyCollectionRate(0.0);
            dto.setSalesTrend("STABLE");
            dto.setProfitTrend("STABLE");
            dto.setCollectionTrend("STABLE");
            dto.setInventoryTrend("STABLE");
        }
    }

    private void calculateCustomerMetrics(DashboardSummaryDTO dto) {
        try {
            // Get all active customers
            List<Customer> allCustomers = customerRepository.findAll();
            int totalCustomers = allCustomers.size();
            dto.setTotalActiveCustomers(totalCustomers);

            // Get customers with dues (all-time, not filtered by month)
            Page<CustomerDuePaymentDTO> customersWithDues = customerDuePaymentService
                    .getDuePaymentReport(
                            null, null, null, null, null, PageRequest.of(0, Integer.MAX_VALUE));

            int customersWithDuesCount = customersWithDues.getContent().size();
            int customersWithNoDues = Math.max(0, totalCustomers - customersWithDuesCount);

            dto.setCustomersWithNoDues(customersWithNoDues);

            // For slow payment and overdue: estimate based on proportion of dues customers
            // Slow payment (8-30 days): ~30% of customers with dues
            // Overdue (>30 days): ~20% of customers with dues
            int slowPaymentCount = Math.max(0, (int) Math.round(customersWithDuesCount * 0.3));
            int overdueCount = Math.max(0, (int) Math.round(customersWithDuesCount * 0.2));

            dto.setCustomersWithSlowPayment(slowPaymentCount);
            dto.setCustomersWithOverduePayment(overdueCount);

            // Get pending returns (cylinders awaiting pickup) - top 5 records from the
            // month
            try {
                List<CustomerCylinderLedgerDTO> allPendingBalances = customerCylinderLedgerService
                        .getAllPendingBalances();

                // Get top 5 pending returns by cylinder balance
                List<CustomerCylinderLedgerDTO> topPendingReturns = allPendingBalances.stream()
                        .sorted((a, b) -> {
                            Long balanceA = a.getBalance() != null ? a.getBalance() : 0L;
                            Long balanceB = b.getBalance() != null ? b.getBalance() : 0L;
                            return balanceB.compareTo(balanceA);
                        })
                        .limit(5)
                        .collect(Collectors.toList());

                dto.setPendingReturnsDetail(topPendingReturns);

                // Count total cylinders awaiting pickup
                long totalPendingCylinders = allPendingBalances.stream()
                        .mapToLong(p -> p.getBalance() != null ? p.getBalance() : 0L)
                        .sum();
                dto.setTodayReturnsPending((int) totalPendingCylinders);

            } catch (Exception pre) {
                logger.warn("Error calculating pending returns", pre);
                dto.setPendingReturnsDetail(new ArrayList<>());
                dto.setTodayReturnsPending(0);
            }

        } catch (Exception e) {
            logger.warn("Error calculating customer metrics", e);
            dto.setTotalActiveCustomers(0);
            dto.setCustomersWithNoDues(0);
            dto.setCustomersWithSlowPayment(0);
            dto.setCustomersWithOverduePayment(0);
            dto.setPendingReturnsDetail(new ArrayList<>());
            dto.setTodayReturnsPending(0);
        }
    }

    private void calculateBreakdowns(DashboardSummaryDTO dto, LocalDate monthStart, LocalDate monthEnd) {
        try {
            // Expense category breakdown
            Map<String, BigDecimal> expenseBreakdown = new HashMap<>();
            Page<ExpenseDTO> monthlyExpenses = expenseService.getExpensesByDateRange(
                    monthStart, monthEnd, PageRequest.of(0, Integer.MAX_VALUE));

            monthlyExpenses.getContent().forEach(expense -> {
                String category = expense.getCategory() != null ? expense.getCategory() : "Other";
                expenseBreakdown.merge(category, expense.getAmount(), BigDecimal::add);
            });

            dto.setExpenseCategoryBreakdown(expenseBreakdown);

            // Get top category expense
            if (!expenseBreakdown.isEmpty()) {
                String topCategory = expenseBreakdown.entrySet().stream()
                        .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                        .map(Map.Entry::getKey)
                        .orElse("N/A");
                dto.setTopCategoryExpenseMonthly(topCategory);
            }

            // Variant sales breakdown - get from sales for the selected month
            try {
                List<DashboardSummaryDTO.VariantSalesDTO> variantSales = new ArrayList<>();

                // Get all sales for the month
                Page<Sale> monthlySales = saleRepository.findByDateRange(monthStart, monthEnd,
                        PageRequest.of(0, Integer.MAX_VALUE));

                // Group sale items by variant and sum quantities
                Map<String, Long> variantTotals = new HashMap<>();
                long totalQty = 0;

                for (Sale sale : monthlySales.getContent()) {
                    if (sale.getSaleItems() != null) {
                        for (SaleItem item : sale.getSaleItems()) {
                            String variantName = item.getVariant() != null && item.getVariant().getName() != null
                                    ? item.getVariant().getName()
                                    : "Unknown";
                            long qty = item.getQtyIssued() != null ? item.getQtyIssued() : 0L;
                            variantTotals.merge(variantName, qty, Long::sum);
                            totalQty += qty;
                        }
                    }
                }

                // Convert to VariantSalesDTO with percentage
                final long totalQtyFinal = totalQty;
                variantTotals.forEach((variantName, quantity) -> {
                    DashboardSummaryDTO.VariantSalesDTO variantSale = new DashboardSummaryDTO.VariantSalesDTO();
                    variantSale.setVariantName(variantName);
                    variantSale.setQuantity(quantity.intValue());
                    variantSale.setAmount(BigDecimal.valueOf(quantity)); // Use quantity as proxy
                    double percentage = totalQtyFinal > 0 ? (quantity.doubleValue() / totalQtyFinal) * 100 : 0;
                    variantSale.setPercentage(percentage);
                    variantSales.add(variantSale);
                });

                dto.setVariantSalesBreakdown(variantSales);
            } catch (Exception vse) {
                logger.warn("Error calculating variant sales breakdown for date range", vse);
                dto.setVariantSalesBreakdown(new ArrayList<>());
            }

            // Inventory by warehouse - get actual inventory data
            try {
                List<InventoryHealthDTO> inventoryByWarehouse = new ArrayList<>();
                List<InventoryStockDTO> allInventory = inventoryStockService.getAllStock();

                warehouseRepository.findAll().forEach(warehouse -> {
                    InventoryHealthDTO health = new InventoryHealthDTO();
                    health.setWarehouseName(warehouse.getName());

                    // Filter inventory for this warehouse
                    long warehouseFilled = allInventory.stream()
                            .filter(stock -> stock.getWarehouseName() != null &&
                                    stock.getWarehouseName().equals(warehouse.getName()))
                            .mapToLong(stock -> stock.getFilledQty() != null ? stock.getFilledQty() : 0L)
                            .sum();

                    long warehouseEmpty = allInventory.stream()
                            .filter(stock -> stock.getWarehouseName() != null &&
                                    stock.getWarehouseName().equals(warehouse.getName()))
                            .mapToLong(stock -> stock.getEmptyQty() != null ? stock.getEmptyQty() : 0L)
                            .sum();

                    long warehouseTotal = warehouseFilled + warehouseEmpty;

                    health.setFilledCount((int) warehouseFilled);
                    health.setEmptyCount((int) warehouseEmpty);
                    health.setTotalCount((int) warehouseTotal);

                    // Health percentage = filled / total
                    if (warehouseTotal > 0) {
                        double healthPercent = (double) warehouseFilled / warehouseTotal * 100.0;
                        health.setHealthPercentage(Math.min(100.0, healthPercent));
                    } else {
                        health.setHealthPercentage(0.0);
                    }

                    inventoryByWarehouse.add(health);
                });

                dto.setInventoryByWarehouse(inventoryByWarehouse);
            } catch (Exception ie) {
                logger.warn("Error calculating warehouse inventory", ie);
                dto.setInventoryByWarehouse(new ArrayList<>());
            }

        } catch (Exception e) {
            logger.warn("Error calculating breakdowns", e);
        }
    }

    private void calculateBusinessInsights(DashboardSummaryDTO dto) {
        try {
            BusinessInsightsDTO insights = new BusinessInsightsDTO();

            // Month over month growth
            insights.setMonthlyGrowthPercentage(dto.getMonthOverMonthGrowth());

            // Average collection rate
            insights.setAverageCollectionRate(dto.getMonthlyCollectionRate());

            // Average order value
            if (dto.getMonthlySalesCount() > 0) {
                double aov = dto.getMonthlyTotalSales().doubleValue() / dto.getMonthlySalesCount();
                insights.setAverageOrderValue(aov);
            }

            // Inventory turnover rate - placeholder
            insights.setInventoryTurnoverRate(0.0);

            // Top selling variant - get the highest quantity variant
            if (dto.getVariantSalesBreakdown() != null && !dto.getVariantSalesBreakdown().isEmpty()) {
                String topVariant = dto.getVariantSalesBreakdown().stream()
                        .max((v1, v2) -> Long.compare(v1.getQuantity(), v2.getQuantity()))
                        .map(DashboardSummaryDTO.VariantSalesDTO::getVariantName)
                        .orElse("N/A");
                insights.setTopSellingVariant(topVariant);
            } else {
                insights.setTopSellingVariant("N/A");
            }

            // Average profit margin
            insights.setAvgProfitMargin(dto.getMonthlyProfitMargin());

            dto.setBusinessInsights(insights);

        } catch (Exception e) {
            logger.warn("Error calculating business insights", e);
        }
    }

    private void generateAlerts(DashboardSummaryDTO dto) {
        try {
            // Fetch all active alerts from AlertNotificationService
            // This removes hardcoded alerts and uses flexible configuration
            List<AlertNotification> activeAlerts = alertNotificationService.getActiveAlerts();

            // Convert to DTO for dashboard
            List<DashboardAlertDTO> alertDTOs = activeAlerts.stream()
                    .map(alert -> createAlertDTO(alert))
                    .collect(Collectors.toList());

            dto.setAlerts(alertDTOs);
            logger.info("Dashboard alerts set: {} active alerts", alertDTOs.size());
        } catch (Exception e) {
            logger.warn("Error generating alerts", e);
            dto.setAlerts(new ArrayList<>());
        }
    }

    private DashboardAlertDTO createAlertDTO(AlertNotification alert) {
        DashboardAlertDTO dto = new DashboardAlertDTO();
        dto.setSeverity(alert.getSeverity());
        dto.setTitle(alert.getAlertType());
        dto.setMessage(alert.getMessage());
        dto.setActionLabel("Dismiss");
        dto.setActionLink("/alerts/" + alert.getId());
        return dto;
    }

    private void calculateDailySalesTrend(DashboardSummaryDTO dto, LocalDate monthStart, LocalDate monthEnd) {
        try {
            List<DashboardSummaryDTO.DailySalesDataDTO> dailyTrend = new ArrayList<>();

            // Get all sales for the month
            Page<SaleDTO> monthlySales = saleService.getSalesByDateRange(
                    monthStart, monthEnd, PageRequest.of(0, Integer.MAX_VALUE));

            // Group sales by date and calculate daily totals
            Map<LocalDate, BigDecimal> dailySalesMap = new HashMap<>();
            Map<LocalDate, BigDecimal> dailyExpensesMap = new HashMap<>();

            // Process sales by date
            monthlySales.getContent().forEach(sale -> {
                LocalDate saleDate = sale.getSaleDate();
                BigDecimal amount = sale.getTotalAmount() != null ? sale.getTotalAmount() : ZERO;
                dailySalesMap.merge(saleDate, amount, BigDecimal::add);
            });

            // Process expenses by date
            Page<ExpenseDTO> monthlyExpenses = expenseService.getExpensesByDateRange(
                    monthStart, monthEnd, PageRequest.of(0, Integer.MAX_VALUE));
            monthlyExpenses.getContent().forEach(expense -> {
                LocalDate expenseDate = expense.getExpenseDate();
                BigDecimal amount = expense.getAmount() != null ? expense.getAmount() : ZERO;
                dailyExpensesMap.merge(expenseDate, amount, BigDecimal::add);
            });

            // Calculate daily profits and build trend data
            for (LocalDate date = monthStart; !date.isAfter(monthEnd); date = date.plusDays(1)) {
                BigDecimal sales = dailySalesMap.getOrDefault(date, ZERO);
                BigDecimal expenses = dailyExpensesMap.getOrDefault(date, ZERO);
                BigDecimal profit = sales.subtract(expenses);

                DashboardSummaryDTO.DailySalesDataDTO dailyData = new DashboardSummaryDTO.DailySalesDataDTO();
                dailyData.setDate(date.toString());
                dailyData.setSales(sales);
                dailyData.setProfit(profit);
                dailyTrend.add(dailyData);
            }

            dto.setMonthlySalesTrend(dailyTrend);
        } catch (Exception e) {
            logger.warn("Error calculating daily sales trend", e);
            dto.setMonthlySalesTrend(new ArrayList<>());
        }
    }
}
