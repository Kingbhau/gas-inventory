package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "sale", indexes = {
        @Index(name = "idx_sale_customer_id", columnList = "customer_id"),
        @Index(name = "idx_sale_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_sale_sale_date", columnList = "saleDate")
})
public class Sale extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotNull(message = "Warehouse is required.")
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @NotNull(message = "Customer is required.")
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Sale date is required.")
    @PastOrPresent(message = "Sale date cannot be in the future.")
    @Column(nullable = false)
    private LocalDate saleDate;

    @NotNull(message = "Total amount is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount cannot be negative.")
    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> saleItems = new ArrayList<>();

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

    @Override
    public String toString() {
        return "Sale{" +
                "id=" + id +
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
