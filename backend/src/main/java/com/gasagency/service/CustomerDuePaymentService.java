package com.gasagency.service;

import com.gasagency.dto.response.CustomerDuePaymentDTO;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import com.gasagency.util.LoggerUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerDuePaymentService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerDuePaymentService.class);

    private final CustomerCylinderLedgerRepository ledgerRepository;

    public CustomerDuePaymentService(CustomerCylinderLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * OPTIMIZED: Uses database-level aggregation for due payment calculations
     * Prevents loading entire result set into memory
     */
    @Transactional(readOnly = true)
    public Page<CustomerDuePaymentDTO> getDuePaymentReport(
            LocalDate fromDate,
            LocalDate toDate,
            Long customerId,
            Double minAmount,
            Double maxAmount,
            Pageable pageable) {

        LoggerUtil.logBusinessEntry(logger, "GET_DUE_PAYMENT_REPORT",
                "fromDate", fromDate, "toDate", toDate, "customerId", customerId);

        try {
            // Fast path: no filters, use latest due per customer (database query)
            if (fromDate == null && toDate == null && customerId == null
                    && minAmount == null && maxAmount == null) {
                Page<CustomerCylinderLedger> page = ledgerRepository.findLatestDuePerCustomer(pageable);
                List<CustomerCylinderLedger> latestLedgers = page.getContent();
                if (latestLedgers.isEmpty()) {
                    return new PageImpl<>(Collections.emptyList(), pageable, page.getTotalElements());
                }

                List<Long> customerIds = latestLedgers.stream()
                        .map(ledger -> ledger.getCustomer().getId())
                        .collect(Collectors.toList());

                Map<Long, Object[]> aggregateMap = ledgerRepository.getCustomerLedgerAggregates(customerIds)
                        .stream()
                        .collect(Collectors.toMap(
                                row -> (Long) row[0],
                                row -> row
                        ));

                List<CustomerDuePaymentDTO> data = latestLedgers.stream()
                        .map(ledger -> {
                            Object[] agg = aggregateMap.get(ledger.getCustomer().getId());
                            BigDecimal totalSalesAmount = agg != null ? (BigDecimal) agg[1] : BigDecimal.ZERO;
                            BigDecimal amountReceived = agg != null ? (BigDecimal) agg[2] : BigDecimal.ZERO;
                            LocalDate lastTransactionDate = agg != null ? (LocalDate) agg[3] : ledger.getTransactionDate();
                            Long transactionCount = agg != null ? (Long) agg[4] : 0L;

                            return new CustomerDuePaymentDTO(
                                    ledger.getCustomer().getId(),
                                    ledger.getCustomer().getName(),
                                    ledger.getCustomer().getMobile(),
                                    ledger.getCustomer().getAddress(),
                                    totalSalesAmount,
                                    amountReceived,
                                    ledger.getDueAmount() != null ? ledger.getDueAmount() : BigDecimal.ZERO,
                                    lastTransactionDate,
                                    transactionCount);
                        })
                        .collect(Collectors.toList());
                return new PageImpl<>(data, pageable, page.getTotalElements());
            }

            BigDecimal minDue = minAmount != null ? BigDecimal.valueOf(minAmount) : null;
            BigDecimal maxDue = maxAmount != null ? BigDecimal.valueOf(maxAmount) : null;

            // Fetch aggregated due payment data with DB-side filters
            List<Object[]> aggregates = ledgerRepository.findDuePaymentAggregates(
                    fromDate, toDate, customerId, minDue, maxDue);

            List<CustomerDuePaymentDTO> duePaymentData = aggregates.stream()
                    .map(row -> {
                        Long id = (Long) row[0];
                        String name = (String) row[1];
                        String mobile = (String) row[2];
                        String address = (String) row[3];
                        BigDecimal totalSalesAmount = (BigDecimal) row[4];
                        BigDecimal amountReceived = (BigDecimal) row[5];
                        LocalDate lastTransactionDate = (LocalDate) row[6];
                        Long transactionCount = (Long) row[7];

                        BigDecimal dueAmount = totalSalesAmount.subtract(amountReceived);
                        if (dueAmount.signum() < 0) {
                            dueAmount = BigDecimal.ZERO;
                        }

                        return new CustomerDuePaymentDTO(
                                id,
                                name,
                                mobile,
                                address,
                                totalSalesAmount,
                                amountReceived,
                                dueAmount,
                                lastTransactionDate,
                                transactionCount);
                    })
                    .collect(Collectors.toList());

            // Apply pagination at application level (since data is filtered)
            int totalSize = duePaymentData.size();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), totalSize);

            List<CustomerDuePaymentDTO> paginatedData;
            if (start >= totalSize) {
                paginatedData = new ArrayList<>();
            } else {
                paginatedData = new ArrayList<>(duePaymentData.subList(start, end));
            }

            LoggerUtil.logBusinessSuccess(logger, "GET_DUE_PAYMENT_REPORT",
                    "totalRecords", totalSize, "returnedRecords", paginatedData.size());

            return new PageImpl<>(paginatedData, pageable, totalSize);

        } catch (Exception e) {
            LoggerUtil.logBusinessError(logger, "GET_DUE_PAYMENT_REPORT", "Error fetching due payment report",
                    "error", e.getMessage());
            throw new RuntimeException("Error fetching due payment report: " + e.getMessage(), e);
        }
    }

    /**
     * OPTIMIZED: Fetch top debtors using latest ledger entries per customer
     * Avoids loading all customers/ledgers into memory.
     */
    @Transactional(readOnly = true)
    public List<CustomerDuePaymentDTO> getTopDebtors(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        Page<CustomerCylinderLedger> page = ledgerRepository.findLatestDuePerCustomer(
                PageRequest.of(0, safeLimit));

        return page.getContent().stream()
                .map(ledger -> new CustomerDuePaymentDTO(
                        ledger.getCustomer().getId(),
                        ledger.getCustomer().getName(),
                        ledger.getCustomer().getMobile(),
                        ledger.getCustomer().getAddress(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        ledger.getDueAmount() != null ? ledger.getDueAmount() : BigDecimal.ZERO,
                        ledger.getTransactionDate(),
                        0L))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalDueAmount() {
        BigDecimal total = ledgerRepository.sumLatestDueAmounts();
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public long getCustomersWithDueCount() {
        return ledgerRepository.countLatestDueCustomers();
    }

    @Transactional(readOnly = true)
    public CustomerDuePaymentReportSummaryDTO getDuePaymentReportSummary(
            LocalDate fromDate,
            LocalDate toDate,
            Long customerId,
            Double minAmount,
            Double maxAmount) {

        try {
            BigDecimal minDue = minAmount != null ? BigDecimal.valueOf(minAmount) : null;
            BigDecimal maxDue = maxAmount != null ? BigDecimal.valueOf(maxAmount) : null;

            List<Object[]> aggregates = ledgerRepository.findDuePaymentAggregates(
                    fromDate, toDate, customerId, minDue, maxDue);

            List<CustomerDuePaymentDTO> duePaymentData = aggregates.stream()
                    .map(row -> {
                        BigDecimal totalSalesAmount = (BigDecimal) row[4];
                        BigDecimal amountReceived = (BigDecimal) row[5];
                        BigDecimal dueAmount = totalSalesAmount.subtract(amountReceived);
                        if (dueAmount.signum() < 0) {
                            dueAmount = BigDecimal.ZERO;
                        }
                        return new CustomerDuePaymentDTO(
                                (Long) row[0],
                                (String) row[1],
                                (String) row[2],
                                (String) row[3],
                                totalSalesAmount,
                                amountReceived,
                                dueAmount,
                                (LocalDate) row[6],
                                (Long) row[7]);
                    })
                    .collect(Collectors.toList());

            // Calculate summary
            BigDecimal totalDueAmount = duePaymentData.stream()
                    .map(CustomerDuePaymentDTO::getDueAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSalesAmount = duePaymentData.stream()
                    .map(CustomerDuePaymentDTO::getTotalSalesAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalAmountReceived = duePaymentData.stream()
                    .map(CustomerDuePaymentDTO::getAmountReceived)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Long totalCustomers = (long) duePaymentData.size();
            Double avgDueAmount = totalCustomers > 0
                    ? totalDueAmount.doubleValue() / totalCustomers
                    : 0.0;

            return new CustomerDuePaymentReportSummaryDTO(
                    totalDueAmount,
                    totalSalesAmount,
                    totalAmountReceived,
                    totalCustomers,
                    BigDecimal.valueOf(avgDueAmount));

        } catch (Exception e) {
            LoggerUtil.logBusinessError(logger, "GET_DUE_PAYMENT_REPORT_SUMMARY",
                    "Error calculating summary", "error", e.getMessage());
            throw new RuntimeException("Error calculating due payment report summary: " + e.getMessage(), e);
        }
    }

    // Summary DTO for report aggregation
    public static class CustomerDuePaymentReportSummaryDTO {
        private BigDecimal totalDueAmount;
        private BigDecimal totalSalesAmount;
        private BigDecimal totalAmountReceived;
        private Long totalCustomersWithDue;
        private BigDecimal averageDueAmount;

        public CustomerDuePaymentReportSummaryDTO(BigDecimal totalDueAmount, BigDecimal totalSalesAmount,
                BigDecimal totalAmountReceived, Long totalCustomersWithDue, BigDecimal averageDueAmount) {
            this.totalDueAmount = totalDueAmount;
            this.totalSalesAmount = totalSalesAmount;
            this.totalAmountReceived = totalAmountReceived;
            this.totalCustomersWithDue = totalCustomersWithDue;
            this.averageDueAmount = averageDueAmount;
        }

        public BigDecimal getTotalDueAmount() {
            return totalDueAmount;
        }

        public void setTotalDueAmount(BigDecimal totalDueAmount) {
            this.totalDueAmount = totalDueAmount;
        }

        public BigDecimal getTotalSalesAmount() {
            return totalSalesAmount;
        }

        public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
            this.totalSalesAmount = totalSalesAmount;
        }

        public BigDecimal getTotalAmountReceived() {
            return totalAmountReceived;
        }

        public void setTotalAmountReceived(BigDecimal totalAmountReceived) {
            this.totalAmountReceived = totalAmountReceived;
        }

        public Long getTotalCustomersWithDue() {
            return totalCustomersWithDue;
        }

        public void setTotalCustomersWithDue(Long totalCustomersWithDue) {
            this.totalCustomersWithDue = totalCustomersWithDue;
        }

        public BigDecimal getAverageDueAmount() {
            return averageDueAmount;
        }

        public void setAverageDueAmount(BigDecimal averageDueAmount) {
            this.averageDueAmount = averageDueAmount;
        }
    }
}

