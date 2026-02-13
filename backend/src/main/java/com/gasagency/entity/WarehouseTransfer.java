package com.gasagency.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "warehouse_transfer", indexes = {
        @Index(name = "idx_wt_transfer_date", columnList = "transfer_date"),
        @Index(name = "idx_wt_warehouses", columnList = "from_warehouse_id, to_warehouse_id, transfer_date"),
        @Index(name = "idx_transfer_from_warehouse", columnList = "from_warehouse_id"),
        @Index(name = "idx_transfer_to_warehouse", columnList = "to_warehouse_id"),
        @Index(name = "idx_transfer_variant", columnList = "variant_id"),
        @Index(name = "idx_transfer_reference_number", columnList = "reference_number", unique = true)
})
public class WarehouseTransfer extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(name = "reference_number", unique = true, nullable = false, length = 50)
    @Pattern(regexp = "^WT-[A-Z0-9]+-[A-Z0-9]+-\\d{6}-\\d{6}$", message = "Reference must match format: WT-FROM_WH-TO_WH-YYYYMM-SEQUENCE")
    private String referenceNumber;

    @NotNull(message = "From warehouse is required.")
    @ManyToOne
    @JoinColumn(name = "from_warehouse_id", nullable = false)
    @JsonBackReference("warehouse-fromTransfers")
    private Warehouse fromWarehouse;

    @NotNull(message = "To warehouse is required.")
    @ManyToOne
    @JoinColumn(name = "to_warehouse_id", nullable = false)
    @JsonBackReference("warehouse-toTransfers")
    private Warehouse toWarehouse;

    @NotNull(message = "Cylinder variant is required.")
    @ManyToOne
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonBackReference("variant-transfers")
    private CylinderVariant variant;

    @NotNull(message = "Quantity is required.")
    @Min(value = 0, message = "Quantity cannot be negative.")
    @Column(nullable = false)
    private Long quantity;

    @NotNull(message = "Filled quantity is required.")
    @Min(value = 0, message = "Filled quantity cannot be negative.")
    @Column(name = "filled_qty", nullable = false)
    private Long filledQty = 0L;

    @NotNull(message = "Empty quantity is required.")
    @Min(value = 0, message = "Empty quantity cannot be negative.")
    @Column(name = "empty_qty", nullable = false)
    private Long emptyQty = 0L;

    @NotNull(message = "Transfer date is required.")
    @Column(nullable = false)
    private LocalDate transferDate;

    @Column(length = 500)
    private String notes;

    public WarehouseTransfer() {
    }

    public WarehouseTransfer(Warehouse fromWarehouse, Warehouse toWarehouse,
            CylinderVariant variant, Long quantity) {
        this.fromWarehouse = Objects.requireNonNull(fromWarehouse, "From warehouse cannot be null");
        this.toWarehouse = Objects.requireNonNull(toWarehouse, "To warehouse cannot be null");
        this.variant = Objects.requireNonNull(variant, "Variant cannot be null");
        this.quantity = Objects.requireNonNull(quantity, "Quantity cannot be null");
        this.transferDate = LocalDate.now();

        // Validate different warehouses
        if (fromWarehouse.getId().equals(toWarehouse.getId())) {
            throw new IllegalArgumentException("Source and destination warehouses cannot be the same");
        }

        // Validate non-negative quantity
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
    }

    public WarehouseTransfer(Warehouse fromWarehouse, Warehouse toWarehouse,
            CylinderVariant variant, Long filledQty, Long emptyQty) {
        this(fromWarehouse, toWarehouse, variant,
                (filledQty != null ? filledQty : 0L) + (emptyQty != null ? emptyQty : 0L));
        this.filledQty = filledQty != null ? filledQty : 0L;
        this.emptyQty = emptyQty != null ? emptyQty : 0L;
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

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Warehouse getFromWarehouse() {
        return fromWarehouse;
    }

    public void setFromWarehouse(Warehouse fromWarehouse) {
        this.fromWarehouse = fromWarehouse;
    }

    public Warehouse getToWarehouse() {
        return toWarehouse;
    }

    public void setToWarehouse(Warehouse toWarehouse) {
        this.toWarehouse = toWarehouse;
    }

    public CylinderVariant getVariant() {
        return variant;
    }

    public void setVariant(CylinderVariant variant) {
        this.variant = variant;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
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

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "WarehouseTransfer{" +
                "id=" + id +
                ", fromWarehouse=" + (fromWarehouse != null ? fromWarehouse.getName() : "null") +
                ", toWarehouse=" + (toWarehouse != null ? toWarehouse.getName() : "null") +
                ", variant=" + (variant != null ? variant.getName() : "null") +
                ", quantity=" + quantity +
                ", transferDate=" + transferDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WarehouseTransfer that = (WarehouseTransfer) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

