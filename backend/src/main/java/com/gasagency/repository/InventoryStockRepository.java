package com.gasagency.repository;

import com.gasagency.entity.InventoryStock;
import com.gasagency.entity.CylinderVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, Long> {
    Optional<InventoryStock> findByVariant(CylinderVariant variant);

    // Pessimistic lock for concurrent access prevention
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryStock i WHERE i.variant = :variant")
    Optional<InventoryStock> findByVariantWithLock(@Param("variant") CylinderVariant variant);
}
