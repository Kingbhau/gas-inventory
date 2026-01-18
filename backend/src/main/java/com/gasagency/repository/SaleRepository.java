package com.gasagency.repository;

import com.gasagency.entity.Sale;
import com.gasagency.entity.Customer;
import com.gasagency.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long>, SaleRepositoryCustom {
        List<Sale> findByCustomer(Customer customer);

        Page<Sale> findByCustomer(Customer customer, Pageable pageable);

        @Query("SELECT s FROM Sale s WHERE s.referenceNumber = :referenceNumber")
        Optional<Sale> findByReferenceNumber(@Param("referenceNumber") String referenceNumber);

        @Query("SELECT COUNT(s) FROM Sale s WHERE s.warehouse = :warehouse " +
                        "AND EXTRACT(MONTH FROM s.createdDate) = EXTRACT(MONTH FROM CAST(:date AS DATE)) " +
                        "AND EXTRACT(YEAR FROM s.createdDate) = EXTRACT(YEAR FROM CAST(:date AS DATE))")
        long countByWarehouseAndCreatedAtMonthYear(@Param("warehouse") Warehouse warehouse,
                        @Param("date") LocalDate date);

}
