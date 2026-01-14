
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        private final MonthlyPriceRepository monthlyPriceRepository;
        private final InventoryStockService inventoryStockService;
        private final CustomerCylinderLedgerService ledgerService;
        private final AuditLogger auditLogger;
        private final PerformanceTracker performanceTracker;

        public SaleService(SaleRepository saleRepository,
                        SaleItemRepository saleItemRepository,
                        CustomerRepository customerRepository,
                        CylinderVariantRepository variantRepository,
                        MonthlyPriceRepository monthlyPriceRepository,
                        InventoryStockService inventoryStockService,
                        CustomerCylinderLedgerService ledgerService,
                        AuditLogger auditLogger,
                        PerformanceTracker performanceTracker) {
                this.saleRepository = saleRepository;
                this.saleItemRepository = saleItemRepository;
                this.customerRepository = customerRepository;
                this.variantRepository = variantRepository;
                this.monthlyPriceRepository = monthlyPriceRepository;
                this.inventoryStockService = inventoryStockService;
                this.ledgerService = ledgerService;
                this.auditLogger = auditLogger;
                this.performanceTracker = performanceTracker;
        }

        public List<SaleDTO> getRecentSales() {
                Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5,
                                org.springframework.data.domain.Sort
                                                .by(org.springframework.data.domain.Sort.Direction.DESC, "saleDate"));
                return saleRepository.findAll(pageable)
                                .map(this::toDTO)
                                .getContent();
        }

        public SaleSummaryDTO getSalesSummary(String fromDate, String toDate, Long customerId,
                        Long variantId, Double minAmount, Double maxAmount) {
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
                                minAmount, maxAmount, Pageable.unpaged()).getContent();
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

                        // Check inventory sufficiency with lock
                        InventoryStock inventoryStock = inventoryStockService.getStockEntityWithLock(variant.getId());
                        logger.debug("Variant: {}, Available filled: {}, Requested: {}",
                                        variant.getName(), inventoryStock.getFilledQty(), itemRequest.getQtyIssued());

                        if (inventoryStock.getFilledQty() < itemRequest.getQtyIssued()) {
                                logger.error("Insufficient inventory for variant: {}. Available: {}, Requested: {}",
                                                variant.getName(), inventoryStock.getFilledQty(),
                                                itemRequest.getQtyIssued());
                                throw new InvalidOperationException(
                                                "Insufficient inventory for variant: " + variant.getName() +
                                                                ". Available: " + inventoryStock.getFilledQty() +
                                                                ", Requested: " + itemRequest.getQtyIssued());
                        }

                        // Get current month price
                        logger.debug("Fetching monthly price for variant: {}", variant.getName());
                        MonthlyPrice monthlyPrice = monthlyPriceRepository
                                        .findByVariantAndMonthYear(variant, LocalDate.now().withDayOfMonth(1))
                                        .orElseThrow(() -> {
                                                logger.error("Monthly price not found for variant: {}",
                                                                variant.getName());
                                                return new ResourceNotFoundException(
                                                                "Monthly price not found for variant");
                                        });

                        // Calculate final price
                        BigDecimal basePrice = monthlyPrice.getBasePrice();
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

                        // Create sale item
                        SaleItem saleItem = new SaleItem(null, variant, itemRequest.getQtyIssued(),
                                        itemRequest.getQtyEmptyReceived(), basePrice, discountAmount, finalPrice);
                        saleItems.add(saleItem);
                }

                logger.info("Sale validation complete - Total amount: {}, Items: {}", totalAmount, saleItems.size());

                // Edge-to-edge: Disallow zero-amount sales
                if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                        logger.error("Attempt to create sale with zero or negative total amount");
                        throw new InvalidOperationException("Sale total amount must be greater than zero.");
                }

                // Create sale FIRST so we have a sale ID for ledger references
                Sale sale = new Sale(customer, LocalDate.now(), totalAmount);
                sale = saleRepository.save(sale);
                logger.info("Sale created with id: {} for customer: {} - Total: {}",
                                sale.getId(), customer.getName(), totalAmount);

                // Now attach sale items and persist
                for (int i = 0; i < saleItems.size(); i++) {
                        saleItems.get(i).setSale(sale);
                        saleItemRepository.save(saleItems.get(i));

                        CreateSaleRequestDTO.SaleItemRequestDTO itemRequest = request.getItems().get(i);
                        CylinderVariant variant = saleItems.get(i).getVariant();

                        logger.debug("Processing sale item - Variant: {}, Qty: {}", variant.getName(),
                                        itemRequest.getQtyIssued());

                        // Update inventory (decrement filled, increment empty)
                        inventoryStockService.decrementFilledQtyWithCheck(variant.getId(), itemRequest.getQtyIssued());
                        inventoryStockService.incrementEmptyQty(variant.getId(), itemRequest.getQtyEmptyReceived());
                        logger.debug("Inventory updated - Variant: {}, Filled qty decrement: {}, Empty qty increment: {}",
                                        variant.getName(), itemRequest.getQtyIssued(),
                                        itemRequest.getQtyEmptyReceived());

                        // Create ledger entry
                        ledgerService.createLedgerEntry(
                                        customer.getId(),
                                        variant.getId(),
                                        sale.getSaleDate(),
                                        "SALE",
                                        sale.getId(),
                                        itemRequest.getQtyIssued(),
                                        itemRequest.getQtyEmptyReceived());
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

        public SaleDTO getSaleById(Long id) {
                logger.debug("Fetching sale with id: {}", id);
                Sale sale = saleRepository.findById(id)
                                .orElseThrow(() -> {
                                        logger.error("Sale not found with id: {}", id);
                                        return new ResourceNotFoundException("Sale not found with id: " + id);
                                });
                return toDTO(sale);
        }

        public Page<SaleDTO> getAllSales(Pageable pageable, String fromDate, String toDate, Long customerId,
                        Long variantId, Double minAmount, Double maxAmount) {
                logger.debug("Fetching all sales with filters: page={}, size={}, customerId={}, variantId={}, minAmount={}, maxAmount={}",
                                pageable.getPageNumber(), pageable.getPageSize(), customerId, variantId, minAmount,
                                maxAmount);
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

                return new SaleDTO(
                                sale.getId(),
                                sale.getCustomer().getId(),
                                sale.getCustomer().getName(),
                                sale.getSaleDate(),
                                sale.getTotalAmount(),
                                items);
        }
}
