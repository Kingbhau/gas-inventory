
package com.gasagency.service;

import com.gasagency.dto.CreateSaleRequestDTO;
import com.gasagency.dto.SaleDTO;
import com.gasagency.dto.SaleItemDTO;
import com.gasagency.dto.SaleSummaryDTO;
import com.gasagency.entity.*;
import com.gasagency.repository.*;
import com.gasagency.exception.ResourceNotFoundException;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.exception.ConcurrencyConflictException;
import com.gasagency.util.AuditLogger;
import com.gasagency.util.PerformanceTracker;
import com.gasagency.util.ReferenceNumberGenerator;
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
        private final WarehouseService warehouseService;
        private final BankAccountRepository bankAccountRepository;
        private final BankAccountService bankAccountService;
        private final AuditLogger auditLogger;
        private final PerformanceTracker performanceTracker;
        private final ReferenceNumberGenerator referenceNumberGenerator;

        public SaleService(SaleRepository saleRepository,
                        SaleItemRepository saleItemRepository,
                        CustomerRepository customerRepository,
                        CylinderVariantRepository variantRepository,
                        CustomerVariantPriceRepository customerVariantPriceRepository,
                        InventoryStockService inventoryStockService,
                        CustomerCylinderLedgerService ledgerService,
                        WarehouseService warehouseService,
                        BankAccountRepository bankAccountRepository,
                        BankAccountService bankAccountService,
                        AuditLogger auditLogger,
                        PerformanceTracker performanceTracker,
                        ReferenceNumberGenerator referenceNumberGenerator) {
                this.saleRepository = saleRepository;
                this.saleItemRepository = saleItemRepository;
                this.customerRepository = customerRepository;
                this.variantRepository = variantRepository;
                this.customerVariantPriceRepository = customerVariantPriceRepository;
                this.inventoryStockService = inventoryStockService;
                this.ledgerService = ledgerService;
                this.warehouseService = warehouseService;
                this.bankAccountRepository = bankAccountRepository;
                this.bankAccountService = bankAccountService;
                this.auditLogger = auditLogger;
                this.performanceTracker = performanceTracker;
                this.referenceNumberGenerator = referenceNumberGenerator;
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
                        Long variantId, Double minAmount, Double maxAmount, String referenceNumber) {
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
                                minAmount, maxAmount, referenceNumber, Pageable.unpaged()).getContent();
                double totalSalesAmount = 0;
                int transactionCount = 0;
                Map<String, Double> customerTotals = new java.util.HashMap<>();
                for (Sale sale : sales) {
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
                                        double itemAmount = item.getFinalPrice() != null
                                                        ? item.getFinalPrice().doubleValue()
                                                        : 0.0;
                                        totalSalesAmount += itemAmount;
                                        transactionCount++;
                                        String customerName = sale.getCustomer() != null ? sale.getCustomer().getName()
                                                        : "Unknown";
                                        customerTotals.put(customerName,
                                                        customerTotals.getOrDefault(customerName, 0.0) + itemAmount);
                                }
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

        @Transactional(isolation = Isolation.READ_COMMITTED)
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

                // Validate modeOfPayment is provided only when amountReceived > 0
                if (request.getAmountReceived() != null && request.getAmountReceived().compareTo(BigDecimal.ZERO) > 0) {
                        if (request.getModeOfPayment() == null || request.getModeOfPayment().trim().isEmpty()) {
                                logger.error("Invalid sale request - modeOfPayment is required when amountReceived > 0");
                                throw new InvalidOperationException(
                                                "Mode of payment is required when payment is received");
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
                                if (itemRequest.getQtyEmptyReceived() > customerBalance) {
                                        logger.error("Attempt to return more empty cylinders than held in sale. Customer: {}, Variant: {}, Held: {}, Attempted Return: {}",
                                                        request.getCustomerId(), itemRequest.getVariantId(),
                                                        customerBalance, itemRequest.getQtyEmptyReceived());
                                        throw new InvalidOperationException(
                                                        "Cannot return more empty cylinders than the customer currently holds for this variant in sale.");
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
                sale.setPaymentMode(request.getModeOfPayment());

                // Generate and set reference number BEFORE saving
                String referenceNumber = referenceNumberGenerator.generateSaleReference(warehouse);
                sale.setReferenceNumber(referenceNumber);

                // If bank account ID is provided and payment mode is not cash, set the bank
                // account
                if (request.getBankAccountId() != null && request.getModeOfPayment() != null &&
                                !request.getModeOfPayment().equalsIgnoreCase("CASH")) {
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

                // Record bank account deposit if payment is via bank account
                if (sale.getBankAccount() != null && request.getAmountReceived() != null &&
                                request.getAmountReceived().compareTo(BigDecimal.ZERO) > 0) {
                        try {
                                bankAccountService.recordDeposit(
                                                sale.getBankAccount().getId(),
                                                request.getAmountReceived(),
                                                sale.getId(),
                                                sale.getReferenceNumber(),
                                                "Payment received from customer: " + customer.getName());
                                logger.info("Bank account deposit recorded for sale id: {} - Amount: {} - Reference: {}",
                                                sale.getId(), request.getAmountReceived(), sale.getReferenceNumber());
                        } catch (Exception e) {
                                logger.error("Error recording bank account deposit for sale id: {}", sale.getId(), e);
                                throw new InvalidOperationException(
                                                "Failed to record bank account deposit: " + e.getMessage());
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
                                        request.getModeOfPayment());
                        logger.debug("Ledger entry created for sale item");
                }

                logger.info("Sale {} completed successfully for customer {}", sale.getId(), customer.getName());

                // Track performance and audit
                long txnDuration = System.currentTimeMillis() - txnStartTime;
                performanceTracker.trackTransaction(transactionId, txnDuration, "COMPLETED");
                auditLogger.logSaleCreated(sale.getId(), customer.getId(), customer.getName(),
                                totalAmount.doubleValue());
                logger.info("TRANSACTION_SUMMARY | txnId={} | saleId={} | customer={} | amount={} | duration={}ms",
                                transactionId, sale.getId(), customer.getName(), totalAmount, txnDuration);

                MDC.remove("transactionId");
                return toDTO(sale);
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

        @Transactional(readOnly = true)
        public Page<SaleDTO> getAllSales(Pageable pageable, String fromDate, String toDate, Long customerId,
                        Long variantId, Double minAmount, Double maxAmount, String referenceNumber) {
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
                                                referenceNumber,
                                                pageable)
                                .map(this::toDTO);
        }

        public Page<SaleDTO> getSalesByCustomer(Long customerId, Pageable pageable) {
                logger.debug("Fetching sales for customer: {} with pagination", customerId);
                Customer customer = customerRepository.findById(customerId)
                                .orElseThrow(() -> {
                                        logger.error("Customer not found with id: {}", customerId);
                                        return new ResourceNotFoundException(
                                                        "Customer not found with id: " + customerId);
                                });
                return saleRepository.findByCustomer(customer, pageable)
                                .map(this::toDTO);
        }

        @Transactional(readOnly = true)
        public Page<SaleDTO> getSalesByDateRange(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
                logger.debug("Fetching sales between {} and {}", fromDate, toDate);
                return saleRepository.findByDateRange(fromDate, toDate, pageable)
                                .map(this::toDTO);
        }

        private SaleDTO toDTO(Sale sale) {
                List<SaleItemDTO> items = sale.getSaleItems().stream()
                                .map(item -> new SaleItemDTO(
                                                item.getId(),
                                                item.getVariant().getId(),
                                                item.getVariant().getName(),
                                                item.getQtyIssued(),
                                                item.getQtyEmptyReceived(),
                                                item.getBasePrice(),
                                                item.getDiscount(),
                                                item.getFinalPrice()))
                                .collect(Collectors.toList());

                String bankAccountName = null;
                Long bankAccountId = null;
                if (sale.getBankAccount() != null) {
                        bankAccountId = sale.getBankAccount().getId();
                        bankAccountName = sale.getBankAccount().getBankName() + " - " +
                                        sale.getBankAccount().getAccountNumber();
                }

                return new SaleDTO(
                                sale.getId(),
                                sale.getReferenceNumber(),
                                sale.getCustomer().getId(),
                                sale.getCustomer().getName(),
                                sale.getSaleDate(),
                                sale.getTotalAmount(),
                                sale.getPaymentMode() != null ? sale.getPaymentMode() : "Cash",
                                bankAccountId,
                                bankAccountName,
                                items);
        }
}
