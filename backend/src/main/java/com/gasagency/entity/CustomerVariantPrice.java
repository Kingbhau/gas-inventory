package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Entity
@Table(name = "customer_variant_price", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "customer_id", "variant_id" })
}, indexes = {
        @Index(name = "idx_cvp_customer_id", columnList = "customer_id"),
        @Index(name = "idx_cvp_variant_id", columnList = "variant_id"),
        @Index(name = "idx_cvp_customer_variant", columnList = "customer_id,variant_id")
})
public class CustomerVariantPrice extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Customer is required")
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @NotNull(message = "Variant is required")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    private CylinderVariant variant;

    @NotNull(message = "Sale price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Sale price must be positive")
    @Column(nullable = false)
    private BigDecimal salePrice;

    @NotNull(message = "Discount price is required")
    @DecimalMin(value = "0.0", message = "Discount price must be non-negative")
    @Column(nullable = false)
    private BigDecimal discountPrice;

    public CustomerVariantPrice() {
    }

    public CustomerVariantPrice(Customer customer, CylinderVariant variant,
            BigDecimal salePrice, BigDecimal discountPrice) {
        this.customer = customer;
        this.variant = variant;
        this.salePrice = salePrice;
        this.discountPrice = discountPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public CylinderVariant getVariant() {
        return variant;
    }

    public void setVariant(CylinderVariant variant) {
        this.variant = variant;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public BigDecimal getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(BigDecimal discountPrice) {
        this.discountPrice = discountPrice;
    }
}
