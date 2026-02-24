package com.gasagency.repository;

import com.gasagency.entity.SupplierTransaction;
import com.gasagency.entity.Supplier;
import com.gasagency.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierTransactionRepository extends JpaRepository<SupplierTransaction, Long> {
    List<SupplierTransaction> findBySupplier(Supplier supplier);

    List<SupplierTransaction> findByWarehouse(Warehouse warehouse);

    @Query("SELECT st FROM SupplierTransaction st WHERE st.reference = :referenceNumber")
    Optional<SupplierTransaction> findByReferenceNumber(@Param("referenceNumber") String referenceNumber);

    @Query("SELECT COUNT(st) FROM SupplierTransaction st " +
            "WHERE EXTRACT(MONTH FROM st.createdDate) = EXTRACT(MONTH FROM CAST(:date AS DATE)) " +
            "AND EXTRACT(YEAR FROM st.createdDate) = EXTRACT(YEAR FROM CAST(:date AS DATE))")
    long countByCreatedAtMonthYear(@Param("date") LocalDate date);

    @Query("SELECT st FROM SupplierTransaction st WHERE st.transactionDate = :date ORDER BY st.transactionDate DESC")
    List<SupplierTransaction> findByTransactionDate(@Param("date") LocalDate date);

    @Query("SELECT st FROM SupplierTransaction st WHERE st.transactionDate = :date " +
            "AND (:createdBy IS NULL OR :createdBy = '' OR st.createdBy = :createdBy) " +
            "ORDER BY st.transactionDate DESC")
    List<SupplierTransaction> findByTransactionDateAndCreatedBy(@Param("date") LocalDate date,
            @Param("createdBy") String createdBy);

    @Query("SELECT st FROM SupplierTransaction st " +
            "WHERE (:referenceNumber IS NULL OR :referenceNumber = '' OR LOWER(st.reference) LIKE LOWER(CONCAT('%', :referenceNumber, '%'))) " +
            "AND (:createdBy IS NULL OR :createdBy = '' OR st.createdBy = :createdBy) " +
            "AND (:supplierId IS NULL OR st.supplier.id = :supplierId) " +
            "AND (:warehouseId IS NULL OR st.warehouse.id = :warehouseId) " +
            "AND (:variantId IS NULL OR st.variant.id = :variantId) " +
            "AND (:fromDate IS NULL OR st.transactionDate >= :fromDate) " +
            "AND (:toDate IS NULL OR st.transactionDate <= :toDate) " +
            "AND (:transactionType IS NULL OR st.transactionType = :transactionType)")
    org.springframework.data.domain.Page<SupplierTransaction> findByFilters(
            @Param("referenceNumber") String referenceNumber,
            @Param("createdBy") String createdBy,
            @Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            @Param("variantId") Long variantId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("transactionType") SupplierTransaction.TransactionType transactionType,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COALESCE(SUM(st.filledReceived), 0) FROM SupplierTransaction st " +
            "WHERE st.transactionType = 'BORROW_IN' AND st.supplier.id = :supplierId " +
            "AND st.warehouse.id = :warehouseId AND st.variant.id = :variantId " +
            "AND (:excludeId IS NULL OR st.id <> :excludeId)")
    Long sumBorrowInFilled(@Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            @Param("variantId") Long variantId,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COALESCE(SUM(st.emptyReceived), 0) FROM SupplierTransaction st " +
            "WHERE st.transactionType = 'BORROW_IN' AND st.supplier.id = :supplierId " +
            "AND st.warehouse.id = :warehouseId AND st.variant.id = :variantId " +
            "AND (:excludeId IS NULL OR st.id <> :excludeId)")
    Long sumBorrowInEmpty(@Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            @Param("variantId") Long variantId,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COALESCE(SUM(st.filledSent), 0) FROM SupplierTransaction st " +
            "WHERE st.transactionType = 'BORROW_OUT' AND st.supplier.id = :supplierId " +
            "AND st.warehouse.id = :warehouseId AND st.variant.id = :variantId " +
            "AND (:excludeId IS NULL OR st.id <> :excludeId)")
    Long sumBorrowOutFilled(@Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            @Param("variantId") Long variantId,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COALESCE(SUM(st.emptySent), 0) FROM SupplierTransaction st " +
            "WHERE st.transactionType = 'BORROW_OUT' AND st.supplier.id = :supplierId " +
            "AND st.warehouse.id = :warehouseId AND st.variant.id = :variantId " +
            "AND (:excludeId IS NULL OR st.id <> :excludeId)")
    Long sumBorrowOutEmpty(@Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            @Param("variantId") Long variantId,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COALESCE(SUM(st.filledReceived), 0) FROM SupplierTransaction st " +
            "WHERE st.transactionType = 'PURCHASE' AND st.supplier.id = :supplierId " +
            "AND st.warehouse.id = :warehouseId AND st.variant.id = :variantId " +
            "AND (:excludeId IS NULL OR st.id <> :excludeId)")
    Long sumPurchaseFilled(@Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            @Param("variantId") Long variantId,
            @Param("excludeId") Long excludeId);

    @Query("SELECT COALESCE(SUM(st.filledSent), 0) FROM SupplierTransaction st " +
            "WHERE st.transactionType = 'PURCHASE_RETURN' AND st.supplier.id = :supplierId " +
            "AND st.warehouse.id = :warehouseId AND st.variant.id = :variantId " +
            "AND (:excludeId IS NULL OR st.id <> :excludeId)")
    Long sumPurchaseReturnFilled(@Param("supplierId") Long supplierId,
            @Param("warehouseId") Long warehouseId,
            @Param("variantId") Long variantId,
            @Param("excludeId") Long excludeId);
}

