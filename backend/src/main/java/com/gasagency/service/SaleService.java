
package com.gasagency.service;

import com.gasagency.dto.request.CreateSaleRequestDTO;
import com.gasagency.dto.response.SaleDTO;
import com.gasagency.dto.response.SaleItemDTO;
import com.gasagency.dto.response.SaleSummaryDTO;
import com.gasagency.dto.response.PaymentModeSummaryDTO;
import com.gasagency.dto.response.InventoryStockDTO;
import com.gasagency.entity.*;
import com.gasagency.repository.*;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.exception.ConcurrencyConflictException;
import com.gasagency.util.AuditLogger;
import com.gasagency.util.PerformanceTracker;
import com.gasagency.util.ReferenceNumberGenerator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SaleService {

        private static final Logger logger = LoggerFactory.getLogger(SaleService.class);

        private final SaleRepository saleRepository;
        private final SaleItemRepository saleItemRepository;
        private final CustomerRepository customerRepository;
        private final CylinderVariantRepository variantRepository;
        private final CustomerVariantPriceRepository customerVariantPriceRepository;
        private final InventoryStockService inventoryStockService;
        private final CustomerCylinderLedgerService ledgerService;
        private final CustomerCylinderLedgerRepository ledgerRepository;
        private final WarehouseService warehouseService;
        private final BankAccountRepository bankAccountRepository;
        private final BankAccountLedgerRepository bankAccountLedgerRepository;
        private final PaymentModeRepository paymentModeRepository;
        private final AuditLogger auditLogger;
        private final PerformanceTracker performanceTracker;
        private final ReferenceNumberGenerator referenceNumberGenerator;
        private final AlertConfigurationService alertConfigService;
        private final AlertNotificationService alertNotificationService;

        public SaleService(SaleRepository saleRepository,
                        SaleItemRepository saleItemRepository,
                        CustomerRepository customerRepository,
                        CylinderVariantRepository variantRepository,
                        CustomerVariantPriceRepository customerVariantPriceRepository,
                        InventoryStockService inventoryStockService,
                        CustomerCylinderLedgerService ledgerService,
                        CustomerCylinderLedgerRepository ledgerRepository,
                        WarehouseService warehouseService,
                        BankAccountRepository bankAccountRepository,
                        BankAccountLedgerRepository bankAccountLedgerRepository,
                        PaymentModeRepository paymentModeRepository,
                        AuditLogger auditLogger,
                        PerformanceTracker performanceTracker,
                        ReferenceNumberGenerator referenceNumberGenerator,
                        AlertConfigurationService alertConfigService,
                        AlertNotificationService alertNotificationService) {
                this.saleRepository = saleRepository;
                this.saleItemRepository = saleItemRepository;
                this.customerRepository = customerRepository;
                this.variantRepository = variantRepository;
                this.customerVariantPriceRepository = customerVariantPriceRepository;
                this.inventoryStockService = inventoryStockService;
                this.ledgerService = ledgerService;
                this.ledgerRepository = ledgerRepository;
                this.warehouseService = warehouseService;
                this.bankAccountRepository = bankAccountRepository;
                this.bankAccountLedgerRepository = bankAccountLedgerRepository;
                this.paymentModeRepository = paymentModeRepository;
                this.auditLogger = auditLogger;
                this.performanceTracker = performanceTracker;
                this.referenceNumberGenerator = referenceNumberGenerator;
                this.alertConfigService = alertConfigService;
                this.alertNotificationService = alertNotificationService;
        }

        @Transactional(readOnly = true)
        public List<SaleDTO> getRecentSales() {
                Pageable pageable = PageRequest.of(0, 5,
                                Sort.by(Sort.Direction.DESC, "saleDate"));
                return saleRepository.findAll(pageable)
                                .map(this::toDTO)
                                .getContent();
        }

        public SaleSummaryDTO getSalesSummary(String fromDate, String toDate, Long customerId,
                        Long variantId, Double minAmount, Double maxAmount, String referenceNumber, String createdBy) {
                LocalDate from = null;
                LocalDate to = null;
                try {
                        if (fromDate != null && !fromDate.isEmpty()) {
                                from = LocalDate.parse(fromDate);
                        }
                        if (toDate != null && !toDate.isEmpty()) {
                                to = LocalDate.parse(toDate);
                        }
                } catch (DateTimeParseException e) {
                        // Optionally log or handle parse error
                }
                // Fetch all filtered sales (no paging)
                List<Sale> sales = saleRepository.findFilteredSalesCustom(from, to, customerId, variantId,
                                minAmount, maxAmount, referenceNumber, createdBy, Pageable.unpaged()).getContent();
                double totalSalesAmount = 0;
                int transactionCount = 0;
                Map<String, Double> customerTotals = new java.util.HashMap<>();
                for (Sale sale : sales) {
                        boolean saleMatched = false;
                        if (sale.getSaleItems() != null) {
                                for (SaleItem item : sale.getSaleItems()) {
                                        // Apply item-level filters
                                        boolean match = true;
                                        if (variantId != null && (item.getVariant() == null
                                                        || !variantId.equals(item.getVariant().getId()))) {
                                                match = false;
                                        }
                                        if (minAmount != null && (item.getFinalPrice() == null
                                                        || item.getFinalPrice().doubleValue() < minAmount)) {
                                                match = false;
                                        }
                                        if (maxAmount != null && (item.getFinalPrice() == null
                                                        || item.getFinalPrice().doubleValue() > maxAmount)) {
                                                match = false;
                                        }
                                        if (!match)
                                                continue;
                                        saleMatched = true;
                                        double itemAmount = item.getFinalPrice() != null
                                                        ? item.getFinalPrice().doubleValue()
                                                        : 0.0;
                                        totalSalesAmount += itemAmount;
                                        String customerName = sale.getCustomer() != null ? sale.getCustomer().getName()
                                                        : "Unknown";
                                        customerTotals.put(customerName,
                                                        customerTotals.getOrDefault(customerName, 0.0) + itemAmount);
                                }
                        }
                        if (saleMatched) {
                                transactionCount++;
                        }
                }
                double avgSaleValue = transactionCount > 0 ? totalSalesAmount / transactionCount : 0;
                String topCustomer = "N/A";
                double maxTotal = -1;
                for (Map.Entry<String, Double> entry : customerTotals.entrySet()) {
                        if (entry.getValue() > maxTotal) {
                                maxTotal = entry.getValue();
                                topCustomer = entry.getKey();
                        }
                }
                return new SaleSummaryDTO(totalSalesAmount, transactionCount, avgSaleValue, topCustomer);
        }

        public SaleDTO createSale(CreateSaleRequestDTO request) {
                int maxRetries = 3;
                int attempt = 0;

                while (attempt < maxRetries) {
                        try {
                                return createSaleInternal(request);
                        } catch (ObjectOptimisticLockingFailureException e) {
                                attempt++;
                                if (attempt >= maxRetries) {
                                        logger.error("Sale creation failed after {} retries due to concurrent modifications",
                                                        maxRetries);
                                        throw new ConcurrencyConflictException(
                                                        "Sale creation failed due to concurrent updates. Please try again.");
                                }
                                logger.warn("Retry {} of {} for sale creation due to concurrent modification", attempt,
                                                maxRetries);
                                try {
                                        Thread.sleep(100 * attempt); // Exponential backoff
                                } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        throw new ConcurrencyConflictException(
                                                        "Sale creation interrupted. Please try again.");
                                }
                        }
                }
                throw new ConcurrencyConflictException("Failed to create sale after maximum retries.");
        }

        @Transactional(isolation = Isolation.REPEATABLE_READ)
        private SaleDTO createSaleInternal(CreateSaleRequestDTO request) {
                String transactionId = UUID.randomUUID().toString();
                MDC.put("transactionId", transactionId);
                long txnStartTime = System.currentTimeMillis();

                logger.info("Creating new sale with request: {}", request);

                // Validate request
                if (request == null || request.getCustomerId() == null) {
                        logger.error("Invalid sale request - request or customer ID is null");

                        throw new InvalidOperationException("Request and customer ID cannot be null");
                }

                if (request.getItems() == null || request.getItems().isEmpty()) {
                        logger.error("Invalid sale request - items list is null or empty");
                        throw new InvalidOperationException("Sale must contain at least one item");
                }

                String paymentModeName = request.getModeOfPayment() != null ? request.getModeOfPayment().trim() : null;
                Boolean requiresBankAccount = null;
                if (paymentModeName != null && !paymentModeName.isEmpty()) {
                        requiresBankAccount = paymentModeRepository.findByName(paymentModeName)
                                        .map(pm -> Boolean.TRUE.equals(pm.getIsBankAccountRequired()))
                                        .orElseThrow(() -> new InvalidOperationException(
                                                        "Invalid payment mode: " + paymentModeName));
                }

                // Validate modeOfPayment is provided only when amountReceived > 0
                if (request.getAmountReceived() != null && request.getAmountReceived().compareTo(BigDecimal.ZERO) > 0) {
                        if (paymentModeName == null || paymentModeName.isEmpty()) {
                                logger.error("Invalid sale request - modeOfPayment is required when amountReceived > 0");
                                throw new InvalidOperationException(
                                                "Mode of payment is required when payment is received");
                        }

                        // Validate bankAccountId is provided when payment mode requires it
                        if (Boolean.TRUE.equals(requiresBankAccount)
                                        && (request.getBankAccountId() == null || request.getBankAccountId() <= 0)) {
                                logger.error("Invalid sale request - bankAccountId is required for payment mode: {}",
                                                paymentModeName);
                                throw new InvalidOperationException(
                                                "Bank account is required for payment mode: " + paymentModeName);
                        }
                }

                logger.debug("Looking up customer with id: {}", request.getCustomerId());
                Customer customer = customerRepository.findById(request.getCustomerId())
                                .orElseThrow(
                                                () -> {
                                                        logger.error("Customer not found with id: {}",
                                                                        request.getCustomerId());
                                                        return new ResourceNotFoundException(
                                                                        "Customer not found with id: "
                                                                                        + request.getCustomerId());
                                                });

                if (!customer.getActive()) {
                        logger.warn("Cannot create sale for inactive customer with id: {}", customer.getId());
                        throw new InvalidOperationException("Cannot create sale for inactive customer");
                }
                // Validate and get warehouse
                logger.debug("Looking up warehouse with id: {}", request.getWarehouseId());
                Warehouse warehouse = warehouseService.getWarehouseEntity(request.getWarehouseId());
                if (warehouse == null) {
                        logger.error("Warehouse not found with id: {}", request.getWarehouseId());
                        throw new ResourceNotFoundException("Warehouse not found with id: " + request.getWarehouseId());
                }

                BigDecimal totalAmount = BigDecimal.ZERO;
                List<SaleItem> saleItems = new ArrayList<>();
                logger.debug("Processing {} sale items", request.getItems().size());

                // Edge-to-edge: Check customer balance for each variant before allowing empty
                // return in sale
                for (CreateSaleRequestDTO.SaleItemRequestDTO itemRequest : request.getItems()) {
                        if (itemRequest.getQtyEmptyReceived() != null && itemRequest.getQtyEmptyReceived() > 0) {
                                Long customerBalance = ledgerService.getPreviousBalance(request.getCustomerId(),
                                                itemRequest.getVariantId());
                                long allowedEmpty = (customerBalance != null ? customerBalance : 0L)
                                                + itemRequest.getQtyIssued();
                                if (itemRequest.getQtyEmptyReceived() > allowedEmpty) {
                                        logger.error("Attempt to return more empty cylinders than allowed in sale. Customer: {}, Variant: {}, Held: {}, Issued: {}, Attempted Return: {}",
                                                        request.getCustomerId(), itemRequest.getVariantId(),
                                                        customerBalance, itemRequest.getQtyIssued(),
                                                        itemRequest.getQtyEmptyReceived());
                                        throw new InvalidOperationException(
                                                        "Cannot return more empty cylinders than the customer will hold after this sale.");
                                }
                        }
                }

                // Validate and lock all inventory items upfront
                for (CreateSaleRequestDTO.SaleItemRequestDTO itemRequest : request.getItems()) {
                        // Validate item request
                        if (itemRequest.getVariantId() == null) {
                                logger.error("Item variant ID is null in sale request");
                                throw new InvalidOperationException("Variant ID cannot be null");
                        }
                        if (itemRequest.getQtyIssued() == null || itemRequest.getQtyIssued() <= 0) {
                                logger.error("Invalid quantity issued: {}", itemRequest.getQtyIssued());
                                throw new InvalidOperationException("Quantity issued must be greater than 0");
                        }
                        if (itemRequest.getQtyEmptyReceived() == null || itemRequest.getQtyEmptyReceived() < 0) {
                                logger.error("Invalid quantity empty received: {}", itemRequest.getQtyEmptyReceived());
                                throw new InvalidOperationException("Quantity empty received cannot be negative");
                        }

                        logger.debug("Looking up variant with id: {}", itemRequest.getVariantId());
                        CylinderVariant variant = variantRepository.findById(itemRequest.getVariantId())
                                        .orElseThrow(() -> {
                                                logger.error("Variant not found with id: {}",
                                                                itemRequest.getVariantId());
                                                return new ResourceNotFoundException(
                                                                "Variant not found with id: "
                                                                                + itemRequest.getVariantId());
                                        });

                        // Check inventory sufficiency with lock - WAREHOUSE-SPECIFIC
                        InventoryStock inventoryStock = inventoryStockService
                                        .getStockByWarehouseAndVariantWithLock(warehouse, variant);
                        logger.debug("Warehouse: {}, Variant: {}, Available filled: {}, Requested: {}",
                                        warehouse.getName(), variant.getName(), inventoryStock.getFilledQty(),
                                        itemRequest.getQtyIssued());

                        if (inventoryStock.getFilledQty() < itemRequest.getQtyIssued()) {
                                logger.error("Insufficient inventory in warehouse {} for variant: {}. Available: {}, Requested: {}",
                                                warehouse.getName(), variant.getName(), inventoryStock.getFilledQty(),
                                                itemRequest.getQtyIssued());
                                throw new InvalidOperationException(
                                                "Insufficient inventory in warehouse " + warehouse.getName() +
                                                                " for variant: " + variant.getName() +
                                                                ". Available: " + inventoryStock.getFilledQty() +
                                                                ", Requested: " + itemRequest.getQtyIssued());
                        }

                        // Get customer-specific pricing - required for sales
                        logger.debug("Fetching customer-specific pricing for variant: {} and customer: {}",
                                        variant.getName(), customer.getId());
                        var customerVariantPrice = customerVariantPriceRepository
                                        .findByCustomerIdAndVariantId(customer.getId(), variant.getId())
                                        .orElseThrow(() -> {
                                                logger.error("Customer-specific price not found for variant: {} and customer: {}",
                                                                variant.getName(), customer.getId());
                                                return new ResourceNotFoundException(
                                                                "Customer-specific price not configured for variant: "
                                                                                + variant.getName());
                                        });

                        BigDecimal basePrice = customerVariantPrice.getSalePrice();
                        BigDecimal subtotal = basePrice.multiply(BigDecimal.valueOf(itemRequest.getQtyIssued()));
                        BigDecimal discountAmount = itemRequest.getDiscount() != null ? itemRequest.getDiscount()
                                        : BigDecimal.ZERO;

                        // Validate discount is not negative
                        if (discountAmount.signum() < 0) {
                                logger.error("Negative discount amount: {}", discountAmount);
                                throw new InvalidOperationException("Discount cannot be negative");
                        }

                        // Validate discount does not exceed subtotal
                        if (discountAmount.compareTo(subtotal) > 0) {
                                logger.error("Discount {} exceeds subtotal {}", discountAmount, subtotal);
                                throw new InvalidOperationException(
                                                "Discount cannot exceed subtotal. Subtotal: " + subtotal +
                                                                ", Discount: " + discountAmount);
                        }

                        BigDecimal finalPrice = subtotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
                        totalAmount = totalAmount.add(finalPrice);
                        logger.debug("Item price calculated - Subtotal: {}, Discount: {}, Final: {}",
                                        subtotal, discountAmount, finalPrice);

                        // Create sale item without sale (sale will be set after creation)
                        SaleItem saleItem = new SaleItem(warehouse, variant, itemRequest.getQtyIssued(),
                                        itemRequest.getQtyEmptyReceived(), basePrice, discountAmount, finalPrice);
                        saleItems.add(saleItem);
                }

                logger.info("Sale validation complete - Total amount: {}, Items: {}", totalAmount, saleItems.size());

                // Edge-to-edge: Disallow zero-amount sales
                if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        logger.error("Attempt to create sale with zero or negative total amount");
                        throw new InvalidOperationException("Sale total amount must be greater than zero.");
                }

                // Validate amountReceived does not exceed total due amount (previous due +
                // current sale)
                if (request.getAmountReceived() != null && request.getAmountReceived().compareTo(BigDecimal.ZERO) > 0) {
                        // Get customer's previous due amount
                        BigDecimal previousDueAmount = ledgerService.getCustomerPreviousDue(request.getCustomerId());
                        BigDecimal totalDueAmount = previousDueAmount.add(totalAmount);

                        logger.info("Payment validation - customerId: {}, previousDue: {}, currentSale: {}, totalDue: {}, amountReceived: {}",
                                        request.getCustomerId(), previousDueAmount, totalAmount, totalDueAmount,
                                        request.getAmountReceived());

                        if (request.getAmountReceived().compareTo(totalDueAmount) > 0) {
                                logger.error("Amount received {} exceeds total due amount {} (previous due: {}, current sale: {})",
                                                request.getAmountReceived(), totalDueAmount, previousDueAmount,
                                                totalAmount);
                                throw new InvalidOperationException(
                                                "Amount received ("
                                                                + request.getAmountReceived().setScale(2,
                                                                                RoundingMode.HALF_UP)
                                                                +
                                                                ") cannot exceed total due amount ("
                                                                + totalDueAmount.setScale(2, RoundingMode.HALF_UP) +
                                                                "). Previous due: "
                                                                + previousDueAmount.setScale(2, RoundingMode.HALF_UP) +
                                                                ", Current sale: "
                                                                + totalAmount.setScale(2, RoundingMode.HALF_UP));
                        }
                }

                // Create sale FIRST so we have a sale ID for ledger references
                Sale sale = new Sale(warehouse, customer, LocalDate.now(), totalAmount);
                // Set payment mode as provided (can be null)
                String normalizedPaymentMode = paymentModeName != null
                                ? paymentModeName.toUpperCase()
                                : null;
                sale.setPaymentMode(normalizedPaymentMode);

                // Generate and set reference number BEFORE saving
                String referenceNumber = referenceNumberGenerator.generateSaleReference(warehouse);
                sale.setReferenceNumber(referenceNumber);

                // If bank account ID is provided and payment mode is not cash, set the bank
                // account
                Long bankAccountIdForLedger = Boolean.TRUE.equals(requiresBankAccount)
                                ? request.getBankAccountId()
                                : null;
                if (bankAccountIdForLedger != null) {
                        BankAccount bankAccount = bankAccountRepository.findById(request.getBankAccountId())
                                        .orElseThrow(() -> {
                                                logger.error("Bank account not found with id: {}",
                                                                request.getBankAccountId());
                                                return new ResourceNotFoundException("Bank account not found with id: "
                                                                + request.getBankAccountId());
                                        });
                        sale.setBankAccount(bankAccount);
                        logger.info("Bank account linked to sale: {}", bankAccount.getBankName());
                }

                sale = saleRepository.save(sale);
                logger.info("Sale created with id: {} for customer: {} - Total: {} - Reference: {}",
                                sale.getId(), customer.getName(), totalAmount, referenceNumber);

                // Refresh the sale entity to ensure bankAccount relationship is loaded
                if (sale.getId() != null) {
                        sale = saleRepository.findById(sale.getId()).orElse(sale);
                }

                // Record bank account transaction if payment is via bank account
                final Sale finalSale = sale;
                if (finalSale.getBankAccount() != null && request.getAmountReceived() != null &&
                                request.getAmountReceived().compareTo(BigDecimal.ZERO) > 0) {
                        try {
                                bankAccountRepository.findById(finalSale.getBankAccount().getId())
                                                .ifPresent(bankAccount -> {
                                                        BankAccountLedger ledger = new BankAccountLedger(
                                                                        bankAccount,
                                                                        "DEPOSIT",
                                                                        request.getAmountReceived(),
                                                                        null,
                                                                        finalSale,
                                                                        referenceNumberGenerator
                                                                                        .generateBankTransactionReference(
                                                                                                        bankAccount.getCode(),
                                                                                                        "DEP"),
                                                                        "Payment received from customer: "
                                                                                        + customer.getName());
                                                        bankAccountLedgerRepository.save(ledger);
                                                        logger.info("Bank ledger entry recorded for sale id: {} - Amount: {}",
                                                                        finalSale.getId(), request.getAmountReceived());
                                                });
                        } catch (Exception e) {
                                logger.error("Error recording bank ledger entry for sale id: {}", finalSale.getId(), e);
                        }
                }

                // Now attach sale items and persist
                for (int i = 0; i < saleItems.size(); i++) {
                        saleItems.get(i).setSale(sale);
                        saleItemRepository.save(saleItems.get(i));

                        CreateSaleRequestDTO.SaleItemRequestDTO itemRequest = request.getItems().get(i);
                        CylinderVariant variant = saleItems.get(i).getVariant();

                        logger.debug("Processing sale item - Variant: {}, Qty: {}", variant.getName(),
                                        itemRequest.getQtyIssued());

                        // Update inventory (decrement filled, increment empty) - WAREHOUSE-SPECIFIC
                        inventoryStockService.decrementFilledQty(warehouse, variant,
                                        Long.valueOf(itemRequest.getQtyIssued()));
                        inventoryStockService.incrementEmptyQty(warehouse, variant,
                                        Long.valueOf(itemRequest.getQtyEmptyReceived()));
                        logger.debug("Inventory updated in warehouse {} - Variant: {}, Filled qty decrement: {}, Empty qty increment: {}",
                                        warehouse.getName(), variant.getName(), itemRequest.getQtyIssued(),
                                        itemRequest.getQtyEmptyReceived());

                        // Create ledger entry
                        ledgerService.createLedgerEntry(
                                        customer.getId(),
                                        warehouse.getId(),
                                        variant.getId(),
                                        sale.getSaleDate(),
                                        "SALE",
                                        sale.getId(),
                                        itemRequest.getQtyIssued(),
                                        itemRequest.getQtyEmptyReceived(),
                                        totalAmount,
                                        request.getAmountReceived(),
                                request.getModeOfPayment(),
                                bankAccountIdForLedger);
                        logger.debug("Ledger entry created for sale item");
                }

                logger.info("Sale {} completed successfully for customer {}", sale.getId(), customer.getName());

                // Check for low stock alerts in real-time after sale
                checkAndCreateLowStockAlerts(warehouse);

                // Track performance and audit
                long txnDuration = System.currentTimeMillis() - txnStartTime;
                performanceTracker.trackTransaction(transactionId, txnDuration, "COMPLETED");
                auditLogger.logSaleCreated(sale.getId(), customer.getId(), customer.getName(),
                                totalAmount.doubleValue());
                logger.info("TRANSACTION_SUMMARY | txnId={} | saleId={} | customer={} | amount={} | duration={}ms",
                                transactionId, sale.getId(), customer.getName(), totalAmount, txnDuration);

                MDC.remove("transactionId");

                // Clear dashboard cache since sales affect daily/monthly metrics
                clearDashboardCache();

                // Convert to DTO with sale items already loaded to avoid
                // LazyInitializationException
                return toDTOWithItems(sale, saleItems);
        }

        @Transactional(readOnly = true)
        public SaleDTO getSaleById(Long id) {
                logger.debug("Fetching sale with id: {}", id);
                Sale sale = saleRepository.findById(id)
                                .orElseThrow(() -> {
                                        logger.error("Sale not found with id: {}", id);
                                        return new ResourceNotFoundException("Sale not found with id: " + id);
                                });
                return toDTO(sale);
        }

        /**
         * Check for low stock after sale and create alerts if thresholds exceeded
         * This runs immediately after sale to provide real-time alerts
         */
        private void checkAndCreateLowStockAlerts(Warehouse warehouse) {
                try {
                        logger.debug("Starting real-time low stock check for warehouse: {}", warehouse.getName());

                        // Get alert configuration for LOW_STOCK_WAREHOUSE
                        java.util.Optional<com.gasagency.entity.AlertConfiguration> configOpt = alertConfigService
                                        .getConfigOptional("LOW_STOCK_WAREHOUSE");

                        if (configOpt.isEmpty()) {
                                logger.warn("Alert configuration for LOW_STOCK_WAREHOUSE not found");
                                return;
                        }

                        com.gasagency.entity.AlertConfiguration config = configOpt.get();
                        if (!config.getEnabled()) {
                                logger.debug("LOW_STOCK_WAREHOUSE alerts are disabled");
                                return;
                        }

                        int filledThreshold = config.getFilledCylinderThreshold() != null
                                        ? config.getFilledCylinderThreshold()
                                        : 50;
                        int emptyThreshold = config.getEmptyCylinderThreshold() != null
                                        ? config.getEmptyCylinderThreshold()
                                        : 50;

                        logger.debug("Alert thresholds - Filled: {}, Empty: {}", filledThreshold, emptyThreshold);

                        // Get current stock for this warehouse
                        java.util.List<InventoryStockDTO> warehouseStocks = inventoryStockService
                                        .getStockDTOsByWarehouse(warehouse);

                        if (warehouseStocks == null || warehouseStocks.isEmpty()) {
                                logger.warn("No stock found for warehouse: {}", warehouse.getName());
                                return;
                        }

                        Long warehouseId = warehouse.getId();
                        String warehouseName = warehouse.getName();

                        // Check EACH variant individually (per-variant checking, not warehouse totals)
                        for (InventoryStockDTO stock : warehouseStocks) {
                                String variantName = stock.getVariantName() != null ? stock.getVariantName()
                                                : "Variant " + stock.getVariantId();
                                long filledQty = stock.getFilledQty() != null ? stock.getFilledQty() : 0;
                                long emptyQty = stock.getEmptyQty() != null ? stock.getEmptyQty() : 0;

                                logger.debug("Checking variant {} - Filled: {} (threshold: {}), Empty: {} (threshold: {})",
                                                variantName, filledQty, filledThreshold, emptyQty, emptyThreshold);

                                // Check filled cylinders for this variant
                                if (filledQty < filledThreshold) {
                                        String alertKey = "LOW_STOCK_FILLED_WH_" + warehouseId + "_VAR_"
                                                        + stock.getVariantId();
                                        String message = warehouseName + " - " + variantName + ": Only " + filledQty +
                                                        " filled cylinders (threshold: " + filledThreshold + ")";

                                        try {
                                                alertNotificationService.createOrUpdateAlert(
                                                                "LOW_STOCK_WAREHOUSE",
                                                                alertKey,
                                                                warehouseId,
                                                                null,
                                                                message,
                                                                "warning");
                                                logger.warn("ALERT: Real-time low filled stock alert created - {}",
                                                                message);
                                        } catch (Exception e) {
                                                logger.error("Error creating filled stock alert for warehouse {} variant {}",
                                                                warehouseId, stock.getVariantId(), e);
                                        }
                                }

                                // Check empty cylinders for this variant
                                if (emptyQty < emptyThreshold) {
                                        String alertKey = "LOW_STOCK_EMPTY_WH_" + warehouseId + "_VAR_"
                                                        + stock.getVariantId();
                                        String message = warehouseName + " - " + variantName + ": Only " + emptyQty +
                                                        " empty cylinders (threshold: " + emptyThreshold + ")";

                                        try {
                                                alertNotificationService.createOrUpdateAlert(
                                                                "LOW_STOCK_WAREHOUSE",
                                                                alertKey,
                                                                warehouseId,
                                                                null,
                                                                message,
                                                                "warning");
                                                logger.warn("ALERT: Real-time low empty stock alert created - {}",
                                                                message);
                                        } catch (Exception e) {
                                                logger.error("Error creating empty stock alert for warehouse {} variant {}",
                                                                warehouseId, stock.getVariantId(), e);
                                        }
                                }
                        }

                        logger.debug("Low stock check completed for warehouse: {}", warehouseName);
                } catch (Exception e) {
                        logger.error("Error checking for low stock alerts after sale", e);
                }
        }

        @Transactional(readOnly = true)
        public Page<SaleDTO> getAllSales(Pageable pageable, String fromDate, String toDate, Long customerId,
                        Long variantId, Double minAmount, Double maxAmount, String referenceNumber, String createdBy) {
                logger.debug("Fetching all sales with filters: page={}, size={}, customerId={}, variantId={}, minAmount={}, maxAmount={}, referenceNumber={}",
                                pageable.getPageNumber(), pageable.getPageSize(), customerId, variantId, minAmount,
                                maxAmount, referenceNumber);
                LocalDate from = null;
                LocalDate to = null;
                try {
                        if (fromDate != null && !fromDate.isEmpty()) {
                                from = LocalDate.parse(fromDate);
                        }
                        if (toDate != null && !toDate.isEmpty()) {
                                to = LocalDate.parse(toDate);
                        }
                } catch (DateTimeParseException e) {
                        // Optionally log or handle parse error
                }
                // Use custom repository method for filtering
                return saleRepository
                                .findFilteredSalesCustom(from, to, customerId, variantId, minAmount, maxAmount,
                                                referenceNumber, createdBy,
                                                pageable)
                                .map(this::toDTO);
        }

        public Page<SaleDTO> getSalesByCustomer(Long customerId, Pageable pageable) {
                logger.debug("Fetching sales for customer: {} with pagination", customerId);
                return saleRepository.findByCustomerId(customerId, pageable)
                                .map(this::toDTO);
        }

        @Transactional(readOnly = true)
        public Page<SaleDTO> getSalesByDateRange(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
                logger.debug("Fetching sales between {} and {}", fromDate, toDate);
                return saleRepository.findByDateRange(fromDate, toDate, pageable)
                                .map(this::toDTO);
        }

        public PaymentModeSummaryDTO getPaymentModeSummary(String fromDate, String toDate,
                        Long customerId,
                        String paymentMode, Long variantId, Long bankAccountId, Double minAmount, Double maxAmount,
                        Integer minTransactionCount) {
                LocalDate from = null;
                LocalDate to = null;
                try {
                        if (fromDate != null && !fromDate.isEmpty()) {
                                from = LocalDate.parse(fromDate);
                        }
                        if (toDate != null && !toDate.isEmpty()) {
                                to = LocalDate.parse(toDate);
                        }
                } catch (DateTimeParseException e) {
                        // Optionally log or handle parse error
                }

                final LocalDate finalFrom = from;
                final LocalDate finalTo = to;
                BigDecimal minAmountValue = minAmount != null ? BigDecimal.valueOf(minAmount) : null;
                BigDecimal maxAmountValue = maxAmount != null ? BigDecimal.valueOf(maxAmount) : null;

                // Get ledger entries filtered in DB
                List<CustomerCylinderLedger> ledgers = ledgerRepository.findForPaymentModeSummary(
                                finalFrom,
                                finalTo,
                                customerId,
                                paymentMode,
                                bankAccountId,
                                variantId,
                                minAmountValue,
                                maxAmountValue);

                PaymentModeSummaryDTO summary = new PaymentModeSummaryDTO();
                Map<String, PaymentModeSummaryDTO.PaymentModeStats> stats = new java.util.HashMap<>();
                double totalAmount = 0;
                int totalTransactions = 0;

                for (CustomerCylinderLedger ledger : ledgers) {
                        // Skip entries without payment mode (prevents null map keys)
                        String ledgerPaymentMode = ledger.getPaymentMode() != null && !ledger.getPaymentMode().isEmpty()
                                        ? ledger.getPaymentMode().trim().toUpperCase()
                                        : null;
                        if (ledgerPaymentMode == null) {
                                continue;
                        }
                        double ledgerAmount = ledger.getAmountReceived() != null
                                        ? ledger.getAmountReceived().doubleValue()
                                        : 0.0;

                        stats.computeIfAbsent(ledgerPaymentMode,
                                        key -> new PaymentModeSummaryDTO.PaymentModeStats(key, key, 0,
                                                        0))
                                        .setTotalAmount(stats.get(ledgerPaymentMode).getTotalAmount() + ledgerAmount);

                        stats.get(ledgerPaymentMode)
                                        .setTransactionCount(stats.get(ledgerPaymentMode).getTransactionCount() + 1);

                        totalAmount += ledgerAmount;
                        totalTransactions++;
                }

                // Apply min transaction count filter
                if (minTransactionCount != null && minTransactionCount > 0) {
                        stats.entrySet().removeIf(
                                        entry -> entry.getValue().getTransactionCount() < minTransactionCount);
                        // Recalculate totals
                        totalAmount = 0;
                        totalTransactions = 0;
                        for (PaymentModeSummaryDTO.PaymentModeStats stat : stats.values()) {
                                totalAmount += stat.getTotalAmount();
                                totalTransactions += stat.getTransactionCount();
                        }
                }

                summary.setPaymentModeStats(stats);
                summary.setTotalAmount(totalAmount);
                summary.setTotalTransactions(totalTransactions);

                return summary;
        }

        private SaleDTO toDTO(Sale sale) {
                // Fetch all ledger entries for this sale in one query
                List<CustomerCylinderLedger> saleLedgers = ledgerRepository.findBySaleId(sale.getId());

                // Create a map of ledger entries by variant ID for quick lookup
                Map<Long, CustomerCylinderLedger> ledgerMap = saleLedgers.stream()
                                .collect(Collectors.toMap(
                                                ledger -> ledger.getVariant().getId(),
                                                ledger -> ledger,
                                                (existing, replacement) -> existing));

                // Get payment mode from first ledger entry (all items in same sale should have
                // same payment mode)
                final String paymentMode = !saleLedgers.isEmpty() ? saleLedgers.get(0).getPaymentMode() : null;

                List<SaleItemDTO> items = sale.getSaleItems().stream()
                                .map(item -> {
                                        // Get ledger data from map (already fetched)
                                        CustomerCylinderLedger ledger = ledgerMap.get(item.getVariant().getId());

                                        BigDecimal amountReceived = ledger != null ? ledger.getAmountReceived() : null;
                                        BigDecimal dueAmount = ledger != null ? ledger.getDueAmount() : null;
                                        String itemPaymentMode = ledger != null ? ledger.getPaymentMode() : paymentMode;

                                        return new SaleItemDTO(
                                                        item.getId(),
                                                        item.getVariant().getId(),
                                                        item.getVariant().getName(),
                                                        item.getQtyIssued(),
                                                        item.getQtyEmptyReceived(),
                                                        item.getBasePrice(),
                                                        item.getDiscount(),
                                                        item.getFinalPrice(),
                                                        amountReceived,
                                                        dueAmount,
                                                        itemPaymentMode);
                                })
                                .collect(Collectors.toList());

                String bankAccountName = null;
                Long bankAccountId = null;
                if (sale.getBankAccount() != null) {
                        bankAccountId = sale.getBankAccount().getId();
                        bankAccountName = sale.getBankAccount().getBankName() + " - " +
                                        sale.getBankAccount().getAccountNumber();
                }

                SaleDTO dto = new SaleDTO(
                                sale.getId(),
                                sale.getReferenceNumber(),
                                sale.getCustomer().getId(),
                                sale.getCustomer().getName(),
                                sale.getSaleDate(),
                                sale.getTotalAmount(),
                                paymentMode,
                                bankAccountId,
                                bankAccountName,
                                items);
                dto.setCreatedBy(sale.getCreatedBy());
                dto.setCreatedDate(sale.getCreatedDate());
                dto.setUpdatedBy(sale.getUpdatedBy());
                dto.setUpdatedDate(sale.getUpdatedDate());
                return dto;
        }

        /**
         * Convert Sale to DTO with pre-loaded sale items to avoid
         * LazyInitializationException
         */
        private SaleDTO toDTOWithItems(Sale sale, List<SaleItem> saleItems) {
                // Fetch all ledger entries for this sale in one query
                List<CustomerCylinderLedger> saleLedgers = ledgerRepository.findBySaleId(sale.getId());

                // Create a map of ledger entries by variant ID for quick lookup
                Map<Long, CustomerCylinderLedger> ledgerMap = saleLedgers.stream()
                                .collect(Collectors.toMap(
                                                ledger -> ledger.getVariant().getId(),
                                                ledger -> ledger,
                                                (existing, replacement) -> existing));

                // Get payment mode from first ledger entry (all items in same sale should have
                // same payment mode)
                final String paymentMode = !saleLedgers.isEmpty() ? saleLedgers.get(0).getPaymentMode() : null;

                List<SaleItemDTO> items = saleItems.stream()
                                .map(item -> {
                                        // Get ledger data from map (already fetched)
                                        CustomerCylinderLedger ledger = ledgerMap.get(item.getVariant().getId());

                                        BigDecimal amountReceived = ledger != null ? ledger.getAmountReceived() : null;
                                        BigDecimal dueAmount = ledger != null ? ledger.getDueAmount() : null;
                                        String itemPaymentMode = ledger != null ? ledger.getPaymentMode() : paymentMode;

                                        return new SaleItemDTO(
                                                        item.getId(),
                                                        item.getVariant().getId(),
                                                        item.getVariant().getName(),
                                                        item.getQtyIssued(),
                                                        item.getQtyEmptyReceived(),
                                                        item.getBasePrice(),
                                                        item.getDiscount(),
                                                        item.getFinalPrice(),
                                                        amountReceived,
                                                        dueAmount,
                                                        itemPaymentMode);
                                })
                                .collect(Collectors.toList());

                String bankAccountName = null;
                Long bankAccountId = null;
                if (sale.getBankAccount() != null) {
                        bankAccountId = sale.getBankAccount().getId();
                        bankAccountName = sale.getBankAccount().getBankName() + " - " +
                                        sale.getBankAccount().getAccountNumber();
                }

                SaleDTO dto = new SaleDTO(
                                sale.getId(),
                                sale.getReferenceNumber(),
                                sale.getCustomer().getId(),
                                sale.getCustomer().getName(),
                                sale.getSaleDate(),
                                sale.getTotalAmount(),
                                paymentMode,
                                bankAccountId,
                                bankAccountName,
                                items);
                dto.setCreatedBy(sale.getCreatedBy());
                dto.setCreatedDate(sale.getCreatedDate());
                dto.setUpdatedBy(sale.getUpdatedBy());
                dto.setUpdatedDate(sale.getUpdatedDate());
                return dto;
        }

        @CacheEvict(value = "dashboardCache", allEntries = true)
        public void clearDashboardCache() {
                // This method is called whenever data changes to invalidate dashboard cache
        }
}


