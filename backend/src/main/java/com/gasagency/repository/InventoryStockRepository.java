package com.gasagency.repository;

import com.gasagency.entity.InventoryStock;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {

    // Warehouse-aware queries
    Optional<InventoryStock> findByWarehouseAndVariant(Warehouse warehouse, CylinderVariant variant);

    List<InventoryStock> findByWarehouse(Warehouse warehouse);

    Optional<InventoryStock> findByVariant(CylinderVariant variant);

    // Pessimistic lock for concurrent access prevention - warehouse aware
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryStock i WHERE i.warehouse = :warehouse AND i.variant = :variant")
    Optional<InventoryStock> findByWarehouseAndVariantWithLock(@Param("warehouse") Warehouse warehouse,
            @Param("variant") CylinderVariant variant);

    // Pessimistic lock for variant-only query
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryStock i WHERE i.variant = :variant")
    Optional<InventoryStock> findByVariantWithLock(@Param("variant") CylinderVariant variant);

    // Query to check if stock exists for warehouse and variant
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InventoryStock i WHERE i.warehouse = :warehouse AND i.variant = :variant")
    boolean existsByWarehouseAndVariant(@Param("warehouse") Warehouse warehouse,
            @Param("variant") CylinderVariant variant);
}
