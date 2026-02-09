package com.gasagency.repository;

import com.gasagency.entity.SaleItem;
import com.gasagency.entity.CylinderVariant;
import com.gasagency.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findBySale(Sale sale);

    List<SaleItem> findByVariant(CylinderVariant variant);

    @Query("SELECT si.variant.name, COALESCE(SUM(si.qtyIssued), 0) FROM SaleItem si " +
                    "WHERE si.sale.saleDate BETWEEN :fromDate AND :toDate " +
                    "GROUP BY si.variant.name")
    List<Object[]> sumQtyByVariantBetween(
                    @Param("fromDate") LocalDate fromDate,
                    @Param("toDate") LocalDate toDate);
}
