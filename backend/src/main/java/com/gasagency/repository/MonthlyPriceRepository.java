package com.gasagency.repository;

import com.gasagency.entity.MonthlyPrice;
import com.gasagency.entity.CylinderVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyPriceRepository extends JpaRepository<MonthlyPrice, Long> {
    Optional<MonthlyPrice> findByVariantAndMonthYear(CylinderVariant variant, LocalDate monthYear);

    List<MonthlyPrice> findByVariant(CylinderVariant variant);

    Optional<MonthlyPrice> findTopByVariantAndMonthYearLessThanEqualOrderByMonthYearDesc(CylinderVariant variant,
            LocalDate monthYear);

    boolean existsByVariantAndMonthYearAndIdNot(CylinderVariant variant, LocalDate monthYear, Long id);
}

