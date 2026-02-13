package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "sale", indexes = {
        // Performance indexes for sales lookups
        @Index(name = "idx_sale_date", columnList = "sale_date"),
        @Index(name = "idx_sale_customer_date", columnList = "customer_id, sale_date"),
        @Index(name = "idx_sale_warehouse_date", columnList = "warehouse_id, sale_date"),

        // Legacy indexes
        @Index(name = "idx_sale_customer_id", columnList = "customer_id"),
        @Index(name = "idx_sale_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_sale_reference_number", columnList = "reference_number", unique = true)
})
public class Sale extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "reference_number", unique = true, nullable = false, length = 50)
    @Pattern(regexp = "^SO-[A-Z0-9]+-\\d{6}-\\d{6}$", message = "Reference must match format: SO-WAREHOUSE-YYYYMM-SEQUENCE")
    private String referenceNumber;

    @NotNull(message = "Warehouse is required.")
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    @JsonBackReference("warehouse-sales")
    private Warehouse warehouse;

    @NotNull(message = "Customer is required.")
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonBackReference("customer-sales")
    private Customer customer;

    @NotNull(message = "Sale date is required.")
    @PastOrPresent(message = "Sale date cannot be in the future.")
    @Column(nullable = false)
    private LocalDate saleDate;

    @NotNull(message = "Total amount is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount cannot be negative.")
    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 50)
    private String paymentMode;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "bank_account_id")
    @JsonBackReference("bankAccount-sales")
    private BankAccount bankAccount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> saleItems = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("sale-ledgers")
    private List<CustomerCylinderLedger> ledgers = new ArrayList<>();

    public Sale() {
    }

    public Sale(Warehouse warehouse, Customer customer, LocalDate saleDate, BigDecimal totalAmount) {
        this.warehouse = Objects.requireNonNull(warehouse, "Warehouse cannot be null");
        this.customer = Objects.requireNonNull(customer, "Customer cannot be null");
        this.saleDate = Objects.requireNonNull(saleDate, "Sale date cannot be null");
        this.totalAmount = Objects.requireNonNull(totalAmount, "Total amount cannot be null");

        if (totalAmount.signum() < 0) {
            throw new IllegalArgumentException("Total amount cannot be negative");
        }

        this.createdAt = LocalDateTime.now();
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

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SaleItem> getSaleItems() {
        return saleItems;
    }

    public void setSaleItems(List<SaleItem> saleItems) {
        this.saleItems = saleItems;
    }

    public List<CustomerCylinderLedger> getLedgers() {
        return ledgers;
    }

    public void setLedgers(List<CustomerCylinderLedger> ledgers) {
        this.ledgers = ledgers;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public BankAccount getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(BankAccount bankAccount) {
        this.bankAccount = bankAccount;
    }

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
                ", referenceNumber='" + referenceNumber + '\'' +
                ", warehouse=" + (warehouse != null ? warehouse.getName() : "null") +
                ", customer=" + (customer != null ? customer.getName() : "null") +
                ", saleDate=" + saleDate +
                ", totalAmount=" + totalAmount +
                ", version=" + version +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Sale sale = (Sale) o;
        return Objects.equals(id, sale.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

