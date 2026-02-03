package com.gasagency.service;

import com.gasagency.dto.DayBookDTO;
import com.gasagency.dto.DayBookSummaryDTO;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DayBookService {

    private final CustomerCylinderLedgerRepository ledgerRepository;

    public DayBookService(CustomerCylinderLedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /**
     * Get all transactions for the current day (both Sales and Empty Returns) with
     * pagination
     */
    @Transactional(readOnly = true)
    public Page<DayBookDTO> getCurrentDayTransactions(Pageable pageable) {
        LocalDate today = LocalDate.now();
        return getTransactionsByDate(today, pageable);
    }

    /**
     * Get all transactions for a specific date with pagination
     */
    @Transactional(readOnly = true)
    public Page<DayBookDTO> getTransactionsByDate(LocalDate date, Pageable pageable) {
        // Fetch all ledger entries for the given date (SALE and EMPTY_RETURN types)
        List<CustomerCylinderLedger> ledgers = ledgerRepository.findByTransactionDateAndRefTypeIn(
                date,
                List.of(
                        CustomerCylinderLedger.TransactionType.SALE,
                        CustomerCylinderLedger.TransactionType.EMPTY_RETURN));

        // Convert ledger entries to DayBook DTOs
        List<DayBookDTO> dayBookList = ledgers.stream()
                .map(this::convertLedgerToDayBook)
                .collect(Collectors.toList());

        // Apply pagination manually
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, dayBookList.size());

        List<DayBookDTO> pageContent = dayBookList.subList(start, Math.min(end, dayBookList.size()));

        return new PageImpl<>(pageContent, pageable, dayBookList.size());
    }

    /**
     * Get all transactions for a specific date without pagination (for summary)
     */
    @Transactional(readOnly = true)
    public DayBookSummaryDTO getTransactionsByDateSummary(LocalDate date) {
        // Fetch all ledger entries for the given date (SALE and EMPTY_RETURN types)
        List<CustomerCylinderLedger> ledgers = ledgerRepository.findByTransactionDateAndRefTypeIn(
                date,
                List.of(
                        CustomerCylinderLedger.TransactionType.SALE,
                        CustomerCylinderLedger.TransactionType.EMPTY_RETURN));

        // Convert ledger entries to DayBook DTOs
        List<DayBookDTO> dayBookList = ledgers.stream()
                .map(this::convertLedgerToDayBook)
                .collect(Collectors.toList());

        // Calculate summary statistics
        long totalFilledCount = dayBookList.stream()
                .mapToLong(dto -> dto.getFilledCount() != null ? dto.getFilledCount() : 0)
                .sum();

        long totalEmptyCount = dayBookList.stream()
                .mapToLong(dto -> dto.getEmptyCount() != null ? dto.getEmptyCount() : 0)
                .sum();

        BigDecimal totalAmount = dayBookList.stream()
                .map(DayBookDTO::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAmountReceived = dayBookList.stream()
                .map(DayBookDTO::getAmountReceived)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // For total due amount, get the latest due amount per customer by ID (first
        // occurrence since list is sorted by latest first)
        Map<Long, DayBookDTO> latestPerCustomer = dayBookList.stream()
                .collect(Collectors.toMap(
                        DayBookDTO::getCustomerId,
                        dto -> dto,
                        (existing, replacement) -> existing));

        BigDecimal totalDueAmount = latestPerCustomer.values().stream()
                .map(DayBookDTO::getDueAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DayBookSummaryDTO(
                dayBookList,
                totalFilledCount,
                totalEmptyCount,
                totalAmount,
                totalAmountReceived,
                totalDueAmount,
                dayBookList.size());
    }

    /**
     * Convert CustomerCylinderLedger to DayBookDTO
     */
    private DayBookDTO convertLedgerToDayBook(CustomerCylinderLedger ledger) {
        DayBookDTO dayBook = new DayBookDTO();
        dayBook.setId(ledger.getId());
        dayBook.setCustomerId(ledger.getCustomer() != null ? ledger.getCustomer().getId() : null);
        dayBook.setTransactionDate(ledger.getTransactionDate());
        dayBook.setCustomerName(ledger.getCustomer() != null ? ledger.getCustomer().getName() : "N/A");
        dayBook.setWarehouseName(ledger.getWarehouse() != null ? ledger.getWarehouse().getName() : "N/A");
        dayBook.setVariantName(ledger.getVariant() != null ? ledger.getVariant().getName() : "N/A");

        // Determine filled and empty counts based on transaction type
        if (ledger.getRefType() == CustomerCylinderLedger.TransactionType.SALE) {
            dayBook.setFilledCount(ledger.getFilledOut());
            dayBook.setEmptyCount(ledger.getEmptyIn());
        } else if (ledger.getRefType() == CustomerCylinderLedger.TransactionType.EMPTY_RETURN) {
            dayBook.setFilledCount(0L);
            dayBook.setEmptyCount(ledger.getEmptyIn());
        }

        dayBook.setTotalAmount(ledger.getTotalAmount());
        dayBook.setAmountReceived(ledger.getAmountReceived());
        dayBook.setDueAmount(ledger.getDueAmount());
        dayBook.setPaymentMode(ledger.getPaymentMode());
        dayBook.setTransactionType(ledger.getRefType().toString());

        return dayBook;
    }
}
