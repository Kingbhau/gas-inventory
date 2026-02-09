package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cylinder_variant", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) }, indexes = {
        @Index(name = "idx_variant_name", columnList = "name"),
        @Index(name = "idx_variant_active", columnList = "active")
})
public class CylinderVariant extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Variant name is required.")
    @Size(max = 100, message = "Variant name must be at most 100 characters.")
    @Column(nullable = false, unique = true)
    private String name;

    @NotNull(message = "Weight is required.")
    @DecimalMin(value = "0.1", inclusive = true, message = "Weight must be positive.")
    @Column(nullable = false)
    private Double weightKg;

    @NotNull(message = "Active status is required.")
    @Column(nullable = false)
    private Boolean active = true;

    @DecimalMin(value = "0.0", inclusive = true, message = "Base price must be non-negative.")
    @Column(nullable = true)
    private java.math.BigDecimal basePrice;

    public CylinderVariant() {
    }

    public CylinderVariant(String name, Double weightKg) {
        this.name = name;
        this.weightKg = weightKg;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Double weightKg) {
        this.weightKg = weightKg;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public java.math.BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(java.math.BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("variant-inventoryStocks")
    private List<InventoryStock> inventoryStocks = new ArrayList<>();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("variant-customerPrices")
    private List<CustomerVariantPrice> customerPrices = new ArrayList<>();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("variant-monthlyPrices")
    private List<MonthlyPrice> monthlyPrices = new ArrayList<>();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("variant-saleItems")
    private List<SaleItem> saleItems = new ArrayList<>();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("variant-ledgers")
    private List<CustomerCylinderLedger> ledgerEntries = new ArrayList<>();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("variant-supplierTransactions")
    private List<SupplierTransaction> supplierTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference("variant-transfers")
    private List<WarehouseTransfer> transfers = new ArrayList<>();

    public List<InventoryStock> getInventoryStocks() {
        return inventoryStocks;
    }

    public void setInventoryStocks(List<InventoryStock> inventoryStocks) {
        this.inventoryStocks = inventoryStocks;
    }

    public List<CustomerVariantPrice> getCustomerPrices() {
        return customerPrices;
    }

    public void setCustomerPrices(List<CustomerVariantPrice> customerPrices) {
        this.customerPrices = customerPrices;
    }

    public List<MonthlyPrice> getMonthlyPrices() {
        return monthlyPrices;
    }

    public void setMonthlyPrices(List<MonthlyPrice> monthlyPrices) {
        this.monthlyPrices = monthlyPrices;
    }

    public List<SaleItem> getSaleItems() {
        return saleItems;
    }

    public void setSaleItems(List<SaleItem> saleItems) {
        this.saleItems = saleItems;
    }

    public List<CustomerCylinderLedger> getLedgerEntries() {
        return ledgerEntries;
    }

    public void setLedgerEntries(List<CustomerCylinderLedger> ledgerEntries) {
        this.ledgerEntries = ledgerEntries;
    }

    public List<SupplierTransaction> getSupplierTransactions() {
        return supplierTransactions;
    }

    public void setSupplierTransactions(List<SupplierTransaction> supplierTransactions) {
        this.supplierTransactions = supplierTransactions;
    }

    public List<WarehouseTransfer> getTransfers() {
        return transfers;
    }

    public void setTransfers(List<WarehouseTransfer> transfers) {
        this.transfers = transfers;
    }
}
