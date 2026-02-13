package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "inventory_stock", indexes = {
        @Index(name = "idx_is_warehouse_variant", columnList = "warehouse_id, variant_id"),
        @Index(name = "idx_is_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_is_variant", columnList = "variant_id"),
        @Index(name = "idx_stock_warehouse_variant", columnList = "warehouse_id, variant_id"),
        @Index(name = "idx_stock_last_updated", columnList = "last_updated")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "warehouse_id", "variant_id" }, name = "uq_warehouse_variant")
})
public class InventoryStock extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotNull(message = "Warehouse is required.")
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    @JsonBackReference("warehouse-inventoryStocks")
    private Warehouse warehouse;

    @NotNull(message = "Variant is required.")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonBackReference("variant-inventoryStocks")
    private CylinderVariant variant;

    @NotNull(message = "Filled quantity is required.")
    @Min(value = 0, message = "Filled quantity cannot be negative.")
    @Column(nullable = false)
    private Long filledQty = 0L;

    @NotNull(message = "Empty quantity is required.")
    @Min(value = 0, message = "Empty quantity cannot be negative.")
    @Column(nullable = false)
    private Long emptyQty = 0L;

    @NotNull(message = "Last updated is required.")
    @Column(nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public InventoryStock() {
    }

    public InventoryStock(CylinderVariant variant) {
        this.warehouse = null;
        this.variant = Objects.requireNonNull(variant, "Variant cannot be null");
        this.filledQty = 0L;
        this.emptyQty = 0L;
        this.lastUpdated = LocalDateTime.now();
    }

    public InventoryStock(Warehouse warehouse, CylinderVariant variant) {
        this.warehouse = Objects.requireNonNull(warehouse, "Warehouse cannot be null");
        this.variant = Objects.requireNonNull(variant, "Variant cannot be null");
        this.filledQty = 0L;
        this.emptyQty = 0L;
        this.lastUpdated = LocalDateTime.now();
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

    public CylinderVariant getVariant() {
        return variant;
    }

    public void setVariant(CylinderVariant variant) {
        this.variant = variant;
    }

    public Long getFilledQty() {
        return filledQty;
    }

    public void setFilledQty(Long filledQty) {
        this.filledQty = filledQty;
    }

    public Long getEmptyQty() {
        return emptyQty;
    }

    public void setEmptyQty(Long emptyQty) {
        this.emptyQty = emptyQty;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getTotalQty() {
        return filledQty + emptyQty;
    }

    @Override
    public String toString() {
        return "InventoryStock{" +
                "id=" + id +
                ", warehouse=" + (warehouse != null ? warehouse.getName() : "null") +
                ", variant=" + (variant != null ? variant.getName() : "null") +
                ", filledQty=" + filledQty +
                ", emptyQty=" + emptyQty +
                ", version=" + version +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        InventoryStock that = (InventoryStock) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(warehouse, that.warehouse) &&
                Objects.equals(variant, that.variant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, warehouse, variant);
    }
}

