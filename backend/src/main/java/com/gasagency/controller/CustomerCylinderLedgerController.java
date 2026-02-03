
package com.gasagency.controller;

import com.gasagency.dto.CustomerCylinderLedgerDTO;
import com.gasagency.service.CustomerCylinderLedgerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gasagency.dto.CustomerBalanceDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ledger")
public class CustomerCylinderLedgerController {

    public static class EmptyReturnRequest {
        public Long customerId;
        public Long warehouseId;
        public Long variantId;
        public LocalDate transactionDate;
        public Long emptyIn;
        public java.math.BigDecimal amountReceived;
        public String paymentMode;
        public Long bankAccountId;

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public Long getWarehouseId() {
            return warehouseId;
        }

        public void setWarehouseId(Long warehouseId) {
            this.warehouseId = warehouseId;
        }

        public Long getVariantId() {
            return variantId;
        }

        public void setVariantId(Long variantId) {
            this.variantId = variantId;
        }

        public LocalDate getTransactionDate() {
            return transactionDate;
        }

        public void setTransactionDate(LocalDate transactionDate) {
            this.transactionDate = transactionDate;
        }

        public Long getEmptyIn() {
            return emptyIn;
        }

        public void setEmptyIn(Long emptyIn) {
            this.emptyIn = emptyIn;
        }

        public java.math.BigDecimal getAmountReceived() {
            return amountReceived;
        }

        public void setAmountReceived(java.math.BigDecimal amountReceived) {
            this.amountReceived = amountReceived;
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

    @GetMapping("/pending-summary")
    public ResponseEntity<List<CustomerCylinderLedgerDTO>> getAllPendingBalances() {
        return ResponseEntity.ok(service.getAllPendingBalances());
    }

    private final CustomerCylinderLedgerService service;

    public CustomerCylinderLedgerController(CustomerCylinderLedgerService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerCylinderLedgerDTO> getLedgerEntry(@PathVariable Long id) {
        return ResponseEntity.ok(service.getLedgerEntryById(id));
    }

    // Batch endpoint: Get balances for a page of customers (all variants)
    @GetMapping("/customer-balances")
    public ResponseEntity<List<CustomerBalanceDTO>> getCustomerBalances(
            @RequestParam int page,
            @RequestParam int size) {
        return ResponseEntity.ok(service.getCustomerBalancesForPage(page, size));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerCylinderLedgerDTO>> getAllLedger(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(service.getAllLedger(pageable));
    }

    // Endpoint: Record empty cylinder return (without sale)
    @PostMapping("/empty-return")
    public ResponseEntity<CustomerCylinderLedgerDTO> recordEmptyReturn(@RequestBody EmptyReturnRequest request) {
        // Validate amountReceived does not exceed customer's due amount
        if (request.amountReceived != null && request.amountReceived.compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal customerDueAmount = service.getCustomerPreviousDue(request.customerId);
            if (request.amountReceived.compareTo(customerDueAmount) > 0) {
                throw new com.gasagency.exception.InvalidOperationException(
                        "Amount received (" + request.amountReceived.setScale(2, java.math.RoundingMode.HALF_UP) +
                                ") cannot exceed customer due amount ("
                                + customerDueAmount.setScale(2, java.math.RoundingMode.HALF_UP) + ")");
            }

            // Validate paymentMode is provided when amountReceived > 0
            if (request.paymentMode == null || request.paymentMode.trim().isEmpty()) {
                throw new com.gasagency.exception.InvalidOperationException(
                        "Payment mode is required when amount is received");
            }

            // Validate bankAccountId is provided when payment mode is not CASH
            if (!request.paymentMode.equalsIgnoreCase("CASH") &&
                    (request.bankAccountId == null || request.bankAccountId <= 0)) {
                throw new com.gasagency.exception.InvalidOperationException(
                        "Bank account is required for payment mode: " + request.paymentMode);
            }
        }

        // For empty returns, set refId to 0L (not null) to satisfy DB constraint
        // Create ledger entry with amount received
        CustomerCylinderLedgerDTO dto = service.createLedgerEntry(
                request.customerId,
                request.warehouseId,
                request.variantId,
                request.transactionDate,
                "EMPTY_RETURN",
                0L,
                0L,
                request.emptyIn,
                java.math.BigDecimal.ZERO,
                request.amountReceived != null ? request.amountReceived : java.math.BigDecimal.ZERO);

        // If payment mode is provided, update the ledger entry
        if (request.paymentMode != null && !request.paymentMode.isEmpty()) {
            service.updatePaymentMode(dto.getId(), request.paymentMode);
        }

        // Record bank account deposit if payment is via bank account
        if (request.bankAccountId != null && request.paymentMode != null &&
                !request.paymentMode.equalsIgnoreCase("CASH")) {
            try {
                service.recordBankAccountDeposit(
                        request.bankAccountId,
                        request.amountReceived != null ? request.amountReceived : java.math.BigDecimal.ZERO,
                        dto.getId(),
                        "Empty cylinder return refund");
            } catch (Exception e) {
                throw new RuntimeException("Failed to record bank account deposit: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<CustomerCylinderLedgerDTO>> getLedgerByCustomer(@PathVariable Long customerId) {
        return ResponseEntity.ok(service.getLedgerByCustomer(customerId));
    }

    @GetMapping("/customer/{customerId}/paginated")
    public ResponseEntity<Page<CustomerCylinderLedgerDTO>> getLedgerByCustomerPaginated(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(service.getLedgerByCustomer(customerId, pageable));
    }

    // Endpoint: Get all stock movements (ledger entries) for inventory movement
    // history
    @GetMapping("/movements")
    public ResponseEntity<List<CustomerCylinderLedgerDTO>> getAllMovements() {
        return ResponseEntity.ok(service.getAllMovements());
    }

    // Endpoint: Get stock movements for a specific warehouse
    @GetMapping("/movements/warehouse/{warehouseId}")
    public ResponseEntity<List<CustomerCylinderLedgerDTO>> getMovementsByWarehouse(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(service.getMovementsByWarehouse(warehouseId));
    }

    @GetMapping("/customer/{customerId}/variant/{variantId}")
    public ResponseEntity<List<CustomerCylinderLedgerDTO>> getLedgerByCustomerAndVariant(
            @PathVariable Long customerId, @PathVariable Long variantId) {
        return ResponseEntity.ok(service.getLedgerByCustomerAndVariant(customerId, variantId));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<List<CustomerCylinderLedgerDTO>> getLedgerByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(service.getLedgerByVariant(variantId));
    }

    @GetMapping("/customer/{customerId}/variant/{variantId}/balance")
    public ResponseEntity<Long> getBalance(@PathVariable Long customerId, @PathVariable Long variantId) {
        return ResponseEntity.ok(service.getCurrentBalance(customerId, variantId));
    }

    // Record a payment transaction
    @PostMapping("/payment")
    public ResponseEntity<CustomerCylinderLedgerDTO> recordPayment(
            @RequestBody CustomerCylinderLedgerService.PaymentRequest paymentRequest) {
        return ResponseEntity.ok(service.recordPayment(paymentRequest));
    }

    // Get complete summary for a customer (across all ledger entries)
    @GetMapping("/customer/{customerId}/summary")
    public ResponseEntity<Map<String, Object>> getCustomerSummary(@PathVariable Long customerId) {
        return ResponseEntity.ok(service.getCustomerLedgerSummary(customerId));
    }

    // Get empty returns with filtering
    @GetMapping("/empty-returns")
    public ResponseEntity<Page<CustomerCylinderLedgerDTO>> getEmptyReturns(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        LocalDate from = null;
        LocalDate to = null;
        try {
            if (fromDate != null && !fromDate.isEmpty()) {
                from = LocalDate.parse(fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                to = LocalDate.parse(toDate);
            }
        } catch (Exception e) {
            // Invalid date format, continue without filtering
        }

        return ResponseEntity.ok(service.getEmptyReturns(from, to, customerId, variantId, pageable));
    }

    // Update a ledger entry with full chain recalculation
    // Validates that no due amounts go negative anywhere in the chain
    @PutMapping("/{ledgerId}")
    public ResponseEntity<CustomerCylinderLedgerDTO> updateLedgerEntry(
            @PathVariable Long ledgerId,
            @RequestBody Map<String, Object> updateData) {
        return ResponseEntity.ok(service.updateLedgerEntry(ledgerId, updateData));
    }

    // Admin endpoint to repair/recalculate all balances with correct formula
    @PostMapping("/admin/repair-balances")
    public ResponseEntity<Map<String, String>> repairAllBalances() {
        service.recalculateAllBalances();
        return ResponseEntity.ok(Map.of("status", "success", "message", "All balances have been recalculated"));
    }
}
