package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

@Entity
@Table(name = "sale_item", indexes = {
        @Index(name = "idx_saleitem_sale_id", columnList = "sale_id"),
        @Index(name = "idx_saleitem_variant_id", columnList = "variant_id")
})
public class SaleItem extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Sale is required.")
    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @NotNull(message = "Variant is required.")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
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

    public SaleItem(Sale sale, CylinderVariant variant, Long qtyIssued, Long qtyEmptyReceived,
            BigDecimal basePrice, BigDecimal discount, BigDecimal finalPrice) {
        this.sale = sale;
        this.variant = variant;
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

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
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
}
