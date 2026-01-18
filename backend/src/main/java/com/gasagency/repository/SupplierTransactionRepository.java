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
}
