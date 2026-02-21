package com.gasagency.service;

import com.gasagency.dto.response.CustomerCylinderLedgerDTO;
import com.gasagency.dto.response.CustomerLedgerSummaryDTO;
import com.gasagency.dto.response.CustomerLedgerVariantSummaryDTO;
import com.gasagency.dto.request.LedgerUpdateRequestDTO;
import com.gasagency.dto.request.PaymentRequestDTO;
import com.gasagency.dto.response.WarehouseTransferDTO;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Warehouse;
import com.gasagency.entity.BankAccount;
import com.gasagency.entity.PaymentMode;
import com.gasagency.entity.Sale;
import com.gasagency.entity.WarehouseTransfer;
import com.gasagency.entity.BankAccountLedger;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import com.gasagency.repository.CustomerRepository;
import com.gasagency.repository.CylinderVariantRepository;
import com.gasagency.repository.WarehouseRepository;
import com.gasagency.repository.BankAccountRepository;
import com.gasagency.repository.PaymentModeRepository;
import com.gasagency.repository.SaleRepository;
import com.gasagency.repository.WarehouseTransferRepository;
import com.gasagency.repository.BankAccountLedgerRepository;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.util.LoggerUtil;
import com.gasagency.util.ReferenceNumberGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import com.gasagency.dto.response.CustomerBalanceDTO;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CustomerCylinderLedgerService {

        private static final Logger logger = LoggerFactory.getLogger(CustomerCylinderLedgerService.class);
        private final CustomerCylinderLedgerRepository repository;
        private final CustomerRepository customerRepository;
        private final CylinderVariantRepository variantRepository;
        private final WarehouseRepository warehouseRepository;
        private final InventoryStockService inventoryStockService;
        private final WarehouseTransferService warehouseTransferService;
        private final BankAccountRepository bankAccountRepository;
        private final ReferenceNumberGenerator referenceNumberGenerator;
        private final SaleRepository saleRepository;
        private final WarehouseTransferRepository warehouseTransferRepository;
        private final BankAccountLedgerRepository bankAccountLedgerRepository;
        private final PaymentModeRepository paymentModeRepository;

        public CustomerCylinderLedgerService(CustomerCylinderLedgerRepository repository,
                        CustomerRepository customerRepository,
                        CylinderVariantRepository variantRepository,
                        WarehouseRepository warehouseRepository,
                        InventoryStockService inventoryStockService,
                        WarehouseTransferService warehouseTransferService,
                        BankAccountRepository bankAccountRepository,
                        ReferenceNumberGenerator referenceNumberGenerator,
                        SaleRepository saleRepository,
                        WarehouseTransferRepository warehouseTransferRepository,
                        BankAccountLedgerRepository bankAccountLedgerRepository,
                        PaymentModeRepository paymentModeRepository) {
                this.repository = repository;
                this.customerRepository = customerRepository;
                this.variantRepository = variantRepository;
                this.warehouseRepository = warehouseRepository;
                this.inventoryStockService = inventoryStockService;
                this.warehouseTransferService = warehouseTransferService;
                this.bankAccountRepository = bankAccountRepository;
                this.referenceNumberGenerator = referenceNumberGenerator;
                this.saleRepository = saleRepository;
                this.warehouseTransferRepository = warehouseTransferRepository;
                this.bankAccountLedgerRepository = bankAccountLedgerRepository;
                this.paymentModeRepository = paymentModeRepository;
        }

        // Get all ledger entries sorted by date descending (for stock movement history)
        public List<CustomerCylinderLedgerDTO> getAllMovements() {
                List<CustomerCylinderLedgerDTO> ledgerMovements = repository.findAll().stream()
                                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                                .map(this::toDTO)
                                .collect(Collectors.toList());

                // Get all warehouse transfers
                try {
                        List<WarehouseTransferDTO> allTransfers = warehouseTransferService
                                        .getAllTransfers();
                        if (allTransfers != null && !allTransfers.isEmpty()) {
                                List<CustomerCylinderLedgerDTO> transferMovements = allTransfers.stream()
                                                .map(this::transferToLedgerDTO)
                                                .collect(Collectors.toList());

                                // Combine and sort by date, then by createdDate (latest timestamp first)
                                List<CustomerCylinderLedgerDTO> combined = new ArrayList<>();
                                combined.addAll(ledgerMovements);
                                combined.addAll(transferMovements);
                                combined.sort((a, b) -> {
                                        LocalDate dateA = a.getTransactionDate() != null ? a.getTransactionDate()
                                                        : LocalDate.now();
                                        LocalDate dateB = b.getTransactionDate() != null ? b.getTransactionDate()
                                                        : LocalDate.now();
                                        int dateComparison = dateB.compareTo(dateA);
                                        if (dateComparison != 0) {
                                                return dateComparison;
                                        }
                                        // If dates are equal, sort by createdDate descending (latest record first)
                                        java.time.LocalDateTime timeA = a.getCreatedAt();
                                        java.time.LocalDateTime timeB = b.getCreatedAt();
                                        if (timeA == null)
                                                timeA = java.time.LocalDateTime.MIN;
                                        if (timeB == null)
                                                timeB = java.time.LocalDateTime.MIN;
                                        return timeB.compareTo(timeA);
                                });

                                return combined;
                        }
                } catch (Exception e) {
                        LoggerUtil.logBusinessError(logger, "GET_ALL_MOVEMENTS", "Error fetching transfers");
                }

                return ledgerMovements;
        }

        /**
         * Paginated ledger movements (ledger entries only)
         * Use this for high-performance movement history screens.
         */
        public Page<CustomerCylinderLedgerDTO> getAllMovements(Pageable pageable) {
                return repository.findAll(pageable)
                                .map(this::toDTO);
        }

        public Page<CustomerCylinderLedgerDTO> getAllMovements(Pageable pageable, Long variantId,
                        CustomerCylinderLedger.TransactionType refType) {
                return repository.findMovementsFiltered(variantId, refType, pageable)
                                .map(this::toDTO);
        }

        public Page<CustomerCylinderLedgerDTO> getAllMovements(Pageable pageable, Long variantId, String refType) {
                return getAllMovements(pageable, variantId, parseRefType(refType));
        }

        /**
         * Paginated movements including warehouse transfers (merged in memory)
         * Use when Transfer entries must be shown alongside ledger entries.
         */
        public Page<CustomerCylinderLedgerDTO> getAllMovementsMerged(Pageable pageable, Long variantId,
                        CustomerCylinderLedger.TransactionType refType) {
                List<CustomerCylinderLedgerDTO> combined = new ArrayList<>();

                boolean includeTransfers = refType == null
                                || refType == CustomerCylinderLedger.TransactionType.TRANSFER;
                boolean includeLedgers = refType == null
                                || refType != CustomerCylinderLedger.TransactionType.TRANSFER;

                if (includeLedgers) {
                        List<CustomerCylinderLedgerDTO> ledgers = repository
                                        .findMovementsFiltered(variantId, refType, Pageable.unpaged())
                                        .getContent()
                                        .stream()
                                        .map(this::toDTO)
                                        .collect(Collectors.toList());
                        combined.addAll(ledgers);
                }

                if (includeTransfers) {
                        List<WarehouseTransferDTO> transfers = warehouseTransferService
                                        .getAllTransfers(variantId);
                        List<CustomerCylinderLedgerDTO> transferMovements = transfers.stream()
                                        .map(this::transferToLedgerDTO)
                                        .collect(Collectors.toList());
                        combined.addAll(transferMovements);
                }

                combined.sort((a, b) -> {
                        LocalDate dateA = a.getTransactionDate() != null ? a.getTransactionDate() : LocalDate.now();
                        LocalDate dateB = b.getTransactionDate() != null ? b.getTransactionDate() : LocalDate.now();
                        int dateComparison = dateB.compareTo(dateA);
                        if (dateComparison != 0) {
                                return dateComparison;
                        }
                        java.time.LocalDateTime timeA = a.getCreatedAt() != null ? a.getCreatedAt()
                                        : java.time.LocalDateTime.MIN;
                        java.time.LocalDateTime timeB = b.getCreatedAt() != null ? b.getCreatedAt()
                                        : java.time.LocalDateTime.MIN;
                        return timeB.compareTo(timeA);
                });

                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), combined.size());
                List<CustomerCylinderLedgerDTO> pageContent = start >= combined.size()
                                ? java.util.Collections.emptyList()
                                : combined.subList(start, end);
                return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, combined.size());
        }

        public Page<CustomerCylinderLedgerDTO> getAllMovementsMerged(Pageable pageable, Long variantId, String refType) {
                return getAllMovementsMerged(pageable, variantId, parseRefType(refType));
        }

        // Get ledger entries for a specific warehouse sorted by date descending
        public List<CustomerCylinderLedgerDTO> getMovementsByWarehouse(Long warehouseId) {
                // Get customer ledger movements for this warehouse
                List<CustomerCylinderLedgerDTO> ledgerMovements = repository.findByWarehouseId(warehouseId).stream()
                                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                                .map(this::toDTO)
                                .collect(Collectors.toList());

                // Get warehouse transfers involving this warehouse
                try {
                        List<WarehouseTransferDTO> allTransfers = warehouseTransferService
                                        .getTransfersForWarehouseAndVariant(warehouseId, null);
                        if (allTransfers != null && !allTransfers.isEmpty()) {
                                List<CustomerCylinderLedgerDTO> transferMovements = allTransfers.stream()
                                                .map(this::transferToLedgerDTO)
                                                .collect(Collectors.toList());

                                // Combine and sort by date, then by createdDate (latest timestamp first)
                                List<CustomerCylinderLedgerDTO> combined = new ArrayList<>();
                                combined.addAll(ledgerMovements);
                                combined.addAll(transferMovements);
                                combined.sort((a, b) -> {
                                        LocalDate dateA = a.getTransactionDate() != null ? a.getTransactionDate()
                                                        : LocalDate.now();
                                        LocalDate dateB = b.getTransactionDate() != null ? b.getTransactionDate()
                                                        : LocalDate.now();
                                        int dateComparison = dateB.compareTo(dateA);
                                        if (dateComparison != 0) {
                                                return dateComparison;
                                        }
                                        // If dates are equal, sort by createdDate descending (latest record first)
                                        java.time.LocalDateTime timeA = a.getCreatedAt();
                                        java.time.LocalDateTime timeB = b.getCreatedAt();
                                        if (timeA == null)
                                                timeA = java.time.LocalDateTime.MIN;
                                        if (timeB == null)
                                                timeB = java.time.LocalDateTime.MIN;
                                        return timeB.compareTo(timeA);
                                });

                                return combined;
                        }
                } catch (Exception e) {
                        LoggerUtil.logBusinessError(logger, "GET_MOVEMENTS_BY_WAREHOUSE", "Error fetching transfers",
                                        "warehouseId", warehouseId);
                }

                return ledgerMovements;
        }

        /**
         * Paginated ledger movements for a warehouse (ledger entries only)
         */
        public Page<CustomerCylinderLedgerDTO> getMovementsByWarehouse(Long warehouseId, Pageable pageable) {
                return repository.findByWarehouseId(warehouseId, pageable)
                                .map(this::toDTO);
        }

        public Page<CustomerCylinderLedgerDTO> getMovementsByWarehouse(Long warehouseId, Pageable pageable,
                        Long variantId, CustomerCylinderLedger.TransactionType refType) {
                return repository.findMovementsFilteredByWarehouse(warehouseId, variantId, refType, pageable)
                                .map(this::toDTO);
        }

        public Page<CustomerCylinderLedgerDTO> getMovementsByWarehouse(Long warehouseId, Pageable pageable,
                        Long variantId, String refType) {
                return getMovementsByWarehouse(warehouseId, pageable, variantId, parseRefType(refType));
        }

        /**
         * Paginated movements including transfers for a warehouse (merged in memory)
         */
        public Page<CustomerCylinderLedgerDTO> getMovementsByWarehouseMerged(Long warehouseId, Pageable pageable,
                        Long variantId, CustomerCylinderLedger.TransactionType refType) {
                List<CustomerCylinderLedgerDTO> combined = new ArrayList<>();

                boolean includeTransfers = refType == null
                                || refType == CustomerCylinderLedger.TransactionType.TRANSFER;
                boolean includeLedgers = refType == null
                                || refType != CustomerCylinderLedger.TransactionType.TRANSFER;

                if (includeLedgers) {
                        List<CustomerCylinderLedgerDTO> ledgers = repository
                                        .findMovementsFilteredByWarehouse(warehouseId, variantId, refType,
                                                        Pageable.unpaged())
                                        .getContent()
                                        .stream()
                                        .map(this::toDTO)
                                        .collect(Collectors.toList());
                        combined.addAll(ledgers);
                }

                if (includeTransfers) {
                        List<WarehouseTransferDTO> transfers = warehouseTransferService
                                        .getTransfersForWarehouseAndVariant(warehouseId, variantId);
                        List<CustomerCylinderLedgerDTO> transferMovements = transfers.stream()
                                        .map(this::transferToLedgerDTO)
                                        .collect(Collectors.toList());
                        combined.addAll(transferMovements);
                }

                combined.sort((a, b) -> {
                        LocalDate dateA = a.getTransactionDate() != null ? a.getTransactionDate() : LocalDate.now();
                        LocalDate dateB = b.getTransactionDate() != null ? b.getTransactionDate() : LocalDate.now();
                        int dateComparison = dateB.compareTo(dateA);
                        if (dateComparison != 0) {
                                return dateComparison;
                        }
                        java.time.LocalDateTime timeA = a.getCreatedAt() != null ? a.getCreatedAt()
                                        : java.time.LocalDateTime.MIN;
                        java.time.LocalDateTime timeB = b.getCreatedAt() != null ? b.getCreatedAt()
                                        : java.time.LocalDateTime.MIN;
                        return timeB.compareTo(timeA);
                });

                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), combined.size());
                List<CustomerCylinderLedgerDTO> pageContent = start >= combined.size()
                                ? java.util.Collections.emptyList()
                                : combined.subList(start, end);
                return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, combined.size());
        }

        public Page<CustomerCylinderLedgerDTO> getMovementsByWarehouseMerged(Long warehouseId, Pageable pageable,
                        Long variantId, String refType) {
                return getMovementsByWarehouseMerged(warehouseId, pageable, variantId, parseRefType(refType));
        }

        private CustomerCylinderLedger.TransactionType parseRefType(String refType) {
                if (refType == null || refType.trim().isEmpty()) {
                        return null;
                }
                String normalized = refType.trim().toUpperCase();
                if ("RETURN".equals(normalized)) {
                        return CustomerCylinderLedger.TransactionType.EMPTY_RETURN;
                }
                return CustomerCylinderLedger.TransactionType.valueOf(normalized);
        }

        /**
         * Returns balances for all customers on a page (all active variants per
         * customer)
         */
        public List<CustomerBalanceDTO> getCustomerBalancesForPage(int page, int size) {
                // Get paged customers
                Pageable pageable = PageRequest.of(page,
                                size);
                Page<Customer> customerPage = customerRepository.findAll(pageable);
                List<Customer> customers = customerPage.getContent();
                List<CylinderVariant> variants = variantRepository.findAllByActive(true);
                Map<Long, Map<Long, CustomerCylinderLedger>> latestByCustomerVariant = new HashMap<>();
                if (!customers.isEmpty()) {
                        List<CustomerCylinderLedger> latestLedgers = repository
                                        .findLatestPerCustomerVariantForCustomers(customers);
                        for (CustomerCylinderLedger ledger : latestLedgers) {
                                if (ledger.getCustomer() == null || ledger.getVariant() == null) {
                                        continue;
                                }
                                Long customerId = ledger.getCustomer().getId();
                                Long variantId = ledger.getVariant().getId();
                                latestByCustomerVariant
                                                .computeIfAbsent(customerId, k -> new HashMap<>())
                                                .put(variantId, ledger);
                        }
                }
                List<CustomerBalanceDTO> result = new ArrayList<>();
                for (Customer customer : customers) {
                        List<CustomerBalanceDTO.VariantBalance> variantBalances = new ArrayList<>();
                        for (CylinderVariant variant : variants) {
                                CustomerCylinderLedger ledger = latestByCustomerVariant
                                                .getOrDefault(customer.getId(), java.util.Collections.emptyMap())
                                                .get(variant.getId());
                                Long balance = ledger != null && ledger.getBalance() != null
                                                ? ledger.getBalance()
                                                : 0L;
                                variantBalances.add(new CustomerBalanceDTO.VariantBalance(
                                                variant.getId(), variant.getName(), balance));
                        }
                        result.add(new CustomerBalanceDTO(
                                        customer.getId(), customer.getName(), variantBalances));
                }
                return result;
        }

        public List<CustomerBalanceDTO> getCustomerBalancesForCustomers(List<Long> customerIds) {
                if (customerIds == null || customerIds.isEmpty()) {
                        return new ArrayList<>();
                }
                List<Customer> customers = customerRepository.findAllById(customerIds);
                Map<Long, Customer> customerMap = customers.stream()
                                .collect(Collectors.toMap(Customer::getId, c -> c));
                List<CylinderVariant> variants = variantRepository.findAllByActive(true);
                Map<Long, Map<Long, CustomerCylinderLedger>> latestByCustomerVariant = new HashMap<>();
                if (!customers.isEmpty()) {
                        List<CustomerCylinderLedger> latestLedgers = repository
                                        .findLatestPerCustomerVariantForCustomers(customers);
                        for (CustomerCylinderLedger ledger : latestLedgers) {
                                if (ledger.getCustomer() == null || ledger.getVariant() == null) {
                                        continue;
                                }
                                Long customerId = ledger.getCustomer().getId();
                                Long variantId = ledger.getVariant().getId();
                                latestByCustomerVariant
                                                .computeIfAbsent(customerId, k -> new HashMap<>())
                                                .put(variantId, ledger);
                        }
                }
                List<CustomerBalanceDTO> result = new ArrayList<>();
                for (Long customerId : customerIds) {
                        Customer customer = customerMap.get(customerId);
                        if (customer == null) {
                                continue;
                        }
                        List<CustomerBalanceDTO.VariantBalance> variantBalances = new ArrayList<>();
                        for (CylinderVariant variant : variants) {
                                CustomerCylinderLedger ledger = latestByCustomerVariant
                                                .getOrDefault(customer.getId(), java.util.Collections.emptyMap())
                                                .get(variant.getId());
                                Long balance = ledger != null && ledger.getBalance() != null
                                                ? ledger.getBalance()
                                                : 0L;
                                variantBalances.add(new CustomerBalanceDTO.VariantBalance(
                                                variant.getId(), variant.getName(), balance));
                        }
                        result.add(new CustomerBalanceDTO(
                                        customer.getId(), customer.getName(), variantBalances));
                }
                return result;
        }

        public List<CustomerCylinderLedgerDTO> getAllPendingBalances() {
                List<CustomerCylinderLedgerDTO> result = new java.util.ArrayList<>();
                List<Customer> customers = customerRepository.findAllByActive(true);
                if (customers.isEmpty()) {
                        return result;
                }
                List<CustomerCylinderLedger> latestLedgers = repository
                                .findLatestPositiveBalancesForCustomers(customers);
                for (CustomerCylinderLedger ledger : latestLedgers) {
                        result.add(toDTO(ledger));
                }
                return result;
        }

        @Transactional
        public CustomerCylinderLedgerDTO createLedgerEntry(Long customerId, Long warehouseId, Long variantId,
                        LocalDate transactionDate, String refType, Long refId,
                        Long filledOut, Long emptyIn) {
                return createLedgerEntry(customerId, warehouseId, variantId, transactionDate, refType, refId,
                                filledOut, emptyIn, false, null);
        }

        @Transactional
        public CustomerCylinderLedgerDTO createLedgerEntry(Long customerId, Long warehouseId, Long variantId,
                        LocalDate transactionDate, String refType, Long refId,
                        Long filledOut, Long emptyIn, boolean ignoreEmptyForBalance, String note) {
                LoggerUtil.logBusinessEntry(logger, "CREATE_LEDGER_ENTRY", "customerId", customerId, "warehouseId",
                                warehouseId, "variantId", variantId);

                Customer customer = customerRepository.findByIdForUpdate(customerId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "CREATE_LEDGER_ENTRY", "Customer not found",
                                                        "customerId", customerId);
                                        return new ResourceNotFoundException(
                                                        "Customer not found with id: " + customerId);
                                });

                Warehouse warehouse = null;
                if (warehouseId != null) {
                        warehouse = warehouseRepository.findById(warehouseId)
                                        .orElseThrow(() -> {
                                                LoggerUtil.logBusinessError(logger, "CREATE_LEDGER_ENTRY",
                                                                "Warehouse not found",
                                                                "warehouseId", warehouseId);
                                                return new ResourceNotFoundException(
                                                                "Warehouse not found with id: " + warehouseId);
                                        });
                }

                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "CREATE_LEDGER_ENTRY", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });

                // Validate inputs
                if (filledOut < 0 || emptyIn < 0) {
                        LoggerUtil.logBusinessError(logger, "CREATE_LEDGER_ENTRY", "Negative quantities", "filledOut",
                                        filledOut, "emptyIn", emptyIn);
                        throw new IllegalArgumentException("Quantities cannot be negative");
                }

                // === PREVENT DUPLICATE LEDGER ENTRIES ===
                CustomerCylinderLedger.TransactionType type = CustomerCylinderLedger.TransactionType.valueOf(refType);
                if (refId != null && type == CustomerCylinderLedger.TransactionType.SALE) {
                        // Check if this transaction already exists (with lock to prevent race
                        // condition)
                        java.util.Optional<CustomerCylinderLedger> existingLedger = repository.findByRefIdWithLock(
                                        customerId, variantId, refId, type);
                        if (existingLedger.isPresent()) {
                                LoggerUtil.logBusinessError(logger, "CREATE_LEDGER_ENTRY",
                                                "Duplicate ledger entry attempted", "customerId", customerId,
                                                "refId", refId, "refType", refType);
                                throw new IllegalArgumentException(
                                                "Ledger entry already exists for this transaction. Duplicate entry prevented.");
                        }
                }

                // Calculate balance correctly
                // filledOut = cylinders given to customer (increases balance)
                // emptyIn = empty cylinders returned (decreases balance)
                // Get latest balance with lock to ensure consistency
                Long previousBalance = getPreviousBalanceWithLock(customerId, variantId);
                // Prevent returning more empties than the customer currently holds
                if (!ignoreEmptyForBalance && emptyIn > previousBalance + filledOut) {
                        LoggerUtil.logBusinessError(logger, "CREATE_LEDGER_ENTRY",
                                        "Empty return exceeds filled cylinders held (after this sale)", "customerId",
                                        customerId,
                                        "variantId", variantId, "previousBalance", previousBalance, "filledOut",
                                        filledOut, "emptyIn", emptyIn);
                        throw new IllegalArgumentException(
                                        "Cannot return more empty cylinders than the customer will hold for this variant after this sale.");
                }
                Long balance = ignoreEmptyForBalance
                                ? previousBalance + filledOut
                                : previousBalance + filledOut - emptyIn;

                // For transaction types that require a reference, enforce refId not null
                if ((type == CustomerCylinderLedger.TransactionType.SALE) && refId == null) {
                        throw new IllegalArgumentException("Reference ID is required for SALE transactions");
                }

                // Create new ledger entry for each transaction
                CustomerCylinderLedger ledger = new CustomerCylinderLedger(
                                customer, warehouse, variant, transactionDate,
                                type,
                                refId, filledOut, emptyIn, balance);
                if (note != null && !note.trim().isEmpty()) {
                        ledger.setNote(note.trim());
                }
                ledger = repository.save(ledger);

                // Populate transaction reference based on transaction type (Industry Standard -
                // Denormalized for Performance)
                String transactionReference = null;
                if (refId != null) {
                        try {
                                switch (type) {
                                        case SALE:
                                                Sale sale = saleRepository.findById(refId).orElse(null);
                                                if (sale != null) {
                                                        transactionReference = sale.getReferenceNumber();
                                                        ledger.setSale(sale); // Set the sale object directly
                                                        ledger = repository.save(ledger); // Save immediately after
                                                                                          // setting sale
                                                }
                                                break;
                                        case TRANSFER:
                                                WarehouseTransfer transfer = warehouseTransferRepository.findById(refId)
                                                                .orElse(null);
                                                if (transfer != null) {
                                                        transactionReference = transfer.getReferenceNumber();
                                                }
                                                break;
                                        case PAYMENT:
                                                BankAccountLedger bankLedger = bankAccountLedgerRepository
                                                                .findById(refId).orElse(null);
                                                if (bankLedger != null) {
                                                        transactionReference = bankLedger.getReferenceNumber();
                                                }
                                                break;
                                        default:
                                                break;
                                }

                                if (transactionReference != null) {
                                        ledger.setTransactionReference(transactionReference);
                                }
                        } catch (Exception e) {
                                logger.warn("Could not fetch transaction reference for type: {} and refId: {}", type,
                                                refId, e);
                        }
                }

                // Generate return reference for EMPTY_RETURN transactions
                if (type == CustomerCylinderLedger.TransactionType.EMPTY_RETURN && warehouse != null) {
                        String returnReference = referenceNumberGenerator.generateEmptyReturnReference(warehouse);
                        ledger.setTransactionReference(returnReference);
                        ledger = repository.save(ledger);
                        logger.info("Return reference generated for empty return: {} - Reference: {}",
                                        ledger.getId(), returnReference);
                } else if (transactionReference != null || ledger.getTransactionReference() != null) {
                        ledger = repository.save(ledger);
                }

                LoggerUtil.logBusinessSuccess(logger, "CREATE_LEDGER_ENTRY", "id", ledger.getId(), "customerId",
                                customerId, "balance", balance);
                LoggerUtil.logAudit("CREATE", "LEDGER", "ledgerId", ledger.getId(), "customerId", customerId);

                // Update inventory for EMPTY_RETURN
                if (type == CustomerCylinderLedger.TransactionType.EMPTY_RETURN && emptyIn > 0) {
                        inventoryStockService.incrementEmptyQty(warehouse, variant, emptyIn);
                }

                return toDTO(ledger);
        }

        // Overloaded method for SALE transactions with amount details
        @Transactional
        public CustomerCylinderLedgerDTO createLedgerEntry(Long customerId, Long warehouseId, Long variantId,
                        LocalDate transactionDate, String refType, Long refId,
                        Long filledOut, Long emptyIn, BigDecimal totalAmount, BigDecimal amountReceived) {

                return createLedgerEntry(customerId, warehouseId, variantId, transactionDate, refType, refId,
                                filledOut, emptyIn, totalAmount, amountReceived, false, null);
        }

        @Transactional
        public CustomerCylinderLedgerDTO createLedgerEntry(Long customerId, Long warehouseId, Long variantId,
                        LocalDate transactionDate, String refType, Long refId,
                        Long filledOut, Long emptyIn, BigDecimal totalAmount, BigDecimal amountReceived,
                        boolean ignoreEmptyForBalance, String note) {

                CustomerCylinderLedgerDTO dto = createLedgerEntry(customerId, warehouseId, variantId,
                                transactionDate, refType, refId, filledOut, emptyIn, ignoreEmptyForBalance, note);

                // Update the created entry with amount details
                if (totalAmount != null || amountReceived != null) {
                        CustomerCylinderLedger ledger = repository.findById(dto.getId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Ledger entry not found"));

                        if (totalAmount != null) {
                                ledger.setTotalAmount(totalAmount);
                        }
                        if (amountReceived != null) {
                                ledger.setAmountReceived(amountReceived);
                        }

                        // Calculate running balance (due amount) - cumulative from all previous
                        // transactions
                        // Special case: if totalAmount is 0, check if there's amountReceived
                        // (payment-only transaction)
                        if (totalAmount != null && totalAmount.compareTo(java.math.BigDecimal.ZERO) == 0) {
                                final Long ledgerId = ledger.getId();
                                // Get previous running balance from latest entry
                                List<CustomerCylinderLedger> previousEntries = repository
                                                .findLatestByCustomerIdExcludingId(
                                                                ledger.getCustomer().getId(),
                                                                ledgerId,
                                                                PageRequest.of(0, 1));

                                BigDecimal previousBalance = BigDecimal.ZERO;
                                if (!previousEntries.isEmpty()) {
                                        previousBalance = previousEntries.get(0).getDueAmount() != null
                                                        ? previousEntries.get(0).getDueAmount()
                                                        : java.math.BigDecimal.ZERO;
                                }

                                // If amountReceived is provided, subtract it from previous balance (payment
                                // reduces due amount)
                                // Otherwise just carry forward the previous balance
                                if (amountReceived != null && amountReceived.compareTo(java.math.BigDecimal.ZERO) > 0) {
                                        BigDecimal newDue = previousBalance.subtract(amountReceived);
                                        // Ensure due amount doesn't go negative (customer overpaid)
                                        if (newDue.signum() < 0) {
                                                newDue = java.math.BigDecimal.ZERO;
                                        }
                                        ledger.setDueAmount(newDue);
                                } else {
                                        ledger.setDueAmount(previousBalance);
                                }
                        } else if (totalAmount != null && amountReceived != null) {
                                final Long ledgerId = ledger.getId();
                                // Get previous running balance from latest entry
                                List<CustomerCylinderLedger> previousEntries = repository
                                                .findLatestByCustomerIdExcludingId(
                                                                ledger.getCustomer().getId(),
                                                                ledgerId,
                                                                PageRequest.of(0, 1));

                                BigDecimal previousBalance = BigDecimal.ZERO;
                                if (!previousEntries.isEmpty()) {
                                        previousBalance = previousEntries.get(0).getDueAmount() != null
                                                        ? previousEntries.get(0).getDueAmount()
                                                        : java.math.BigDecimal.ZERO;
                                }

                                // Cumulative balance = previous balance + current transaction amount - payment
                                // received
                                BigDecimal currentTransactionDue = totalAmount.subtract(amountReceived);
                                BigDecimal cumulativeDue = previousBalance.add(currentTransactionDue);
                                // Ensure due amount doesn't go negative (customer overpaid)
                                if (cumulativeDue.signum() < 0) {
                                        cumulativeDue = java.math.BigDecimal.ZERO;
                                }
                                ledger.setDueAmount(cumulativeDue);
                        } else if (totalAmount != null) {
                                final Long ledgerId = ledger.getId();
                                // No payment received in this transaction
                                List<CustomerCylinderLedger> previousEntries = repository
                                                .findLatestByCustomerIdExcludingId(
                                                                ledger.getCustomer().getId(),
                                                                ledgerId,
                                                                PageRequest.of(0, 1));

                                BigDecimal previousBalance = BigDecimal.ZERO;
                                if (!previousEntries.isEmpty()) {
                                        previousBalance = previousEntries.get(0).getDueAmount() != null
                                                        ? previousEntries.get(0).getDueAmount()
                                                        : java.math.BigDecimal.ZERO;
                                }

                                // Cumulative balance = previous balance + current transaction amount
                                BigDecimal cumulativeDue = previousBalance.add(totalAmount);
                                ledger.setDueAmount(cumulativeDue);
                        }

                        ledger = repository.save(ledger);
                        return toDTO(ledger);
                }

                return dto;
        }

        @Transactional
        public CustomerCylinderLedgerDTO createLedgerEntry(Long customerId, Long warehouseId, Long variantId,
                        LocalDate transactionDate, String refType, Long refId,
                        Long filledOut, Long emptyIn, BigDecimal totalAmount, BigDecimal amountReceived,
                        String modeOfPayment) {

                CustomerCylinderLedgerDTO dto = createLedgerEntry(customerId, warehouseId, variantId,
                                transactionDate, refType, refId, filledOut, emptyIn, totalAmount, amountReceived);

                // Update the created entry with payment mode
                if (modeOfPayment != null) {
                        CustomerCylinderLedger ledger = repository.findById(dto.getId())
                                        .orElseThrow(() -> new RuntimeException("Ledger entry not found"));
                        ledger.setPaymentMode(modeOfPayment);
                        ledger = repository.save(ledger);
                        return toDTO(ledger);
                }

                return dto;
        }

        @Transactional
        public CustomerCylinderLedgerDTO createLedgerEntry(Long customerId, Long warehouseId, Long variantId,
                        LocalDate transactionDate, String refType, Long refId,
                        Long filledOut, Long emptyIn, BigDecimal totalAmount, BigDecimal amountReceived,
                        String modeOfPayment, Long bankAccountId) {

                CustomerCylinderLedgerDTO dto = createLedgerEntry(customerId, warehouseId, variantId,
                                transactionDate, refType, refId, filledOut, emptyIn, totalAmount, amountReceived,
                                modeOfPayment);

                // Update the created entry with bank account ID
                if (bankAccountId != null) {
                        CustomerCylinderLedger ledger = repository.findById(dto.getId())
                                        .orElseThrow(() -> new RuntimeException("Ledger entry not found"));

                        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Bank account not found with id: " + bankAccountId));

                        ledger.setBankAccount(bankAccount);
                        ledger = repository.save(ledger);
                        return toDTO(ledger);
                }

                return dto;
        }

        @Transactional
        public CustomerCylinderLedgerDTO createLedgerEntry(Long customerId, Long warehouseId, Long variantId,
                        LocalDate transactionDate, String refType, Long refId,
                        Long filledOut, Long emptyIn, BigDecimal totalAmount, BigDecimal amountReceived,
                        String modeOfPayment, Long bankAccountId, boolean ignoreEmptyForBalance, String note) {

                CustomerCylinderLedgerDTO dto = createLedgerEntry(customerId, warehouseId, variantId,
                                transactionDate, refType, refId, filledOut, emptyIn, totalAmount, amountReceived,
                                ignoreEmptyForBalance, note);

                if (modeOfPayment != null && !modeOfPayment.trim().isEmpty()) {
                        CustomerCylinderLedger ledger = repository.findById(dto.getId())
                                        .orElseThrow(() -> new RuntimeException("Ledger entry not found"));
                        ledger.setPaymentMode(modeOfPayment);
                        if (bankAccountId != null && bankAccountId > 0) {
                                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                                .orElse(null);
                                if (bankAccount != null) {
                                        ledger.setBankAccount(bankAccount);
                                }
                        }
                        ledger = repository.save(ledger);
                        return toDTO(ledger);
                }

                return dto;
        }

        public CustomerCylinderLedgerDTO getLedgerEntryById(Long id) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "LEDGER", "id", id);

                CustomerCylinderLedger ledger = repository.findById(id)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_LEDGER_ENTRY",
                                                        "Ledger entry not found", "id", id);
                                        return new ResourceNotFoundException(
                                                        "Ledger entry not found with id: " + id);
                                });
                return toDTO(ledger);
        }

        public List<CustomerCylinderLedgerDTO> getAllLedger() {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_ALL", "LEDGER");

                return repository.findAll().stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public Page<CustomerCylinderLedgerDTO> getAllLedger(Pageable pageable) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "LEDGER", "page", pageable.getPageNumber(),
                                "size", pageable.getPageSize());

                return repository.findAll(pageable)
                                .map(this::toDTO);
        }

        public Page<CustomerCylinderLedgerDTO> getEmptyReturns(LocalDate fromDate, LocalDate toDate, Long customerId,
                        Long variantId, String createdBy, Pageable pageable) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_EMPTY_RETURNS", "LEDGER",
                                "fromDate", fromDate, "toDate", toDate, "customerId", customerId,
                                "variantId", variantId, "page", pageable.getPageNumber(), "size",
                                pageable.getPageSize());

                return repository.findEmptyReturns(fromDate, toDate, customerId, variantId,
                                (createdBy != null && !createdBy.isEmpty()) ? createdBy : null,
                                pageable)
                                .map(this::toDTO);
        }

        public Page<CustomerCylinderLedgerDTO> getPayments(LocalDate fromDate, LocalDate toDate, Long customerId,
                        String paymentMode, Long bankAccountId, String createdBy, Pageable pageable) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_PAYMENTS", "LEDGER",
                                "fromDate", fromDate, "toDate", toDate, "customerId", customerId,
                                "paymentMode", paymentMode, "bankAccountId", bankAccountId,
                                "page", pageable.getPageNumber(), "size", pageable.getPageSize());

                return repository.findPayments(
                                fromDate,
                                toDate,
                                customerId,
                                (paymentMode != null && !paymentMode.isEmpty()) ? paymentMode : null,
                                bankAccountId,
                                (createdBy != null && !createdBy.isEmpty()) ? createdBy : null,
                                pageable)
                                .map(this::toDTO);
        }

        public java.math.BigDecimal getPaymentsSummary(LocalDate fromDate, LocalDate toDate, Long customerId,
                        String paymentMode, Long bankAccountId, String createdBy) {
                return repository.sumPayments(
                                fromDate,
                                toDate,
                                customerId,
                                (paymentMode != null && !paymentMode.isEmpty()) ? paymentMode : null,
                                bankAccountId,
                                (createdBy != null && !createdBy.isEmpty()) ? createdBy : null);
        }

        public List<CustomerCylinderLedgerDTO> getLedgerByCustomer(Long customerId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "LEDGER", "customerId", customerId);

                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_LEDGER_BY_CUSTOMER",
                                                        "Customer not found", "customerId", customerId);
                                        return new ResourceNotFoundException(
                                                        "Customer not found with id: " + customerId);
                                });
                return repository.findByCustomer(customer).stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public Page<CustomerCylinderLedgerDTO> getLedgerByCustomer(Long customerId, Pageable pageable) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT_PAGINATED", "LEDGER", "customerId", customerId,
                                "page", pageable.getPageNumber(), "size", pageable.getPageSize());

                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_LEDGER_BY_CUSTOMER_PAGINATED",
                                                        "Customer not found", "customerId", customerId);
                                        return new ResourceNotFoundException(
                                                        "Customer not found with id: " + customerId);
                                });
                return repository.findByCustomer(customer, pageable)
                                .map(this::toDTO);
        }

        public List<CustomerCylinderLedgerDTO> getLedgerByCustomerAndVariant(Long customerId, Long variantId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "LEDGER", "customerId", customerId, "variantId",
                                variantId);

                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_LEDGER_BY_CUSTOMER_VARIANT",
                                                        "Customer not found", "customerId", customerId);
                                        return new ResourceNotFoundException(
                                                        "Customer not found with id: " + customerId);
                                });
                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_LEDGER_BY_CUSTOMER_VARIANT",
                                                        "Variant not found", "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                return repository.findByCustomerAndVariant(customer, variant).stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        public List<CustomerCylinderLedgerDTO> getLedgerByVariant(Long variantId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "LEDGER", "variantId", variantId);

                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_LEDGER_BY_VARIANT",
                                                        "Variant not found", "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });
                return repository.findByVariant(variant).stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public Map<Long, java.math.BigDecimal> getLatestDueAmountsForCustomers(List<Long> customerIds) {
                if (customerIds == null || customerIds.isEmpty()) {
                        return java.util.Collections.emptyMap();
                }
                List<CustomerCylinderLedger> latestLedgers = repository.findLatestLedgerForCustomerIds(customerIds);
                Map<Long, java.math.BigDecimal> result = new java.util.HashMap<>();
                for (CustomerCylinderLedger ledger : latestLedgers) {
                        if (ledger.getCustomer() == null || ledger.getCustomer().getId() == null) {
                                continue;
                        }
                        java.math.BigDecimal due = ledger.getDueAmount();
                        result.put(ledger.getCustomer().getId(), due != null ? due : java.math.BigDecimal.ZERO);
                }
                return result;
        }

        public Long getCurrentBalance(Long customerId, Long variantId) {
                LoggerUtil.logDatabaseOperation(logger, "SELECT", "LEDGER_BALANCE", "customerId", customerId,
                                "variantId", variantId);

                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_BALANCE", "Customer not found",
                                                        "customerId", customerId);
                                        return new ResourceNotFoundException(
                                                        "Customer not found with id: " + customerId);
                                });
                CylinderVariant variant = variantRepository.findById(variantId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "GET_BALANCE", "Variant not found",
                                                        "variantId", variantId);
                                        return new ResourceNotFoundException(
                                                        "Variant not found with id: " + variantId);
                                });

                List<CustomerCylinderLedger> ledgers = repository.findByCustomerAndVariant(customer, variant);
                if (ledgers.isEmpty()) {
                        return 0L;
                }
                return ledgers.get(ledgers.size() - 1).getBalance();
        }

        public Long getPreviousBalance(Long customerId, Long variantId) {
                // Use findLatestLedger for deterministic balance calculation
                List<CustomerCylinderLedger> latestLedgers = repository.findLatestLedger(customerId, variantId);
                if (latestLedgers.isEmpty()) {
                        return 0L;
                }
                return latestLedgers.get(0).getBalance();
        }

        /**
         * Get previous balance with pessimistic lock to ensure consistency during
         * concurrent operations.
         * This prevents race conditions in balance calculations.
         * 
         * NOTE: Called from @Transactional method, uses parent transaction for lock
         * operation.
         */
        public Long getPreviousBalanceWithLock(Long customerId, Long variantId) {
                LoggerUtil.logDatabaseOperation(logger, "GET_BALANCE_WITH_LOCK", "CUSTOMER_CYLINDER_LEDGER",
                                "customerId", customerId, "variantId", variantId);

                List<CustomerCylinderLedger> latestLedgers = repository
                                .findLatestLedgerWithLock(customerId, variantId, PageRequest.of(0, 1));
                if (!latestLedgers.isEmpty()) {
                        return latestLedgers.get(0).getBalance();
                }
                return 0L;
        }

        /**
         * Get count of pending return cylinders for a customer
         * Used for alert detection
         */
        public long getPendingReturnCountForCustomer(Long customerId) {
                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Customer not found with id: " + customerId));

                return repository.sumPositiveEmptyInByCustomer(customer.getId());
        }

        private CustomerCylinderLedgerDTO toDTO(CustomerCylinderLedger ledger) {
                Long variantId = ledger.getVariant() != null ? ledger.getVariant().getId() : null;
                String variantName = ledger.getVariant() != null ? ledger.getVariant().getName() : null;

                CustomerCylinderLedgerDTO dto = new CustomerCylinderLedgerDTO(
                                ledger.getId(),
                                ledger.getCustomer().getId(),
                                ledger.getCustomer().getName(),
                                variantId,
                                variantName,
                                ledger.getTransactionDate(),
                                ledger.getRefType().toString(),
                                ledger.getRefId(),
                                ledger.getFilledOut(),
                                ledger.getEmptyIn(),
                                ledger.getBalance());
                dto.setCreatedAt(ledger.getCreatedDate());
                dto.setUpdatedDate(ledger.getUpdatedDate());
                dto.setTotalAmount(ledger.getTotalAmount());
                dto.setAmountReceived(ledger.getAmountReceived());
                dto.setDueAmount(ledger.getDueAmount());
                dto.setPaymentMode(ledger.getPaymentMode());
                dto.setTransactionReference(ledger.getTransactionReference());
                dto.setUpdateReason(ledger.getUpdateReason());
                dto.setNote(ledger.getNote());
                dto.setCreatedBy(ledger.getCreatedBy());
                dto.setUpdatedBy(ledger.getUpdatedBy());
                if (ledger.getBankAccount() != null) {
                        dto.setBankAccountId(ledger.getBankAccount().getId());
                        dto.setBankAccountName(ledger.getBankAccount().getBankName());
                        dto.setBankAccountNumber(ledger.getBankAccount().getAccountNumber());
                }
                return dto;
        }

        private CustomerCylinderLedgerDTO transferToLedgerDTO(WarehouseTransferDTO transfer) {
                // Convert transfer to ledger DTO format for display
                CustomerCylinderLedgerDTO dto = new CustomerCylinderLedgerDTO(
                                transfer.getId(),
                                0L, // No customer for transfers
                                "Transfer", // Show as system transfer
                                transfer.getVariantId(),
                                transfer.getVariantName(),
                                transfer.getTransferDate(),
                                "Transfer",
                                transfer.getId(),
                                transfer.getFilledQty(),
                                transfer.getEmptyQty(),
                                0L);
                dto.setFromWarehouseId(transfer.getFromWarehouseId());
                dto.setFromWarehouseName(transfer.getFromWarehouseName());
                dto.setToWarehouseId(transfer.getToWarehouseId());
                dto.setToWarehouseName(transfer.getToWarehouseName());
                dto.setCreatedAt(transfer.getCreatedAt());
                dto.setCreatedBy(transfer.getCreatedBy());
                return dto;
        }

        /**
         * Record a payment transaction
         */
        @Transactional
        public CustomerCylinderLedgerDTO recordPayment(PaymentRequestDTO paymentRequest) {
                PaymentRequest request = new PaymentRequest();
                request.setCustomerId(paymentRequest.getCustomerId());
                request.setAmount(paymentRequest.getAmount());
                request.setPaymentDate(paymentRequest.getPaymentDate());
                request.setPaymentMode(paymentRequest.getPaymentMode());
                request.setBankAccountId(paymentRequest.getBankAccountId());
                return recordPayment(request);
        }

        /**
         * Record a payment transaction
         */
        @Transactional
        public CustomerCylinderLedgerDTO recordPayment(PaymentRequest paymentRequest) {
                LoggerUtil.logDatabaseOperation(logger, "INSERT", "PAYMENT", "customerId", paymentRequest.customerId,
                                "amount", paymentRequest.amount);

                Customer customer = customerRepository.findByIdForUpdate(paymentRequest.customerId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "RECORD_PAYMENT", "Customer not found",
                                                        "customerId", paymentRequest.customerId);
                                        return new ResourceNotFoundException(
                                                        "Customer not found with id: " + paymentRequest.customerId);
                                });

                // Validate that customer is active
                if (!customer.getActive()) {
                        LoggerUtil.logBusinessError(logger, "RECORD_PAYMENT",
                                        "Cannot record payment for inactive customer",
                                        "customerId", paymentRequest.customerId, "customerName", customer.getName());
                        throw new InvalidOperationException(
                                        "Cannot record payment for inactive customer: " + customer.getName());
                }

                // Payment is customer-level, not variant-specific
                CustomerCylinderLedger ledger = new CustomerCylinderLedger();
                ledger.setCustomer(customer);
                ledger.setWarehouse(null);
                ledger.setVariant(null); // Payment has no specific variant
                ledger.setTransactionDate(
                                paymentRequest.paymentDate != null ? paymentRequest.paymentDate : LocalDate.now());
                ledger.setRefType(CustomerCylinderLedger.TransactionType.PAYMENT);
                ledger.setFilledOut(0L);
                ledger.setEmptyIn(0L);
                ledger.setBalance(0L);
                ledger.setTotalAmount(java.math.BigDecimal.ZERO);
                ledger.setAmountReceived(paymentRequest.amount);

                // Calculate running balance (remaining customer debt) BEFORE this payment
                List<CustomerCylinderLedger> latestEntries = repository
                                .findLatestLedgerForCustomerIds(java.util.Collections.singletonList(customer.getId()));
                BigDecimal currentDue = BigDecimal.ZERO;
                if (!latestEntries.isEmpty()) {
                        BigDecimal latestDue = latestEntries.get(0).getDueAmount();
                        currentDue = latestDue != null ? latestDue : java.math.BigDecimal.ZERO;
                }

                // Validate payment amount doesn't exceed current due
                if (paymentRequest.amount.compareTo(currentDue) > 0) {
                        LoggerUtil.logBusinessError(logger, "RECORD_PAYMENT", "Payment exceeds due amount",
                                        "customerId", customer.getId(), "paymentAmount", paymentRequest.amount,
                                        "dueAmount", currentDue);
                        throw new IllegalArgumentException(
                                        "Payment amount Rs " + paymentRequest.amount
                                                        + " cannot exceed due amount Rs " + currentDue);
                }

                // Calculate running balance (remaining customer debt) AFTER this payment
                BigDecimal cumulativeBalance = currentDue;

                // Subtract current payment from balance
                cumulativeBalance = cumulativeBalance.subtract(paymentRequest.amount);

                // Set dueAmount to the running balance (can be 0 or negative if overpaid)
                ledger.setDueAmount(cumulativeBalance.max(BigDecimal.ZERO)); // Don't store negative due
                                                                             // amounts
                ledger.setRefId(null);
                ledger.setPaymentMode(paymentRequest.paymentMode);

                // Set bank account if required by payment mode configuration
                if (paymentRequest.bankAccountId != null && paymentRequest.paymentMode != null) {
                        // Fetch the payment mode configuration to check if bank account is required
                        PaymentMode paymentMode = paymentModeRepository.findByName(paymentRequest.paymentMode)
                                        .orElse(null);

                        boolean bankAccountRequired = paymentMode != null ? paymentMode.getIsBankAccountRequired()
                                        : false;

                        if (bankAccountRequired) {
                                BankAccount bankAccount = bankAccountRepository.findById(paymentRequest.bankAccountId)
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Bank account not found with id: "
                                                                                + paymentRequest.bankAccountId));
                                ledger.setBankAccount(bankAccount);
                        }
                }

                CustomerCylinderLedger savedLedger = repository.save(ledger);

                // Record bank account transaction if required by payment mode configuration
                if (paymentRequest.bankAccountId != null && paymentRequest.paymentMode != null) {
                        // Fetch the payment mode configuration to check if bank account is required
                        PaymentMode paymentMode = paymentModeRepository.findByName(paymentRequest.paymentMode)
                                        .orElse(null);

                        boolean bankAccountRequired = paymentMode != null ? paymentMode.getIsBankAccountRequired()
                                        : false;

                        if (bankAccountRequired) {
                                try {
                                        BankAccount bankAccount = bankAccountRepository
                                                        .findById(paymentRequest.bankAccountId)
                                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                                        "Bank account not found with id: "
                                                                                        + paymentRequest.bankAccountId));

                                        BankAccountLedger ledgerEntry = new BankAccountLedger(
                                                        bankAccount,
                                                        "DEPOSIT",
                                                        paymentRequest.amount,
                                                        null,
                                                        null,
                                                        referenceNumberGenerator.generateBankTransactionReference(
                                                                        bankAccount.getCode(), "DEP"),
                                                        "Due payment received from customer: " + customer.getName());
                                        bankAccountLedgerRepository.save(ledgerEntry);
                                        logger.info("Bank account ledger entry recorded for due payment - Customer: {}, Amount: {}",
                                                        customer.getName(), paymentRequest.amount);
                                } catch (Exception e) {
                                        logger.error("Error recording bank ledger entry for due payment", e);
                                }
                        }
                }

                LoggerUtil.logBusinessSuccess(logger, "RECORD_PAYMENT", "customerId", customer.getId(),
                                "amount", paymentRequest.amount);

                return toDTO(savedLedger);
        }

        /**
         * Request class for recording payments
         */
        public static class PaymentRequest {
                public Long customerId;
                public java.math.BigDecimal amount;
                public LocalDate paymentDate;
                public String paymentMode;
                public Long bankAccountId;

                public PaymentRequest() {
                }

                public PaymentRequest(Long customerId, java.math.BigDecimal amount, LocalDate paymentDate) {
                        this.customerId = customerId;
                        this.amount = amount;
                        this.paymentDate = paymentDate;
                }

                public PaymentRequest(Long customerId, java.math.BigDecimal amount, LocalDate paymentDate,
                                String paymentMode) {
                        this.customerId = customerId;
                        this.amount = amount;
                        this.paymentDate = paymentDate;
                        this.paymentMode = paymentMode;
                }

                public PaymentRequest(Long customerId, java.math.BigDecimal amount, LocalDate paymentDate,
                                String paymentMode, Long bankAccountId) {
                        this.customerId = customerId;
                        this.amount = amount;
                        this.paymentDate = paymentDate;
                        this.paymentMode = paymentMode;
                        this.bankAccountId = bankAccountId;
                }

                // Getters and setters
                public Long getCustomerId() {
                        return customerId;
                }

                public void setCustomerId(Long customerId) {
                        this.customerId = customerId;
                }

                public java.math.BigDecimal getAmount() {
                        return amount;
                }

                public void setAmount(java.math.BigDecimal amount) {
                        this.amount = amount;
                }

                public LocalDate getPaymentDate() {
                        return paymentDate;
                }

                public void setPaymentDate(LocalDate paymentDate) {
                        this.paymentDate = paymentDate;
                }

                public String getPaymentMode() {
                        return paymentMode;
                }

                public void setPaymentMode(String paymentMode) {
                        this.paymentMode = paymentMode;
                }

                public Long getBankAccountId() {
                        return bankAccountId;
                }

                public void setBankAccountId(Long bankAccountId) {
                        this.bankAccountId = bankAccountId;
                }
        }

        // Get complete summary for a customer (across all ledger entries)
        public CustomerLedgerSummaryDTO getCustomerLedgerSummary(Long customerId) {
                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Customer not found with id: " + customerId));

                List<CustomerCylinderLedger> allEntries = repository.findByCustomer(customer);
                Map<Long, CustomerLedgerVariantSummaryDTO> variantSummary = new HashMap<>();

                // Get the latest balance for each variant (most recent entry per variant)
                Map<Long, CustomerCylinderLedger> latestByVariant = new HashMap<>();

                for (CustomerCylinderLedger entry : allEntries) {
                        // Skip PAYMENT transactions (they don't affect variant-specific balances)
                        if (entry.getRefType() == CustomerCylinderLedger.TransactionType.PAYMENT) {
                                continue;
                        }

                        if (entry.getVariant() != null) {
                                Long variantId = entry.getVariant().getId();
                                // Keep the latest entry for each variant (by ID, assuming higher ID = later)
                                if (!latestByVariant.containsKey(variantId)
                                                || entry.getId() > latestByVariant.get(variantId).getId()) {
                                        latestByVariant.put(variantId, entry);
                                }
                        }
                }

                // Build summary from latest entries per variant
                for (Map.Entry<Long, CustomerCylinderLedger> entry : latestByVariant.entrySet()) {
                        CustomerCylinderLedger ledger = entry.getValue();
                        if (ledger.getVariant() != null) {
                                Long variantId = ledger.getVariant().getId();
                                String variantName = ledger.getVariant().getName();

                                CustomerLedgerVariantSummaryDTO vSummary = new CustomerLedgerVariantSummaryDTO();
                                vSummary.setVariantName(variantName);
                                // Use the balance field which represents filled cylinders with customer
                                Long filledCount = ledger.getBalance() != null && ledger.getBalance() > 0
                                                ? ledger.getBalance()
                                                : 0L;
                                vSummary.setFilledCount(filledCount);
                                // Return pending = cylinders with customer that need to be returned (same as
                                // filledCount)
                                vSummary.setReturnPending(filledCount);

                                variantSummary.put(variantId, vSummary);
                        }
                }

                CustomerLedgerSummaryDTO summary = new CustomerLedgerSummaryDTO();
                summary.setVariants(new ArrayList<>(variantSummary.values()));
                return summary;
        }

        // Update payment mode for a ledger entry
        public void updatePaymentMode(Long ledgerId, String paymentMode) {
                CustomerCylinderLedger ledger = repository.findById(ledgerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Ledger entry not found"));
                ledger.setPaymentMode(paymentMode);
                repository.save(ledger);
        }

        /**
         * Record a bank account transaction for customer ledger operations (empty
         * returns, due payments, etc.) with proper reference number generation
         */
        public void recordBankAccountDeposit(Long bankAccountId, BigDecimal amount, Long ledgerId,
                        String description) {
                if (bankAccountId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        return;
                }

                try {
                        // Set bank account reference on the ledger entry
                        CustomerCylinderLedger ledger = repository.findById(ledgerId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Ledger entry not found with id: " + ledgerId));

                        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Bank account not found with id: " + bankAccountId));

                        // Generate proper reference number using ReferenceNumberGenerator
                        String bankCode = bankAccount.getCode() != null ? bankAccount.getCode().trim() : "BANK";
                        String referenceNumber = referenceNumberGenerator.generateBankTransactionReference(
                                        bankCode, "DEP");

                        ledger.setBankAccount(bankAccount);
                        repository.save(ledger);

                        // Record the deposit in bank account ledger
                        BankAccountLedger ledgerEntry = new BankAccountLedger(
                                        bankAccount,
                                        "DEPOSIT",
                                        amount,
                                        null,
                                        null,
                                        referenceNumber,
                                        description);
                        bankAccountLedgerRepository.save(ledgerEntry);
                        logger.info("Bank account ledger entry recorded - BankAccountId: {}, Amount: {}, Reference: {}",
                                        bankAccountId, amount, referenceNumber);
                } catch (Exception e) {
                        logger.error("Error recording bank account transaction", e);
                        throw new RuntimeException("Failed to record bank account transaction: " + e.getMessage());
                }
        }

        /**
         * Record a bank account transaction for customer ledger operations (empty
         * returns, due payments, etc.)
         */
        public void recordBankAccountTransaction(Long bankAccountId, BigDecimal amount, Long ledgerId,
                        String referenceNumber, String description) {
                if (bankAccountId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        return;
                }

                try {
                        // Set bank account reference on the ledger entry
                        CustomerCylinderLedger ledger = repository.findById(ledgerId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Ledger entry not found with id: " + ledgerId));

                        BankAccount bankAccount = bankAccountRepository.findById(bankAccountId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Bank account not found with id: " + bankAccountId));

                        ledger.setBankAccount(bankAccount);
                        repository.save(ledger);

                        // Record the deposit in bank account ledger
                        BankAccountLedger ledgerEntry = new BankAccountLedger(
                                        bankAccount,
                                        "DEPOSIT",
                                        amount,
                                        null,
                                        null,
                                        referenceNumber,
                                        description);
                        bankAccountLedgerRepository.save(ledgerEntry);
                        logger.info("Bank account ledger entry recorded - BankAccountId: {}, Amount: {}, Reference: {}",
                                        bankAccountId, amount, referenceNumber);
                } catch (Exception e) {
                        logger.error("Error recording bank account transaction", e);
                        throw new RuntimeException("Failed to record bank account transaction: " + e.getMessage());
                }
        }

        /**
         * Get customer's previous due amount (the most recent ledger entry's due
         * amount)
         * This represents the total outstanding amount the customer owes
         * 
         * @param customerId The customer ID
         * @return Previous due amount, or ZERO if no previous entries exist
         */
        public BigDecimal getCustomerPreviousDue(Long customerId) {
                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Customer not found with id: " + customerId));

                List<CustomerCylinderLedger> ledgerEntries = repository.findByCustomer(customer);

                logger.info("getCustomerPreviousDue - customerId: {}, total entries found: {}", customerId,
                                ledgerEntries != null ? ledgerEntries.size() : 0);

                if (ledgerEntries == null || ledgerEntries.isEmpty()) {
                        logger.info("No ledger entries found for customer {}", customerId);
                        return BigDecimal.ZERO;
                }

                // Get the most recent entry (highest ID - latest created)
                CustomerCylinderLedger latestEntry = null;
                Long maxId = -1L;
                for (CustomerCylinderLedger entry : ledgerEntries) {
                        if (entry.getId() > maxId) {
                                maxId = entry.getId();
                                latestEntry = entry;
                        }
                }

                if (latestEntry == null) {
                        logger.info("No valid latest entry found for customer {}", customerId);
                        return BigDecimal.ZERO;
                }

                BigDecimal dueAmount = latestEntry.getDueAmount();
                if (dueAmount == null) {
                        dueAmount = BigDecimal.ZERO;
                }

                logger.info("Latest ledger for customer {} - Entry ID: {}, Due Amount: {}, Total Amount: {}, Amount Received: {}",
                                customerId, latestEntry.getId(), dueAmount, latestEntry.getTotalAmount(),
                                latestEntry.getAmountReceived());

                return dueAmount;
        }

        /**
         * Update a ledger entry and recalculate all subsequent entries
         * EDGE CASES HANDLED:
         * 1. Prevents negative due amounts anywhere in chain
         * 2. Updates inventory if filledOut/emptyIn changes
         * 3. Recalculates balance (inventory count) for all entries after
         * 4. Validates updated entry doesn't break cumulative chain
         * 5. Shows detailed error messages if validation fails
         */
        @Transactional
        public CustomerCylinderLedgerDTO updateLedgerEntry(Long ledgerId, LedgerUpdateRequestDTO updateData) {
                LoggerUtil.logBusinessEntry(logger, "UPDATE_LEDGER", "ledgerId", ledgerId,
                                "updateData", updateData != null ? updateData.toString() : "null");

                // ==================== VALIDATION ====================

                // 1. Entry exists
                CustomerCylinderLedger entry = repository.findById(ledgerId)
                                .orElseThrow(() -> new ResourceNotFoundException("Ledger entry not found"));

                // 2. Check if update is allowed for this transaction type
                if (entry.getRefType() != CustomerCylinderLedger.TransactionType.SALE &&
                                entry.getRefType() != CustomerCylinderLedger.TransactionType.EMPTY_RETURN &&
                                entry.getRefType() != CustomerCylinderLedger.TransactionType.PAYMENT) {
                        throw new InvalidOperationException(
                                        "Updates are only allowed for SALE, EMPTY_RETURN, and PAYMENT transactions. " +
                                                        "Current transaction type: " + entry.getRefType());
                }

                // 3. Validate which fields can be edited based on transaction type
                boolean isEmptyReturn = entry.getRefType() == CustomerCylinderLedger.TransactionType.EMPTY_RETURN;
                boolean isPayment = entry.getRefType() == CustomerCylinderLedger.TransactionType.PAYMENT;

                if (isEmptyReturn && updateData != null && updateData.getFilledOut() != null) {
                        throw new InvalidOperationException("Cannot edit filled count for EMPTY_RETURN transactions");
                }
                if (isPayment && updateData != null && (updateData.getFilledOut() != null
                                || updateData.getEmptyIn() != null
                                || updateData.getTotalAmount() != null)) {
                        throw new InvalidOperationException("Can only edit amount received for PAYMENT transactions");
                }

                Customer customer = entry.getCustomer();
                CylinderVariant variant = entry.getVariant();
                Warehouse warehouse = entry.getWarehouse();

                // 3. Store OLD values to calculate inventory impact
                long oldFilledOut = entry.getFilledOut();
                long oldEmptyIn = entry.getEmptyIn();
                BigDecimal oldTotalAmount = entry.getTotalAmount();
                BigDecimal oldAmountReceived = entry.getAmountReceived();

                // 3. Extract new values (if provided)
                Long newFilledOut = updateData != null && updateData.getFilledOut() != null
                                ? updateData.getFilledOut()
                                : oldFilledOut;
                Long newEmptyIn = updateData != null && updateData.getEmptyIn() != null
                                ? updateData.getEmptyIn()
                                : oldEmptyIn;
                BigDecimal newTotalAmount = updateData != null && updateData.getTotalAmount() != null
                                ? updateData.getTotalAmount()
                                : oldTotalAmount;
                BigDecimal newAmountReceived = updateData != null && updateData.getAmountReceived() != null
                                ? updateData.getAmountReceived()
                                : oldAmountReceived;

                // If filled count changes for a SALE and total wasn't provided, recalculate total
                if (entry.getRefType() == CustomerCylinderLedger.TransactionType.SALE
                                && entry.getRefId() != null
                                && updateData != null
                                && updateData.getTotalAmount() == null
                                && updateData.getFilledOut() != null) {
                        try {
                                Sale saleForRecalc = saleRepository.findById(entry.getRefId()).orElse(null);
                                if (saleForRecalc != null && saleForRecalc.getSaleItems() != null) {
                                        for (com.gasagency.entity.SaleItem item : saleForRecalc.getSaleItems()) {
                                                if (variant != null && item.getVariant().getId().equals(variant.getId())) {
                                                        BigDecimal basePrice = item.getBasePrice() != null
                                                                        ? item.getBasePrice()
                                                                        : BigDecimal.ZERO;
                                                        BigDecimal discount = item.getDiscount() != null
                                                                        ? item.getDiscount()
                                                                        : BigDecimal.ZERO;
                                                        BigDecimal subtotal = basePrice.multiply(
                                                                        BigDecimal.valueOf(newFilledOut));
                                                        BigDecimal recalculatedFinal = subtotal.subtract(discount)
                                                                        .setScale(2, RoundingMode.HALF_UP);
                                                        if (recalculatedFinal.compareTo(BigDecimal.ZERO) < 0) {
                                                                recalculatedFinal = BigDecimal.ZERO;
                                                        }
                                                        newTotalAmount = recalculatedFinal;
                                                        break;
                                                }
                                        }
                                }
                        } catch (Exception e) {
                                logger.warn("UPDATE_LEDGER: Failed to recalculate total amount for sale {}",
                                                entry.getRefId(), e);
                        }
                }

                // 4. Validate new values are non-negative
                if (newFilledOut < 0 || newEmptyIn < 0) {
                        throw new InvalidOperationException("Filled/Empty count cannot be negative");
                }
                if (newTotalAmount.compareTo(BigDecimal.ZERO) < 0 ||
                                newAmountReceived.compareTo(BigDecimal.ZERO) < 0) {
                        throw new InvalidOperationException("Total or received amounts cannot be negative");
                }
                // ==================== SIMULATION: CHECK CHAIN RECALCULATION
                // ====================
                // Get entries SAME VARIANT ONLY for balance calculation (only for
                // SALE/EMPTY_RETURN)
                List<CustomerCylinderLedger> variantEntries = new ArrayList<>();
                if (variant != null) {
                        variantEntries = repository.findByCustomerAndVariantOrdered(customer, variant);
                }

                // Get entries for ALL VARIANTS for due amount calculation (cumulative debt)
                List<CustomerCylinderLedger> allEntries = repository.findByCustomerOrdered(customer);

                // Check if entry is within the latest 15 records (per variant)
                if (variantEntries.size() > 15) {
                        int earliestEditableIndex = variantEntries.size() - 15;
                        int currentIndex = -1;
                        for (int i = 0; i < variantEntries.size(); i++) {
                                if (variantEntries.get(i).getId().equals(ledgerId)) {
                                        currentIndex = i;
                                        break;
                                }
                        }

                        if (currentIndex < earliestEditableIndex) {
                                throw new InvalidOperationException(
                                                "Cannot edit entries older than the latest 15 records. " +
                                                                "This entry is #"
                                                                + (variantEntries.size() - currentIndex)
                                                                + " from the latest. " +
                                                                "Only the latest 15 entries (per variant) can be edited.");
                        }
                }

                // Find entry index in VARIANT list for balance calculation
                int entryIndex = -1;
                if (!variantEntries.isEmpty()) {
                        for (int i = 0; i < variantEntries.size(); i++) {
                                if (variantEntries.get(i).getId().equals(ledgerId)) {
                                        entryIndex = i;
                                        break;
                                }
                        }

                        if (entryIndex == -1) {
                                throw new ResourceNotFoundException("Entry not found in variant ledger chain");
                        }
                }
                // For PAYMENT transactions, entryIndex will be -1 (no variant entries)

                // Calculate balance change for this entry (PER VARIANT) - only if variant
                // exists
                // balance = previousBalance + filledOut - emptyIn
                // filledOut increases balance (cylinders given to customer)
                // emptyIn decreases balance (empty cylinders returned from customer)
                long oldBalance = 0;
                long newBalance = 0;

                if (variant != null && entryIndex >= 0) {
                        oldBalance = (entryIndex > 0 ? variantEntries.get(entryIndex - 1).getBalance() : 0)
                                        + oldFilledOut - oldEmptyIn;
                        newBalance = (entryIndex > 0 ? variantEntries.get(entryIndex - 1).getBalance() : 0)
                                        + newFilledOut - newEmptyIn;

                        // Validate balance won't go negative
                        if (newBalance < 0) {
                                throw new InvalidOperationException(
                                                "Cannot update: New balance would be negative (" + newBalance
                                                                + " cylinders). "
                                                                +
                                                                "Current balance is " + oldBalance + " cylinders.");
                        }
                }

                // Calculate due contribution for this entry
                BigDecimal newDueContribution = newTotalAmount.subtract(newAmountReceived);

                // Variables for chain validation and recalculation
                // Find entry index in ALL list for due amount calculation
                int allEntriesIndex = -1;
                for (int i = 0; i < allEntries.size(); i++) {
                        if (allEntries.get(i).getId().equals(ledgerId)) {
                                allEntriesIndex = i;
                                break;
                        }
                }

                List<Long> affectedEntries = new ArrayList<>();
                BigDecimal newCumulativeDue = BigDecimal.ZERO;

                // Get previous cumulative due to calculate this entry's new cumulative due
                // (from ALL VARIANTS chain)
                BigDecimal prevCumulativeDue = BigDecimal.ZERO;
                if (allEntriesIndex > 0) {
                        prevCumulativeDue = allEntries.get(allEntriesIndex - 1).getDueAmount();
                        if (prevCumulativeDue == null) {
                                prevCumulativeDue = BigDecimal.ZERO;
                        }
                }
                newCumulativeDue = prevCumulativeDue.add(newDueContribution);

                // Validate the updated entry's due does not go negative (post-edit)
                if (newCumulativeDue.compareTo(BigDecimal.ZERO) < 0) {
                        throw new InvalidOperationException(
                                        "Amount received (Rs" + newAmountReceived +
                                                        ") cannot exceed total amount owed after this update (Rs "
                                                        + newCumulativeDue + ").");
                }

                // Validation loop for both balance (per variant) and due (all variants)
                BigDecimal runningDue = newCumulativeDue;
                long runningBalance = newBalance;
                StringBuilder validationErrors = new StringBuilder();

                // Validate balance chain (per-variant entries after this one in variantEntries)
                for (int i = entryIndex + 1; i < variantEntries.size(); i++) {
                        CustomerCylinderLedger nextEntry = variantEntries.get(i);

                        // Calculate what balance would be (per variant)
                        // balance = previousBalance + filledOut - emptyIn
                        long nextBalance = runningBalance + nextEntry.getFilledOut() - nextEntry.getEmptyIn();

                        // Check if balance would go negative
                        if (nextBalance < 0) {
                                validationErrors.append("Entry ").append(nextEntry.getId())
                                                .append(" (dated ").append(nextEntry.getTransactionDate())
                                                .append(") would have negative balance: ").append(nextBalance)
                                                .append(" cylinders. ");
                        }

                        if (validationErrors.length() == 0) {
                                runningBalance = nextBalance;
                                affectedEntries.add(nextEntry.getId());
                        }
                }

                // Validate due chain (all variant entries after this one in allEntries)
                runningDue = newCumulativeDue;
                for (int i = allEntriesIndex + 1; i < allEntries.size(); i++) {
                        CustomerCylinderLedger nextEntry = allEntries.get(i);
                        BigDecimal nextTotalAmount = nextEntry.getTotalAmount() != null ? nextEntry.getTotalAmount()
                                        : BigDecimal.ZERO;
                        BigDecimal nextAmountReceived = nextEntry.getAmountReceived() != null
                                        ? nextEntry.getAmountReceived()
                                        : BigDecimal.ZERO;

                        BigDecimal nextEntryDue = nextTotalAmount.subtract(nextAmountReceived);
                        BigDecimal nextCumulativeDue = runningDue.add(nextEntryDue);

                        // Check if due would go negative
                        if (nextCumulativeDue.compareTo(BigDecimal.ZERO) < 0) {
                                validationErrors.append("Entry ").append(nextEntry.getId())
                                                .append(" (dated ").append(nextEntry.getTransactionDate())
                                                .append(") would have negative due: Rs ").append(nextCumulativeDue)
                                                .append(". ");
                        }

                        if (validationErrors.length() == 0) {
                                runningDue = nextCumulativeDue;
                        }
                }

                // If validation failed, reject the update
                if (validationErrors.length() > 0) {
                        LoggerUtil.logBusinessError(logger, "UPDATE_LEDGER", "Chain validation failed",
                                        "ledgerId", ledgerId, "error", validationErrors.toString());
                        throw new InvalidOperationException(
                                        "Cannot update: " + validationErrors.toString() +
                                                        " Update would result in negative values in subsequent entries.");
                }

                LoggerUtil.logBusinessEntry(logger, "UPDATE_LEDGER_VALIDATION_PASSED",
                                "affectedEntriesCount", affectedEntries.size(),
                                "oldValues", "filled=" + oldFilledOut + ", empty=" + oldEmptyIn +
                                                ", total=" + oldTotalAmount + ", received=" + oldAmountReceived,
                                "newValues", "filled=" + newFilledOut + ", empty=" + newEmptyIn +
                                                ", total=" + newTotalAmount + ", received=" + newAmountReceived);

                // ==================== UPDATE INVENTORY ====================

                // For SALE: track both filled (sold) and empty (returned) changes
                // For EMPTY_RETURN: track empty changes
                // PAYMENT transactions don't affect inventory
                long filledQtyChange = 0;
                long emptyQtyChange = 0;

                if (entry.getRefType() == CustomerCylinderLedger.TransactionType.SALE) {
                        filledQtyChange = oldFilledOut - newFilledOut; // If reduced, add back
                        emptyQtyChange = newEmptyIn - oldEmptyIn; // If increased, add to inventory
                } else if (entry.getRefType() == CustomerCylinderLedger.TransactionType.EMPTY_RETURN) {
                        emptyQtyChange = newEmptyIn - oldEmptyIn; // If increased, add to inventory
                }
                // PAYMENT type: no inventory changes

                if ((filledQtyChange != 0 || emptyQtyChange != 0) && variant != null) {
                        try {
                                if (entry.getRefType() == CustomerCylinderLedger.TransactionType.SALE) {
                                        // For SALE filled qty: positive change means add back to inventory
                                        if (filledQtyChange > 0) {
                                                inventoryStockService.incrementFilledQty(warehouse, variant,
                                                                filledQtyChange);
                                        } else if (filledQtyChange < 0) {
                                                inventoryStockService.decrementFilledQtyWithCheck(warehouse, variant,
                                                                -filledQtyChange);
                                        }
                                        // For SALE empty qty: positive change means add returned cylinders to inventory
                                        if (emptyQtyChange > 0) {
                                                inventoryStockService.incrementEmptyQty(warehouse, variant,
                                                                emptyQtyChange);
                                        } else if (emptyQtyChange < 0) {
                                                inventoryStockService.decrementEmptyQty(warehouse, variant,
                                                                -emptyQtyChange);
                                        }
                                } else if (entry.getRefType() == CustomerCylinderLedger.TransactionType.EMPTY_RETURN) {
                                        // For EMPTY_RETURN: positive emptyQtyChange means more empties returned to
                                        // warehouse
                                        // Increment empty qty in warehouse
                                        if (emptyQtyChange > 0) {
                                                inventoryStockService.incrementEmptyQty(warehouse, variant,
                                                                emptyQtyChange);
                                        } else if (emptyQtyChange < 0) {
                                                inventoryStockService.decrementEmptyQty(warehouse, variant,
                                                                -emptyQtyChange);
                                        }
                                }
                                LoggerUtil.logBusinessEntry(logger, "UPDATE_LEDGER_INVENTORY",
                                                "warehouseId", warehouse.getId(),
                                                "variantId", variant.getId(),
                                                "filledQtyChange", filledQtyChange,
                                                "emptyQtyChange", emptyQtyChange);
                        } catch (Exception e) {
                                LoggerUtil.logBusinessError(logger, "UPDATE_LEDGER_INVENTORY_FAILED",
                                                "error", e.getMessage(), "filledQtyChange", filledQtyChange,
                                                "emptyQtyChange", emptyQtyChange);
                                throw new InvalidOperationException("Failed to update inventory: " + e.getMessage());
                        }
                }

                // ==================== UPDATE THIS ENTRY ====================

                entry.setFilledOut(newFilledOut);
                entry.setEmptyIn(newEmptyIn);
                entry.setTotalAmount(newTotalAmount);
                entry.setAmountReceived(newAmountReceived);
                entry.setDueAmount(newCumulativeDue);
                entry.setBalance(newBalance);

                // Set update reason if provided
                if (updateData != null && updateData.getUpdateReason() != null) {
                        entry.setUpdateReason(updateData.getUpdateReason());
                }

                // Set payment mode if provided
                if (updateData != null && updateData.getPaymentMode() != null) {
                        entry.setPaymentMode(updateData.getPaymentMode());
                }

                // Set bank account if provided
                if (updateData != null && updateData.getBankAccountId() != null) {
                        Long bankAccountId = updateData.getBankAccountId();
                                BankAccount bankAccount = bankAccountRepository.findById(bankAccountId).orElse(null);
                                entry.setBankAccount(bankAccount);
                } else if (updateData != null && updateData.getBankAccountId() == null) {
                        entry.setBankAccount(null);
                }

                repository.save(entry);

                // ==================== UPDATE RELATED SALE RECORD ====================

                // If this is a SALE transaction, update the Sale and SaleItem records
                if (entry.getRefType() == CustomerCylinderLedger.TransactionType.SALE && entry.getRefId() != null) {
                        try {
                                Sale sale = saleRepository.findById(entry.getRefId()).orElse(null);
                                if (sale != null) {
                                        // Update the sale with new total amount
                                        sale.setTotalAmount(newTotalAmount);
                                        saleRepository.save(sale);

                                        logger.info("UPDATE_SALE: saleId={}, oldTotal={}, newTotal={}",
                                                        entry.getRefId(), oldTotalAmount, newTotalAmount);

                                        // Update SaleItems with new quantity/price if quantities or total changed
                                        if (oldFilledOut != newFilledOut || oldEmptyIn != newEmptyIn
                                                        || oldTotalAmount.compareTo(newTotalAmount) != 0) {
                                                List<com.gasagency.entity.SaleItem> saleItems = sale.getSaleItems();
                                                if (saleItems != null && !saleItems.isEmpty()) {
                                                        for (com.gasagency.entity.SaleItem item : saleItems) {
                                                                if (item.getVariant().getId().equals(variant.getId())) {
                                                                        // Update quantities
                                                                        item.setQtyIssued(newFilledOut);
                                                                        item.setQtyEmptyReceived(newEmptyIn);
                                                                        // Update final price
                                                                        item.setFinalPrice(newTotalAmount);
                                                                        break;
                                                                }
                                                        }
                                                }
                                        }
                                }
                        } catch (Exception e) {
                                logger.error("Failed to update Sale/SaleItem records: {}", e.getMessage());
                                // Continue anyway - ledger is updated even if sale update fails
                        }
                }

                // ==================== RECALCULATE BALANCE FOR SUBSEQUENT VARIANT ENTRIES
                // ====================

                // Reset runningBalance for per-variant recalculation (only if variant exists)
                if (variant != null && entryIndex >= 0) {
                        runningBalance = newBalance;

                        logger.info("RECALC_BALANCE_START: entry={}, newBalance={}", ledgerId, newBalance);

                        for (int i = entryIndex + 1; i < variantEntries.size(); i++) {
                                CustomerCylinderLedger nextEntry = variantEntries.get(i);

                                // Recalculate balance (inventory count) - per variant only
                                runningBalance = runningBalance + nextEntry.getFilledOut() - nextEntry.getEmptyIn();

                                logger.info("RECALC_BALANCE: id={}, oldBalance={}, newBalance={}, filled={}, empty={}",
                                                nextEntry.getId(), nextEntry.getBalance(), runningBalance,
                                                nextEntry.getFilledOut(), nextEntry.getEmptyIn());

                                nextEntry.setBalance(runningBalance);
                                repository.save(nextEntry);

                                LoggerUtil.logBusinessEntry(logger, "UPDATE_LEDGER_RECALC_BALANCE",
                                                "entryId", nextEntry.getId(), "newBalance", runningBalance);
                        }
                }

                // ==================== RECALCULATE DUE FOR SUBSEQUENT ALL-VARIANT ENTRIES
                // ====================

                // Reset runningDue for all-variant recalculation
                runningDue = newCumulativeDue;

                logger.info("RECALC_DUE_START: entry={}, newDue={}", ledgerId, newCumulativeDue);

                for (int i = allEntriesIndex + 1; i < allEntries.size(); i++) {
                        CustomerCylinderLedger nextEntry = allEntries.get(i);
                        BigDecimal nextTotalAmount = nextEntry.getTotalAmount() != null ? nextEntry.getTotalAmount()
                                        : BigDecimal.ZERO;
                        BigDecimal nextAmountReceived = nextEntry.getAmountReceived() != null
                                        ? nextEntry.getAmountReceived()
                                        : BigDecimal.ZERO;

                        // Recalculate due (cumulative across ALL variants)
                        BigDecimal nextEntryDue = nextTotalAmount.subtract(nextAmountReceived);
                        BigDecimal nextCumulativeDue = runningDue.add(nextEntryDue);
                        if (nextCumulativeDue.compareTo(BigDecimal.ZERO) < 0) {
                                nextCumulativeDue = BigDecimal.ZERO;
                        }

                        logger.info("RECALC_DUE: id={}, oldDue={}, newDue={}",
                                        nextEntry.getId(), nextEntry.getDueAmount(), nextCumulativeDue);

                        nextEntry.setDueAmount(nextCumulativeDue);
                        repository.save(nextEntry);

                        LoggerUtil.logBusinessEntry(logger, "UPDATE_LEDGER_RECALC_DUE",
                                        "entryId", nextEntry.getId(), "newDue", nextCumulativeDue);

                        runningDue = nextCumulativeDue;
                }

                LoggerUtil.logBusinessSuccess(logger, "UPDATE_LEDGER", "ledgerId", ledgerId,
                                "affectedEntries", affectedEntries.size());
                LoggerUtil.logAudit("UPDATE", "LEDGER_ENTRY", "ledgerId", ledgerId,
                                "customerId", customer.getId(),
                                "oldValues", "total=" + oldTotalAmount + ", received=" + oldAmountReceived,
                                "newValues", "total=" + newTotalAmount + ", received=" + newAmountReceived);

                return toDTO(entry);
        }

        // Repair function to recalculate all balances with correct formula
        @Transactional
        public void recalculateAllBalances() {
                List<CustomerCylinderLedger> allEntries = repository.findAll();

                // Group by customer and variant for separate recalculation
                Map<String, List<CustomerCylinderLedger>> groupedEntries = allEntries.stream()
                                .collect(Collectors.groupingBy(
                                                e -> e.getCustomer().getId() + "_" + e.getVariant().getId()));

                int totalUpdated = 0;

                for (List<CustomerCylinderLedger> entries : groupedEntries.values()) {
                        // Sort by transaction date
                        entries.sort((a, b) -> a.getTransactionDate().compareTo(b.getTransactionDate()));

                        long runningBalance = 0;
                for (CustomerCylinderLedger entry : entries) {
                        // balance = previousBalance + filledOut - emptyIn
                        long newBalance = runningBalance + entry.getFilledOut() - entry.getEmptyIn();

                                if (newBalance != entry.getBalance()) {
                                        long oldBalance = entry.getBalance();
                                        entry.setBalance(newBalance);
                                        repository.save(entry);
                                        totalUpdated++;
                                        logger.info("BALANCE_REPAIR: Entry {} balance recalculated from {} to {}",
                                                        entry.getId(), oldBalance, newBalance);
                                }

                                runningBalance = newBalance;
                        }
                }

                logger.info("BALANCE_REPAIR_COMPLETE: Total entries recalculated: {}", totalUpdated);
        }
}


