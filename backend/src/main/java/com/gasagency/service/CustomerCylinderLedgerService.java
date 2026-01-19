package com.gasagency.service;

import com.gasagency.dto.CustomerCylinderLedgerDTO;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Warehouse;
import com.gasagency.entity.BankAccount;
import com.gasagency.entity.Sale;
import com.gasagency.entity.WarehouseTransfer;
import com.gasagency.entity.BankAccountLedger;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import com.gasagency.repository.CustomerRepository;
import com.gasagency.repository.CylinderVariantRepository;
import com.gasagency.repository.WarehouseRepository;
import com.gasagency.repository.BankAccountRepository;
import com.gasagency.repository.SaleRepository;
import com.gasagency.repository.WarehouseTransferRepository;
import com.gasagency.repository.BankAccountLedgerRepository;
import com.gasagency.exception.ResourceNotFoundException;
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
import com.gasagency.dto.CustomerBalanceDTO;
import java.util.ArrayList;
import java.math.BigDecimal;

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
        private final BankAccountService bankAccountService;
        private final ReferenceNumberGenerator referenceNumberGenerator;
        private final SaleRepository saleRepository;
        private final WarehouseTransferRepository warehouseTransferRepository;
        private final BankAccountLedgerRepository bankAccountLedgerRepository;

        public CustomerCylinderLedgerService(CustomerCylinderLedgerRepository repository,
                        CustomerRepository customerRepository,
                        CylinderVariantRepository variantRepository,
                        WarehouseRepository warehouseRepository,
                        InventoryStockService inventoryStockService,
                        WarehouseTransferService warehouseTransferService,
                        BankAccountRepository bankAccountRepository,
                        BankAccountService bankAccountService,
                        ReferenceNumberGenerator referenceNumberGenerator,
                        SaleRepository saleRepository,
                        WarehouseTransferRepository warehouseTransferRepository,
                        BankAccountLedgerRepository bankAccountLedgerRepository) {
                this.repository = repository;
                this.customerRepository = customerRepository;
                this.variantRepository = variantRepository;
                this.warehouseRepository = warehouseRepository;
                this.inventoryStockService = inventoryStockService;
                this.warehouseTransferService = warehouseTransferService;
                this.bankAccountRepository = bankAccountRepository;
                this.bankAccountService = bankAccountService;
                this.referenceNumberGenerator = referenceNumberGenerator;
                this.saleRepository = saleRepository;
                this.warehouseTransferRepository = warehouseTransferRepository;
                this.bankAccountLedgerRepository = bankAccountLedgerRepository;
        }

        // Get all ledger entries sorted by date descending (for stock movement history)
        public List<CustomerCylinderLedgerDTO> getAllMovements() {
                List<CustomerCylinderLedgerDTO> ledgerMovements = repository.findAll().stream()
                                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                                .map(this::toDTO)
                                .collect(Collectors.toList());

                // Get all warehouse transfers
                try {
                        List<com.gasagency.dto.WarehouseTransferDTO> allTransfers = warehouseTransferService
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

        // Get ledger entries for a specific warehouse sorted by date descending
        public List<CustomerCylinderLedgerDTO> getMovementsByWarehouse(Long warehouseId) {
                // Get customer ledger movements for this warehouse
                List<CustomerCylinderLedgerDTO> ledgerMovements = repository.findByWarehouseId(warehouseId).stream()
                                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                                .map(this::toDTO)
                                .collect(Collectors.toList());

                // Get warehouse transfers involving this warehouse
                try {
                        List<com.gasagency.dto.WarehouseTransferDTO> allTransfers = warehouseTransferService
                                        .getAllTransfers();
                        if (allTransfers != null && !allTransfers.isEmpty()) {
                                List<CustomerCylinderLedgerDTO> transferMovements = allTransfers.stream()
                                                .filter(t -> t.getFromWarehouseId() != null
                                                                && t.getToWarehouseId() != null &&
                                                                (t.getFromWarehouseId().equals(warehouseId)
                                                                                || t.getToWarehouseId()
                                                                                                .equals(warehouseId)))
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
                List<CustomerBalanceDTO> result = new ArrayList<>();
                for (Customer customer : customers) {
                        List<CustomerBalanceDTO.VariantBalance> variantBalances = new ArrayList<>();
                        for (CylinderVariant variant : variants) {
                                List<CustomerCylinderLedger> latestLedger = repository
                                                .findLatestLedger(customer.getId(), variant.getId());
                                Long balance = (!latestLedger.isEmpty() && latestLedger.get(0).getBalance() != null)
                                                ? latestLedger.get(0).getBalance()
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
                List<CylinderVariant> variants = variantRepository.findAllByActive(true);
                for (Customer customer : customers) {
                        for (CylinderVariant variant : variants) {
                                List<CustomerCylinderLedger> latestLedger = repository
                                                .findLatestLedger(customer.getId(), variant.getId());
                                if (!latestLedger.isEmpty()) {
                                        CustomerCylinderLedger ledger = latestLedger.get(0);
                                        CustomerCylinderLedgerDTO dto = toDTO(ledger);
                                        result.add(dto);
                                }
                        }
                }
                return result;
        }

        @Transactional
        public CustomerCylinderLedgerDTO createLedgerEntry(Long customerId, Long warehouseId, Long variantId,
                        LocalDate transactionDate, String refType, Long refId,
                        Long filledOut, Long emptyIn) {
                LoggerUtil.logBusinessEntry(logger, "CREATE_LEDGER_ENTRY", "customerId", customerId, "warehouseId",
                                warehouseId, "variantId", variantId);

                Customer customer = customerRepository.findById(customerId)
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

                // Calculate balance correctly
                // filledOut = cylinders given to customer (increases balance)
                // emptyIn = empty cylinders returned (decreases balance)
                Long previousBalance = getPreviousBalance(customerId, variantId);
                // Prevent returning more empties than the customer currently holds
                if (emptyIn > previousBalance + filledOut) {
                        LoggerUtil.logBusinessError(logger, "CREATE_LEDGER_ENTRY",
                                        "Empty return exceeds filled cylinders held (after this sale)", "customerId",
                                        customerId,
                                        "variantId", variantId, "previousBalance", previousBalance, "filledOut",
                                        filledOut, "emptyIn", emptyIn);
                        throw new IllegalArgumentException(
                                        "Cannot return more empty cylinders than the customer will hold for this variant after this sale.");
                }
                Long balance = previousBalance + filledOut - emptyIn;

                // For transaction types that require a reference, enforce refId not null
                CustomerCylinderLedger.TransactionType type = CustomerCylinderLedger.TransactionType.valueOf(refType);
                if ((type == CustomerCylinderLedger.TransactionType.SALE) && refId == null) {
                        throw new IllegalArgumentException("Reference ID is required for SALE transactions");
                }

                // Create new ledger entry for each transaction
                CustomerCylinderLedger ledger = new CustomerCylinderLedger(
                                customer, warehouse, variant, transactionDate,
                                type,
                                refId, filledOut, emptyIn, balance);
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
        public CustomerCylinderLedgerDTO createLedgerEntry(Long customerId, Long warehouseId, Long variantId,
                        LocalDate transactionDate, String refType, Long refId,
                        Long filledOut, Long emptyIn, BigDecimal totalAmount, BigDecimal amountReceived) {

                CustomerCylinderLedgerDTO dto = createLedgerEntry(customerId, warehouseId, variantId,
                                transactionDate, refType, refId, filledOut, emptyIn);

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
                                                .findByCustomer(ledger.getCustomer())
                                                .stream()
                                                .filter(e -> !e.getId().equals(ledgerId))
                                                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                                                .limit(1)
                                                .collect(Collectors.toList());

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
                                                .findByCustomer(ledger.getCustomer())
                                                .stream()
                                                .filter(e -> !e.getId().equals(ledgerId))
                                                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                                                .limit(1)
                                                .collect(Collectors.toList());

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
                                                .findByCustomer(ledger.getCustomer())
                                                .stream()
                                                .filter(e -> !e.getId().equals(ledgerId))
                                                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                                                .limit(1)
                                                .collect(Collectors.toList());

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
                dto.setTotalAmount(ledger.getTotalAmount());
                dto.setAmountReceived(ledger.getAmountReceived());
                dto.setDueAmount(ledger.getDueAmount());
                dto.setPaymentMode(ledger.getPaymentMode());
                dto.setTransactionReference(ledger.getTransactionReference());
                if (ledger.getBankAccount() != null) {
                        dto.setBankAccountId(ledger.getBankAccount().getId());
                        dto.setBankAccountName(ledger.getBankAccount().getBankName());
                }
                return dto;
        }

        private CustomerCylinderLedgerDTO transferToLedgerDTO(com.gasagency.dto.WarehouseTransferDTO transfer) {
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
                return dto;
        }

        /**
         * Record a payment transaction
         */
        @Transactional
        public CustomerCylinderLedgerDTO recordPayment(PaymentRequest paymentRequest) {
                LoggerUtil.logDatabaseOperation(logger, "INSERT", "PAYMENT", "customerId", paymentRequest.customerId,
                                "amount", paymentRequest.amount);

                Customer customer = customerRepository.findById(paymentRequest.customerId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "RECORD_PAYMENT", "Customer not found",
                                                        "customerId", paymentRequest.customerId);
                                        return new ResourceNotFoundException(
                                                        "Customer not found with id: " + paymentRequest.customerId);
                                });

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
                // Get all previous ledger entries for this customer
                List<CustomerCylinderLedger> previousEntries = repository.findByCustomer(customer);
                BigDecimal currentDue = BigDecimal.ZERO;

                for (CustomerCylinderLedger entry : previousEntries) {
                        if (entry.getRefType() == CustomerCylinderLedger.TransactionType.PAYMENT) {
                                currentDue = currentDue
                                                .subtract(entry.getAmountReceived() != null ? entry.getAmountReceived()
                                                                : java.math.BigDecimal.ZERO);
                        } else {
                                java.math.BigDecimal transactionDue = (entry.getTotalAmount() != null
                                                ? entry.getTotalAmount()
                                                : java.math.BigDecimal.ZERO)
                                                .subtract(entry.getAmountReceived() != null ? entry.getAmountReceived()
                                                                : java.math.BigDecimal.ZERO);
                                currentDue = currentDue.add(transactionDue);
                        }
                }

                // Validate payment amount doesn't exceed current due
                if (paymentRequest.amount.compareTo(currentDue) > 0) {
                        LoggerUtil.logBusinessError(logger, "RECORD_PAYMENT", "Payment exceeds due amount",
                                        "customerId", customer.getId(), "paymentAmount", paymentRequest.amount,
                                        "dueAmount", currentDue);
                        throw new IllegalArgumentException(
                                        "Payment amount ₹" + paymentRequest.amount
                                                        + " cannot exceed due amount ₹" + currentDue);
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

                // Set bank account if payment is via bank
                if (paymentRequest.bankAccountId != null && paymentRequest.paymentMode != null &&
                                !paymentRequest.paymentMode.equalsIgnoreCase("CASH")) {
                        BankAccount bankAccount = bankAccountRepository.findById(paymentRequest.bankAccountId)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Bank account not found with id: "
                                                                        + paymentRequest.bankAccountId));
                        ledger.setBankAccount(bankAccount);
                }

                CustomerCylinderLedger savedLedger = repository.save(ledger);

                // Record bank account deposit if payment mode is bank account
                if (paymentRequest.bankAccountId != null && paymentRequest.paymentMode != null &&
                                !paymentRequest.paymentMode.equalsIgnoreCase("CASH")) {
                        try {
                                bankAccountService.recordDeposit(
                                                paymentRequest.bankAccountId,
                                                paymentRequest.amount,
                                                savedLedger.getId(),
                                                "DUE-PAYMENT-" + savedLedger.getId(),
                                                "Due payment received from customer: " + customer.getName());
                                logger.info("Bank account deposit recorded for due payment - Customer: {}, Amount: {}",
                                                customer.getName(), paymentRequest.amount);
                        } catch (Exception e) {
                                logger.error("Error recording bank account deposit for due payment", e);
                                throw new RuntimeException("Failed to record bank account deposit: " + e.getMessage());
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
        public Map<String, Object> getCustomerLedgerSummary(Long customerId) {
                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Customer not found with id: " + customerId));

                List<CustomerCylinderLedger> allEntries = repository.findByCustomer(customer);
                Map<String, Object> summary = new HashMap<>();
                Map<Long, Map<String, Object>> variantSummary = new HashMap<>();

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

                                java.util.Map<String, Object> vSummary = new java.util.HashMap<>();
                                vSummary.put("variantName", variantName);
                                // Use the balance field which represents filled cylinders with customer
                                Long filledCount = ledger.getBalance() != null && ledger.getBalance() > 0
                                                ? ledger.getBalance()
                                                : 0L;
                                vSummary.put("filledCount", filledCount);
                                // Return pending = cylinders with customer that need to be returned (same as
                                // filledCount)
                                vSummary.put("returnPending", filledCount);

                                variantSummary.put(variantId, vSummary);
                        }
                }

                summary.put("variants", new ArrayList<>(variantSummary.values()));
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
                        bankAccountService.recordDeposit(
                                        bankAccountId,
                                        amount,
                                        ledgerId,
                                        referenceNumber,
                                        description);
                        logger.info("Bank account transaction recorded - BankAccountId: {}, Amount: {}, Reference: {}",
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
}
