package com.gasagency.service;

import com.gasagency.dto.DayBookDTO;
import com.gasagency.dto.DayBookSummaryDTO;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.entity.Expense;
import com.gasagency.entity.BankDeposit;
import com.gasagency.entity.SupplierTransaction;
import com.gasagency.entity.WarehouseTransfer;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import com.gasagency.repository.ExpenseRepository;
import com.gasagency.repository.BankDepositRepository;
import com.gasagency.repository.SupplierTransactionRepository;
import com.gasagency.repository.WarehouseTransferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DayBookService {

    private final CustomerCylinderLedgerRepository ledgerRepository;
    private final WarehouseTransferRepository warehouseTransferRepository;
    private final SupplierTransactionRepository supplierTransactionRepository;
    private final BankDepositRepository bankDepositRepository;
    private final ExpenseRepository expenseRepository;

    public DayBookService(CustomerCylinderLedgerRepository ledgerRepository,
            WarehouseTransferRepository warehouseTransferRepository,
            SupplierTransactionRepository supplierTransactionRepository,
            BankDepositRepository bankDepositRepository,
            ExpenseRepository expenseRepository) {
        this.ledgerRepository = ledgerRepository;
        this.warehouseTransferRepository = warehouseTransferRepository;
        this.supplierTransactionRepository = supplierTransactionRepository;
        this.bankDepositRepository = bankDepositRepository;
        this.expenseRepository = expenseRepository;
    }

    /**
     * Get all transactions for the current day (both Sales and Empty Returns) with
     * pagination
     */
    @Transactional(readOnly = true)
    public Page<DayBookDTO> getCurrentDayTransactions(Pageable pageable, String createdBy, String transactionType) {
        LocalDate today = LocalDate.now();
        return getTransactionsByDate(today, pageable, createdBy, transactionType);
    }

    /**
     * Get all transactions for a specific date with pagination
     */
    @Transactional(readOnly = true)
    public Page<DayBookDTO> getTransactionsByDate(LocalDate date, Pageable pageable, String createdBy, String transactionType) {
        List<DayBookDTO> dayBookList = new ArrayList<>();

        // Ledger entries: sales, empty returns, payments, and transfers
        List<CustomerCylinderLedger> ledgers = ledgerRepository.findByTransactionDateAndRefTypeIn(
                date,
                List.of(
                        CustomerCylinderLedger.TransactionType.SALE,
                        CustomerCylinderLedger.TransactionType.EMPTY_RETURN,
                        CustomerCylinderLedger.TransactionType.PAYMENT,
                        CustomerCylinderLedger.TransactionType.TRANSFER));

        ledgers.stream()
                .filter(ledger -> createdBy == null || createdBy.isEmpty() || createdBy.equals(ledger.getCreatedBy()))
                .map(this::convertLedgerToDayBook)
                .forEach(dayBookList::add);

        // Warehouse transfers
        List<WarehouseTransfer> transfers = warehouseTransferRepository.findByDateRange(date, date);
        transfers.stream()
                .filter(transfer -> createdBy == null || createdBy.isEmpty() || createdBy.equals(transfer.getCreatedBy()))
                .map(this::convertWarehouseTransferToDayBook)
                .forEach(dayBookList::add);

        // Supplier transactions
        List<SupplierTransaction> supplierTransactions = supplierTransactionRepository.findByTransactionDate(date);
        supplierTransactions.stream()
                .filter(tx -> createdBy == null || createdBy.isEmpty() || createdBy.equals(tx.getCreatedBy()))
                .map(this::convertSupplierTransactionToDayBook)
                .forEach(dayBookList::add);

        // Bank deposits
        List<BankDeposit> deposits = bankDepositRepository.findByDepositDate(date);
        deposits.stream()
                .filter(deposit -> createdBy == null || createdBy.isEmpty() || createdBy.equals(deposit.getCreatedBy()))
                .map(this::convertBankDepositToDayBook)
                .forEach(dayBookList::add);

        // Expenses
        List<Expense> expenses = expenseRepository.findByExpenseDateBetween(date, date);
        expenses.stream()
                .filter(expense -> createdBy == null || createdBy.isEmpty() || createdBy.equals(expense.getCreatedBy()))
                .map(this::convertExpenseToDayBook)
                .forEach(dayBookList::add);

        // Filter by transaction type (optional)
        if (transactionType != null && !transactionType.isEmpty()) {
            String typeFilter = transactionType.trim().toUpperCase();
            dayBookList = dayBookList.stream()
                    .filter(dto -> dto.getTransactionType() != null
                            && dto.getTransactionType().trim().equalsIgnoreCase(typeFilter))
                    .collect(Collectors.toList());
        }

        // Sort newest first (createdDate if available, else transactionDate)
        dayBookList.sort(
                Comparator.comparing(DayBookDTO::getCreatedDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DayBookDTO::getTransactionDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DayBookDTO::getTransactionType, Comparator.nullsLast(String::compareToIgnoreCase)));

        if (pageable.isUnpaged()) {
            return new PageImpl<>(dayBookList, pageable, dayBookList.size());
        }

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
    public DayBookSummaryDTO getTransactionsByDateSummary(LocalDate date, String createdBy, String transactionType) {
        List<DayBookDTO> dayBookList = getTransactionsByDate(date, Pageable.unpaged(), createdBy, transactionType).getContent();

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
                .filter(dto -> dto.getCustomerId() != null)
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
        dayBook.setCreatedDate(ledger.getCreatedDate());
        dayBook.setCustomerName(ledger.getCustomer() != null ? ledger.getCustomer().getName() : "N/A");
        dayBook.setWarehouseName(ledger.getWarehouse() != null ? ledger.getWarehouse().getName() : "N/A");
        dayBook.setVariantName(ledger.getVariant() != null ? ledger.getVariant().getName() : "N/A");
        dayBook.setPartyName(ledger.getCustomer() != null ? ledger.getCustomer().getName() : "N/A");

        if (ledger.getSale() != null && ledger.getSale().getReferenceNumber() != null) {
            dayBook.setReferenceNumber(ledger.getSale().getReferenceNumber());
        } else if (ledger.getTransactionReference() != null) {
            dayBook.setReferenceNumber(ledger.getTransactionReference());
        }

        // Determine filled and empty counts based on transaction type
        if (ledger.getRefType() == CustomerCylinderLedger.TransactionType.SALE) {
            dayBook.setFilledCount(ledger.getFilledOut());
            dayBook.setEmptyCount(ledger.getEmptyIn());
            dayBook.setDetails(String.format("Variant: %s | Filled: %d | Empty: %d",
                    ledger.getVariant() != null ? ledger.getVariant().getName() : "N/A",
                    ledger.getFilledOut() != null ? ledger.getFilledOut() : 0L,
                    ledger.getEmptyIn() != null ? ledger.getEmptyIn() : 0L));
        } else if (ledger.getRefType() == CustomerCylinderLedger.TransactionType.EMPTY_RETURN) {
            dayBook.setFilledCount(0L);
            dayBook.setEmptyCount(ledger.getEmptyIn());
            dayBook.setDetails(String.format("Variant: %s | Empty Returned: %d",
                    ledger.getVariant() != null ? ledger.getVariant().getName() : "N/A",
                    ledger.getEmptyIn() != null ? ledger.getEmptyIn() : 0L));
        } else if (ledger.getRefType() == CustomerCylinderLedger.TransactionType.PAYMENT) {
            dayBook.setFilledCount(0L);
            dayBook.setEmptyCount(0L);
            dayBook.setDetails("Customer Payment");
        } else if (ledger.getRefType() == CustomerCylinderLedger.TransactionType.TRANSFER) {
            dayBook.setFilledCount(ledger.getFilledOut());
            dayBook.setEmptyCount(ledger.getEmptyIn());
            dayBook.setDetails(String.format("Transfer | Variant: %s | Filled: %d | Empty: %d",
                    ledger.getVariant() != null ? ledger.getVariant().getName() : "N/A",
                    ledger.getFilledOut() != null ? ledger.getFilledOut() : 0L,
                    ledger.getEmptyIn() != null ? ledger.getEmptyIn() : 0L));
        }

        dayBook.setTotalAmount(ledger.getTotalAmount());
        dayBook.setAmountReceived(ledger.getAmountReceived());
        dayBook.setDueAmount(ledger.getDueAmount());
        dayBook.setPaymentMode(ledger.getPaymentMode());
        dayBook.setTransactionType(ledger.getRefType().toString());
        dayBook.setCreatedBy(ledger.getCreatedBy());

        return dayBook;
    }

    private DayBookDTO convertWarehouseTransferToDayBook(WarehouseTransfer transfer) {
        DayBookDTO dayBook = new DayBookDTO();
        dayBook.setId(transfer.getId());
        dayBook.setTransactionDate(transfer.getTransferDate());
        dayBook.setCreatedDate(transfer.getCreatedDate());
        dayBook.setTransactionType("WAREHOUSE_TRANSFER");
        dayBook.setReferenceNumber(transfer.getReferenceNumber());
        dayBook.setPartyName(String.format("%s → %s",
                transfer.getFromWarehouse() != null ? transfer.getFromWarehouse().getName() : "N/A",
                transfer.getToWarehouse() != null ? transfer.getToWarehouse().getName() : "N/A"));
        dayBook.setDetails(String.format("Variant: %s | Qty: %d",
                transfer.getVariant() != null ? transfer.getVariant().getName() : "N/A",
                transfer.getQuantity() != null ? transfer.getQuantity() : 0L));
        dayBook.setTotalAmount(BigDecimal.ZERO);
        dayBook.setAmountReceived(BigDecimal.ZERO);
        dayBook.setDueAmount(BigDecimal.ZERO);
        dayBook.setPaymentMode("-");
        dayBook.setCreatedBy(transfer.getCreatedBy());
        return dayBook;
    }

    private DayBookDTO convertSupplierTransactionToDayBook(SupplierTransaction transaction) {
        DayBookDTO dayBook = new DayBookDTO();
        dayBook.setId(transaction.getId());
        dayBook.setTransactionDate(transaction.getTransactionDate());
        dayBook.setCreatedDate(transaction.getCreatedDate());
        dayBook.setTransactionType("SUPPLIER_TRANSACTION");
        dayBook.setReferenceNumber(transaction.getReference());
        dayBook.setPartyName(transaction.getSupplier() != null ? transaction.getSupplier().getName() : "N/A");
        dayBook.setDetails(String.format("Warehouse: %s | Variant: %s | Filled: %d | Empty: %d",
                transaction.getWarehouse() != null ? transaction.getWarehouse().getName() : "N/A",
                transaction.getVariant() != null ? transaction.getVariant().getName() : "N/A",
                transaction.getFilledReceived() != null ? transaction.getFilledReceived() : 0L,
                transaction.getEmptySent() != null ? transaction.getEmptySent() : 0L));
        dayBook.setTotalAmount(transaction.getAmount());
        dayBook.setAmountReceived(BigDecimal.ZERO);
        dayBook.setDueAmount(BigDecimal.ZERO);
        dayBook.setPaymentMode("-");
        dayBook.setCreatedBy(transaction.getCreatedBy());
        return dayBook;
    }

    private DayBookDTO convertBankDepositToDayBook(BankDeposit deposit) {
        DayBookDTO dayBook = new DayBookDTO();
        dayBook.setId(deposit.getId());
        dayBook.setTransactionDate(deposit.getDepositDate());
        dayBook.setCreatedDate(deposit.getCreatedDate());
        dayBook.setTransactionType("BANK_DEPOSIT");
        dayBook.setReferenceNumber(deposit.getReferenceNumber());
        dayBook.setPartyName(deposit.getBankAccount() != null
                ? deposit.getBankAccount().getBankName() + " - " + deposit.getBankAccount().getAccountNumber()
                : "N/A");
        dayBook.setDetails(deposit.getNotes());
        dayBook.setTotalAmount(deposit.getDepositAmount());
        dayBook.setAmountReceived(BigDecimal.ZERO);
        dayBook.setDueAmount(BigDecimal.ZERO);
        dayBook.setPaymentMode(deposit.getPaymentMode());
        dayBook.setCreatedBy(deposit.getCreatedBy());
        return dayBook;
    }

    private DayBookDTO convertExpenseToDayBook(Expense expense) {
        DayBookDTO dayBook = new DayBookDTO();
        dayBook.setId(expense.getId());
        dayBook.setTransactionDate(expense.getExpenseDate());
        dayBook.setCreatedDate(expense.getCreatedDate());
        dayBook.setTransactionType("EXPENSE");
        dayBook.setReferenceNumber("EXP-" + expense.getId());
        dayBook.setPartyName(expense.getCategory() != null ? expense.getCategory().getName() : "N/A");
        dayBook.setDetails(expense.getDescription());
        dayBook.setTotalAmount(expense.getAmount());
        dayBook.setAmountReceived(BigDecimal.ZERO);
        dayBook.setDueAmount(BigDecimal.ZERO);
        dayBook.setPaymentMode(expense.getPaymentMode());
        dayBook.setCreatedBy(expense.getCreatedBy());
        return dayBook;
    }
}
