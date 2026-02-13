package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "warehouse", indexes = {
        @Index(name = "idx_warehouse_name", columnList = "name", unique = true),
        @Index(name = "idx_warehouse_status", columnList = "status")
})
public class Warehouse extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotBlank(message = "Warehouse name is required.")
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, updatable = false, length = 20)
    private String code; // Auto-generated: WH001, WH002, etc. (Read-only)

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    @JsonBackReference("business-warehouses")
    private BusinessInfo business;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("warehouse-inventoryStocks")
    private List<InventoryStock> inventoryStocks;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("warehouse-sales")
    private List<Sale> sales;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("warehouse-saleItems")
    private List<SaleItem> saleItems;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("warehouse-supplierTransactions")
    private List<SupplierTransaction> supplierTransactions;

    @OneToMany(mappedBy = "fromWarehouse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("warehouse-fromTransfers")
    private List<WarehouseTransfer> fromWarehouseTransfers;

    @OneToMany(mappedBy = "toWarehouse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("warehouse-toTransfers")
    private List<WarehouseTransfer> toWarehouseTransfers;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("warehouse-customerCylinderLedgers")
    private List<CustomerCylinderLedger> customerCylinderLedgers;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Warehouse() {
    }

    public Warehouse(String name) {
        this.name = Objects.requireNonNull(name, "Warehouse name cannot be null");
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Warehouse(String name, String status) {
        this.name = Objects.requireNonNull(name, "Warehouse name cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BusinessInfo getBusiness() {
        return business;
    }

    public void setBusiness(BusinessInfo business) {
        this.business = business;
    }

    public List<InventoryStock> getInventoryStocks() {
        return inventoryStocks;
    }

    public void setInventoryStocks(List<InventoryStock> inventoryStocks) {
        this.inventoryStocks = inventoryStocks;
    }

    public List<Sale> getSales() {
        return sales;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    public List<SaleItem> getSaleItems() {
        return saleItems;
    }

    public void setSaleItems(List<SaleItem> saleItems) {
        this.saleItems = saleItems;
    }

    public List<SupplierTransaction> getSupplierTransactions() {
        return supplierTransactions;
    }

    public void setSupplierTransactions(List<SupplierTransaction> supplierTransactions) {
        this.supplierTransactions = supplierTransactions;
    }

    public List<WarehouseTransfer> getFromWarehouseTransfers() {
        return fromWarehouseTransfers;
    }

    public void setFromWarehouseTransfers(List<WarehouseTransfer> fromWarehouseTransfers) {
        this.fromWarehouseTransfers = fromWarehouseTransfers;
    }

    public List<WarehouseTransfer> getToWarehouseTransfers() {
        return toWarehouseTransfers;
    }

    public void setToWarehouseTransfers(List<WarehouseTransfer> toWarehouseTransfers) {
        this.toWarehouseTransfers = toWarehouseTransfers;
    }

    public List<CustomerCylinderLedger> getCustomerCylinderLedgers() {
        return customerCylinderLedgers;
    }

    public void setCustomerCylinderLedgers(List<CustomerCylinderLedger> customerCylinderLedgers) {
        this.customerCylinderLedgers = customerCylinderLedgers;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "Warehouse{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", version=" + version +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Warehouse warehouse = (Warehouse) o;
        return Objects.equals(id, warehouse.id) &&
                Objects.equals(name, warehouse.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}

