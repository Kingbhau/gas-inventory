package com.gasagency.repository;

import com.gasagency.entity.SalePaymentSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalePaymentSplitRepository extends JpaRepository<SalePaymentSplit, Long> {
    List<SalePaymentSplit> findBySaleId(Long saleId);
}
