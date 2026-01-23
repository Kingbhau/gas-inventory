package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.DecimalMin;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "customer_cylinder_ledger", indexes = {
        // Performance indexes for due payment and transaction queries
        @Index(name = "idx_ccl_customer_trans_date", columnList = "customer_id, transaction_date"),
        @Index(name = "idx_ccl_warehouse_date", columnList = "warehouse_id, transaction_date"),
        @Index(name = "idx_ccl_transaction_date", columnList = "transaction_date"),
        @Index(name = "idx_ccl_ref_type_date", columnList = "ref_type, transaction_date"),
        @Index(name = "idx_ccl_customer_variant", columnList = "customer_id, variant_id"),
        
        // Legacy indexes
        @Index(name = "idx_ledger_customer_id", columnList = "customer_id"),
        @Index(name = "idx_ledger_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_ledger_variant_id", columnList = "variant_id"),
        @Index(name = "idx_ledger_customer_warehouse", columnList = "customer_id, warehouse_id"),
        @Index(name = "idx_customer_warehouse_variant", columnList = "customer_id, warehouse_id, variant_id")
})
public class CustomerCylinderLedger extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotNull(message = "Customer is required.")
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference("customer-ledgers")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = true)
    @JsonBackReference("warehouse-customerCylinderLedgers")
    private Warehouse warehouse;
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = true)
    @JsonBackReference("variant-ledgers")
    private CylinderVariant variant;

    @NotNull(message = "Transaction date is required.")
    @PastOrPresent(message = "Transaction date cannot be in the future.")
    @Column(nullable = false)
    private LocalDate transactionDate;

    @NotNull(message = "Reference type is required.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType refType;

    // Reference ID is only required for transactions that reference another entity
    // (e.g., SALE, PURCHASE)
    @Column(nullable = true)
    private Long refId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = true)
    @JsonBackReference("sale-ledgers")
    private Sale sale; // Direct reference to Sale when refType = SALE

    @NotNull(message = "Filled out is required.")
    @Min(value = 0, message = "Filled out cannot be negative.")
    @Column(nullable = false)
    private Long filledOut;

    @NotNull(message = "Empty in is required.")
    @Min(value = 0, message = "Empty in cannot be negative.")
    @Column(nullable = false)
    private Long emptyIn;

    @NotNull(message = "Balance is required.")
    @Min(value = 0, message = "Balance cannot be negative.")
    @Column(nullable = false)
    private Long balance;

    @DecimalMin(value = "0.0", message = "Total amount must be non-negative.")
    @Column(nullable = true)
    private BigDecimal totalAmount;

    @DecimalMin(value = "0.0", message = "Amount received must be non-negative.")
    @Column(nullable = true)
    private BigDecimal amountReceived;

    @DecimalMin(value = "0.0", message = "Due amount must be non-negative.")
    @Column(nullable = true)
    private BigDecimal dueAmount;

    @Column(nullable = true, length = 50)
    private String paymentMode;

    @Column(name = "transaction_reference", nullable = true, length = 50, unique = true)
    private String transactionReference; // Auto-generated for EMPTY_RETURN transactions

    @Column(nullable = true, length = 1500)
    private String updateReason; // Optional reason for why the ledger entry was updated (includes changes
                                 // summary + user note)

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "bank_account_id", nullable = true)
    @JsonBackReference("bankAccount-ledgers")
    private BankAccount bankAccount;

    public CustomerCylinderLedger() {
    }

    public CustomerCylinderLedger(Customer customer, Warehouse warehouse, CylinderVariant variant,
            LocalDate transactionDate, TransactionType refType, Long refId,
            Long filledOut, Long emptyIn, Long balance) {
        this.customer = Objects.requireNonNull(customer, "Customer cannot be null");
        this.warehouse = warehouse; // Can be null for INITIAL_STOCK transactions
        this.variant = Objects.requireNonNull(variant, "Variant cannot be null");
        this.transactionDate = transactionDate;
        this.refType = refType;
        this.refId = refId;
        this.sale = null; // Can be set separately if needed
        this.filledOut = filledOut;
        this.emptyIn = emptyIn;
        this.balance = balance;
    }

    public enum TransactionType {
        INITIAL_STOCK, SALE, EMPTY_RETURN, TRANSFER, PAYMENT
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public CylinderVariant getVariant() {
        return variant;
    }

    public void setVariant(CylinderVariant variant) {
        this.variant = variant;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public TransactionType getRefType() {
        return refType;
    }

    public void setRefType(TransactionType refType) {
        this.refType = refType;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
    }

    public Long getFilledOut() {
        return filledOut;
    }

    public void setFilledOut(Long filledOut) {
        this.filledOut = filledOut;
    }

    public Long getEmptyIn() {
        return emptyIn;
    }

    public void setEmptyIn(Long emptyIn) {
        this.emptyIn = emptyIn;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAmountReceived() {
        return amountReceived;
    }

    public void setAmountReceived(BigDecimal amountReceived) {
        this.amountReceived = amountReceived;
    }

    public BigDecimal getDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(BigDecimal dueAmount) {
        this.dueAmount = dueAmount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public String getUpdateReason() {
        return updateReason;
    }

    public void setUpdateReason(String updateReason) {
        this.updateReason = updateReason;
    }
}
