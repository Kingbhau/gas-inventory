
package com.gasagency.service;

import com.gasagency.dto.CustomerCylinderLedgerDTO;
import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Warehouse;
import com.gasagency.repository.CustomerCylinderLedgerRepository;
import com.gasagency.repository.CustomerRepository;
import com.gasagency.repository.CylinderVariantRepository;
import com.gasagency.repository.WarehouseRepository;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.util.LoggerUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;
import com.gasagency.dto.CustomerBalanceDTO;
import java.util.ArrayList;

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

        public CustomerCylinderLedgerService(CustomerCylinderLedgerRepository repository,
                        CustomerRepository customerRepository,
                        CylinderVariantRepository variantRepository,
                        WarehouseRepository warehouseRepository,
                        InventoryStockService inventoryStockService,
                        WarehouseTransferService warehouseTransferService) {
                this.repository = repository;
                this.customerRepository = customerRepository;
                this.variantRepository = variantRepository;
                this.warehouseRepository = warehouseRepository;
                this.inventoryStockService = inventoryStockService;
                this.warehouseTransferService = warehouseTransferService;
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

                Warehouse warehouse = warehouseRepository.findById(warehouseId)
                                .orElseThrow(() -> {
                                        LoggerUtil.logBusinessError(logger, "CREATE_LEDGER_ENTRY",
                                                        "Warehouse not found",
                                                        "warehouseId", warehouseId);
                                        return new ResourceNotFoundException(
                                                        "Warehouse not found with id: " + warehouseId);
                                });

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

                LoggerUtil.logBusinessSuccess(logger, "CREATE_LEDGER_ENTRY", "id", ledger.getId(), "customerId",
                                customerId, "balance", balance);
                LoggerUtil.logAudit("CREATE", "LEDGER", "ledgerId", ledger.getId(), "customerId", customerId);

                // Update inventory for EMPTY_RETURN
                if (type == CustomerCylinderLedger.TransactionType.EMPTY_RETURN && emptyIn > 0) {
                        inventoryStockService.incrementEmptyQty(warehouse, variant, emptyIn);
                }

                return toDTO(ledger);
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
                CustomerCylinderLedgerDTO dto = new CustomerCylinderLedgerDTO(
                                ledger.getId(),
                                ledger.getCustomer().getId(),
                                ledger.getCustomer().getName(),
                                ledger.getVariant().getId(),
                                ledger.getVariant().getName(),
                                ledger.getTransactionDate(),
                                ledger.getRefType().toString(),
                                ledger.getRefId(),
                                ledger.getFilledOut(),
                                ledger.getEmptyIn(),
                                ledger.getBalance());
                dto.setCreatedAt(ledger.getCreatedDate());
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
}
