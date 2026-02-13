package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "sale_item", indexes = {
        @Index(name = "idx_saleitem_sale_id", columnList = "sale_id"),
        @Index(name = "idx_saleitem_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_saleitem_variant_id", columnList = "variant_id")
})
public class SaleItem extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotNull(message = "Sale is required.")
    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false)
    @JsonBackReference("sale-items")
    private Sale sale;

    @NotNull(message = "Warehouse is required.")
    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    @JsonBackReference("warehouse-saleItems")
    private Warehouse warehouse;

    @NotNull(message = "Variant is required.")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonBackReference("variant-saleItems")
    private CylinderVariant variant;

    @NotNull(message = "Quantity issued is required.")
    @Min(value = 1, message = "Quantity issued must be at least 1.")
    @Column(nullable = false)
    private Long qtyIssued;

    @NotNull(message = "Empty cylinders received is required.")
    @Min(value = 0, message = "Empty cylinders received cannot be negative.")
    @Column(nullable = false)
    private Long qtyEmptyReceived;

    @NotNull(message = "Base price is required.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be positive.")
    @Column(nullable = false)
    private BigDecimal basePrice;

    @NotNull(message = "Discount is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount cannot be negative.")
    @Column(nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @NotNull(message = "Final price is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Final price cannot be negative.")
    @Column(nullable = false)
    private BigDecimal finalPrice;

    public SaleItem() {
    }

    public SaleItem(Sale sale, Warehouse warehouse, CylinderVariant variant, Long qtyIssued, Long qtyEmptyReceived,
            BigDecimal basePrice, BigDecimal discount, BigDecimal finalPrice) {
        this.sale = Objects.requireNonNull(sale, "Sale cannot be null");
        this.warehouse = Objects.requireNonNull(warehouse, "Warehouse cannot be null");
        this.variant = Objects.requireNonNull(variant, "Variant cannot be null");
        this.qtyIssued = qtyIssued;
        this.qtyEmptyReceived = qtyEmptyReceived;
        this.basePrice = basePrice;
        this.discount = discount;
        this.finalPrice = finalPrice;
    }

    /**
     * Constructor without sale - sale will be set after creation via setSale()
     * Used when creating sale items before the sale is persisted
     */
    public SaleItem(Warehouse warehouse, CylinderVariant variant, Long qtyIssued, Long qtyEmptyReceived,
            BigDecimal basePrice, BigDecimal discount, BigDecimal finalPrice) {
        this.sale = null; // Will be set later via setSale()
        this.warehouse = Objects.requireNonNull(warehouse, "Warehouse cannot be null");
        this.variant = Objects.requireNonNull(variant, "Variant cannot be null");
        this.qtyIssued = qtyIssued;
        this.qtyEmptyReceived = qtyEmptyReceived;
        this.basePrice = basePrice;
        this.discount = discount;
        this.finalPrice = finalPrice;
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

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
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

    public Long getQtyIssued() {
        return qtyIssued;
    }

    public void setQtyIssued(Long qtyIssued) {
        this.qtyIssued = qtyIssued;
    }

    public Long getQtyEmptyReceived() {
        return qtyEmptyReceived;
    }

    public void setQtyEmptyReceived(Long qtyEmptyReceived) {
        this.qtyEmptyReceived = qtyEmptyReceived;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    @Override
    public String toString() {
        return "SaleItem{" +
                "id=" + id +
                ", warehouse=" + (warehouse != null ? warehouse.getName() : "null") +
                ", variant=" + (variant != null ? variant.getName() : "null") +
                ", qtyIssued=" + qtyIssued +
                ", qtyEmptyReceived=" + qtyEmptyReceived +
                ", finalPrice=" + finalPrice +
                ", version=" + version +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SaleItem saleItem = (SaleItem) o;
        return Objects.equals(id, saleItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

