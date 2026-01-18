package com.gasagency.repository;

import com.gasagency.entity.WarehouseTransfer;
import com.gasagency.entity.Warehouse;
import com.gasagency.entity.CylinderVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseTransferRepository extends JpaRepository<WarehouseTransfer, Long> {

        @Query("SELECT wt FROM WarehouseTransfer wt WHERE wt.fromWarehouse = :warehouse ORDER BY wt.transferDate DESC")
        List<WarehouseTransfer> findTransfersFromWarehouse(@Param("warehouse") Warehouse warehouse);

        @Query("SELECT wt FROM WarehouseTransfer wt WHERE wt.toWarehouse = :warehouse ORDER BY wt.transferDate DESC")
        List<WarehouseTransfer> findTransfersToWarehouse(@Param("warehouse") Warehouse warehouse);

        @Query("SELECT wt FROM WarehouseTransfer wt WHERE wt.fromWarehouse = :fromWarehouse AND wt.toWarehouse = :toWarehouse ORDER BY wt.transferDate DESC")
        List<WarehouseTransfer> findTransfers(@Param("fromWarehouse") Warehouse fromWarehouse,
                        @Param("toWarehouse") Warehouse toWarehouse);

        @Query("SELECT wt FROM WarehouseTransfer wt WHERE wt.variant = :variant ORDER BY wt.transferDate DESC")
        List<WarehouseTransfer> findByVariant(@Param("variant") CylinderVariant variant);

        @Query("SELECT wt FROM WarehouseTransfer wt WHERE wt.transferDate BETWEEN :startDate AND :endDate ORDER BY wt.transferDate DESC")
        List<WarehouseTransfer> findByDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT wt FROM WarehouseTransfer wt WHERE (wt.fromWarehouse = :warehouse OR wt.toWarehouse = :warehouse) ORDER BY wt.transferDate DESC")
        List<WarehouseTransfer> findAllTransfersForWarehouse(@Param("warehouse") Warehouse warehouse);

        @Query("SELECT wt FROM WarehouseTransfer wt WHERE wt.referenceNumber = :referenceNumber")
        Optional<WarehouseTransfer> findByReferenceNumber(@Param("referenceNumber") String referenceNumber);

        @Query("SELECT COUNT(wt) FROM WarehouseTransfer wt " +
                        "WHERE EXTRACT(MONTH FROM wt.transferDate) = EXTRACT(MONTH FROM CAST(:date AS DATE)) " +
                        "AND EXTRACT(YEAR FROM wt.transferDate) = EXTRACT(YEAR FROM CAST(:date AS DATE))")
        long countByCreatedAtMonthYear(@Param("date") LocalDate date);
}
