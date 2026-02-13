package com.gasagency.repository;

import com.gasagency.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface SaleRepositoryCustom {
    Page<Sale> findFilteredSalesCustom(
            LocalDate from,
            LocalDate to,
            Long customerId,
            Long variantId,
            Double minAmount,
            Double maxAmount,
            String referenceNumber,
            String createdBy,
            Pageable pageable);

    Page<Sale> findByDateRange(LocalDate fromDate, LocalDate toDate, Pageable pageable);
}

