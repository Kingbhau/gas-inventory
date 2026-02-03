package com.gasagency.repository;

import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerCylinderLedgerRepository extends JpaRepository<CustomerCylinderLedger, Long> {
        List<CustomerCylinderLedger> findByCustomer(Customer customer);

        Page<CustomerCylinderLedger> findByCustomer(Customer customer, Pageable pageable);

        /**
         * OPTIMIZED: Fetch customer ledger entries within a date range at database
         * level
         * This prevents loading unnecessary records and reduces memory usage
         */
        @Query("SELECT l FROM CustomerCylinderLedger l " +
                        "WHERE l.customer = :customer " +
                        "AND l.transactionDate >= :fromDate " +
                        "AND l.transactionDate <= :toDate " +
                        "ORDER BY l.transactionDate DESC")
        List<CustomerCylinderLedger> findByCustomerAndDateRange(
                        @Param("customer") Customer customer,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate);

        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.customer = :customer " +
                        "AND l.variant = :variant ORDER BY l.id ASC")
        List<CustomerCylinderLedger> findByCustomerAndVariant(@Param("customer") Customer customer,
                        @Param("variant") CylinderVariant variant);

        // Get latest ledger entry for a customer-variant combination
        @Query(value = "SELECT l FROM CustomerCylinderLedger l WHERE l.customer.id = :customerId " +
                        "AND l.variant.id = :variantId ORDER BY l.id DESC")
        List<CustomerCylinderLedger> findLatestLedger(@Param("customerId") Long customerId,
                        @Param("variantId") Long variantId);

        List<CustomerCylinderLedger> findByVariant(CylinderVariant variant);

        // Get all ledger entries by sale ID
        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.sale.id = :saleId")
        List<CustomerCylinderLedger> findBySaleId(@Param("saleId") Long saleId);

        // Get ledger entry by sale ID and variant ID
        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.sale.id = :saleId AND l.variant.id = :variantId")
        List<CustomerCylinderLedger> findBySaleAndVariant(@Param("saleId") Long saleId,
                        @Param("variantId") Long variantId);

        // Get all ledger entries for a specific warehouse
        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.warehouse.id = :warehouseId ORDER BY l.transactionDate DESC")
        List<CustomerCylinderLedger> findByWarehouseId(@Param("warehouseId") Long warehouseId);

        // Count EMPTY_RETURN entries for a warehouse in a specific month
        @Query("SELECT COUNT(l) FROM CustomerCylinderLedger l WHERE l.warehouse = :warehouse " +
                        "AND l.refType = 'EMPTY_RETURN' " +
                        "AND EXTRACT(MONTH FROM l.createdDate) = EXTRACT(MONTH FROM CAST(:date AS DATE)) " +
                        "AND EXTRACT(YEAR FROM l.createdDate) = EXTRACT(YEAR FROM CAST(:date AS DATE))")
        long countEmptyReturnsByWarehouseAndMonth(@Param("warehouse") Warehouse warehouse,
                        @Param("date") LocalDate date);

        // Get ledger entries for a specific date and reference type (optimized query)
        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.transactionDate = :transactionDate " +
                        "AND l.refType = :refType ORDER BY l.id DESC")
        List<CustomerCylinderLedger> findByTransactionDateAndRefType(
                        @Param("transactionDate") LocalDate transactionDate,
                        @Param("refType") CustomerCylinderLedger.TransactionType refType);

        // Get ledger entries for a specific date and multiple reference types (Day
        // Book)
        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.transactionDate = :transactionDate " +
                        "AND l.refType IN :refTypes ORDER BY l.transactionDate DESC, l.id DESC")
        List<CustomerCylinderLedger> findByTransactionDateAndRefTypeIn(
                        @Param("transactionDate") LocalDate transactionDate,
                        @Param("refTypes") List<CustomerCylinderLedger.TransactionType> refTypes);

        // Get empty return transactions with optional date range and customer/variant filtering
        @Query("SELECT l FROM CustomerCylinderLedger l " +
                        "WHERE l.refType = 'EMPTY_RETURN' " +
                        "AND (:fromDate IS NULL OR l.transactionDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR l.transactionDate <= :toDate) " +
                        "AND (:customerId IS NULL OR l.customer.id = :customerId) " +
                        "AND (:variantId IS NULL OR l.variant.id = :variantId) " +
                        "ORDER BY l.transactionDate DESC")
        Page<CustomerCylinderLedger> findEmptyReturns(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("customerId") Long customerId,
                        @Param("variantId") Long variantId,
                        Pageable pageable);

        // Lock for reading latest balance without duplicates
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.customer.id = :customerId " +
                        "AND l.variant.id = :variantId ORDER BY l.id DESC LIMIT 1")
        Optional<CustomerCylinderLedger> findLatestLedgerWithLock(@Param("customerId") Long customerId,
                        @Param("variantId") Long variantId);

        // Lock for reference validation (prevent duplicate transactions)
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.customer.id = :customerId " +
                        "AND l.variant.id = :variantId AND l.refId = :refId AND l.refType = :refType")
        Optional<CustomerCylinderLedger> findByRefIdWithLock(@Param("customerId") Long customerId,
                        @Param("variantId") Long variantId, @Param("refId") Long refId,
                        @Param("refType") CustomerCylinderLedger.TransactionType refType);

        // Lock for reading all ledger entries for a customer on a variant
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.customer.id = :customerId " +
                        "AND l.variant.id = :variantId ORDER BY l.id DESC")
        List<CustomerCylinderLedger> findByCustomerAndVariantWithLock(@Param("customerId") Long customerId,
                        @Param("variantId") Long variantId);
}
