package com.gasagency.repository;

import com.gasagency.entity.SupplierTransaction;
import com.gasagency.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierTransactionRepository extends JpaRepository<SupplierTransaction, Long> {
    List<SupplierTransaction> findBySupplier(Supplier supplier);
}
