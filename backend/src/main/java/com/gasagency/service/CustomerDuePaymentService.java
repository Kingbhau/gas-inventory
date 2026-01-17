package com.gasagency.service;

import com.gasagency.dto.CustomerDuePaymentDTO;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.repository.CustomerRepository;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import com.gasagency.util.LoggerUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomerDuePaymentService {
    private static final Logger logger = LoggerFactory.getLogger(CustomerDuePaymentService.class);

    private final CustomerRepository customerRepository;
    private final CustomerCylinderLedgerRepository ledgerRepository;

    public CustomerDuePaymentService(CustomerRepository customerRepository,
            CustomerCylinderLedgerRepository ledgerRepository) {
        this.customerRepository = customerRepository;
        this.ledgerRepository = ledgerRepository;
    }

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
            // Fetch all customers or specific customer
            List<Customer> customers;
            if (customerId != null) {
                customers = customerRepository.findById(customerId).stream()
                        .collect(Collectors.toList());
            } else {
                customers = customerRepository.findAll();
            }

            // Build due payment data
            List<CustomerDuePaymentDTO> duePaymentData = customers.stream()
                    .map(customer -> buildCustomerDuePaymentDTO(customer, fromDate, toDate, minAmount, maxAmount))
                    .filter(dto -> dto.getDueAmount().compareTo(BigDecimal.ZERO) > 0) // Only include customers with due
                                                                                      // amount
                    .collect(Collectors.toList());

            // Apply pagination manually
            int totalSize = duePaymentData.size();
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), totalSize);

            List<CustomerDuePaymentDTO> paginatedData;
            if (start > totalSize) {
                paginatedData = new ArrayList<>();
            } else {
                paginatedData = duePaymentData.subList(start, end);
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

    @Transactional(readOnly = true)
    public CustomerDuePaymentReportSummaryDTO getDuePaymentReportSummary(
            LocalDate fromDate,
            LocalDate toDate,
            Long customerId,
            Double minAmount,
            Double maxAmount) {

        try {
            // Get all due payment data
            List<Customer> customers;
            if (customerId != null) {
                customers = customerRepository.findById(customerId).stream()
                        .collect(Collectors.toList());
            } else {
                customers = customerRepository.findAll();
            }

            List<CustomerDuePaymentDTO> duePaymentData = customers.stream()
                    .map(customer -> buildCustomerDuePaymentDTO(customer, fromDate, toDate, minAmount, maxAmount))
                    .filter(dto -> dto.getDueAmount().compareTo(BigDecimal.ZERO) > 0)
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

    private CustomerDuePaymentDTO buildCustomerDuePaymentDTO(
            Customer customer,
            LocalDate fromDate,
            LocalDate toDate,
            Double minAmount,
            Double maxAmount) {

        // Fetch ledger entries for this customer
        List<CustomerCylinderLedger> ledgerEntries = ledgerRepository.findByCustomer(customer);

        // Filter by date range if provided
        if (fromDate != null || toDate != null) {
            ledgerEntries = ledgerEntries.stream()
                    .filter(entry -> {
                        if (fromDate != null && entry.getTransactionDate().isBefore(fromDate)) {
                            return false;
                        }
                        if (toDate != null && entry.getTransactionDate().isAfter(toDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        // Calculate totals
        BigDecimal totalSalesAmount = ledgerEntries.stream()
                .map(entry -> entry.getTotalAmount() != null ? entry.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal amountReceived = ledgerEntries.stream()
                .map(entry -> entry.getAmountReceived() != null ? entry.getAmountReceived() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate due amount as: Total Sales - Amount Received
        BigDecimal dueAmount = totalSalesAmount.subtract(amountReceived);
        // Ensure due amount is not negative
        if (dueAmount.signum() < 0) {
            dueAmount = BigDecimal.ZERO;
        }

        // Apply amount filters if provided
        if (minAmount != null && dueAmount.doubleValue() < minAmount) {
            return new CustomerDuePaymentDTO(customer.getId(), customer.getName(), customer.getMobile(),
                    customer.getAddress(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, 0L);
        }
        if (maxAmount != null && dueAmount.doubleValue() > maxAmount) {
            return new CustomerDuePaymentDTO(customer.getId(), customer.getName(), customer.getMobile(),
                    customer.getAddress(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, 0L);
        }

        // Get last transaction date
        LocalDate lastTransactionDate = ledgerEntries.stream()
                .map(CustomerCylinderLedger::getTransactionDate)
                .max(LocalDate::compareTo)
                .orElse(null);

        Long transactionCount = (long) ledgerEntries.size();

        return new CustomerDuePaymentDTO(
                customer.getId(),
                customer.getName(),
                customer.getMobile(),
                customer.getAddress(),
                totalSalesAmount,
                amountReceived,
                dueAmount,
                lastTransactionDate,
                transactionCount);
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
