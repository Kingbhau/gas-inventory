
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
}
