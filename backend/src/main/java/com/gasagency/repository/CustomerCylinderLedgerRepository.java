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

import java.math.BigDecimal;
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

        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.warehouse.id = :warehouseId ORDER BY l.transactionDate DESC, l.id DESC")
        Page<CustomerCylinderLedger> findByWarehouseId(@Param("warehouseId") Long warehouseId, Pageable pageable);

        @Query("SELECT l FROM CustomerCylinderLedger l " +
                        "WHERE (:variantId IS NULL OR l.variant.id = :variantId) " +
                        "AND (:refType IS NULL OR l.refType = :refType) " +
                        "ORDER BY l.transactionDate DESC, l.id DESC")
        Page<CustomerCylinderLedger> findMovementsFiltered(
                        @Param("variantId") Long variantId,
                        @Param("refType") CustomerCylinderLedger.TransactionType refType,
                        Pageable pageable);

        @Query("SELECT l FROM CustomerCylinderLedger l " +
                        "WHERE l.warehouse.id = :warehouseId " +
                        "AND (:variantId IS NULL OR l.variant.id = :variantId) " +
                        "AND (:refType IS NULL OR l.refType = :refType) " +
                        "ORDER BY l.transactionDate DESC, l.id DESC")
        Page<CustomerCylinderLedger> findMovementsFilteredByWarehouse(
                        @Param("warehouseId") Long warehouseId,
                        @Param("variantId") Long variantId,
                        @Param("refType") CustomerCylinderLedger.TransactionType refType,
                        Pageable pageable);

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

        @Query("SELECT COALESCE(SUM(l.amountReceived), 0) FROM CustomerCylinderLedger l " +
                        "WHERE l.transactionDate = :transactionDate AND l.refType = :refType")
        BigDecimal sumAmountReceivedByTransactionDateAndRefType(
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
                        "AND (:createdBy IS NULL OR l.createdBy = :createdBy) " +
                        "ORDER BY l.transactionDate DESC")
        Page<CustomerCylinderLedger> findEmptyReturns(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("customerId") Long customerId,
                        @Param("variantId") Long variantId,
                        @Param("createdBy") String createdBy,
                        Pageable pageable);

        // Get payment transactions with optional date range and customer/mode/bank filtering
        @Query("SELECT l FROM CustomerCylinderLedger l " +
                        "WHERE l.refType = 'PAYMENT' " +
                        "AND (:fromDate IS NULL OR l.transactionDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR l.transactionDate <= :toDate) " +
                        "AND (:customerId IS NULL OR l.customer.id = :customerId) " +
                        "AND (:paymentMode IS NULL OR l.paymentMode = :paymentMode) " +
                        "AND (:bankAccountId IS NULL OR l.bankAccount.id = :bankAccountId) " +
                        "AND (:createdBy IS NULL OR l.createdBy = :createdBy) " +
                        "ORDER BY l.transactionDate DESC, l.id DESC")
        Page<CustomerCylinderLedger> findPayments(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("customerId") Long customerId,
                        @Param("paymentMode") String paymentMode,
                        @Param("bankAccountId") Long bankAccountId,
                        @Param("createdBy") String createdBy,
                        Pageable pageable);

        @Query("SELECT COALESCE(SUM(l.amountReceived), 0) FROM CustomerCylinderLedger l " +
                        "WHERE l.refType = 'PAYMENT' " +
                        "AND (:fromDate IS NULL OR l.transactionDate >= :fromDate) " +
                        "AND (:toDate IS NULL OR l.transactionDate <= :toDate) " +
                        "AND (:customerId IS NULL OR l.customer.id = :customerId) " +
                        "AND (:paymentMode IS NULL OR l.paymentMode = :paymentMode) " +
                        "AND (:bankAccountId IS NULL OR l.bankAccount.id = :bankAccountId) " +
                        "AND (:createdBy IS NULL OR l.createdBy = :createdBy)")
        BigDecimal sumPayments(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("customerId") Long customerId,
                        @Param("paymentMode") String paymentMode,
                        @Param("bankAccountId") Long bankAccountId,
                        @Param("createdBy") String createdBy);

        // Lock for reading latest balance without duplicates
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.customer.id = :customerId " +
                        "AND l.variant.id = :variantId ORDER BY l.id DESC")
        List<CustomerCylinderLedger> findLatestLedgerWithLock(@Param("customerId") Long customerId,
                        @Param("variantId") Long variantId, Pageable pageable);

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

        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.id IN " +
                        "(SELECT MAX(l2.id) FROM CustomerCylinderLedger l2 GROUP BY l2.customer.id) " +
                        "AND l.dueAmount IS NOT NULL AND l.dueAmount > 0 ORDER BY l.dueAmount DESC")
        Page<CustomerCylinderLedger> findLatestDuePerCustomer(Pageable pageable);

        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.id IN " +
                        "(SELECT MAX(l2.id) FROM CustomerCylinderLedger l2 GROUP BY l2.customer.id) " +
                        "AND l.dueAmount IS NOT NULL AND l.dueAmount >= :minDueAmount " +
                        "AND l.customer.active = true " +
                        "AND (:search IS NULL OR :search = '' OR " +
                        "LOWER(l.customer.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "l.customer.mobile LIKE CONCAT('%', :search, '%'))")
        Page<CustomerCylinderLedger> findLatestDuePerCustomerWithSearch(
                        @Param("minDueAmount") BigDecimal minDueAmount,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT COALESCE(SUM(l.dueAmount), 0) FROM CustomerCylinderLedger l WHERE l.id IN " +
                        "(SELECT MAX(l2.id) FROM CustomerCylinderLedger l2 GROUP BY l2.customer.id) " +
                        "AND l.dueAmount IS NOT NULL AND l.dueAmount > 0")
        BigDecimal sumLatestDueAmounts();

        @Query("SELECT COUNT(l) FROM CustomerCylinderLedger l WHERE l.id IN " +
                        "(SELECT MAX(l2.id) FROM CustomerCylinderLedger l2 GROUP BY l2.customer.id) " +
                        "AND l.dueAmount IS NOT NULL AND l.dueAmount > 0")
        long countLatestDueCustomers();

        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.id IN " +
                        "(SELECT MAX(l2.id) FROM CustomerCylinderLedger l2 " +
                        "GROUP BY l2.customer.id, l2.variant.id)")
        List<CustomerCylinderLedger> findLatestPerCustomerVariant();

        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.id IN " +
                        "(SELECT MAX(l2.id) FROM CustomerCylinderLedger l2 " +
                        "WHERE l2.customer IN :customers GROUP BY l2.customer.id, l2.variant.id)")
        List<CustomerCylinderLedger> findLatestPerCustomerVariantForCustomers(
                        @Param("customers") List<Customer> customers);

        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.id IN " +
                        "(SELECT MAX(l2.id) FROM CustomerCylinderLedger l2 " +
                        "WHERE l2.customer IN :customers GROUP BY l2.customer.id, l2.variant.id) " +
                        "AND l.balance > 0")
        List<CustomerCylinderLedger> findLatestPositiveBalancesForCustomers(
                        @Param("customers") List<Customer> customers);

        @Query("SELECT l FROM CustomerCylinderLedger l WHERE l.id IN " +
                        "(SELECT MAX(l2.id) FROM CustomerCylinderLedger l2 " +
                        "WHERE l2.customer.id IN :customerIds GROUP BY l2.customer.id)")
        List<CustomerCylinderLedger> findLatestLedgerForCustomerIds(
                        @Param("customerIds") List<Long> customerIds);

        @Query("SELECT l.customer.id, COALESCE(SUM(l.totalAmount), 0), COALESCE(SUM(l.amountReceived), 0), " +
                        "MAX(l.transactionDate), COUNT(l.id) " +
                        "FROM CustomerCylinderLedger l WHERE l.customer.id IN :customerIds GROUP BY l.customer.id")
        List<Object[]> getCustomerLedgerAggregates(@Param("customerIds") List<Long> customerIds);
}
