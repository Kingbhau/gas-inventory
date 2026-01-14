package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_stock", indexes = {
        @Index(name = "idx_stock_variant_id", columnList = "variant_id"),
        @Index(name = "idx_stock_last_updated", columnList = "lastUpdated")
})
public class InventoryStock extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotNull(message = "Variant is required.")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
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
        this.variant = variant;
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
}
