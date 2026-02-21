
package com.gasagency.controller;

import com.gasagency.dto.response.CustomerBalanceDTO;
import com.gasagency.dto.response.CustomerCylinderLedgerDTO;
import com.gasagency.dto.response.CustomerDueAmountDTO;
import com.gasagency.dto.request.CustomerDueAmountsRequestDTO;
import com.gasagency.dto.response.CustomerLedgerSummaryDTO;
import com.gasagency.dto.request.LedgerUpdateRequestDTO;
import com.gasagency.dto.request.PaymentRequestDTO;
import com.gasagency.dto.response.PaymentsSummaryDTO;
import com.gasagency.dto.response.SimpleStatusDTO;
import com.gasagency.dto.request.EmptyReturnRequestDTO;
import com.gasagency.dto.response.PagedResponseDTO;
import com.gasagency.service.CustomerCylinderLedgerService;
import com.gasagency.repository.PaymentModeRepository;
import com.gasagency.util.ApiResponse;
import com.gasagency.util.ApiResponseUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ledger")
public class CustomerCylinderLedgerController {
    private final PaymentModeRepository paymentModeRepository;

    @GetMapping("/pending-summary")
    public ResponseEntity<ApiResponse<List<CustomerCylinderLedgerDTO>>> getAllPendingBalances() {
        return ResponseEntity.ok(ApiResponseUtil.success("Pending balances retrieved successfully",
                service.getAllPendingBalances()));
    }

    private final CustomerCylinderLedgerService service;

    public CustomerCylinderLedgerController(CustomerCylinderLedgerService service,
            PaymentModeRepository paymentModeRepository) {
        this.service = service;
        this.paymentModeRepository = paymentModeRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerCylinderLedgerDTO>> getLedgerEntry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseUtil.success("Ledger entry retrieved successfully",
                service.getLedgerEntryById(id)));
    }

    // Batch endpoint: Get balances for a page of customers (all variants)
    @GetMapping("/customer-balances")
    public ResponseEntity<ApiResponse<List<CustomerBalanceDTO>>> getCustomerBalances(
            @RequestParam int page,
            @RequestParam int size) {
        return ResponseEntity.ok(ApiResponseUtil.success("Customer balances retrieved successfully",
                service.getCustomerBalancesForPage(page, size)));
    }

    @PostMapping("/customer-balances/by-ids")
    public ResponseEntity<ApiResponse<List<CustomerBalanceDTO>>> getCustomerBalancesByIds(
            @RequestBody CustomerDueAmountsRequestDTO payload) {
        List<Long> rawIds = payload != null ? payload.getCustomerIds() : null;
        List<Long> customerIds = null;
        if (rawIds != null) {
            customerIds = rawIds.stream()
                    .filter(Objects::nonNull)
                    .map(Long::longValue)
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponseUtil.success("Customer balances retrieved successfully",
                service.getCustomerBalancesForCustomers(customerIds)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerCylinderLedgerDTO>>> getAllLedger(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(ApiResponseUtil.success("Ledger entries retrieved successfully",
                service.getAllLedger(pageable)));
    }

    // Endpoint: Record empty cylinder return (without sale)
    @PostMapping("/empty-return")
    public ResponseEntity<ApiResponse<CustomerCylinderLedgerDTO>> recordEmptyReturn(
            @RequestBody EmptyReturnRequestDTO request) {
        String paymentModeName = request.getPaymentMode() != null ? request.getPaymentMode().trim() : null;
        Boolean requiresBankAccount = null;
        if (paymentModeName != null && !paymentModeName.isEmpty()) {
            requiresBankAccount = paymentModeRepository.findByName(paymentModeName)
                    .map(pm -> Boolean.TRUE.equals(pm.getIsBankAccountRequired()))
                    .orElseThrow(() -> new com.gasagency.exception.InvalidOperationException(
                            "Invalid payment mode: " + paymentModeName));
        }

        // Validate amountReceived does not exceed customer's due amount
        if (request.getAmountReceived() != null && request.getAmountReceived().compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal customerDueAmount = service.getCustomerPreviousDue(request.getCustomerId());
            if (request.getAmountReceived().compareTo(customerDueAmount) > 0) {
                throw new com.gasagency.exception.InvalidOperationException(
                        "Amount received (" + request.getAmountReceived().setScale(2, java.math.RoundingMode.HALF_UP) +
                                ") cannot exceed customer due amount ("
                                + customerDueAmount.setScale(2, java.math.RoundingMode.HALF_UP) + ")");
            }

            // Validate paymentMode is provided when amountReceived > 0
            if (paymentModeName == null || paymentModeName.isEmpty()) {
                throw new com.gasagency.exception.InvalidOperationException(
                        "Payment mode is required when amount is received");
            }

            // Validate bankAccountId is provided when payment mode requires it
            if (Boolean.TRUE.equals(requiresBankAccount) &&
                    (request.getBankAccountId() == null || request.getBankAccountId() <= 0)) {
                throw new com.gasagency.exception.InvalidOperationException(
                        "Bank account is required for payment mode: " + request.getPaymentMode());
            }
        }

        // For empty returns, set refId to 0L (not null) to satisfy DB constraint
        // Create ledger entry with amount received
        CustomerCylinderLedgerDTO dto = service.createLedgerEntry(
                request.getCustomerId(),
                request.getWarehouseId(),
                request.getVariantId(),
                request.getTransactionDate(),
                "EMPTY_RETURN",
                0L,
                0L,
                request.getEmptyIn(),
                java.math.BigDecimal.ZERO,
                request.getAmountReceived() != null ? request.getAmountReceived() : java.math.BigDecimal.ZERO);

        // If payment mode is provided, update the ledger entry
        if (paymentModeName != null && !paymentModeName.isEmpty()) {
            service.updatePaymentMode(dto.getId(), paymentModeName);
        }

        // Record bank account deposit if payment is via bank account
        if (Boolean.TRUE.equals(requiresBankAccount) && request.getBankAccountId() != null) {
            try {
                service.recordBankAccountDeposit(
                        request.getBankAccountId(),
                        request.getAmountReceived() != null ? request.getAmountReceived() : java.math.BigDecimal.ZERO,
                        dto.getId(),
                        "Empty cylinder return refund");
            } catch (Exception e) {
                throw new RuntimeException("Failed to record bank account deposit: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(ApiResponseUtil.success("Empty return recorded successfully", dto));
    }

    @PostMapping("/customer-due-amounts")
    public ResponseEntity<ApiResponse<List<CustomerDueAmountDTO>>> getCustomerDueAmounts(
            @RequestBody CustomerDueAmountsRequestDTO payload) {
        List<Long> rawIds = payload != null ? payload.getCustomerIds() : null;
        List<Long> customerIds = null;
        if (rawIds != null) {
            customerIds = rawIds.stream()
                    .filter(Objects::nonNull)
                    .map(Long::longValue)
                    .collect(Collectors.toList());
        }
        List<CustomerDueAmountDTO> response = service.getLatestDueAmountsForCustomers(customerIds).entrySet()
                .stream()
                .map(entry -> {
                    CustomerDueAmountDTO dto = new CustomerDueAmountDTO();
                    dto.setCustomerId(entry.getKey());
                    dto.setDueAmount(entry.getValue());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponseUtil.success("Customer due amounts retrieved successfully", response));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<CustomerCylinderLedgerDTO>>> getLedgerByCustomer(
            @PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponseUtil.success("Customer ledger retrieved successfully",
                service.getLedgerByCustomer(customerId)));
    }

    @GetMapping("/customer/{customerId}/paginated")
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerCylinderLedgerDTO>>> getLedgerByCustomerPaginated(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        return ResponseEntity.ok(ApiResponseUtil.success("Customer ledger retrieved successfully",
                service.getLedgerByCustomer(customerId, pageable)));
    }

    // Endpoint: Get all stock movements (ledger entries) for inventory movement
    // history
    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<List<CustomerCylinderLedgerDTO>>> getAllMovements() {
        return ResponseEntity.ok(ApiResponseUtil.success("Stock movements retrieved successfully",
                service.getAllMovements()));
    }

    // Paginated movements (optionally include transfers)
    @GetMapping("/movements/paged")
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerCylinderLedgerDTO>>> getAllMovementsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) String refType,
            @RequestParam(defaultValue = "true") boolean includeTransfers) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        Page<CustomerCylinderLedgerDTO> movements = includeTransfers
                ? service.getAllMovementsMerged(pageable, variantId, refType)
                : service.getAllMovements(pageable, variantId, refType);
        return ResponseEntity.ok(ApiResponseUtil.success("Stock movements retrieved successfully", movements));
    }

    // Endpoint: Get stock movements for a specific warehouse
    @GetMapping("/movements/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<List<CustomerCylinderLedgerDTO>>> getMovementsByWarehouse(
            @PathVariable Long warehouseId) {
        return ResponseEntity.ok(ApiResponseUtil.success("Warehouse movements retrieved successfully",
                service.getMovementsByWarehouse(warehouseId)));
    }

    // Paginated warehouse movements (optionally include transfers)
    @GetMapping("/movements/warehouse/{warehouseId}/paged")
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerCylinderLedgerDTO>>> getMovementsByWarehousePaged(
            @PathVariable Long warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) String refType,
            @RequestParam(defaultValue = "true") boolean includeTransfers) {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy).and(Sort.by("id").descending()));
        Page<CustomerCylinderLedgerDTO> movements = includeTransfers
                ? service.getMovementsByWarehouseMerged(warehouseId, pageable, variantId, refType)
                : service.getMovementsByWarehouse(warehouseId, pageable, variantId, refType);
        return ResponseEntity.ok(ApiResponseUtil.success("Warehouse movements retrieved successfully", movements));
    }

    @GetMapping("/customer/{customerId}/variant/{variantId}")
    public ResponseEntity<ApiResponse<List<CustomerCylinderLedgerDTO>>> getLedgerByCustomerAndVariant(
            @PathVariable Long customerId, @PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponseUtil.success("Customer ledger retrieved successfully",
                service.getLedgerByCustomerAndVariant(customerId, variantId)));
    }

    @GetMapping("/variant/{variantId}")
    public ResponseEntity<ApiResponse<List<CustomerCylinderLedgerDTO>>> getLedgerByVariant(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponseUtil.success("Variant ledger retrieved successfully",
                service.getLedgerByVariant(variantId)));
    }

    @GetMapping("/customer/{customerId}/variant/{variantId}/balance")
    public ResponseEntity<ApiResponse<Long>> getBalance(@PathVariable Long customerId, @PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponseUtil.success("Customer balance retrieved successfully",
                service.getCurrentBalance(customerId, variantId)));
    }

    // Record a payment transaction
    @PostMapping("/payment")
    public ResponseEntity<ApiResponse<CustomerCylinderLedgerDTO>> recordPayment(
            @RequestBody PaymentRequestDTO paymentRequest) {
        return ResponseEntity.ok(ApiResponseUtil.success("Payment recorded successfully",
                service.recordPayment(paymentRequest)));
    }

    // Get complete summary for a customer (across all ledger entries)
    @GetMapping("/customer/{customerId}/summary")
    public ResponseEntity<ApiResponse<CustomerLedgerSummaryDTO>> getCustomerSummary(@PathVariable Long customerId) {
        return ResponseEntity.ok(ApiResponseUtil.success("Customer ledger summary retrieved successfully",
                service.getCustomerLedgerSummary(customerId)));
    }

    // Get empty returns with filtering
    @GetMapping("/empty-returns")
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerCylinderLedgerDTO>>> getEmptyReturns(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            Authentication authentication) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(direction, sortBy).and(Sort.by(Sort.Direction.DESC, "id"))
        );

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

        String effectiveCreatedBy = resolveCreatedBy(authentication, createdBy);
        return ResponseEntity.ok(ApiResponseUtil.success("Empty returns retrieved successfully",
                service.getEmptyReturns(from, to, customerId, variantId, effectiveCreatedBy, pageable)));
    }

    // Get payment history with filtering
    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<PagedResponseDTO<CustomerCylinderLedgerDTO>>> getPayments(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            Authentication authentication) {
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
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD.");
        }

        String effectiveCreatedBy = resolveCreatedBy(authentication, createdBy);
        return ResponseEntity.ok(ApiResponseUtil.success("Payments retrieved successfully",
                service.getPayments(from, to, customerId, paymentMode, bankAccountId, effectiveCreatedBy, pageable)));
    }

    // Get payment summary for filters
    @GetMapping("/payments-summary")
    public ResponseEntity<ApiResponse<PaymentsSummaryDTO>> getPaymentsSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) Long bankAccountId,
            @RequestParam(required = false) String createdBy,
            Authentication authentication) {
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
            throw new IllegalArgumentException("Invalid date format. Use YYYY-MM-DD.");
        }

        String effectiveCreatedBy = resolveCreatedBy(authentication, createdBy);
        java.math.BigDecimal totalAmount = service.getPaymentsSummary(from, to, customerId, paymentMode, bankAccountId,
                effectiveCreatedBy);
        PaymentsSummaryDTO summary = new PaymentsSummaryDTO(totalAmount);
        return ResponseEntity.ok(ApiResponseUtil.success("Payments summary retrieved successfully", summary));
    }

    private String resolveCreatedBy(Authentication authentication, String requestedCreatedBy) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return requestedCreatedBy;
        }
        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_STAFF".equals(authority.getAuthority()));
        return isStaff ? authentication.getName() : requestedCreatedBy;
    }

    // Update a ledger entry with full chain recalculation
    // Validates that no due amounts go negative anywhere in the chain
    @PutMapping("/{ledgerId}")
    public ResponseEntity<ApiResponse<CustomerCylinderLedgerDTO>> updateLedgerEntry(
            @PathVariable Long ledgerId,
            @RequestBody LedgerUpdateRequestDTO updateData) {
        return ResponseEntity.ok(ApiResponseUtil.success("Ledger entry updated successfully",
                service.updateLedgerEntry(ledgerId, updateData)));
    }

    // Admin endpoint to repair/recalculate all balances with correct formula
    @PostMapping("/admin/repair-balances")
    public ResponseEntity<ApiResponse<SimpleStatusDTO>> repairAllBalances() {
        service.recalculateAllBalances();
        return ResponseEntity.ok(ApiResponseUtil.success("All balances have been recalculated",
                new SimpleStatusDTO("success")));
    }
}

