package com.gasagency.repository;

import com.gasagency.entity.CustomerCylinderLedger;
import com.gasagency.entity.Customer;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CustomerCylinderLedgerRepository extends JpaRepository<CustomerCylinderLedger, Long> {
        List<CustomerCylinderLedger> findByCustomer(Customer customer);

        Page<CustomerCylinderLedger> findByCustomer(Customer customer, Pageable pageable);

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
}
