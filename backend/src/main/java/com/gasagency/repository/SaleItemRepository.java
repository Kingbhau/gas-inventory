package com.gasagency.repository;

import com.gasagency.entity.SaleItem;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findBySale(Sale sale);

    List<SaleItem> findByVariant(CylinderVariant variant);
}
